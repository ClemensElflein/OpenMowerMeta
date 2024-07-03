package de.openmower.backend

import com.fasterxml.jackson.databind.node.JsonNodeFactory
import com.fasterxml.jackson.databind.node.ObjectNode
import de.openmower.backend.logic.ConfigurationService
import de.openmower.backend.logic.OpenMowerContainerManager
import de.openmower.backend.logic.OpenMowerMetaContainerManager
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.scheduling.annotation.Scheduled
import java.util.concurrent.TimeUnit

@SpringBootApplication
@EnableScheduling
class BackendApplication(
    @Value("\${de.openmower.backend.dockerSocketUrl}") private val dockerSocketUrl: String,
    @Value("\${de.openmower.backend.defaultOpenMowerImage}") private val defaultOpenMowerImage: String,
    @Value("\${de.openmower.backend.defaultOpenMowerMetaImage}") private val defaultOpenMowerMetaImage: String,
    private val configurationService: ConfigurationService,
) {
    private val metaConfigInterface = configurationService.Config("open-mower-meta-container")

    private val managedContainerMap =
        mapOf(
            "open-mower" to
                OpenMowerContainerManager(
                    dockerSocketUrl,
                    defaultOpenMowerImage,
                    configurationService.Config("open-mower-container"),
                ),
            "open-mower-meta" to
                OpenMowerMetaContainerManager(
                    dockerSocketUrl,
                    defaultOpenMowerMetaImage,
                    metaConfigInterface,
                ),
        )

    @Scheduled(fixedRate = 10000)
    fun refreshContainers() {
        managedContainerMap.values.forEach { it.refreshContainerStateAndReconnectLogs() }
    }

    @Scheduled(fixedRate = 6, timeUnit = TimeUnit.HOURS)
    fun updateCheck() {
        val metaSettings: ObjectNode =
            metaConfigInterface.getConfiguration("app-config") { JsonNodeFactory.instance.objectNode() }
        if (metaSettings.hasNonNull("checkForUpdates") && metaSettings.get("checkForUpdates").booleanValue()) {
            // Run the update check
            managedContainerMap.values.forEach {
                it.checkForUpdate()
            }
        }
    }

    @Bean
    fun managedContainerMap() = managedContainerMap
}

fun main(args: Array<String>) {
    runApplication<BackendApplication>(*args)
}
