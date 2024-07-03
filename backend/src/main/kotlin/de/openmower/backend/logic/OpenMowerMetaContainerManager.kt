package de.openmower.backend.logic

import com.github.dockerjava.api.command.CreateContainerCmd
import com.github.victools.jsonschema.generator.*
import com.github.victools.jsonschema.module.swagger2.Swagger2Module
import io.swagger.v3.oas.annotations.media.Schema

data class MetaContainerSettings(
    @get:Schema(
        title = "Enable Update Check",
        description =
            "Periodically check for updates for the meta " +
                "container and open mower software. Updates will only be " +
                "shown in the user interface, they will not be applied automatically.",
    )
    val checkForUpdates: Boolean,
)

class OpenMowerMetaContainerManager(
    dockerSocketUrl: String,
    defaultImage: String,
    config: ConfigurationService.Config,
) : ContainerManager(dockerSocketUrl, "open-mower-meta", defaultImage, config) {
    /**
     * This class allows us to generate JSON schemas for any Java / Kotlin object.
     * This way we can show the configuration UI easily.
     */
    private val generator: SchemaGenerator =
        SchemaGenerator(
            SchemaGeneratorConfigBuilder(SchemaVersion.DRAFT_2020_12, OptionPreset.PLAIN_JSON).with(Swagger2Module())
                .without(
                    Option.FORBIDDEN_ADDITIONAL_PROPERTIES_BY_DEFAULT,
                    Option.SCHEMA_VERSION_INDICATOR,
                    Option.NULLABLE_FIELDS_BY_DEFAULT,
                ).build(),
        )

    private val settingsSchema = generator.generateSchema(MetaContainerSettings::class.java).toPrettyString().also { println(it) }

    override fun configureContainer(builder: CreateContainerCmd) {
        // No config needed, if the meta container (running this code!) is stopped, it will be restarted externally by systemd.
    }

    override fun start(): Boolean {
        // Cannot actually start the meta-container (it is running this code, so it is already running.)
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

    override fun getAppSettingsJsonSchema(): String {
        return settingsSchema
    }
}
