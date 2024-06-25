package de.openmower.backend.endpoints

import de.openmower.backend.SuccessResponseDTO
import de.openmower.backend.api.OpenMowerContainerApiService
import de.openmower.backend.logic.ContainerState
import de.openmower.backend.logic.OpenMowerContainerManager
import io.reactivex.rxjava3.disposables.Disposable
import org.slf4j.LoggerFactory
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.messaging.simp.annotation.SubscribeMapping
import org.springframework.stereotype.Controller
import org.springframework.stereotype.Service
import org.springframework.web.socket.CloseStatus
import org.springframework.web.socket.TextMessage
import org.springframework.web.socket.WebSocketHandler
import org.springframework.web.socket.WebSocketMessage
import org.springframework.web.socket.WebSocketSession
import java.util.concurrent.ConcurrentHashMap

@Service
class OpenMowerContainerApiServiceImpl(private val containerManager: OpenMowerContainerManager) :
    OpenMowerContainerApiService {
    override fun executeAction(action: String): SuccessResponseDTO {
        when (action) {
            "start" -> containerManager.start()
            "stop" -> containerManager.stop()
        }
        return SuccessResponseDTO("OK")
    }
}

@Controller
class ContainerWebSocketController(
    private val containerManager: OpenMowerContainerManager,
    private val template: SimpMessagingTemplate,
) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    init {
        // Subscribe to state changes and publish to WebSocket
        containerManager.stateObservable.subscribe { state ->
            template.convertAndSend("/topic/container/state", state)
        }
    }

    @SubscribeMapping("/open-mower-container/state")
    fun onContainerStateSubscribe(): ContainerState? {
        println("websocket connected")
        return containerManager.stateObservable.blockingFirst()
    }
}

@Service
class OpenMowerContainerLogsClient(private val containerManager: OpenMowerContainerManager) : WebSocketHandler {
    private val logger = LoggerFactory.getLogger(this::class.java)

    private val openSockets = ConcurrentHashMap<String, Disposable>()

    override fun afterConnectionEstablished(session: WebSocketSession) {
        session.sendMessage(TextMessage("Websocket Connected ${session.id}"))
        println("Websocket Connected ${session.id}")
        val subscription =
            containerManager.streamLogs().onErrorComplete().subscribe {
                    logLine ->
                session.sendMessage(TextMessage(logLine))
            }
        openSockets[session.id] = subscription
    }

    override fun handleMessage(
        session: WebSocketSession,
        message: WebSocketMessage<*>,
    ) {
    }

    override fun handleTransportError(
        session: WebSocketSession,
        exception: Throwable,
    ) {
        session.close()
    }

    override fun afterConnectionClosed(
        session: WebSocketSession,
        closeStatus: CloseStatus,
    ) {
        println("spring afterConnectionClosed")
        openSockets[session.id]?.dispose()
        openSockets.remove(session.id)
        println("${openSockets.size} sockets open")
        if (openSockets.isEmpty()) {
            logger.info("All Log sockets closed. Stopping log streaming.")
            containerManager.stopLogStream()
        }
    }

    override fun supportsPartialMessages(): Boolean {
        return false
    }
}
