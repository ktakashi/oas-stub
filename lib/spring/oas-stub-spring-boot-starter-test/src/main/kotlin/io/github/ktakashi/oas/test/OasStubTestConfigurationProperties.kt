@file:JvmName("OasStubTestConfigurations")
package io.github.ktakashi.oas.test

import com.fasterxml.jackson.databind.ObjectMapper
import io.github.ktakashi.oas.api.http.ResponseContext
import io.github.ktakashi.oas.model.ApiConfiguration
import io.github.ktakashi.oas.model.ApiData
import io.github.ktakashi.oas.model.ApiDefinitions
import io.github.ktakashi.oas.model.ApiHeaders
import io.github.ktakashi.oas.model.PluginDefinition
import io.github.ktakashi.oas.model.PluginType
import io.github.ktakashi.oas.server.api.OasStubApiForwardingResolver
import io.github.ktakashi.oas.server.handlers.OasStubRoutesBuilder
import io.github.ktakashi.oas.server.options.OasStubOptions
import io.github.ktakashi.oas.server.options.OasStubServerSSLOptions
import io.github.ktakashi.oas.storages.apis.PersistentStorage
import io.github.ktakashi.oas.storages.apis.SessionStorage
import java.nio.charset.StandardCharsets
import java.security.KeyStore
import java.util.Base64
import java.util.SortedMap
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.NestedConfigurationProperty
import org.springframework.core.io.ClassPathResource
import org.springframework.core.io.Resource


@ConfigurationProperties(prefix = OasStubTestProperties.OAS_STUB_TEST_PROPERTY_PREFIX)
data class OasStubTestProperties(
    /**
     * OAS API stub server configuration
     */
    @NestedConfigurationProperty var server: OasStubTestServerProperties = OasStubTestServerProperties(),
    /**
     * OAS API stub definition.
     *
     * The [definitions] must be a map of API name and definition.
     * The API name will be used as an API context as well. This
     * means, if your API name is `petstore`, then the actual path
     * of the API stub will be `${oas.stub.servlet.prefix}/petstore`.
     */
    var definitions: Map<String, OasStubTestDefinitionProperties> = mapOf()
) {
    companion object {
        const val OAS_STUB_TEST_PROPERTY_PREFIX = "oas.stub.test"
        const val OAS_STUB_SERVER_PROPERTY_PREFIX = "${OAS_STUB_TEST_PROPERTY_PREFIX}.server"
    }

    fun toOasStubOptions(objectMapper: ObjectMapper,
                         sessionStorage: SessionStorage,
                         persistentStorage: PersistentStorage,
                         oasStubRoutesBuilders: Set<OasStubRoutesBuilder>,
                         oasStubApiForwardingResolvers: Set<OasStubApiForwardingResolver>) = OasStubOptions.builder()
        .stubOptions()
        .stubPath(server.stubPrefix)
        .adminPath(server.adminPrefix)
        .forwardingPath(server.forwardingPrefix)
        .forwardingResolvers(oasStubApiForwardingResolvers.toList())
        .metricsPath(server.metricsPrefix)
        .recordsPath(server.recordsPrefix)
        .enableAdmin(server.enableAdmin)
        .enableMetrics(server.enableMetrics)
        .enableRecords(server.enableRecords)
        .objectMapper(objectMapper)
        .sessionStorage(sessionStorage)
        .persistentStorage(persistentStorage)
        .routesBuilders(oasStubRoutesBuilders)
        .staticConfigurations(server.stubConfigurations)
        .parent()
        .serverOptions()
        .port(server.port)
        .httpsPort(server.httpsPort)
        .enableAccessLog(server.enableAccessLog)
        .ssl().also { ssl ->
            server.ssl?.let {
                it.keystore?.let { ks ->
                    ssl.keyAlias(ks.keyAlias)
                        .keyStore(ks.toKeyStore())
                }
                it.truststore?.let { ks ->
                    ssl.trustStore(ks.toKeyStore())
                }
                ssl.clientAuth(it.clientAuth)
            }
        }.parent()
        .parent()
        .build()
}

