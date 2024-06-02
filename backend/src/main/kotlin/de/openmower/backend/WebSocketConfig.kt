package de.openmower.backend

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Configuration
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.messaging.simp.config.MessageBrokerRegistry
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker
import org.springframework.web.socket.config.annotation.StompEndpointRegistry
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer

@Configuration
@EnableWebSocketMessageBroker
@Order(Ordered.HIGHEST_PRECEDENCE + 99)
class WebSocketConfig(
    @Value("\${de.openmower.backend.deploymentUrl}") private val baseUrl: String,
) : WebSocketMessageBrokerConfigurer {
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
}
