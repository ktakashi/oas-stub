package io.github.ktakashi.oas.server.handlers

import com.fasterxml.jackson.databind.ObjectMapper
import io.github.ktakashi.oas.engine.apis.ApiRegistrationService
import io.github.ktakashi.oas.model.ApiConfiguration
import io.github.ktakashi.oas.model.ApiDefinitions
import io.github.ktakashi.oas.model.PluginDefinition
import io.github.ktakashi.oas.model.PluginType
import io.github.ktakashi.oas.server.io.bodyToMono
import io.github.ktakashi.oas.server.options.OasStubServerOptions
import io.netty.handler.codec.http.HttpHeaderNames
import io.netty.handler.codec.http.HttpHeaderValues
import io.netty.handler.codec.http.HttpMethod
import io.netty.handler.codec.http.HttpResponseStatus
import io.netty.handler.codec.http.QueryStringDecoder
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.reactivestreams.Publisher
import reactor.core.publisher.Mono
import reactor.netty.http.server.HttpServerRequest
import reactor.netty.http.server.HttpServerResponse
import reactor.netty.http.server.HttpServerRoutes


private typealias AdminApiHandler = (HttpMethod, HttpServerRequest, HttpServerResponse) -> Publisher<Void>

internal const val PATH_VARIABLE_NAME = "context"
internal const val PATH_SEGMENT = "{$PATH_VARIABLE_NAME}"
private const val PARAMETER_NAME = "api"

class OasStubAdminRoutesBuilder(private val options: OasStubServerOptions): KoinComponent {
    private val apiRegistrationService: ApiRegistrationService by inject()
    private val objectMapper: ObjectMapper by inject()
    fun build(routes: HttpServerRoutes) {
        if (options.enableAdmin) {
            routes
                .get(options.adminPath) { _, response ->
                    response.header(HttpHeaderNames.CONTENT_TYPE, HttpHeaderValues.APPLICATION_JSON)
                        .sendObject(apiRegistrationService.getAllNames())
                }
                .path("${options.adminPath}/$PATH_SEGMENT", ::adminContext)
                .path("${options.adminPath}/$PATH_SEGMENT/options", adminContextApi(ApiDefinitions::options, ApiDefinitions::updateOptions))
                .path("${options.adminPath}/$PATH_SEGMENT/configurations", adminContextApi(ApiDefinitions::configurations, ApiDefinitions::updateConfigurations))
                .path("${options.adminPath}/$PATH_SEGMENT/headers", adminContextApi(ApiDefinitions::headers, ApiDefinitions::updateHeaders))
                .path("${options.adminPath}/$PATH_SEGMENT/data", adminContextApi(ApiDefinitions::data, ApiDefinitions::updateData))
                .path("${options.adminPath}/$PATH_SEGMENT/delay", adminContextApi(ApiDefinitions::delay, ApiDefinitions::updateDelay))

                .path("${options.adminPath}/$PATH_SEGMENT/configurations/options", adminConfigurationApi(ApiConfiguration::options, ApiConfiguration::updateOptions))
                .path("${options.adminPath}/$PATH_SEGMENT/configurations/headers", adminConfigurationApi(ApiConfiguration::headers, ApiConfiguration::updateHeaders))
                .path("${options.adminPath}/$PATH_SEGMENT/configurations/data", adminConfigurationApi(ApiConfiguration::data, ApiConfiguration::updateData))
                .path("${options.adminPath}/$PATH_SEGMENT/configurations/delay", adminConfigurationApi(ApiConfiguration::delay, ApiConfiguration::updateDelay))
                .get("${options.adminPath}/$PATH_SEGMENT/configurations/plugins") { request, response ->
                    getConfigurationProperty(request, response, request.queryParameters(), ApiConfiguration::plugin)
                }
                .delete("${options.adminPath}/$PATH_SEGMENT/configurations/plugins") { request, response ->
                    deleteConfigurationProperty( request, response, request.queryParameters(), ApiConfiguration::updatePlugin)
                }
                .put("${options.adminPath}/$PATH_SEGMENT/configurations/plugins/groovy") { request, response ->
                    putConfigurationProperty(request, response, request.queryParameters()) { configuration, script: String ->
                        PluginDefinition(type = PluginType.GROOVY, script = script).let {
                            configuration.updatePlugin(it)
                        }
                    }
                }
        }
    }

