package io.github.ktakashi.oas.web.rests

import io.github.ktakashi.oas.engine.apis.ApiRegistrationService
import io.github.ktakashi.oas.model.ApiConfiguration
import io.github.ktakashi.oas.model.ApiData
import io.github.ktakashi.oas.model.ApiDefinitions
import io.github.ktakashi.oas.model.ApiDelay
import io.github.ktakashi.oas.model.ApiHeaders
import io.github.ktakashi.oas.model.ApiOptions
import io.github.ktakashi.oas.model.PluginDefinition
import io.github.ktakashi.oas.model.PluginType
import io.github.ktakashi.oas.web.annotations.Admin
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.inject.Inject
import jakarta.inject.Named
import jakarta.inject.Singleton
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.DELETE
import jakarta.ws.rs.GET
import jakarta.ws.rs.POST
import jakarta.ws.rs.PUT
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.Produces
import jakarta.ws.rs.QueryParam
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response
import java.net.URI
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.util.Optional
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionStage

// TODO
typealias UriTemplate = String

@Admin
@Path("/")
@Tag(name = "API info", description = "API info")
@Named @Singleton
class ApiController @Inject constructor(private val apiRegistrationService: ApiRegistrationService) {
    @Operation(summary = "Get API names", description = "Retrieves list of API names")
    @ApiResponse(responseCode = "200", description = "List of names")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    fun get() = apiRegistrationService.getAllNames()
}

@Admin
@Path("/{context}")
@Tag(name = "API Context", description = "API context CRUD")
@Named @Singleton
class ContextController @Inject constructor(apiRegistrationService: ApiRegistrationService): AbstractContextController(apiRegistrationService) {
    @Operation(summary = "Get API definition", description = "Retrieves the API definition associated to the {context}")
    @ApiResponse(responseCode = "200", description = "Specified context is found")
    @ApiResponse(responseCode = "404", description = "Specified context is not found", content = [Content(schema = Schema())])
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    fun get(@PathParam("context") context: String): Response = getApiDefinitions(context)
            .map { v -> Response.ok().entity(v).build() }
            .orElseGet { Response.status(Response.Status.NOT_FOUND).build() }

    @Operation(summary = "Create API definition", description = "Creates an API definition of the {context} with full definition")
    @ApiResponse(responseCode = "201", description = "The API is created")
    @ApiResponse(
            responseCode = "422",
            description = "The uploaded OAS specification is not parsable and/or configurations paths are not listed on the specification",
            content = [Content(schema = Schema())]
    )
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    fun postFull(@PathParam("context") context: String, request: ApiDefinitions): CompletionStage<Response> = CompletableFuture.supplyAsync {
        if (apiRegistrationService.saveApiDefinitions(context, request)) {
            Response.created(URI.create("/${context}")).entity(request).build()
        } else {
            Response.status(Response.Status.NOT_FOUND).build()
        }
    }

    @Operation(summary = "Create API definition", description = "Creates an API definition of the {context} with OAS content")
    @ApiResponse(responseCode = "201", description = "The API is created")
    @ApiResponse(responseCode = "422", description = "The uploaded OAS specification is not parsable", content = [Content(schema = Schema())])
    @POST
    @Consumes(value = [MediaType.TEXT_PLAIN, MediaType.APPLICATION_OCTET_STREAM])
    @Produces(MediaType.APPLICATION_JSON)
    fun post(@PathParam("context") context: String, request: String): Response = ApiDefinitions(request).let { apiDefinitions ->
        if (apiRegistrationService.saveApiDefinitions(context, apiDefinitions)) {
            Response.created(URI.create("/${context}")).entity(apiDefinitions).build()
        } else {
            Response.status(422).build()
        }
    }

    @Operation(summary = "Delete API definition", description = "Deletes the API definition of the {context}")
    @ApiResponse(responseCode = "204", description = "The API is deleted", content = [Content(schema = Schema())])
    @ApiResponse(responseCode = "04", description = "The {context} does not exist", content = [Content(schema = Schema())])
    @DELETE
    fun delete(@PathParam("context") context: String): CompletionStage<Response> = CompletableFuture.supplyAsync {
        if (apiRegistrationService.deleteApiDefinitions(context)) {
            Response.noContent().build()
        } else {
            Response.status(Response.Status.NOT_FOUND).build()
        }
    }
}

