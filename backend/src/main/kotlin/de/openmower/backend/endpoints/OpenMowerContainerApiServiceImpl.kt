package de.openmower.backend.endpoints

import de.openmower.backend.SuccessResponseDTO
import de.openmower.backend.api.OpenMowerContainerApiService
import de.openmower.backend.logic.ContainerState
import de.openmower.backend.logic.OpenMowerContainerManager
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.messaging.simp.annotation.SubscribeMapping
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Controller
import org.springframework.stereotype.Service

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
    init {
        // Subscribe to state changes and publish to WebSocket
        containerManager.stateObservable.subscribe { state ->
            template.convertAndSend("/topic/container/state", state)
        }
    }

    @Scheduled(fixedRate = 1000)
    fun test() {
        println("test")
        template.convertAndSend("/topic/container/state", containerManager.stateObservable.blockingFirst())
    }

    @SubscribeMapping("/container/state")
    fun onContainerStateSubscribe(): ContainerState? {
        println("websocket connected")
        return containerManager.stateObservable.blockingFirst()
    }
}