    private fun adminContext(method: HttpMethod, request: HttpServerRequest, response: HttpServerResponse): Publisher<Void>  = when (method) {
        HttpMethod.GET -> request.param(PATH_VARIABLE_NAME)?.let { context ->
            apiRegistrationService.getApiDefinitions(context).map { v ->
                response.header(HttpHeaderNames.CONTENT_TYPE, HttpHeaderValues.APPLICATION_JSON)
                    .status(HttpResponseStatus.OK)
                    .sendObject(v)
            }.switchIfEmpty(Mono.defer { Mono.just(notFound(response)) })
                .flatMap { r -> r.then() }
        } ?: sendNotFound(response)
        HttpMethod.POST -> request.param(PATH_VARIABLE_NAME)?.let { context ->
            val ct = request.requestHeaders()[HttpHeaderNames.CONTENT_TYPE]
            if (ct != null && (ct.startsWith("application/octet-stream") || ct.startsWith("text/plain") )) {
                request.bodyToMono<ApiDefinitions>(objectMapper)
                    .flatMap { body -> apiRegistrationService.saveApiDefinitions(context, body) }
                    .map { response.status(HttpResponseStatus.CREATED).header(HttpHeaderNames.LOCATION, "/$context") }
                    .switchIfEmpty(Mono.defer { Mono.just(notFound(response)) })
                    .flatMap { r -> r.send() }
            } else {
                sendNotFound(response)
            }
        } ?: sendNotFound(response)
        HttpMethod.PUT -> request.param(PATH_VARIABLE_NAME)?.let { context ->
            request.bodyToMono<ApiDefinitions>(objectMapper)
                .flatMap { apiRegistrationService.saveApiDefinitions(context, it) }
                .map { response.status(HttpResponseStatus.CREATED)
                    .header(HttpHeaderNames.LOCATION, "/$context")
                }
                .switchIfEmpty(Mono.defer { Mono.just(notFound(response)) })
                .flatMap { r -> r.send() }
        } ?: sendNotFound(response)
        HttpMethod.DELETE -> request.param(PATH_VARIABLE_NAME)?.let { context ->
            apiRegistrationService.deleteApiDefinitions(context)
                .map { noContent(response) }
                .switchIfEmpty(Mono.defer { Mono.just(notFound(response)) })
                .flatMap { r -> r.send() }
        } ?: sendNotFound(response)
        else -> sendMethodNotAllowed(response)
    }

    private inline fun <reified T> adminContextApi(noinline retriever: (ApiDefinitions) -> T?, noinline updater: (ApiDefinitions, T?) -> ApiDefinitions): AdminApiHandler {
        return { method, request, response ->
            when (method) {
                HttpMethod.GET -> getApiDefinitionsProperty(request, response, retriever)
                HttpMethod.PUT -> putApiDefinitionsProperty(request, response, updater)
                HttpMethod.DELETE -> deleteApiDefinitionsProperty(request, response, updater)
                else -> sendMethodNotAllowed(response)
            }
        }
    }

    private inline fun <reified T> adminConfigurationApi(noinline retriever: (ApiConfiguration) -> T?, noinline updater: (ApiConfiguration, T?) -> ApiConfiguration): AdminApiHandler {
        return { method, request, response ->
            val parameters = request.queryParameters()
            if (parameters.containsKey(PARAMETER_NAME)) {
                when (method) {
                    HttpMethod.GET -> getConfigurationProperty(request, response, parameters, retriever)
                    HttpMethod.PUT -> putConfigurationProperty(request, response, parameters, updater)
                    HttpMethod.DELETE -> deleteConfigurationProperty(request, response, parameters, updater)
                    else -> sendMethodNotAllowed(response)
                }
            } else {
                sendNotFound(response)
            }
        }
    }

    private inline fun <reified T> putConfigurationProperty(request: HttpServerRequest, response: HttpServerResponse, parameters: Map<String, List<String>>, noinline updater: (ApiConfiguration, T) -> ApiConfiguration) =
        request.bodyToMono<T>(objectMapper).flatMap { body ->
            updateConfigurationProperty(request, parameters) { config -> updater(config, body) }
                .map { v -> response.status(HttpResponseStatus.OK)
                    .header(HttpHeaderNames.CONTENT_TYPE, HttpHeaderValues.APPLICATION_JSON)
                    .sendObject(v as Any)
                }.map { def -> response.status(HttpResponseStatus.OK)
                    .header(HttpHeaderNames.CONTENT_TYPE, HttpHeaderValues.APPLICATION_JSON)
                    .sendObject(def as Any)
                }
        }.switchIfEmpty(Mono.defer { Mono.just(notFound(response)) })
            .flatMap { r -> r.then() }

    private fun <T> deleteConfigurationProperty(request: HttpServerRequest, response: HttpServerResponse, parameters: Map<String, List<String>>, updater: (ApiConfiguration, T?) -> ApiConfiguration) =
        updateConfigurationProperty(request, parameters) { config -> updater(config, null) }
            .map { v -> response.status(HttpResponseStatus.OK)
                .header(HttpHeaderNames.CONTENT_TYPE, HttpHeaderValues.APPLICATION_JSON)
                .sendObject(v as Any)
            }.map { noContent(response) }
            .switchIfEmpty(Mono.defer { Mono.just(notFound(response)) })
            .flatMap { r -> r.then() }

