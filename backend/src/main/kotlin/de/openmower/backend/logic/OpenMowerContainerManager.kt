package de.openmower.backend.logic

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.github.dockerjava.api.command.CreateContainerCmd
import com.github.dockerjava.api.model.Volume
import net.jimblackler.jsonschemafriend.Schema
import net.jimblackler.jsonschemafriend.SchemaStore
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

    /**
     * Fetches the JSON schema for the configuration.
     * We either use a cached value or the default provided with this software (early versions of open mower do not provide this file).
     */
    private var schema =
        run {
            val cachedSchema = config.getConfiguration<String>("json-schema-cache")
            return@run if (cachedSchema.isNullOrBlank()) {
                this.javaClass.getResourceAsStream("/assets/open_mower.default.schema.json")
                    ?.bufferedReader()
                    ?.readText()
                    ?: throw IllegalStateException("Could not load open_mower default json schema")
            } else {
                cachedSchema
            }
        }

    /**
     * Walks through the JSON schema, gets all properties with x-environment-variable set and collects them in the result map.
     * This effectively extracts the settings we want to pass to open mower and flattens the hierarchy.
     */
    private fun buildEnvironment(
        jsonSchema: Schema,
        data: JsonNode?,
        result: MutableMap<String, String>,
    ) {
        // Collect conditionally shown schemas
        jsonSchema.allOf?.forEach { schema ->
            if (schema.then != null) {
                buildEnvironment(schema.then, data, result)
            }
        }
        jsonSchema.properties.forEach { field, prop ->
            if (prop.explicitTypes.contains("object")) {
                // Check, if the provided data has that property. If so, select it and recurse, otherwise we still
                // recurse in order to collect default values
                if (data?.hasNonNull(field) == true) {
                    buildEnvironment(prop, data.get(field), result)
                } else {
                    // recurse without data
                    buildEnvironment(prop, null, result)
                }
            } else {
                val schemaObjectMap = prop.schemaObject as? Map<*, *>
                if (schemaObjectMap?.containsKey("x-environment-variable") == true) {
                    // Found a property with environment attached
                    val variableName = schemaObjectMap["x-environment-variable"]?.toString()
                    val value = data?.get(field)?.textValue() ?: prop.default?.toString()

                    // Check, if we need to remap the value
                    if (schemaObjectMap["x-remap-values"] != null) {
                        val remapMap = schemaObjectMap["x-remap-values"] as? Map<*, *>
                        val remappedValue = remapMap?.get(value)?.toString()
                        println("$variableName $remappedValue")
                        if (variableName != null && remappedValue != null) {
                            result[variableName] = remappedValue
                        }
                    } else {
                        println("$variableName $value")
                        if (variableName != null && value != null) {
                            result[variableName] = value
                        }
                    }
                }
            }
            println(prop)
        }
        if (jsonSchema.parent == null) {
            // we're in the root, look for additional properties in the data
            if (data?.hasNonNull("custom_environment") == true) {
                (data.get("custom_environment") as? ObjectNode)?.fields()?.forEach { (field, node) ->
                    if (field != null && node != null) {
                        result[field] = node.textValue()
                    }
                }
            }
            if (jsonSchema.properties?.containsKey("custom_environment") == true) {
                jsonSchema.properties["custom_environment"].let {
                        schema ->
                    schema?.properties?.forEach { (key, value) ->
                        if (value != null && key != null) {
                            // Add all custom properties
                            result[key] = value.toString()
                        }
                    }
                }
            }
        }
    }

    // A dummy file, since we need to mount some config into the container.
    val tmpFile = File.createTempFile("dummy_config", ".sh")

    override fun configureContainer(builder: CreateContainerCmd) {
        // Get the app settings, build the environment and pass it to the container
        val settings = getAppSettings()
        val environmentMap = mutableMapOf<String, String>()
        val schemaStore = SchemaStore()
        val schema = schemaStore.loadSchemaJson(schema)
        buildEnvironment(schema, settings, environmentMap)

        val environmentKeyValue = environmentMap.map { "${it.key}=${it.value}" }
        propertyCache["environment"] = environmentKeyValue.joinToString("\n")

        // Mount the config
        builder.withEnv(environmentKeyValue)
            // Mount a dummy file, we provide the environment directly
            .withVolumes(
                Volume("${tmpFile.absolutePath}:/config/mower_config.sh"),
            )
    }

    override fun getCustomProperty(key: String): String {
        return when (key) {
            in propertyCache -> propertyCache[key]!!
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
                val str = tarStream.readAllBytes().toString(Charsets.UTF_8).substringAfter("OM_SOFTWARE_VERSION=").trim()
                tarStream.close()
                istr.close()
                tmpFile.delete()
                str
            } catch (e: IOException) {
                "error"
            }
        }
    }

    fun getOpenMowerSettingsSchema(): String? {
        return null
    }

    override fun getAppProperties(): Map<String, String> {
        return propertyCache
    }

    override fun onContainerCreated() {
        propertyCache["om-version"] = getOpenMowerVersion()

        // Refresh the settings JSON schema from the container.
        val updatedSchema = getOpenMowerSettingsSchema()
        if (!updatedSchema.isNullOrBlank()) {
            config.setConfiguration("json-schema-cache", updatedSchema)
        }
    }

    override fun onContainerDestroyed() {
        propertyCache.clear()
    }

    override fun saveAppSettings(json: JsonNode) {
        super.saveAppSettings(json)
        if (containerId != null) {
            // Restart the container to apply changes
            stop()
            start()
        }
    }

    override fun getAppSettingsJsonSchema(): String {
        return schema
    }
}
