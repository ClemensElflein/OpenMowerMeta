package de.openmower.backend.logic

import com.github.dockerjava.api.command.CreateContainerCmd

class OpenMowerMetaContainerManager(
    dockerSocketUrl: String,
    defaultImage: String,
    config: ConfigurationService.Config,
) : ContainerManager(dockerSocketUrl, "open-mower-meta", defaultImage, config) {
    override fun configureContainer(builder: CreateContainerCmd) {
        builder.withPortSpecs(listOf("1234:8080"))
    }
}