    private fun updateConfigurationProperty(request: HttpServerRequest, parameters: Map<String, List<String>>, updater: (ApiConfiguration) -> ApiConfiguration) =
        request.param(PATH_VARIABLE_NAME)?.let { context ->
            apiRegistrationService.getApiDefinitions(context).mapNotNull { def ->
                parameters[PARAMETER_NAME]?.get(0)?.let {
                    val api = URLDecoder.decode(it, StandardCharsets.UTF_8)
                    val newDef = if (def.configurations == null) def.updateConfigurations(mapOf()) else def
                    updater(newDef.configurations?.get(api) ?: ApiConfiguration()).let {
                        newDef.updateConfiguration(api, it)
                    }
                }
            }.flatMap { def -> apiRegistrationService.saveApiDefinitions(context, def) }
        } ?: Mono.empty()

    private fun <T> getConfigurationProperty(request: HttpServerRequest, response: HttpServerResponse, parameters: Map<String, List<String>>, retriever: (ApiConfiguration) -> T?) =
        request.param(PATH_VARIABLE_NAME)?.let { context ->
            apiRegistrationService.getApiDefinitions(context).mapNotNull { def ->
                parameters[PARAMETER_NAME]?.get(0)?.let {
                    val api = URLDecoder.decode(it, StandardCharsets.UTF_8)
                    def.configurations?.let { v -> v[api]?.let(retriever) }
                }
            }.map { v -> response.status(HttpResponseStatus.OK)
                .header(HttpHeaderNames.CONTENT_TYPE, HttpHeaderValues.APPLICATION_JSON)
                .sendObject(v as Any)
            }.switchIfEmpty(Mono.defer { Mono.just(notFound(response)) })
                .flatMap { r -> r.then() }
        } ?: sendNotFound(response)

    private inline fun <reified T> putApiDefinitionsProperty(request: HttpServerRequest, response: HttpServerResponse, noinline updater: (ApiDefinitions, T?) -> ApiDefinitions) =
        request.bodyToMono<T>(objectMapper).flatMap { body ->
            updateApiDefinitionProperty(request) { def -> updater(def, body) }
                .map { v -> response.status(HttpResponseStatus.OK)
                    .header(HttpHeaderNames.CONTENT_TYPE, HttpHeaderValues.APPLICATION_JSON)
                    .sendObject(v)
                }
        }.switchIfEmpty(Mono.defer { Mono.just(notFound(response)) })
            .flatMap { r -> r.then() }

    private inline fun <reified T> deleteApiDefinitionsProperty(request: HttpServerRequest, response: HttpServerResponse, noinline updater: (ApiDefinitions, T?) -> ApiDefinitions) =
        request.bodyToMono<T>(objectMapper).flatMap {
            updateApiDefinitionProperty(request) { def -> updater(def, null) }
                .map { noContent(response) }
        }.switchIfEmpty(Mono.defer { Mono.just(notFound(response)) })
            .flatMap { r -> r.then() }

    private fun updateApiDefinitionProperty(request: HttpServerRequest, updater: (ApiDefinitions) -> ApiDefinitions) =
        request.param(PATH_VARIABLE_NAME)?.let { context ->
            apiRegistrationService.getApiDefinitions(context)
                .map(updater)
                .flatMap { apiRegistrationService.saveApiDefinitions(context, it) }
        } ?: Mono.empty()

    private fun <T> getApiDefinitionsProperty(request: HttpServerRequest, response: HttpServerResponse, retriever: (ApiDefinitions) -> T?) =
        request.param(PATH_VARIABLE_NAME)?.let { context ->
            apiRegistrationService.getApiDefinitions(context)
                .mapNotNull(retriever)
                .map { response.status(HttpResponseStatus.OK)
                    .header(HttpHeaderNames.CONTENT_TYPE, HttpHeaderValues.APPLICATION_JSON)
                    .sendObject(it as Any)
                }.switchIfEmpty(Mono.defer { Mono.just(notFound(response)) })
                .flatMap { r -> r.then() }
        } ?: sendNotFound(response)

}

internal fun sendNotFound(response: HttpServerResponse) = notFound(response).send()

internal fun sendMethodNotAllowed(response: HttpServerResponse) = methodNotAllowed(response).send()

internal fun sendNoContent(response: HttpServerResponse) = noContent(response).send()

internal fun notFound(response: HttpServerResponse): HttpServerResponse =
    response.status(HttpResponseStatus.NOT_FOUND)
internal fun noContent(response: HttpServerResponse): HttpServerResponse =
    response.status(HttpResponseStatus.NO_CONTENT)
internal fun methodNotAllowed(response: HttpServerResponse): HttpServerResponse =
    response.status(HttpResponseStatus.METHOD_NOT_ALLOWED)

private fun HttpServerRoutes.path(uri: String, init: AdminApiHandler): HttpServerRoutes {
    get(uri) { request, response ->
        init(HttpMethod.GET, request, response)
    }
    post(uri) { request, response ->
        init(HttpMethod.POST, request, response)
    }
    put(uri) { request, response ->
        init(HttpMethod.PUT, request, response)
    }
    delete(uri) { request, response ->
        init(HttpMethod.DELETE, request, response)
    }
    // patch?
    return this
}

private fun HttpServerRequest.queryParameters() = QueryStringDecoder(uri()).parameters()