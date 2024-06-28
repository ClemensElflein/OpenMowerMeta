package de.openmower.backend.logic

import com.fasterxml.jackson.databind.DatabindException
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.io.File
import java.io.IOException
import kotlin.reflect.KClass

/**
 * Represents a service for managing configurations.
 *
 * @property configurationFilePath The path to the configuration file.
 */
@Service
class ConfigurationService(
    @Value("\${de.openmower.backend.configurationFilePath}") private val configurationFilePath: String,
) {
    private val logger = LoggerFactory.getLogger(this::class.java)
    private val configFile =
        File(
            configurationFilePath.replaceFirst(Regex("^~"), System.getProperty("user.home")),
            "config.json",
        ).absoluteFile
    private val mapper = jacksonObjectMapper().registerModule(JavaTimeModule())

    // Keys are namespaces, values are key-value pairs.
    private var configuration: MutableMap<String, MutableMap<String, JsonNode>> = mutableMapOf()

    /**
     * This class represents a configuration for a specific namespace. It provides methods for setting and getting configuration values.
     *
     * @property namespace The namespace for this configuration.
     * @constructor Creates a new Config instance with the given namespace.
     * @param namespace The namespace for this configuration.
     */
    inner class Config(private val namespace: String) {
        constructor(type: KClass<*>) : this(type.simpleName.toString())

        /**
         * Sets a configuration value for the given key.
         *
         * @param key The key of the configuration.
         * @param value The value to set for the configuration.
         * @return `true` if the configuration value was successfully set, `false` otherwise.
         */
        fun setConfiguration(
            key: String,
            value: Any,
        ): Boolean {
            return this@ConfigurationService.setConfiguration(namespace, key, value)
        }

        /**
         * Retrieves the configuration for the specified key and type.
         * Use the convenience functions instead of this in order to avoid casting.
         *
         * @param key the configuration key
         * @param type the type of configuration to retrieve
         * @return the configuration value for the specified key and type, or null if not found
         */
        fun getConfiguration(
            key: String,
            type: KClass<*>,
        ): Any? {
            return this@ConfigurationService.getConfiguration(namespace, key, type)
        }

        /**
         * Retrieves the configuration value for the given key and type.
         *
         * @param key The key of the configuration value.
         * @return The configuration value for the given key and type, or null if it doesn't exist or is of a different type.
         */
        inline fun <reified T> getConfiguration(key: String): T? {
            return getConfiguration(key, T::class) as? T
        }

        /**
         * Retrieves the configuration value for the given key.
         *
         * @param key The key of the configuration value to retrieve.
         * @param default A lambda function that provides a default value if the configuration value is not found or is of a different type.
         * @return The configuration value for the given key, or the default value if not found or of a different type.
         */
        inline fun <reified T> getConfiguration(
            key: String,
            default: () -> T,
        ): T {
            return getConfiguration(key, T::class) as? T ?: default()
        }
    }

    init {
        if (!configFile.exists()) {
            configFile.parentFile.mkdirs()
            configFile.createNewFile()
            // Save empty map initially
            storeConfiguration()
        }
        if (!loadConfiguration()) {
            throw Exception("Error loading configuration file!")
        }
    }

    private fun loadConfiguration(): Boolean {
        synchronized(configuration) {
            try {
                configuration = mapper.readValue(configFile)
                logger.info("Configuration loaded successfully!")
                return true
            } catch (e: DatabindException) {
                logger.error("Error loading configuration file!", e)
                return false
            }
        }
    }

    private fun storeConfiguration(): Boolean {
        synchronized(configuration) {
            try {
                configFile.outputStream().use { stream ->
                    mapper.writerWithDefaultPrettyPrinter().writeValue(stream, configuration)
                }
                logger.info("Successfully stored configuration!")
                return true
            } catch (e: IOException) {
                logger.error("Error storing configuration file!", e)
                return false
            }
        }
    }

    private final fun setConfiguration(
        ns: String,
        key: String,
        value: Any,
    ): Boolean {
        synchronized(configuration) {
            try {
                // Serialize to JSON Node, set in the map and serialize the new config.
                val map = configuration.getOrPut(ns) { mutableMapOf() }
                map[key] = mapper.valueToTree<JsonNode>(value)
            } catch (e: IOException) {
                logger.error("Error storing config file!", e)
            }
            return storeConfiguration()
        }
    }

    private final fun getConfiguration(
        ns: String,
        key: String,
        type: KClass<*>,
    ): Any? {
        synchronized(configuration) {
            val node = configuration[ns]?.get(key) ?: return null
            try {
                return mapper.treeToValue(node, type.java)
            } catch (e: IOException) {
                logger.error("Error deserializing config (key=$key)!", e)
                return null
            }
        }
    }
}
