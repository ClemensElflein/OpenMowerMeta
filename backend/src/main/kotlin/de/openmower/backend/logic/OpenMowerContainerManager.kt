package de.openmower.backend.logic

import com.github.dockerjava.api.model.Volume
import com.github.dockerjava.core.DefaultDockerClientConfig
import com.github.dockerjava.core.DockerClientImpl
import com.github.dockerjava.okhttp.OkDockerHttpClient
import io.reactivex.rxjava3.subjects.BehaviorSubject
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

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

@Service
class OpenMowerContainerManager(
    @Value("\${de.openmower.backend.dockerSocketUrl}") private val dockerSocketUrl: String,
    @Value("\${de.openmower.backend.containerName}") private val containerName: String,
    @Value("\${de.openmower.backend.imageRegistry}") private val imageRegistry: String,
) {
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

    final val stateObservable: BehaviorSubject<ContainerState> = BehaviorSubject.create()

    init {
        findContainerHandle()
        fetchConfiguration()
        stateObservable.subscribe { value ->
            logger.info("New State: $value")
        }
    }

    private fun updateContainerStateManual(executionState: ExecutionState) {
        stateObservable.onNext(stateObservable.value?.copy(executionState = executionState) ?: ContainerState(executionState, "unknown"))
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
                stateObservable.onNext(newState)
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
                stateObservable.onNext(newState)
                return newState
            } catch (e: Exception) {
                logger.error("Error updating container state", e)
                val newState = ContainerState(ExecutionState.ERROR, "unknown")
                stateObservable.onNext(newState)
                return newState
            }
        }
    }

    fun start(): Boolean {
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
                        dockerClient.createContainerCmd("$imageRegistry:latest")
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
}
