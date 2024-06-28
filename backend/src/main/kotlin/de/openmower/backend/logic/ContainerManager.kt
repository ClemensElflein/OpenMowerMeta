package de.openmower.backend.logic

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.JsonNodeFactory
import com.github.dockerjava.api.async.ResultCallback
import com.github.dockerjava.api.command.CreateContainerCmd
import com.github.dockerjava.api.model.Frame
import com.github.dockerjava.api.model.PullResponseItem
import com.github.dockerjava.core.DefaultDockerClientConfig
import com.github.dockerjava.core.DockerClientImpl
import com.github.dockerjava.okhttp.OkDockerHttpClient
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.subjects.BehaviorSubject
import io.reactivex.rxjava3.subjects.ReplaySubject
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import java.io.Closeable
import java.time.Instant
import java.util.concurrent.CountDownLatch

/**
 * This enum class represents the execution states of a container.
 *
 * @property id The id of the state.
 *
 * @constructor Creates an ExecutionState with the given id.
 */
enum class ExecutionState(val id: String) {
    // Additional state, if we don't know the state (yet)
    UNKNOWN("unknown"),

    // Additional state, if there was an error fetching the state
    ERROR("error"),

    // Additional state for when we're pulling the image
    PULLING("pulling"),

    // The "official" docker states
    CREATED("created"),
    EXITED("exited"),
    STARTING("starting"),
    RESTARTING("restarting"),
    RUNNING("running"),
    DEAD("dead"),
    PAUSED("paused"),
    STOPPING("stopping"),
    ;

    companion object {
        fun fromValue(value: String): ExecutionState = entries.firstOrNull { it.id == value } ?: ERROR
    }
}

/**
 * This data class represents the state of a container.
 *
 * @property executionState The execution state of the container.
 * @property runningImage The name of the running image.
 * @property runningImageVersion The version of the running image.
 * @property configuredImage The name of the configured image.
 * @property configuredImageVersion The version of the configured image.
 */
data class ContainerState(
    val executionState: ExecutionState,
    val runningImage: String,
    val runningImageVersion: String,
    val configuredImage: String,
    val configuredImageVersion: String,
)

/**
 * Manages a Docker container.
 * On startup, the ContainerManager will look for a container with the specified name and track that.
 * If no such container exists, it will be created on start automatically.
 *
 * The ContainerManager keeps track of which image to use for a specified container, is able to pull the image and create
 * containers from the specified images.
 *
 * Subclass ContainerManager in order to add custom functionality (e.g. custom settings).
 * You can use the `config` member to store the settings. Don't overwrite `image` or `imageVersion` keys though.
 *
 *
 * @property dockerSocketUrl the URL of the Docker socket
 * @property containerName the name of the managed container
 * @property defaultImage the default image to use when pulling
 * @property config the configuration service for retrieving and setting container configurations
 */
