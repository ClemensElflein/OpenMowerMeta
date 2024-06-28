package de.openmower.backend.logic

import com.github.dockerjava.api.command.CreateContainerCmd
import com.github.dockerjava.api.model.Volume

class OpenMowerContainerManager(
    dockerSocketUrl: String,
    defaultImage: String,
    config: ConfigurationService.Config,
) :
    ContainerManager(dockerSocketUrl, "open-mower", defaultImage, config) {
    override fun configureContainer(builder: CreateContainerCmd) {
        // Mount the config
        builder.withVolumes(
            Volume(
                "/home/clemens/mower_config.sh:/config/mower_config.sh",
            ),
        )
    }
}
