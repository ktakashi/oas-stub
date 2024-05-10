package io.github.ktakashi.oas.server.handlers

import io.github.ktakashi.oas.engine.apis.ApiRegistrationService
import io.github.ktakashi.oas.model.ApiConfiguration
import io.github.ktakashi.oas.model.ApiDefinitions
import io.github.ktakashi.oas.model.PluginDefinition
import io.github.ktakashi.oas.model.PluginType
import io.github.ktakashi.oas.server.http.RouterHttpMethod
import io.github.ktakashi.oas.server.http.RouterHttpRequest
import io.github.ktakashi.oas.server.http.RouterHttpResponse
import io.github.ktakashi.oas.server.options.OasStubStubOptions
import io.netty.buffer.ByteBufAllocator
import io.netty.handler.codec.http.HttpHeaderNames
import io.netty.handler.codec.http.HttpHeaderValues
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import reactor.core.publisher.Mono


internal const val PATH_VARIABLE_NAME = "context"
internal const val PATH_SEGMENT = "{$PATH_VARIABLE_NAME}"
private const val PARAMETER_NAME = "api"

class OasStubAdminRoutesBuilder(private val options: OasStubStubOptions): OasStubRoutesBuilder, KoinComponent {
    private val apiRegistrationService: ApiRegistrationService by inject()

    override fun build(routes: OasStubRoutes) {
        if (options.enableAdmin) {
            routes {
                get(options.adminPath) { request ->
                    apiRegistrationService.getAllNames().map { ByteBufAllocator.DEFAULT.buffer().writeBytes(it.toByteArray()) }
                        .collectList()
                        .map { names ->
                            request.responseBuilder()
                                .ok()
                                .header(HttpHeaderNames.CONTENT_TYPE.toString(), HttpHeaderValues.APPLICATION_JSON.toString())
                                .body(names)
                        }
                }
                path("${options.adminPath}/$PATH_SEGMENT", ::adminContext)
                path("${options.adminPath}/$PATH_SEGMENT/options", adminContextApi(ApiDefinitions::options, ApiDefinitions::updateOptions))
                path("${options.adminPath}/$PATH_SEGMENT/configurations", adminContextApi(ApiDefinitions::configurations, ApiDefinitions::updateConfigurations))
                path("${options.adminPath}/$PATH_SEGMENT/headers", adminContextApi(ApiDefinitions::headers, ApiDefinitions::updateHeaders))
                path("${options.adminPath}/$PATH_SEGMENT/data", adminContextApi(ApiDefinitions::data, ApiDefinitions::updateData))
                path("${options.adminPath}/$PATH_SEGMENT/delay", adminContextApi(ApiDefinitions::delay, ApiDefinitions::updateDelay))

                path("${options.adminPath}/$PATH_SEGMENT/configurations/options", adminConfigurationApi(ApiConfiguration::options, ApiConfiguration::updateOptions))
                path("${options.adminPath}/$PATH_SEGMENT/configurations/headers", adminConfigurationApi(ApiConfiguration::headers, ApiConfiguration::updateHeaders))
                path("${options.adminPath}/$PATH_SEGMENT/configurations/data", adminConfigurationApi(ApiConfiguration::data, ApiConfiguration::updateData))
                path("${options.adminPath}/$PATH_SEGMENT/configurations/delay", adminConfigurationApi(ApiConfiguration::delay, ApiConfiguration::updateDelay))
                get("${options.adminPath}/$PATH_SEGMENT/configurations/plugins") { request ->
                    getConfigurationProperty(request, ApiConfiguration::plugin)
                }
                delete("${options.adminPath}/$PATH_SEGMENT/configurations/plugins") { request ->
                    deleteConfigurationProperty( request, ApiConfiguration::updatePlugin)
                }
                put("${options.adminPath}/$PATH_SEGMENT/configurations/plugins/groovy") { request ->
                    request.bodyToInputStream().flatMap { body ->
                        updateConfigurationProperty(request) { configuration ->
                            PluginDefinition(type = PluginType.GROOVY, script = body.reader().use { it.readText() }).let {
                                configuration.updatePlugin(it)
                            }
                        }.map { v -> request.responseBuilder().ok()
                            .header(HttpHeaderNames.CONTENT_TYPE.toString(), HttpHeaderValues.APPLICATION_JSON.toString())
                            .body(v)
                        }
                    }.switchIfEmpty(Mono.defer { notFound(request) })
                }
            }
        }
    }

