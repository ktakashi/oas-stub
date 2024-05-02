package io.github.ktakashi.oas.web.reactive.rests
/*
import io.github.ktakashi.oas.engine.apis.ApiRegistrationService
import io.github.ktakashi.oas.model.ApiConfiguration
import io.github.ktakashi.oas.model.ApiDefinitions
import io.github.ktakashi.oas.model.PluginDefinition
import io.github.ktakashi.oas.model.PluginType
import io.github.ktakashi.oas.web.reactive.RouterFunctionBuilder
import java.net.URI
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.server.RequestPredicates.accept
import org.springframework.web.reactive.function.server.RequestPredicates.path
import org.springframework.web.reactive.function.server.RequestPredicates.queryParam
import org.springframework.web.reactive.function.server.RouterFunctions.Builder
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse

private const val API_QUERY_PARAMETER_NAME = "api"
private val apiQueryParameterPredicate = queryParam(API_QUERY_PARAMETER_NAME) { true }
private const val CONTEXT_VARIABLE_NAME = "context"
private const val CONTEXT_PATH_SEGMENT = "/{$CONTEXT_VARIABLE_NAME}"

class ApiRouterFunctionBuilder(private val adminPath: String,
                               private val apiRegistrationService: ApiRegistrationService)
    : RouterFunctionBuilder {
    override fun build(builder: Builder): Builder = builder.path(adminPath) { admin ->
        admin.GET { _ ->
            ServerResponse.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(apiRegistrationService.getAllNames())
        }.path(CONTEXT_PATH_SEGMENT, ::adminContextId)
            .nest(path(CONTEXT_PATH_SEGMENT)) { context ->
                context.path("/options", adminContextApi(ApiDefinitions::options, ApiDefinitions::updateOptions))
                context.path("/configurations", adminContextApi(ApiDefinitions::configurations, ApiDefinitions::updateConfigurations))
                context.path("/headers", adminContextApi(ApiDefinitions::headers, ApiDefinitions::updateHeaders))
                context.path("/data", adminContextApi(ApiDefinitions::data, ApiDefinitions::updateData))
                context.path("/delay", adminContextApi(ApiDefinitions::delay, ApiDefinitions::updateDelay))
                context.nest(path("/configurations")) { conf ->
                    conf.path("/options", adminConfigurationApi(ApiConfiguration::options, ApiConfiguration::updateOptions))
                    conf.path("/headers", adminConfigurationApi(ApiConfiguration::headers, ApiConfiguration::updateHeaders))
                    conf.path("/data", adminConfigurationApi(ApiConfiguration::data, ApiConfiguration::updateData))
                    conf.path("/delay", adminConfigurationApi(ApiConfiguration::delay, ApiConfiguration::updateDelay))
                    conf.path("/plugins") { plugin ->
                        plugin.GET(apiQueryParameterPredicate) { request -> getConfigurationProperty(request, ApiConfiguration::plugin) }
                        plugin.DELETE(apiQueryParameterPredicate) { request -> deleteConfigurationProperty(request, ApiConfiguration::updatePlugin) }
                        plugin.PUT("/groovy", apiQueryParameterPredicate) { request ->
                            putConfigurationProperty(request) { configuration, script: String ->
                                PluginDefinition(type = PluginType.GROOVY, script =  script).let {
                                    configuration.updatePlugin(it)
                                }
                            }
                        }
                    }
                }
            }
    }

    private fun adminContextId(builder: Builder) {
        builder.GET { request ->
            apiRegistrationService.getApiDefinitions(request.pathVariable(CONTEXT_VARIABLE_NAME))
                .map { v -> ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).bodyValue(v) }
                .orElseGet { ServerResponse.notFound().build() }

        }.POST(accept(MediaType.APPLICATION_JSON)) { request ->
            request.bodyToMono(ApiDefinitions::class.java)
                .flatMap { body ->
                    val id = request.pathVariable(CONTEXT_VARIABLE_NAME)
                    if (apiRegistrationService.saveApiDefinitions(id, body)) {
                        ServerResponse.created(URI.create("/$id")).contentType(MediaType.APPLICATION_JSON).bodyValue(body)
                    } else {
                        ServerResponse.notFound().build()
                    }
                }
        }.POST(accept(MediaType.TEXT_PLAIN, MediaType.APPLICATION_OCTET_STREAM)) { request ->
            request.bodyToMono(String::class.java)
                .map { ApiDefinitions(it) }
                .flatMap { body ->
                    val id = request.pathVariable(CONTEXT_VARIABLE_NAME)
                    if (apiRegistrationService.saveApiDefinitions(id, body)) {
                        ServerResponse.created(URI.create("/$id")).contentType(MediaType.APPLICATION_JSON).bodyValue(body)
                    } else {
                        ServerResponse.unprocessableEntity().build()
                    }
                }
        }.DELETE { request ->
            val id = request.pathVariable(CONTEXT_VARIABLE_NAME)
            if (apiRegistrationService.deleteApiDefinitions(id)) {
                ServerResponse.noContent().build()
            } else {
                ServerResponse.notFound().build()
            }
        }
    }

    private inline fun <reified T> adminContextApi(noinline retrieve: (ApiDefinitions) -> T?, noinline updator: (ApiDefinitions, T?) -> ApiDefinitions): (Builder) -> Unit {
        return { builder ->
            builder.GET { request -> getApiDefinitionsProperty(request, retrieve) }
                .PUT { request -> putApiDefinitionProperty(request, updator) }
                .DELETE { request -> deleteApiDefinitionProperty(request, updator) }
        }
    }

    private inline fun <reified T> adminConfigurationApi(noinline retrieve: (ApiConfiguration) -> T?, noinline updator: (ApiConfiguration, T?) -> ApiConfiguration): (Builder) -> Unit {
        return { builder ->
            builder.GET(apiQueryParameterPredicate) { request -> getConfigurationProperty(request, retrieve)}
                .PUT(apiQueryParameterPredicate) { request -> putConfigurationProperty(request, updator)}
                .DELETE(apiQueryParameterPredicate) { request -> deleteConfigurationProperty(request, updator)}
        }
    }

    private fun <T> getConfigurationProperty(request: ServerRequest, retriever: (ApiConfiguration) -> T) =
        apiRegistrationService.getApiDefinitions(request.pathVariable(CONTEXT_VARIABLE_NAME))
            .map { def ->
                val api = request.queryParam(API_QUERY_PARAMETER_NAME).orElseThrow()
                val decodedApi = URLDecoder.decode(api, StandardCharsets.UTF_8)
                def.configurations?.let { v -> v[decodedApi]?.let(retriever) }
            }.map { v -> ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).bodyValue(v as Any) }
            .orElseGet { ServerResponse.notFound().build() }

    private inline fun <reified T> putConfigurationProperty(request: ServerRequest, noinline updator: (ApiConfiguration, T) -> ApiConfiguration) = request.bodyToMono(T::class.java)
        .flatMap { body ->
            updateConfigurationProperty(request) { config -> updator(config, body) }
                .map { ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).bodyValue(body as Any) }
                .orElseGet { ServerResponse.notFound().build() }
        }

    private fun <T> deleteConfigurationProperty(request: ServerRequest, updator: (ApiConfiguration, T?) -> ApiConfiguration) =
            updateConfigurationProperty(request) { config -> updator(config, null) }
                .map { ServerResponse.noContent().build() }
                .orElseGet { ServerResponse.notFound().build() }


    private fun updateConfigurationProperty(request: ServerRequest, updator: (ApiConfiguration) -> ApiConfiguration) = request.pathVariable(CONTEXT_VARIABLE_NAME).let { context ->
        apiRegistrationService.getApiDefinitions(context)
        .map { def ->
            val api = request.queryParam(API_QUERY_PARAMETER_NAME).orElseThrow()
            val decodedApi = URLDecoder.decode(api, StandardCharsets.UTF_8)
            if (def.configurations == null)  {
                def.updateConfigurations(mapOf())
            }
            updator(def.configurations?.get(decodedApi) ?: ApiConfiguration()).let {
                def.updateConfiguration(decodedApi, it)
            }
        }.map { def -> if (apiRegistrationService.saveApiDefinitions(context, def)) def else null }
    }

    private inline fun <reified T> putApiDefinitionProperty(request: ServerRequest, noinline updator: (ApiDefinitions, T?) -> ApiDefinitions) = request.bodyToMono(T::class.java).flatMap { body ->
        updateApiDefinitionProperty(request) { def -> updator(def, body) }
            .map { v -> ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).bodyValue(v as Any) }
            .orElseGet { ServerResponse.notFound().build() }
    }

    private fun <T> deleteApiDefinitionProperty(request: ServerRequest, updator: (ApiDefinitions, T?) -> ApiDefinitions) =
        updateApiDefinitionProperty(request) { def -> updator(def, null) }
            .map { v -> ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).bodyValue(v as Any) }
            .orElseGet { ServerResponse.notFound().build() }


    private fun updateApiDefinitionProperty(request: ServerRequest, updator: (ApiDefinitions) -> ApiDefinitions) =
        request.pathVariable(CONTEXT_VARIABLE_NAME).let { context ->
            apiRegistrationService.getApiDefinitions(context)
                .map(updator)
                .map { def -> if (apiRegistrationService.saveApiDefinitions(context, def)) def else null }
        }


    private fun <T> getApiDefinitionsProperty(request: ServerRequest, retriever: (ApiDefinitions) -> T?) =
        apiRegistrationService.getApiDefinitions(request.pathVariable(CONTEXT_VARIABLE_NAME))
            .map(retriever)
            .map { v -> ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).bodyValue(v as Any) }
            .orElseGet { ServerResponse.notFound().build() }
}


*/