@Admin
@Path("/{context}/options")
@Tag(name = "API Context Options", description = "API context options CRUD")
@Named @Singleton
class ContextOptionsController @Inject constructor(apiRegistrationService: ApiRegistrationService): AbstractContextController(apiRegistrationService) {
    @Operation(summary = "Get API options", description = "Get API options of the {context}")
    @ApiResponse(responseCode = "200", description = "Get the API options")
    @ApiResponse(responseCode = "404", description = "Specified context is not found", content = [Content(schema = Schema())])
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    fun get(@PathParam("context") context: String) = getApiDefinitionsProperty(context, ApiDefinitions::options)

    @Operation(summary = "Update API options", description = "Updates API options of the {context}")
    @ApiResponse(responseCode = "200", description = "The API options are updated")
    @ApiResponse(responseCode = "404", description = "Specified context is not found", content = [Content(schema = Schema())])
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    fun put(@PathParam("context") context: String, request: ApiOptions) = putApiDefinitionsProperty(context) { def ->
        def.updateOptions(request)
    }

    @Operation(summary = "Delete API options", description = "Delete API options of the {context}")
    @ApiResponse(responseCode = "200", description = "The API options are deleted", content = [Content(schema = Schema())])
    @ApiResponse(responseCode = "404", description = "Specified context is not found", content = [Content(schema = Schema())])
    @DELETE
    fun delete(@PathParam("context") context: String) = deleteApiDefinitionsProperty(context) { def ->
        def.updateOptions(null)
    }
}

@Admin
@Path("/{context}/configurations")
@Tag(name = "API Context configurations", description = "API context configurations CRUD")
@Named @Singleton
class ContextConfigurationsController @Inject constructor(apiRegistrationService: ApiRegistrationService): AbstractContextController(apiRegistrationService) {
    @Operation(summary = "Get API configurations", description = "Get API configurations of the {context}")
    @ApiResponse(responseCode = "200", description = "Get the API configurations")
    @ApiResponse(responseCode = "404", description = "Specified context is not found", content = [Content(schema = Schema())])
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    fun get(@PathParam("context") context: String) = getApiDefinitionsProperty(context, ApiDefinitions::configurations)

    @Operation(summary = "Update API configurations", description = "Updates API configurations of the {context}")
    @ApiResponse(responseCode = "200", description = "The API configurations are updated")
    @ApiResponse(
            responseCode = "404",
            description = "Specified context is not found and/or API paths are not matching to the specification",
            content = [Content(schema = Schema())]
    )
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    fun put(@PathParam("context") context: String, request: Map<String, ApiConfiguration>) = putApiDefinitionsProperty(context) { def ->
        def.updateConfigurations(request)
    }

    @Operation(summary = "Delete API configurations", description = "Delete API configurations of the {context}")
    @ApiResponse(responseCode = "200", description = "The API configurations are deleted", content = [Content(schema = Schema())])
    @ApiResponse(responseCode = "404", description = "Specified context is not found", content = [Content(schema = Schema())])
    @DELETE
    fun delete(@PathParam("context") context: String) = deleteApiDefinitionsProperty(context) { def ->
        def.updateConfigurations(null)
    }
}

@Admin
@Path("/{context}/headers")
@Tag(name = "API Context headers", description = "API context headers CRUD")
@Named @Singleton
class ContextHeadersController @Inject constructor(apiRegistrationService: ApiRegistrationService): AbstractContextController(apiRegistrationService) {
    @Operation(summary = "Get API headers", description = "Get API headers of the {context}")
    @ApiResponse(responseCode = "200", description = "Get the API headers")
    @ApiResponse(responseCode = "404", description = "Specified context is not found", content = [Content(schema = Schema())])
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    fun get(@PathParam("context") context: String) = getApiDefinitionsProperty(context, ApiDefinitions::headers)