abstract class ContainerManager(
    private val dockerSocketUrl: String,
    private val containerName: String,
    defaultImage: String,
    private val config: ConfigurationService.Config,
) {
    private val defaultImage = defaultImage.substringBefore(":")
    private val defaultImageVersion = defaultImage.substringAfter(":")

    /**
     * Represents the image to be used by this container.
     *
     * @get Retrieves the configuration value for the "image" key. If the value is not found or is of a different type, it returns the default image value.
     * @set Sets the given value as the configuration value for the "image" key.
     */
    var configuredImage: String
        get() = config.getConfiguration("image") { defaultImage }
        set(value) =
            run {
                config.setConfiguration("image", value)
            }

    /**
     * Represents the version of the image used by the container.
     * The version is retrieved from the configuration using the key "image-version".
     * If the key is not found or the value is of a different type,
     * the default value `defaultImageVersion` is used.
     * Setting a new value for `imageVersion` updates the configuration with the new value.
     *
     * Updating this value does not restart the container!
     */
    var configuredImageVersion: String
        get() = config.getConfiguration("image-version") { defaultImageVersion }
        set(value) =
            run {
                config.setConfiguration("image-version", value)
            }

    /**
     * Observable state of this container manager.
     */
    val stateObservable: BehaviorSubject<ContainerState> = BehaviorSubject.create()
    val logSubject: ReplaySubject<Pair<Instant, String>> = ReplaySubject.createWithSize(500)

    private val logger = LoggerFactory.getLogger(this::class.java)
    private val dockerClientConfig =
        DefaultDockerClientConfig.createDefaultConfigBuilder()
            .withDockerHost(dockerSocketUrl)
            .build()

    private val httpClient =
        OkDockerHttpClient.Builder()
            .dockerHost(dockerClientConfig.dockerHost)
            .build()
    protected val dockerClient = DockerClientImpl.getInstance(dockerClientConfig, httpClient)

    /**
     * The ID of the managed container. If this is none, there is no active container.
     */
    protected var containerId: String? = null

    // Callback for the current log stream from docker.
    private var logCallback: ResultCallback<Frame>? = null

    // True, if there are websockets connected and we're generally interested in logs
    private var logStreamingEnabled = false

    init {
        // On startup we look for a container with the given name to track it.
        findContainerHandle()
    }

    /**
     * Set the container state manually, because we have additional states (e.g. PULLING).
     * Also sometime transitions like STOPPING must be set like this
     */
    private fun updateContainerStateManual(executionState: ExecutionState) {
        synchronized(stateObservable) {
            stateObservable.onNext(
                stateObservable.value?.copy(executionState = executionState) ?: ContainerState(
                    executionState,
                    "none", "none", configuredImage, configuredImageVersion,
                ),
            )
        }
    }

    /**
     * This method finds an existing container with the specified name and assigns its ID to containerId,
     * if found. It also updates the container's state.
     */
    private fun findContainerHandle() {
        synchronized(this) {
            // Look for an existing container with name `containerName` and assign containerId, if found.
            logger.info("Looking for container $containerName")
            try {
                containerId =
                    dockerClient.listContainersCmd()
                        .withShowAll(true)
                        .withNameFilter(listOf(containerName))
                        .withLimit(1)
                        .exec()?.firstOrNull()?.id
            } catch (e: Exception) {
                logger.error("Error fetching container list: ${e.message}")
            }
            if (containerId != null) {
                logger.info("Container $containerName found")
            } else {
                logger.info("Did not find container.")
            }
            updateContainerState()
        }
    }

    /**
     * Updates the state of the container by querying the dockerClient.
     *
     * @return The updated ContainerState object.
     */
    private fun updateContainerState(): ContainerState {
        synchronized(this) {
            val id = containerId
            if (id == null) {
                val newState =
                    ContainerState(ExecutionState.EXITED, "none", "none", configuredImage, configuredImageVersion)
                synchronized(stateObservable) {
                    stateObservable.onNext(newState)
                }
                return newState
            }
            try {
                val containerInfo = dockerClient.inspectContainerCmd(id).exec()

                val (runningImage, runningImageVersion) =
                    run {
                        val split = containerInfo.config.image?.split(":")
                        if (split?.size != 2) {
                            listOf("unknown", "unknown")
                        } else {
                            split
                        }
                    }

                val newState =
                    ContainerState(
                        ExecutionState.fromValue(containerInfo.state.status ?: "error"),
                        runningImage,
                        runningImageVersion,
                        configuredImage,
                        configuredImageVersion,
                    )
                synchronized(stateObservable) {
                    stateObservable.onNext(newState)
                }
                return newState
            } catch (e: Exception) {
                logger.error("Error updating container state", e)
                val newState =
                    ContainerState(ExecutionState.ERROR, "none", "none", configuredImage, configuredImageVersion)
                synchronized(stateObservable) {
                    stateObservable.onNext(newState)
                }
                return newState
            }
        }
    }

    /**
     * Pulls the specified image from the Docker registry.
     *
     * @return true if the image is pulled successfully, false otherwise
     */
    open fun pullImage(): Boolean {
        if (containerId != null) {
            stop()
        }
        updateContainerStateManual(ExecutionState.PULLING)
        try {
            logger.info("pulling $configuredImage:$configuredImageVersion")
            var success = false
            val countDownLatch = CountDownLatch(1)
            dockerClient.pullImageCmd("$configuredImage:$configuredImageVersion")
                .exec(
                    object : ResultCallback<PullResponseItem> {
                        private var closeable: Closeable? = null

                        override fun close() {
                            closeable?.close()
                            closeable = null
                            success = false
                            countDownLatch.countDown()
                        }

                        override fun onStart(c: Closeable?) {
                            closeable = c
                            logger.info("Started pulling $configuredImage:$configuredImageVersion")
                        }

                        override fun onError(e: Throwable?) {
                            logger.error("Error during pulling $configuredImage:$configuredImageVersion", e)
                            success = false
                            countDownLatch.countDown()
                        }

                        override fun onComplete() {
                            logger.info("completed pulling image $configuredImage:$configuredImageVersion")
                            success = true
                            countDownLatch.countDown()
                        }

                        override fun onNext(p0: PullResponseItem?) {
                        }
                    },
                )
            countDownLatch.await()
            updateContainerStateManual(if (success) ExecutionState.EXITED else ExecutionState.ERROR)
            return success
        } catch (e: Exception) {
            logger.error("Error pulling image $configuredImage:$configuredImageVersion", e)
            updateContainerStateManual(ExecutionState.ERROR)
            return false
        }
    }

    abstract fun configureContainer(builder: CreateContainerCmd)

    protected fun ensureContainerCreated(): Boolean {
        synchronized(this) {
            // Check, if we already have a container. If so, we're done
            if (containerId != null) {
                return true
            }
            // Check, if we have the image already. If not, pull
            if (dockerClient.listImagesCmd().withImageNameFilter("$configuredImage:$configuredImageVersion")
                    .exec()
                    .isEmpty()
            ) {
                if (!pullImage()) {
                    return false
                }
            }

            // Create the container
            containerId =
                try {
                    dockerClient.createContainerCmd("$configuredImage:$configuredImageVersion")
                        .also { configureContainer(it) }
                        .withName(containerName).exec()?.id
                } catch (e: Exception) {
                    logger.error("Error creating container.", e)
                    updateContainerStateManual(ExecutionState.ERROR)
                    null
                }

            updateContainerState()

            return containerId != null
        }
    }

    /**
     * Starts the container.
     *
     * @return true if the container is started successfully, false otherwise
     */
    open fun start(): Boolean {
        synchronized(this) {
            if (containerId != null) {
                // We already have a container, pull the latest state
                updateContainerState()

                // Check, if container is already running. If so, we're done
                if (stateObservable.value?.executionState == ExecutionState.RUNNING) {
                    return true
                }

                stop()
            }

            // Ensure the image is pulled and the container is created.
            if (!ensureContainerCreated()) {
                return false
            }

            updateContainerStateManual(ExecutionState.STARTING)

            // We have a container, start it.
            return try {
                logger.info("Starting container.")
                // !! is safe here, since we're in synchronized(this) and ensureContainerCreated() returned true.
                dockerClient.startContainerCmd(containerId!!)
                    .exec()
                logger.info("Container started successfully.")
                updateContainerState()
                true
            } catch (e: Exception) {
                logger.error("Error starting container.", e)
                updateContainerStateManual(ExecutionState.ERROR)
                false
            }
        }
    }

    /**
     * Stops the container.
     */
    open fun stop() {
        val id = containerId ?: return
        synchronized(this) {
            // Fetch the current state, since we must not stop a stopped container
            updateContainerState()
            try {
                // Only stop, if actually running. Otherwise just remove.
                if (stateObservable.value?.executionState == ExecutionState.RUNNING) {
                    updateContainerStateManual(ExecutionState.STOPPING)
                    logger.info("Stopping container.")
                    dockerClient.stopContainerCmd(id).exec()
                    logger.info("Waiting for container stop")
                    dockerClient.waitContainerCmd(id).start().awaitCompletion()
                }
                logger.info("Removing container")
                dockerClient.removeContainerCmd(id).withForce(true).exec()
                containerId = null
            } catch (e: Exception) {
                logger.error("Error stopping container.", e)
            }
            updateContainerState()
        }
    }

    fun startLogStream() {
        synchronized(this) {
            // Allow reconnect and tell the timer to connect, even if the container is down.
            logStreamingEnabled = true

            val containerId = containerId ?: return
            if (logCallback != null) {
                // Stream is active, don't do anything
                return
            }

            // no log stream, start it

            // Find a last log, if we've streamed before. This way we continue where we left off
            val lastLog = if (logSubject.hasValue()) logSubject.value else null

            // Either use lastLog or the last 10 minutes of logs
            val since = lastLog?.first ?: Instant.now().minusSeconds(600L)

            logCallback =
                dockerClient.logContainerCmd(containerId)
                    .withFollowStream(true)
                    .withStdOut(true)
                    .withStdErr(true)
                    .withTimestamps(true)
                    .withSince((since.toEpochMilli() / 1000).toInt())
                    .exec(
                        object : ResultCallback<Frame> {
                            private var closeable: Closeable? = null
                            private var lastTime: Instant? = null

                            override fun close() {
                                // Close the actual connection and remove the callback from the Manager.
                                // This way, the reconnect knows that we're not streaming anymore.
                                closeable?.close()
                                closeable = null
                                synchronized(this@ContainerManager) {
                                    logCallback = null
                                }
                            }

                            override fun onStart(closeable: Closeable?) {
                                // Store a reference to the closeable, so that we can close it, if needed.
                                this.closeable = closeable

                                // Check, that the time is newer than the last log in order to avoid duplicates
                                // we can't adjust the "since" parameter, because it only has second accuracy
                                lastTime = if (logSubject.hasValue()) logSubject.value?.first else null
                            }

                            override fun onNext(frame: Frame) {
                                // We have requested timestamps, so parse them.
                                val split =
                                    frame.payload.toString(Charsets.UTF_8).split(' ', ignoreCase = false, limit = 2)
                                if (split.size != 2) {
                                    // If not a success, just use "now"
                                    synchronized(logSubject) {
                                        logSubject.onNext(Instant.now() to split[0])
                                    }
                                } else {
                                    val time =
                                        try {
                                            Instant.parse(split[0])
                                        } catch (e: Exception) {
                                            Instant.now()
                                        }

                                    // accept the log, if we don't have a lastTime, or if the log is newer.
                                    if (lastTime == null || time.isAfter(lastTime)) {
                                        synchronized(logSubject) {
                                            logSubject.onNext(time to split[1])
                                        }
                                    }
                                }
                            }

                            override fun onError(throwable: Throwable) {
                                // Close connection onError
                                close()
                            }

                            override fun onComplete() {
                                // Close connection onComplete
                                close()
                            }
                        },
                    )
        }
    }

    @Scheduled(fixedRate = 10000)
    fun refreshContainerStateAndReconnectLogs() {
        synchronized(this) {
            updateContainerState()
            // Only reconnect, if logging should be enable, the logger is dead and the container is running
            if (logStreamingEnabled && logCallback == null && stateObservable.value?.executionState == ExecutionState.RUNNING) {
                logger.info("reconnecting log stream")
                // We should be streaming logs, but we don't, reconnect logs
                startLogStream()
            }
        }
    }

    fun stopLogStream() {
        synchronized(this) {
            logStreamingEnabled = false
            logCallback?.close()
        }
    }

    fun streamLogs(): Observable<String> {
        startLogStream()
        return logSubject.map { (_, str) -> str }
    }

    /**
     * Can be overwritten by specialized containers to show a settings form to the user.
     */
    open fun getAppSettingsJsonSchema(): String {
        return "{}"
    }

    /**
     * JSON settings. Should be used by the derived classes to configure the container.
     */
    fun saveAppSettings(json: JsonNode) {
        config.setConfiguration("app-config", json)
    }

    /**
     * Read settings, can be used by the derived class to configure the container.
     * Also this is used to present the current settings to the user.
     */
    fun getAppSettings(): JsonNode {
        return config.getConfiguration("app-config") { JsonNodeFactory.instance.objectNode() }
    }

    /**
     * Read custom properties for a given container (e.g. runtime version of the app, if image is a rolling tag)
     */
    abstract fun getCustomProperty(key: String): String
}