    private fun adminContext(request: RouterHttpRequest): Mono<RouterHttpResponse>  = when (request.method) {
        "GET" -> request.param(PATH_VARIABLE_NAME)?.let { context ->
            apiRegistrationService.getApiDefinitions(context).map { v ->
                request.responseBuilder()
                    .ok()
                    .header(HttpHeaderNames.CONTENT_TYPE.toString(), HttpHeaderValues.APPLICATION_JSON.toString())
                    .body(v)
            }.switchIfEmpty(Mono.defer { notFound(request) })
        } ?: notFound(request)
        "POST" -> request.param(PATH_VARIABLE_NAME)?.let { context ->
            request.getHeader(HttpHeaderNames.CONTENT_TYPE.toString())?.let { ct ->
                if (ct.startsWith("application/octet-stream") || ct.startsWith("text/plain")) {
                    request.bodyToInputStream()
                        .flatMap { body -> apiRegistrationService.saveApiDefinitions(context, ApiDefinitions(body.reader().use { it.readText() }) ) }
                        .map { request.responseBuilder().created("/$context").build() }
                        .switchIfEmpty(Mono.defer { notFound(request) })
                } else if (ct.startsWith("application/json")) {
                    request.bodyToMono(ApiDefinitions::class.java)
                        .flatMap { body -> apiRegistrationService.saveApiDefinitions(context, body) }
                        .map { request.responseBuilder().created("/$context").build() }
                        .switchIfEmpty(Mono.defer { notFound(request) })
                } else {
                    notFound(request)
                }
            }
        } ?: notFound(request)
        "PUT" -> request.param(PATH_VARIABLE_NAME)?.let { context ->
            request.bodyToMono(ApiDefinitions::class.java)
                .flatMap { apiRegistrationService.saveApiDefinitions(context, it) }
                .map { request.responseBuilder().created("/$context").build() }
                .switchIfEmpty(Mono.defer { notFound(request) })
        } ?: notFound(request)
        "DELETE" -> request.param(PATH_VARIABLE_NAME)?.let { context ->
            apiRegistrationService.deleteApiDefinitions(context)
                .map { request.responseBuilder().noContent().build() }
                .switchIfEmpty(Mono.defer { notFound(request) })
        } ?: notFound(request)
        else -> methodNotAllowed(request)
    }

    private fun methodNotAllowed(request: RouterHttpRequest) =
        Mono.just(request.responseBuilder().methodNotAllowed().build())

    private fun notFound(request: RouterHttpRequest) =
        Mono.just(request.responseBuilder().notFound().build())

    private inline fun <reified T> adminContextApi(noinline retriever: (ApiDefinitions) -> T?, noinline updater: (ApiDefinitions, T?) -> ApiDefinitions) = OasStubRouteHandler { request ->
        when (request.method) {
            "GET" -> getApiDefinitionsProperty(request, retriever)
            "PUT" -> putApiDefinitionsProperty(request, updater)
            "DELETE" -> deleteApiDefinitionsProperty(request, updater)
            else -> methodNotAllowed(request)
        }
    }

    private inline fun <reified T> adminConfigurationApi(noinline retriever: (ApiConfiguration) -> T?, noinline updater: (ApiConfiguration, T?) -> ApiConfiguration) = OasStubRouteHandler { request ->
        if (request.queryParameters.containsKey(PARAMETER_NAME)) {
            when (request.method) {
                "GET" -> getConfigurationProperty(request, retriever)
                "PUT" -> putConfigurationProperty(request, updater)
                "DELETE" -> deleteConfigurationProperty(request, updater)
                else -> methodNotAllowed(request)
            }
        } else {
            notFound(request)
        }
    }

    private inline fun <reified T> putConfigurationProperty(request: RouterHttpRequest, noinline updater: (ApiConfiguration, T) -> ApiConfiguration) =
        request.bodyToMono(T::class.java).flatMap { body ->
            updateConfigurationProperty(request) { config -> updater(config, body) }
                .map { v -> request.responseBuilder().ok()
                    .header(HttpHeaderNames.CONTENT_TYPE.toString(), HttpHeaderValues.APPLICATION_JSON.toString())
                    .body(v) }
        }.switchIfEmpty(Mono.defer { notFound(request) })