    @Operation(summary = "Update API headers", description = "Updates API headers of the {context}")
    @ApiResponse(responseCode = "200", description = "The API headers are updated")
    @ApiResponse(responseCode = "404", description = "Specified context is not found", content = [Content(schema = Schema())])
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    fun put(@PathParam("context") context: String, request: ApiHeaders) = putApiDefinitionsProperty(context) { def ->
        def.updateHeaders(request)
    }

    @Operation(summary = "Delete API headers", description = "Deletes API headers of the {context}")
    @ApiResponse(responseCode = "200", description = "The API headers are deleted", content = [Content(schema = Schema())])
    @ApiResponse(responseCode = "404", description = "Specified context is not found", content = [Content(schema = Schema())])
    @DELETE
    fun delete(@PathParam("context") context: String) = deleteApiDefinitionsProperty(context) { def ->
        def.updateHeaders(null)
    }
}

@Admin
@Path("/{context}/data")
@Tag(name = "API Context data", description = "API context data CRUD")
@Named @Singleton
class ContextDataController @Inject constructor(apiRegistrationService: ApiRegistrationService): AbstractContextController(apiRegistrationService) {
    @Operation(summary = "Get API data", description = "Get API data of the {context}")
    @ApiResponse(responseCode = "200", description = "Get the API data")
    @ApiResponse(responseCode = "404", description = "Specified context is not found", content = [Content(schema = Schema())])
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    fun get(@PathParam("context") context: String) = getApiDefinitionsProperty(context, ApiDefinitions::data)

    @Operation(summary = "Update API data", description = "Updates API data of the {context}")
    @ApiResponse(responseCode = "200", description = "The API data are updated")
    @ApiResponse(responseCode = "404", description = "Specified context is not found", content = [Content(schema = Schema())])
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    fun put(@PathParam("context") context: String, request: ApiData) = putApiDefinitionsProperty(context) {
        def -> def.updateData(request)
    }

    @Operation(summary = "Delete API data", description = "Deletes API data of the {context}")
    @ApiResponse(responseCode = "200", description = "The API data are deleted", content = [Content(schema = Schema())])
    @ApiResponse(responseCode = "404", description = "Specified context is not found", content = [Content(schema = Schema())])
    @DELETE
    fun delete(@PathParam("context") context: String) = deleteApiDefinitionsProperty(context) {
        def -> def.updateData(null)
    }
}

@Admin
@Path("/{context}/delay")
@Tag(name = "API Context delay", description = "API context delay CRUD")
@Named @Singleton
class ContextDelayController @Inject constructor(apiRegistrationService: ApiRegistrationService): AbstractContextController(apiRegistrationService) {
    @Operation(summary = "Get API delay", description = "Get API delay of the {context}")
    @ApiResponse(responseCode = "200", description = "Get the API delay")
    @ApiResponse(responseCode = "404", description = "Specified context is not found", content = [Content(schema = Schema())])
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    fun get(@PathParam("context") context: String) = getApiDefinitionsProperty(context, ApiDefinitions::delay)

    @Operation(summary = "Update API delay", description = "Updates API delay of the {context}")
    @ApiResponse(responseCode = "200", description = "The API delay are updated")
    @ApiResponse(responseCode = "404", description = "Specified context is not found", content = [Content(schema = Schema())])
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    fun put(@PathParam("context") context: String, request: ApiDelay) = putApiDefinitionsProperty(context) {
        def -> def.updateDelay(request)
    }

    @Operation(summary = "Delete API delay", description = "Deletes API delay of the {context}")
    @ApiResponse(responseCode = "200", description = "The API delay are deleted", content = [Content(schema = Schema())])
    @ApiResponse(responseCode = "404", description = "Specified context is not found", content = [Content(schema = Schema())])
    @DELETE
    fun delete(@PathParam("context") context: String) = deleteApiDefinitionsProperty(context) {
        def -> def.updateDelay(null)
    }
}

@Admin
@Path("/{context}/configurations/plugins")
@Tag(name = "API Plugin", description = "Single API plugin configuration CRUD")
@Named @Singleton
class PluginConfigurationsController @Inject constructor(apiRegistrationService: ApiRegistrationService): AbstractSingleApiController(apiRegistrationService) {

