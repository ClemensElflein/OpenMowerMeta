package de.openmower.backend.endpoints

import de.openmower.backend.ContainerStateDTO
import de.openmower.backend.ExecutionStateDTO
import de.openmower.backend.ImageDescriptionDTO
import de.openmower.backend.api.ContainerApiService
import de.openmower.backend.logic.ContainerManager
import de.openmower.backend.logic.ContainerState
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.messaging.simp.annotation.SubscribeMapping
import org.springframework.stereotype.Controller
import org.springframework.stereotype.Service

@Service
class ContainerApiServiceImpl(
    @Qualifier("managedContainerMap") private val managedContainers: Map<String, ContainerManager>,
) : ContainerApiService {
    override fun executeAction(
        id: String,
        action: String,
    ): ContainerStateDTO {
        val manager = managedContainers[id] ?: throw IllegalArgumentException("No container with id $id in managed container")
        when (action) {
            "start" -> manager.start()
            "stop" -> manager.stop()
        }
        return manager.stateObservable.blockingFirst().toDto()
    }

    override fun getImageById(id: String): ImageDescriptionDTO {
        val manager = managedContainers[id] ?: throw IllegalArgumentException("No container with id $id in managed container")
        return ImageDescriptionDTO(manager.configuredImage, manager.configuredImageVersion)
    }

    override fun getState(id: String): ContainerStateDTO {
        val manager = managedContainers[id] ?: throw IllegalArgumentException("No container with id $id in managed container")
        return manager.stateObservable.blockingFirst().toDto()
    }

    override fun updateImageById(
        id: String,
        imageDescriptionDTO: ImageDescriptionDTO,
    ): ImageDescriptionDTO {
        val manager = managedContainers[id] ?: throw IllegalArgumentException("No container with id $id in managed container")
        manager.configuredImage = imageDescriptionDTO.image
        manager.configuredImageVersion = imageDescriptionDTO.imageVersion
        return getImageById(id)
    }
}

private fun ContainerState.toDto() =
    this.let {
        ContainerStateDTO(
            executionState = ExecutionStateDTO.valueOf(it.executionState.id),
            runningImage = it.runningImage,
            runningImageVersion = it.runningImageVersion,
            configuredImage = it.configuredImage,
            configuredImageVersion = it.configuredImageVersion,
        )
    }

@Controller
class ContainerWebSocketController(
    @Qualifier("managedContainerMap") private val managedContainers: Map<String, ContainerManager>,
    private val template: SimpMessagingTemplate,
) {
    init {
        // Subscribe to state changes and publish to WebSocket
        managedContainers.forEach { id, manager ->
            manager.stateObservable.subscribe { state ->
                template.convertAndSend("/topic/$id/state", state)
            }
        }
    }

    @SubscribeMapping("/{id}/state")
    fun onContainerStateSubscribe(id: String): ContainerStateDTO? {
        println("websocket connected to $id")
        val manager = managedContainers[id] ?: throw IllegalArgumentException("No container with id $id in managed container")
        return manager.stateObservable.value?.toDto()
    }
}
//
// @Service
// class OpenMowerContainerLogsClient(private val containerManager: ContainerManager) : WebSocketHandler {
//    private val logger = LoggerFactory.getLogger(this::class.java)
//
//    private val openSockets = ConcurrentHashMap<String, Disposable>()
//
//    override fun afterConnectionEstablished(session: WebSocketSession) {
//        session.sendMessage(TextMessage("Websocket Connected ${session.id}"))
//        println("Websocket Connected ${session.id}")
//        val subscription =
//            containerManager.streamLogs().onErrorComplete().subscribe { logLine ->
//                session.sendMessage(TextMessage(logLine))
//            }
//        openSockets[session.id] = subscription
//    }
//
//    override fun handleMessage(
//        session: WebSocketSession,
//        message: WebSocketMessage<*>,
//    ) {
//    }
//
//    override fun handleTransportError(
//        session: WebSocketSession,
//        exception: Throwable,
//    ) {
//        session.close()
//    }
//
//    override fun afterConnectionClosed(
//        session: WebSocketSession,
//        closeStatus: CloseStatus,
//    ) {
//        println("spring afterConnectionClosed")
//        openSockets[session.id]?.dispose()
//        openSockets.remove(session.id)
//        println("${openSockets.size} sockets open")
//        if (openSockets.isEmpty()) {
//            logger.info("All Log sockets closed. Stopping log streaming.")
//            containerManager.stopLogStream()
//        }
//    }
//
//    override fun supportsPartialMessages(): Boolean {
//        return false
//    }
// }
