package de.openmower.backend.logic

import com.github.dockerjava.api.command.CreateContainerCmd

class OpenMowerMetaContainerManager(
    dockerSocketUrl: String,
    defaultImage: String,
    config: ConfigurationService.Config,
) : ContainerManager(dockerSocketUrl, "open-mower-meta", defaultImage, config) {
    override fun configureContainer(builder: CreateContainerCmd) {
        // No config needed, if the meta container (running this code!) is stopped, it will be restarted externally by systemd.
    }

    override fun start(): Boolean {
        // Cannot actually start the meta-container (it is running this code!)
        return true
    }

    override fun stop() {
        // Don't actually stop, WE ONLY want to stop this container after A SUCCESSFUL UPDATE!
    }

    override fun getCustomProperty(key: String): String {
        return "unknown property"
    }

    override fun getAppProperties(): Map<String, String> {
        return emptyMap()
    }

    override fun pullImage(): Boolean {
        val success = super.pullImage()
        if (success) {
            // This is the only place where we actually stop the container and hope that systemd recreates it using the new image version we pulled before stopping.
            super.stop()
        }
        return success
    }
}