    @Operation(summary = "Get API plugin configuration", description = "Get the plugin of the API if exists")
    @ApiResponse(responseCode = "200", description = "The API plugin is retrieved", content = [Content(schema = Schema())])
    @ApiResponse(responseCode = "404", description = "Specified context or API (plugin) is not found", content = [Content(schema = Schema())])
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    fun get(@PathParam("context") context: String, @QueryParam("api") api: UriTemplate) = getConfigurationProperty(context, api) { v ->
        v?.plugin
    }

    @Operation(summary = "Update API plugin configuration", description = "Update plugin of the API")
    @ApiResponse(responseCode = "200", description = "The API is updated")
    @ApiResponse(responseCode = "404", description = "Specified context or API is not found", content = [Content(schema = Schema())])
    @Path("/groovy")
    @PUT
    @Consumes(value = [MediaType.APPLICATION_OCTET_STREAM, MediaType.TEXT_PLAIN])
    @Produces(MediaType.APPLICATION_JSON)
    fun put(@PathParam("context") context: String, @QueryParam("api") api: UriTemplate, request: String) = putConfigurationProperty(context, api) { apiConfiguration ->
        PluginDefinition(type = PluginType.GROOVY, script = request).let {
            apiConfiguration?.updatePlugin(it) ?: ApiConfiguration(plugin = it)
        }
    }

    @Operation(summary = "Delete API plugin configuration", description = "Deletes the plugin of the API if exists")
    @ApiResponse(responseCode = "204", description = "The API plugin is deleted", content = [Content(schema = Schema())])
    @ApiResponse(responseCode = "404", description = "Specified context or API (plugin) is not found", content = [Content(schema = Schema())])
    @DELETE
    fun delete(@PathParam("context") context: String, @QueryParam("api") api: UriTemplate) = deleteConfigurationProperty(context, api) { v ->
        v?.updatePlugin(null)
    }
}

@Admin
@Path("/{context}/configurations/headers")
@Tag(name = "API Headers", description = "Single API headers CRUD")
@Named @Singleton
class HeadersConfigurationsController @Inject constructor(apiRegistrationService: ApiRegistrationService): AbstractSingleApiController(apiRegistrationService) {

    @Operation(summary = "Get API headers configuration", description = "Get the headers of the API if exists")
    @ApiResponse(responseCode = "200", description = "The API headers is retrieved", content = [Content(schema = Schema())])
    @ApiResponse(responseCode = "404", description = "Specified context or API (headers) is not found", content = [Content(schema = Schema())])
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    fun get(@PathParam("context") context: String, @QueryParam("api") api: UriTemplate) = getConfigurationProperty(context, api) { v ->
        v?.headers
    }

    @Operation(summary = "Update API headers configuration", description = "Updates an API headers of the {context}")
    @ApiResponse(responseCode = "200", description = "The API headers are updated")
    @ApiResponse(responseCode = "404", description = "Specified context or API is not found", content = [Content(schema = Schema())])
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    fun put(@PathParam("context") context: String, @QueryParam("api") api: UriTemplate, request: ApiHeaders) = putConfigurationProperty(context, api) { apiConfiguration ->
        apiConfiguration?.updateHeaders(request) ?: ApiConfiguration(headers = request)
    }

    @Operation(summary = "Delete API headers configuration", description = "Deletes the headers of the API if exists")
    @ApiResponse(responseCode = "204", description = "The API headers is deleted", content = [Content(schema = Schema())])
    @ApiResponse(responseCode = "404", description = "Specified context or API (headers) is not found", content = [Content(schema = Schema())])
    @DELETE
    fun delete(@PathParam("context") context: String, @QueryParam("api") api: UriTemplate) = deleteConfigurationProperty(context, api) { v ->
        v?.updateHeaders(null)
    }
}

@Admin
@Path("/{context}/configurations/options")
@Tag(name = "API Options", description = "Single API options CRUD")
@Named @Singleton
class OptionsConfigurationsController @Inject constructor(apiRegistrationService: ApiRegistrationService): AbstractSingleApiController(apiRegistrationService) {