data class OasStubTestServerProperties
@JvmOverloads constructor(
    /**
     * OAS Stub server, stub path
     */
    var stubPrefix: String = OasStubOptions.DEFAULT_STUB_PATH,
    /**
     * OAS Stub server, admin path
     */
    var adminPrefix: String = OasStubOptions.DEFAULT_ADMIN_PATH,
    /**
     * OAS Stub server, forwarding path.
     */
    var forwardingPrefix: String = OasStubOptions.DEFAULT_FORWARDING_PATH,
    /**
     * OAS Stub server, metrics path
     *
     * This will be appended to admin path
     */
    var metricsPrefix: String = OasStubOptions.DEFAULT_METRICS_PATH,
    /**
     * OAS Stub server, records path
     *
     * This will be appended to admin path
     */
    var recordsPrefix: String = OasStubOptions.DEFAULT_RECORDS_PATH,
    /**
     * Flag to enable admin endpoints
     */
    var enableAdmin : Boolean = true,
    /**
     * Flag to enable metrics endpoints
     */
    var enableMetrics: Boolean = true,
    /**
     * Flag to enable records endpoints
     */
    var enableRecords: Boolean = true,
    /**
     * Enables access log
     */
    var enableAccessLog: Boolean = false,
    /**
     * HTTP port of the server. 0 means random
     */
    var port: Int = 8080,
    /**
     * HTTPS port of the server. 0 means random
     */
    var httpsPort: Int = -1,
    /**
     * Static stub configuration locations.
     */
    var stubConfigurations: List<String> = listOf(),
    /**
     * Resets stub configuration on each test execution.
     */
    var resetConfigurationAfterEachTest: Boolean = false,
    /**
     * SSL configuration
     */
    @NestedConfigurationProperty var  ssl: OasStubServerSSLProperties? = null
)

data class OasStubServerSSLProperties
@JvmOverloads constructor(
    /**
     * HTTPS keystore
     */
    @NestedConfigurationProperty var keystore: OasStubKeyStoreProperties? = null,
    /**
     * HTTPS truststore
     */
    @NestedConfigurationProperty var truststore: OasStubTrustStoreProperties? = null,
    /**
     * Client authentication type.
     */
    var clientAuth: OasStubServerSSLOptions.ClientAuth = OasStubServerSSLOptions.ClientAuth.NONE,
)

interface OasStubKeyStore {
    /**
     * Key store location.
     *
     * If this is provided, the value is used even if [base64] is provided
     */
    var location: Resource?

    /**
     * Base64 encoded key store
     */
    var base64: String?

    /**
     * Key store password
     */
    var password: String

    /**
     * Key store type. Default `JKS`
     */
    var type: String

    fun toKeyStore(): KeyStore {
        val inputStream = location?.inputStream
            ?: base64?.let { Base64.getMimeDecoder().wrap(it.byteInputStream()) }
            ?: throw IllegalStateException("Either location or base64 must be provided")
        val ks = KeyStore.getInstance(type)
        ks.load(inputStream, password.toCharArray())
        return ks
    }
}

data class OasStubKeyStoreProperties
@JvmOverloads constructor(
    override var location: Resource? = null,
    override var base64: String? = null,
    override var password: String,
    override var type: String = "JKS",
    /**
     * Key alias
     */
    var keyAlias: String,
    /**
     * Key password of the above alias
     */
    var keyPassword: String,
): OasStubKeyStore

data class OasStubTrustStoreProperties
@JvmOverloads constructor(
    override var location: Resource? = null,
    override var base64: String? = null,
    override var password: String,
    override var type: String = "JKS",
): OasStubKeyStore

