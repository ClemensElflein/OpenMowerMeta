package de.openmower.backend

import de.openmower.backend.endpoints.OpenMowerContainerLogsClient
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Configuration
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.messaging.simp.config.MessageBrokerRegistry
import org.springframework.web.socket.config.annotation.*

@Configuration
@EnableWebSocketMessageBroker
@EnableWebSocket
@Order(Ordered.HIGHEST_PRECEDENCE + 99)
class WebSocketConfig(
    @Value("\${de.openmower.backend.deploymentUrl}") private val baseUrl: String,
    private val openMowerContainerLogsClient: OpenMowerContainerLogsClient,
) : WebSocketMessageBrokerConfigurer, WebSocketConfigurer {
    override fun registerStompEndpoints(registry: StompEndpointRegistry) {
        registry.setPreserveReceiveOrder(true)
        registry.addEndpoint("/stomp")
            .setAllowedOrigins(baseUrl)
    }

    override fun configureMessageBroker(config: MessageBrokerRegistry) {
        // Route the /topic to both the application and the message broker.
        // By doing so, the application can be notified upon a subscription and can provide the initial values directly.
        // The broker will then maintain the continuous delivery of updates to the subscriptions.
        config.setApplicationDestinationPrefixes("/topic", "/app")
        config.enableSimpleBroker("/topic", "/queue")
    }

    override fun registerWebSocketHandlers(registry: WebSocketHandlerRegistry) {
        // TODO: Migrate this to STOMP, so that we can have multiple topics for logs instead of one handler for each container.
        registry.addHandler(openMowerContainerLogsClient, "/open-mower-container/logs")
            .setAllowedOrigins(baseUrl)
    }
}