    @Operation(summary = "Get API options configuration", description = "Get the options of the API if exists")
    @ApiResponse(responseCode = "200", description = "The API options is retrieved", content = [Content(schema = Schema())])
    @ApiResponse(responseCode = "404", description = "Specified context or API (options) is not found", content = [Content(schema = Schema())])
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    fun get(@PathParam("context") context: String, @QueryParam("api") api: UriTemplate) = getConfigurationProperty(context, api) { v ->
        v?.options
    }

    @Operation(summary = "Update API options configuration", description = "Updates an API options of the {context}")
    @ApiResponse(responseCode = "200", description = "The API options is updated")
    @ApiResponse(responseCode = "404", description = "Specified context or API is not found", content = [Content(schema = Schema())])
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    fun put(@PathParam("context") context: String, @QueryParam("api") api: UriTemplate, request: ApiOptions) = putConfigurationProperty(context, api) { apiConfiguration ->
        apiConfiguration?.updateOptions(request) ?: ApiConfiguration(options = request)
    }

    @Operation(summary = "Delete API options configuration", description = "Deletes the options of the API if exists")
    @ApiResponse(responseCode = "204", description = "The API options is deleted", content = [Content(schema = Schema())])
    @ApiResponse(responseCode = "404", description = "Specified context or API (options) is not found", content = [Content(schema = Schema())])
    @DELETE
    fun delete(@PathParam("context") context: String, @QueryParam("api") api: UriTemplate) = deleteConfigurationProperty(context, api) { v ->
        v?.updateOptions(null)
    }
}

@Admin
@Path("/{context}/configurations/data")
@Tag(name = "API Data", description = "Single API data CRUD")
@Named @Singleton
class DataConfigurationsController @Inject constructor(apiRegistrationService: ApiRegistrationService): AbstractSingleApiController(apiRegistrationService) {

    @Operation(summary = "Get API data configuration", description = "Get the data of the API if exists")
    @ApiResponse(responseCode = "200", description = "The API data is retrieved", content = [Content(schema = Schema())])
    @ApiResponse(responseCode = "404", description = "Specified context or API (data) is not found", content = [Content(schema = Schema())])
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    fun get(@PathParam("context") context: String, @QueryParam("api") api: UriTemplate) = getConfigurationProperty(context, api) { v ->
        v?.data
    }

    @Operation(summary = "Update API data configuration", description = "Updates an API data of the {context}")
    @ApiResponse(responseCode = "200", description = "The API data is updated")
    @ApiResponse(responseCode = "404", description = "Specified context or API is not found", content = [Content(schema = Schema())])
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    fun put(@PathParam("context") context: String, @QueryParam("api") api: UriTemplate, request: ApiData) = putConfigurationProperty(context, api) { apiConfiguration ->
        apiConfiguration?.updateData(request) ?: ApiConfiguration(data = request)
    }

    @Operation(summary = "Delete API data configuration", description = "Deletes the data of the API if exists")
    @ApiResponse(responseCode = "204", description = "The API data is deleted", content = [Content(schema = Schema())])
    @ApiResponse(responseCode = "404", description = "Specified context or API (data) is not found", content = [Content(schema = Schema())])
    @DELETE
    fun delete(@PathParam("context") context: String, @QueryParam("api") api: UriTemplate) = deleteConfigurationProperty(context, api) { v ->
        v?.updateData(null)
    }
}

@Admin
@Path("/{context}/configurations/delay")
@Tag(name = "API Delay", description = "Single API delay CRUD")
@Named @Singleton
class DelayConfigurationsController @Inject constructor(apiRegistrationService: ApiRegistrationService): AbstractSingleApiController(apiRegistrationService) {

    @Operation(summary = "Get API delay configuration", description = "Get the delay of the API if exists")
    @ApiResponse(responseCode = "200", description = "The API data is retrieved", content = [Content(schema = Schema())])
    @ApiResponse(responseCode = "404", description = "Specified context or API (delay) is not found", content = [Content(schema = Schema())])
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    fun get(@PathParam("context") context: String, @QueryParam("api") api: UriTemplate) = getConfigurationProperty(context, api) { v ->
        v?.delay
    }

