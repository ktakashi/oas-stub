package io.github.ktakashi.oas.test

import io.github.ktakashi.oas.configuration.AutoOasEngineConfiguration
import io.github.ktakashi.oas.engine.apis.ApiRegistrationService
import io.github.ktakashi.oas.model.ApiConfiguration
import io.github.ktakashi.oas.model.ApiData
import io.github.ktakashi.oas.model.ApiDefinitions
import io.github.ktakashi.oas.model.ApiHeaders
import io.github.ktakashi.oas.model.PluginDefinition
import io.github.ktakashi.oas.model.PluginType
import io.github.ktakashi.oas.plugin.apis.ResponseContext
import java.nio.charset.StandardCharsets
import java.util.*
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.context.properties.NestedConfigurationProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.io.ClassPathResource
import org.springframework.core.io.Resource

internal const val OAS_STUB_TEST_SERVICE_NAME = "oasStubTestService"

@AutoConfiguration(
        after = [AutoOasEngineConfiguration::class]
)
@Configuration
@EnableConfigurationProperties(OasStubTestProperties::class)
class AutoOasStubTestConfiguration(private val properties: OasStubTestProperties) {
    @Bean(OAS_STUB_TEST_SERVICE_NAME)
    @ConditionalOnMissingBean
    fun oasStubTestService(apiRegistrationService: ApiRegistrationService) = OasStubTestService(properties, apiRegistrationService)
}

@ConfigurationProperties(prefix = "oas.stub.test")
data class OasStubTestProperties(var definitions: Map<String, OasStubTestDefinition> = mapOf())

data class OasStubTestDefinition(
        var specification: Resource,
        var configurations: Map<String, OasStubTestConfiguration> = mapOf(),
        @NestedConfigurationProperty var headers: OasStubTestHeaders = OasStubTestHeaders()
) {
    fun toApiDefinitions() = ApiDefinitions(
            specification = specification.inputStream.reader().readText(),
            configurations = configurations.entries.associate { (k, v) -> k to v.toApiConfiguration() },
            headers = headers.toApiHeaders()
    )
}

data class OasStubTestHeaders(
        var request: SortedMap<String, List<String>> = sortedMapOf(String.CASE_INSENSITIVE_ORDER),
        var response: SortedMap<String, List<String>> = sortedMapOf(String.CASE_INSENSITIVE_ORDER)
) {
   fun toApiHeaders() = ApiHeaders(request = request, response= response)
}

data class OasStubTestConfiguration(
        @NestedConfigurationProperty var headers: OasStubTestHeaders = OasStubTestHeaders(),
        @NestedConfigurationProperty var plugin: OasStubTestPlugin = OasStubTestPlugin(),
        var data: Map<String, Any> = mapOf()
) {
    fun toApiConfiguration() = ApiConfiguration(headers = headers.toApiHeaders(), plugin = plugin?.toPluginDefinition(), data = ApiData(data))
}

class OasStubTestResources {
    companion object {
        @JvmField
        val DEFAULT_PLUGIN: Resource = ClassPathResource("/oas/stub/plugins/DefaultResponsePlugin.groovy")
        @JvmField
        val DEFAULT_PLUGIN_SCRIPT = DEFAULT_PLUGIN.inputStream.reader().readText()
    }

    data class DefaultResponseModel
    @JvmOverloads constructor(val status: Int? = null, val headers: Map<String, List<String>>? = null, val response: String? = null) {
        fun toResponseContext(original: ResponseContext): ResponseContext {
            val header = sortedMapOf<String, List<String>>(String.CASE_INSENSITIVE_ORDER)
            header.putAll(original.headers)
            if (headers != null) {
                header.putAll(headers)
            }
            return original.mutate()
                    .status(status ?: original.status)
                    .headers(header)
                    .content(response?.let(this::readResponse) ?: original.content.orElse(null))
                    .build()
        }

        private fun readResponse(response: String): ByteArray {
            if (response.startsWith("classpath:")) {
                val resource = ClassPathResource(response.substring("classpath:".length))
                return resource.inputStream.readAllBytes()
            }
            return response.toByteArray(StandardCharsets.UTF_8)
        }

    }
}


data class OasStubTestPlugin(
        var script: Resource = OasStubTestResources.DEFAULT_PLUGIN,
        var type: PluginType = PluginType.GROOVY
) {
    fun toPluginDefinition() = PluginDefinition(script = script.inputStream.reader().readText(), type = type)
}