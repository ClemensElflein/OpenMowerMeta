package de.openmower.backend.endpoints

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.treeToValue
import de.openmower.backend.ContainerStateDTO
import de.openmower.backend.ExecutionStateDTO
import de.openmower.backend.GetAppSettingsById200ResponseDTO
import de.openmower.backend.ImageDescriptionDTO
import de.openmower.backend.api.ContainerApiService
import de.openmower.backend.logic.ContainerManager
import de.openmower.backend.logic.ContainerState
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.messaging.handler.annotation.DestinationVariable
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.messaging.simp.annotation.SubscribeMapping
import org.springframework.stereotype.Controller
import org.springframework.stereotype.Service
import java.time.OffsetDateTime

@Service
class ContainerApiServiceImpl(
    @Qualifier("managedContainerMap") private val managedContainers: Map<String, ContainerManager>,
) : ContainerApiService {
    private val mapper = jacksonObjectMapper()

    override fun executeAction(
        id: String,
        action: String,
    ): ContainerStateDTO {
        val manager = managedContainers[id] ?: throw IllegalArgumentException("No container with id $id in managed container")
        when (action) {
            "start" -> manager.start()
            "stop" -> manager.stop()
            "pull" -> manager.pullImage()
        }
        return manager.stateObservable.blockingFirst().toDto()
    }

    override fun getAppSettingsById(id: String): GetAppSettingsById200ResponseDTO {
        val manager = managedContainers[id] ?: throw IllegalArgumentException("No container with id $id in managed container")
        val settings = mapper.treeToValue<Map<String, Any>>(manager.getAppSettings())
        val settingsSchema = manager.getAppSettingsJsonSchema()
        return GetAppSettingsById200ResponseDTO(settings, settingsSchema)
    }

    override fun getCustomProperty(
        id: String,
        key: String,
    ): String {
        val manager = managedContainers[id] ?: throw IllegalArgumentException("No container with id $id in managed container")
        return manager.getCustomProperty(key)
    }

    override fun getImageById(id: String): ImageDescriptionDTO {
        val manager = managedContainers[id] ?: throw IllegalArgumentException("No container with id $id in managed container")
        return ImageDescriptionDTO(manager.configuredImage, manager.configuredImageTag)
    }

    override fun getState(id: String): ContainerStateDTO {
        val manager = managedContainers[id] ?: throw IllegalArgumentException("No container with id $id in managed container")
        return manager.stateObservable.blockingFirst().toDto()
    }

    override fun saveAppSettingsById(
        id: String,
        requestBody: Map<String, Any>,
    ): Map<String, Any> {
        val manager = managedContainers[id] ?: throw IllegalArgumentException("No container with id $id in managed container")
        manager.saveAppSettings(mapper.valueToTree(requestBody))
        return getAppSettingsById(id).value
    }

    override fun updateImageById(
        id: String,
        imageDescriptionDTO: ImageDescriptionDTO,
    ): ImageDescriptionDTO {
        val manager = managedContainers[id] ?: throw IllegalArgumentException("No container with id $id in managed container")
        manager.configuredImage = imageDescriptionDTO.image
        manager.configuredImageTag = imageDescriptionDTO.imageTag
        return getImageById(id)
    }
}

private fun ContainerState.toDto() =
    this.let {
        ContainerStateDTO(
            executionState = ExecutionStateDTO.entries.firstOrNull { v -> v.value == it.executionState.id } ?: ExecutionStateDTO.error,
            runningImage = it.runningImage,
            runningImageTag = it.runningImageTag,
            configuredImage = it.configuredImage,
            configuredImageTag = it.configuredImageTag,
            startedAt = it.startedAt?.let { t -> OffsetDateTime.parse(t) },
            appProperties = it.appProperties,
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
                template.convertAndSend("/topic/container/$id/state", state.toDto())
            }
        }
    }

    @SubscribeMapping("/container/{id}/state")
    fun onContainerStateSubscribe(
        @DestinationVariable id: String,
    ): ContainerStateDTO? {
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