    @Operation(summary = "Update API delay configuration", description = "Updates an API delay of the {context}")
    @ApiResponse(responseCode = "200", description = "The API delay is updated")
    @ApiResponse(responseCode = "404", description = "Specified context or API is not found", content = [Content(schema = Schema())])
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    fun put(@PathParam("context") context: String, @QueryParam("api") api: UriTemplate, request: ApiDelay) = putConfigurationProperty(context, api) { apiConfiguration ->
        apiConfiguration?.updateDelay(request) ?: ApiConfiguration(delay = request)
    }

    @Operation(summary = "Delete API delay configuration", description = "Deletes the delay of the API if exists")
    @ApiResponse(responseCode = "204", description = "The API delay is deleted", content = [Content(schema = Schema())])
    @ApiResponse(responseCode = "404", description = "Specified context or API (delay) is not found", content = [Content(schema = Schema())])
    @DELETE
    fun delete(@PathParam("context") context: String, @QueryParam("api") api: UriTemplate) = deleteConfigurationProperty(context, api) { v ->
        v?.updateDelay(null)
    }
}


abstract class AbstractContextController(protected val apiRegistrationService: ApiRegistrationService) {
    protected fun getApiDefinitions(context: String) = apiRegistrationService.getApiDefinitions(context)

    protected fun <T> getApiDefinitionsProperty(context: String, retriever: (ApiDefinitions) -> T?): Response =
            getApiDefinitions(context)
                    .map<T>(retriever)
                    .map { v -> Response.ok().entity(v).build() }
                    .orElseGet { Response.status(Response.Status.NOT_FOUND).build() }

    protected fun putApiDefinitionsProperty(context: String, updator: (ApiDefinitions) -> ApiDefinitions): Response = updateApiDefinitionsProperty(context, updator)
            .map { def -> Response.ok().entity(def).build() }
            .orElseGet { Response.status(Response.Status.NOT_FOUND).build() }

    protected fun deleteApiDefinitionsProperty(context: String, updator: (ApiDefinitions) -> ApiDefinitions): Response = updateApiDefinitionsProperty(context, updator)
            .map { Response.noContent().build() }
            .orElseGet { Response.status(Response.Status.NOT_FOUND).build() }

    private fun updateApiDefinitionsProperty(context: String, updator: (ApiDefinitions) -> ApiDefinitions): Optional<ApiDefinitions> =
            getApiDefinitions(context).map(updator)
                    .map { def -> if (apiRegistrationService.saveApiDefinitions(context, def)) def else null }

}


abstract class AbstractSingleApiController(private val apiRegistrationService: ApiRegistrationService) {
    protected fun <T> getConfigurationProperty(context: String, api: UriTemplate, retriever: (ApiConfiguration?) -> T): Response = getApiDefinitions(context)
            .map { def ->
                val decodedApi = URLDecoder.decode(api, StandardCharsets.UTF_8)
                retriever(def.configurations?.get(decodedApi))
            }
            .map { v -> Response.ok().entity(v).build() }
            .orElseGet { Response.status(Response.Status.NOT_FOUND).build() }

    protected fun putConfigurationProperty(context: String, api: UriTemplate, updator: (ApiConfiguration?) -> ApiConfiguration): Response = updateConfigurationProperty(context, api, updator)
            .map { def -> Response.ok().entity(def).build() }
            .orElseGet { Response.status(Response.Status.NOT_FOUND).build() }

    protected fun deleteConfigurationProperty(context: String, api: UriTemplate, updator: (ApiConfiguration?) -> ApiConfiguration?): Response = updateConfigurationProperty(context, api, updator)
            .map { Response.noContent().build() }
            .orElseGet { Response.status(Response.Status.NOT_FOUND).build() }

    private fun updateConfigurationProperty(context: String, api: UriTemplate, updator: (ApiConfiguration?) -> ApiConfiguration?) = getApiDefinitions(context)
            .map { def ->
                val decodedApi = URLDecoder.decode(api, StandardCharsets.UTF_8)
                updator(def.configurations?.get(decodedApi))?.let { v -> def.updateConfiguration(decodedApi, v) }
            }
            .map { def -> if (apiRegistrationService.saveApiDefinitions(context, def)) def else null }

    private fun getApiDefinitions(context: String) = apiRegistrationService.getApiDefinitions(context)
}