    private fun <T> deleteConfigurationProperty(request: RouterHttpRequest, updater: (ApiConfiguration, T?) -> ApiConfiguration) =
        updateConfigurationProperty(request) { config -> updater(config, null) }
            .map { request.responseBuilder().noContent().build() }
            .switchIfEmpty(Mono.defer { notFound(request) })

    private fun updateConfigurationProperty(request: RouterHttpRequest, updater: (ApiConfiguration) -> ApiConfiguration) =
        request.param(PATH_VARIABLE_NAME)?.let { context ->
            apiRegistrationService.getApiDefinitions(context).mapNotNull { def ->
                request.queryParameters[PARAMETER_NAME]?.get(0)?.let {
                    apiRegistrationService.validPath(def, URLDecoder.decode(it, StandardCharsets.UTF_8)).map { api ->
                        val newDef = if (def.configurations == null) def.updateConfigurations(mapOf()) else def
                        updater(newDef.configurations?.get(api) ?: ApiConfiguration()).let { config ->
                            newDef.updateConfiguration(api, config)
                        }
                    }
                }
            }.flatMap { it }
            .flatMap { def -> apiRegistrationService.saveApiDefinitions(context, def) }
        } ?: Mono.empty()

    private fun <T> getConfigurationProperty(request: RouterHttpRequest, retriever: (ApiConfiguration) -> T?) =
        request.param(PATH_VARIABLE_NAME)?.let { context ->
            apiRegistrationService.getApiDefinitions(context).mapNotNull { def ->
                request.queryParameters[PARAMETER_NAME]?.get(0)?.let {
                    val api = URLDecoder.decode(it, StandardCharsets.UTF_8)
                    def.configurations?.let { v -> v[api]?.let(retriever) }
                }
            }.map { v -> request.responseBuilder().ok()
                .header(HttpHeaderNames.CONTENT_TYPE.toString(), HttpHeaderValues.APPLICATION_JSON.toString())
                .body(v)
            }.switchIfEmpty(Mono.defer { notFound(request) })
        } ?: notFound(request)

    private inline fun <reified T> putApiDefinitionsProperty(request: RouterHttpRequest, noinline updater: (ApiDefinitions, T?) -> ApiDefinitions) =
        request.bodyToMono(T::class.java).flatMap { body ->
            updateApiDefinitionProperty(request) { def -> updater(def, body) }
                .map { v -> request.responseBuilder()
                    .ok()
                    .header(HttpHeaderNames.CONTENT_TYPE.toString(), HttpHeaderValues.APPLICATION_JSON.toString())
                    .body(v) }
        }.switchIfEmpty(Mono.defer { notFound(request) })

    private inline fun <reified T> deleteApiDefinitionsProperty(request: RouterHttpRequest, noinline updater: (ApiDefinitions, T?) -> ApiDefinitions) =
        request.bodyToMono(T::class.java).flatMap {
            updateApiDefinitionProperty(request) { def -> updater(def, null) }
                .map { request.responseBuilder().noContent().build() }
        }.switchIfEmpty(Mono.defer {  notFound(request) })

    private fun updateApiDefinitionProperty(request: RouterHttpRequest, updater: (ApiDefinitions) -> ApiDefinitions) =
        request.param(PATH_VARIABLE_NAME)?.let { context ->
            apiRegistrationService.getApiDefinitions(context)
                .map(updater)
                .flatMap { apiRegistrationService.saveApiDefinitions(context, it) }
        } ?: Mono.empty()

    private fun <T> getApiDefinitionsProperty(request: RouterHttpRequest, retriever: (ApiDefinitions) -> T?) =
        request.param(PATH_VARIABLE_NAME)?.let { context ->
            apiRegistrationService.getApiDefinitions(context)
                .mapNotNull(retriever)
                .map { request.responseBuilder().ok()
                    .header(HttpHeaderNames.CONTENT_TYPE.toString(), HttpHeaderValues.APPLICATION_JSON.toString())
                    .body(it)
                }.switchIfEmpty(Mono.defer { notFound(request) })
        } ?:  notFound(request)
}
