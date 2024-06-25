package de.openmower.backend

import de.openmower.backend.logic.ContainerManager
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication
@EnableScheduling
class BackendApplication(
    @Value("\${de.openmower.backend.dockerSocketUrl}") private val dockerSocketUrl: String,
    @Value("\${de.openmower.backend.imageRegistry}") private val imageRegistry: String
) {
    @Bean
    fun openMowerContainerManager() = ContainerManager(dockerSocketUrl, "open-mower", imageRegistry)
}

fun main(args: Array<String>) {
    runApplication<BackendApplication>(*args)
}
