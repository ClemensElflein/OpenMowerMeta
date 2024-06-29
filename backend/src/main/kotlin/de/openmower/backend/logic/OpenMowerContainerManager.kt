package de.openmower.backend.logic

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.treeToValue
import com.github.dockerjava.api.command.CreateContainerCmd
import com.github.dockerjava.api.model.Volume
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import java.io.File
import java.io.IOException

class OpenMowerContainerManager(
    dockerSocketUrl: String,
    defaultImage: String,
    config: ConfigurationService.Config,
) :
    ContainerManager(dockerSocketUrl, "open-mower", defaultImage, config) {
    private val mapper = jacksonObjectMapper()

    init {
        saveAppSettings(
            mapper.valueToTree<JsonNode>(
                File("/home/clemens/mower_config.sh")
                    .readLines()
                    .mapNotNull { line ->
                        val (key, value) =
                            Regex("export +(.+)=(.+)").matchEntire(line.trim())?.destructured
                                ?: return@mapNotNull null
                        key to value.trim('"')
                    }.toMap(),
            ),
        )
    }

    val tmpFile = File.createTempFile("dummy_config", ".sh")

    override fun configureContainer(builder: CreateContainerCmd) {
        val environment =
            try {
                mapper.treeToValue<Map<String, String>>(getAppSettings())
            } catch (e: Exception) {
                emptyMap()
            }
        // Mount the config
        builder.withEnv(environment.map { "${it.key}=${it.value}" })
            // Mount a dummy file, we provide the environment directly
            .withVolumes(
                Volume("${tmpFile.absolutePath}:/config/mower_config.sh"),
            )
    }

    override fun getCustomProperty(key: String): String {
        return when (key) {
            "om-version" -> getOpenMowerVersion()
            else -> "unknown property"
        }
    }

    /**
     * Retrieves the OpenMower version from the container.
     *
     * @return The OpenMower version as a String. Returns "unknown" if the container is not created or an error occurs.
     */
    fun getOpenMowerVersion(): String {
        synchronized(this) {
            val id = containerId ?: return "unknown"
            return try {
                val tmpFile = File.createTempFile("version_info", ".txt").also { it.deleteOnExit() }
                val istr =
                    dockerClient.copyArchiveFromContainerCmd(id, "/opt/open_mower_ros/version_info.env")
                        .withHostPath(tmpFile.absolutePath)
                        .exec()

                val tarStream = TarArchiveInputStream(istr)
                tarStream.nextEntry
                val str = tarStream.readAllBytes().toString(Charsets.UTF_8).substringAfter("OM_SOFTWARE_VERSION=")
                tarStream.close()
                istr.close()
                tmpFile.delete()
                str
            } catch (e: IOException) {
                "error"
            }
        }
    }
}