data class OasStubTestDefinitionProperties
@JvmOverloads constructor(
    /**
     * OAS API specification.
     *
     * The resource location must be understood by Spring framework.
     * e.g. `classpath:/schema/petstore.yaml`
     */
    var specification: Resource,
    /**
     * OAS API configuration.
     *
     * The [configurations] must be a map of API path and configuration.
     * The API path can be either URI template or specialized URI.
     *
     * If URI template is used, then it matches all the requests, otherwise
     * it only matches the specific request.
     */
    var configurations: Map<String, OasStubTestConfigurationProperties> = mapOf(),
    @NestedConfigurationProperty var headers: OasStubTestHeadersProperties = OasStubTestHeadersProperties()
) {
    fun toApiDefinitions() = ApiDefinitions.builder()
        .specification(specification.inputStream.reader().use { it.readText() })
        .configurations(configurations.entries.associate { (k, v) -> k to v.toApiConfiguration() })
        .headers(headers.toApiHeaders())
        .build()
}

data class OasStubTestHeadersProperties
@JvmOverloads constructor(
    /**
     * HTTP request headers.
     *
     * Keys are the header name, and values are the header value.
     */
    var request: SortedMap<String, List<String>> = sortedMapOf(String.CASE_INSENSITIVE_ORDER),
    /**
     * HTTP response headers.
     *
     * Keys are the header name, and values are the header value.
     */
    var response: SortedMap<String, List<String>> = sortedMapOf(String.CASE_INSENSITIVE_ORDER)
) {
    fun toApiHeaders() = ApiHeaders(request = request, response= response)
}

data class OasStubTestConfigurationProperties
@JvmOverloads constructor(
    /**
     * API header configuration.
     */
    @NestedConfigurationProperty var headers: OasStubTestHeadersProperties?,
    /**
     * API plugin configuration
     */
    @NestedConfigurationProperty var plugin: OasStubTestPlugin = OasStubTestPlugin(),
    /**
     * API data.
     *
     * The data is used by plugin. If the stub API uses default plugin.
     * Then the data must be [OasStubTestResources.DefaultResponseModel]
     */
    var data: Map<String, Any> = mapOf()
) {
    fun toApiConfiguration() = ApiConfiguration.builder()
        .headers(headers?.toApiHeaders())
        .plugin(plugin.toPluginDefinition())
        .data(ApiData(data.toMutableMap()))
        .build()
}

class OasStubTestResources {
    companion object {
        /**
         * Default plugin resource
         */
        @JvmField
        val DEFAULT_PLUGIN: Resource = ClassPathResource("/oas/stub/plugins/DefaultResponsePlugin.groovy")

        /**
         * Default plugin content
         */
        @JvmField
        val DEFAULT_PLUGIN_SCRIPT = DEFAULT_PLUGIN.inputStream.reader().use { it.readText() }
    }

    /**
     * The model of API data for the default plugin
     *
     * [status] represents the response HTTP status if provided
     *
     * [headers] are populated into the response if provided
     *
     * [response] replaces the default response if provided
     *
     * This model allows users to programmatically change the
     * default plugin's behaviour.
     */
    data class DefaultResponseModel
    @JvmOverloads constructor(
        /**
         * Http status code
         */
        val status: Int? = null,
        /**
         * Response headers.
         *
         * Key is the header name and values are header values.
         */
        val headers: Map<String, List<String>>? = null,
        /**
         * Response content.
         *
         * The default plugin expects response to be String.
         * If binary data is required, then you may need to write
         * your own plugin.
         */
        val response: String? = null
    ) {
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
    /**
     * Plugin script.
     *
     * It uses default plugin, if not specified
     *
     * The resource location must be understood by Spring framework.
     * e.g. `classpath:/plugins/MyBrilliantPlugin.groovy`
     */
    var script: Resource = OasStubTestResources.DEFAULT_PLUGIN,
    /**
     * Plugin type.
     *
     * At this moment, we only support `GROOVY`
     */
    var type: PluginType = PluginType.GROOVY
) {
    fun toPluginDefinition() = PluginDefinition(script = script.inputStream.reader().use { it.readText() }, type = type)
}