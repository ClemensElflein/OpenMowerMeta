package de.openmower.backend.logic

import com.github.dockerjava.api.async.ResultCallback
import com.github.dockerjava.api.model.Frame
import com.github.dockerjava.api.model.Volume
import com.github.dockerjava.core.DefaultDockerClientConfig
import com.github.dockerjava.core.DockerClientImpl
import com.github.dockerjava.okhttp.OkDockerHttpClient
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.subjects.BehaviorSubject
import io.reactivex.rxjava3.subjects.ReplaySubject
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import java.io.Closeable
import java.time.Instant

enum class ExecutionState(val id: String) {
    // Additional state, if we don't know the state (yet)
    UNKNOWN("unknown"),

    // Additional state, if there was an error fetching the state
    ERROR("error"),

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

data class ContainerState(val executionState: ExecutionState, val imageVersion: String)

class ContainerManager(
    private val dockerSocketUrl: String,
    private val containerName: String,
    private val imageRegistry: String,
) {
    /**
     * Observable state of this container manager.
     */
    final val stateObservable: BehaviorSubject<ContainerState> = BehaviorSubject.create()
    final val logSubject: ReplaySubject<Pair<Instant, String>> = ReplaySubject.createWithSize(500)

    private val logger = LoggerFactory.getLogger(this::class.java)
    private val dockerClientConfig =
        DefaultDockerClientConfig.createDefaultConfigBuilder()
            .withDockerHost(dockerSocketUrl)
            .build()

    private val httpClient =
        OkDockerHttpClient.Builder()
            .dockerHost(dockerClientConfig.dockerHost)
            .build()
    private val dockerClient = DockerClientImpl.getInstance(dockerClientConfig, httpClient)

    /**
     * The ID of the managed container. If this is none, there is no active container.
     */
    private var containerId: String? = null

    // Callback for the current log stream from docker.
    private var logCallback: ResultCallback<Frame>? = null

    // True, if there are websockets connected and we're generally interested in logs
    private var logStreamingEnabled = false

    init {
        findContainerHandle()
    }

    private fun updateContainerStateManual(executionState: ExecutionState) {
        synchronized(stateObservable) {
            stateObservable.onNext(
                stateObservable.value?.copy(executionState = executionState) ?: ContainerState(
                    executionState,
                    "unknown",
                ),
            )
        }
    }

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

    private fun updateContainerState(): ContainerState {
        synchronized(this) {
            val id = containerId
            if (id == null) {
                val newState = ContainerState(ExecutionState.EXITED, "unknown")
                synchronized(stateObservable) {
                    stateObservable.onNext(newState)
                }
                return newState
            }
            try {
                val containerInfo = dockerClient.inspectContainerCmd(id).exec()
                val imageVersion = containerInfo.config.image

                val newState =
                    ContainerState(
                        ExecutionState.fromValue(containerInfo.state.status ?: "error"),
                        imageVersion ?: "unknown",
                    )
                synchronized(stateObservable) {
                    stateObservable.onNext(newState)
                }
                return newState
            } catch (e: Exception) {
                logger.error("Error updating container state", e)
                val newState = ContainerState(ExecutionState.ERROR, "unknown")
                synchronized(stateObservable) {
                    stateObservable.onNext(newState)
                }
                return newState
            }
        }
    }

    fun start(imageVersion: String): Boolean {
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

            updateContainerStateManual(ExecutionState.STARTING)

            // Either we stopped the container, or it was already stopped
            containerId =
                try {
                    val id =
                        dockerClient.createContainerCmd("$imageRegistry:$imageVersion")
                            .withVolumes(
                                Volume(
                                    "/home/clemens/mower_config.sh:/config/mower_config.sh",
                                ),
                            )
                            .withName(containerName).exec()?.id
                    if (id != null) {
                        logger.info("Container created successfully. Starting container.")
                        dockerClient.startContainerCmd(id)
                            .exec()
                        logger.info("Container started successfully.")
                    }
                    id
                } catch (e: Exception) {
                    logger.error("Error starting container.", e)
                    null
                }

            updateContainerState()

            return containerId != null
        }
    }

    fun stop() {
        val id = containerId ?: return
        updateContainerStateManual(ExecutionState.STOPPING)
        synchronized(this) {
            try {
                logger.info("Stopping container.")
                dockerClient.stopContainerCmd(id).exec()
                logger.info("Waiting for container stop")
                dockerClient.waitContainerCmd(id).start().awaitCompletion()
                logger.info("Removing container")
                dockerClient.removeContainerCmd(id).withForce(true).exec()
                containerId = null
            } catch (e: Exception) {
                logger.error("Error stopping container.", e)
            }
            updateContainerState()
        }
    }

    fun fetchConfiguration() {
        val istr =
            dockerClient.copyArchiveFromContainerCmd(containerId!!, "/opt/open_mower_ros/version_info.env")
                .withHostPath("/tmp/version_info.env")
                .exec()
        val tarStream = TarArchiveInputStream(istr)
        tarStream.nextEntry
        val str = tarStream.readAllBytes().toString(Charsets.UTF_8)
        println(str)
        tarStream.close()
        istr.close()
    }

    fun startLogStream() {
        synchronized(this) {
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
            // Allow reconnect.
            logStreamingEnabled = true
        }
    }

    @Scheduled(fixedRate = 10000)
    private fun refreshContainerStateAndReconnectLogs() {
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
}
