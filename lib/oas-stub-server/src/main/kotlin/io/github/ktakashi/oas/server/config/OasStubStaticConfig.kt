package io.github.ktakashi.oas.server.config

import io.github.ktakashi.oas.model.ApiConfiguration
import io.github.ktakashi.oas.model.ApiDefinitions
import io.github.ktakashi.oas.model.PluginDefinition
import java.io.InputStream
import java.net.URI
import org.yaml.snakeyaml.Yaml
import tools.jackson.databind.JsonNode
import tools.jackson.databind.json.JsonMapper
import tools.jackson.databind.node.ObjectNode
import tools.jackson.module.kotlin.KotlinModule

object OasStubStaticConfigParser {
    internal val jsonMapper = JsonMapper.builder().findAndAddModules().addModule(KotlinModule.Builder().build()).build()
    fun parse(path: URI): Map<String, ApiDefinitions> {
        val node = path.load()
        require (node is ObjectNode) { "Must be a YAML map or JSON object: $path" }
        return node.properties().asSequence().map { (context, path) ->
            val uri = URI.create(path.asString())
            context to jsonMapper.treeToValue(uri.load(), ApiDefinitions::class.java)
        }.map { (context, definition) ->
            context to definition.mutate()
                .also { builder ->
                    definition.specification?.let { spec -> builder.specification(URI.create(spec).readText()) }
                    definition.configurations?.let { config -> builder.configurations(fixupConfiguration(config)) }
                }.build()
        }.toMap()
    }

    private fun fixupConfiguration(config: Map<String, ApiConfiguration>) = config.asSequence().map { (context, config) ->
        config.let { c ->
            context to c.mutate().also { builder ->
                config.plugin?.let { plugin ->
                    builder.plugin(PluginDefinition(plugin.type, URI.create(plugin.script).readText()!!))
                }
            }.build()
        }
    }.toMap()
}

private fun URI.load() = this.path.let { path ->
    when {
        path.endsWith(".yaml") || path.endsWith(".yml") -> this.readYaml()
        path.endsWith(".json") -> this.readJson()
        else -> throw IllegalArgumentException("Unsupported file type: $path")
    }
}

private fun URI.readYaml() = Yaml().let { yaml ->
    val map: Map<String, Any> = this.openStream()?.let(yaml::load) ?: throw IllegalArgumentException("$this doesn't exists")
    OasStubStaticConfigParser.jsonMapper.valueToTree<JsonNode>(map)
}
private fun URI.readJson() = this.openStream()?.let { OasStubStaticConfigParser.jsonMapper.readTree(it) }
    ?: throw IllegalArgumentException("$this doesn't exists")

private fun URI.openStream(): InputStream? = when (this.scheme) {
    "classpath" -> OasStubStaticConfigParser::class.java.getResourceAsStream(this.path)
    else -> this.toURL().openStream()
}

private fun URI.readText(): String? = this.openStream()?.bufferedReader()?.use { it.readText() }