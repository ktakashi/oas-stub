package io.github.ktakashi.oas.controllers.admin

import io.github.ktakashi.oas.annotations.Admin
import io.github.ktakashi.oas.engine.apis.ApiRegistrationService
import io.github.ktakashi.oas.model.ApiConfiguration
import io.github.ktakashi.oas.model.ApiData
import io.github.ktakashi.oas.model.ApiDefinitions
import io.github.ktakashi.oas.model.ApiHeaders
import io.github.ktakashi.oas.model.ApiOptions
import io.github.ktakashi.oas.model.PluginDefinition
import io.github.ktakashi.oas.model.PluginType
import io.github.ktakashi.oas.models.CreateApiRequest
import io.github.ktakashi.oas.models.PutApiOptionsRequest
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import java.net.URI
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono

@Admin
@RestController
@Tag(name = "API Context", description = "API context CRUD")
class ContextController(private val apiRegistrationService: ApiRegistrationService) {
    @Operation(summary = "Get API definition", description = "Retrieves the API definition associated to the {context}")
    @ApiResponse(responseCode = "200", description = "Specified context is found")
    @ApiResponse(responseCode = "404", description = "Specified context is not found", content = [Content(schema = Schema())])
    @GetMapping(path = [ "/{context}" ], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun getApi(@PathVariable("context") context: String) = getApiDefinitions(context)
            .map { v -> ResponseEntity.ok().body(v) }
            .switchIfEmpty(Mono.defer { Mono.just(ResponseEntity.notFound().build()) })

    @Operation(summary = "Create API definition", description = "Creates an API definition of the {context} with full definition")
    @ApiResponse(responseCode = "201", description = "The API is created")
    @ApiResponse(
            responseCode = "422",
            description = "The uploaded OAS specification is not parsable and/or configurations paths are not listed on the specification",
            content = [Content(schema = Schema())]
    )
    @PostMapping(path = [ "/{context}"], consumes = [MediaType.APPLICATION_JSON_VALUE], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun postApiFull(@PathVariable("context") context: String, @RequestBody request: CreateApiRequest) =
            Mono.defer { Mono.just(apiRegistrationService.saveApiDefinitions(context, request)) }
                    .filter { it }
                    .map { ResponseEntity.created(URI.create("/$context")).body(request) }
                    .switchIfEmpty(Mono.defer { Mono.just(ResponseEntity.unprocessableEntity().build()) })

    @Operation(summary = "Create API definition", description = "Creates an API definition of the {context} with OAS content")
    @ApiResponse(responseCode = "201", description = "The API is created")
    @ApiResponse(responseCode = "422", description = "The uploaded OAS specification is not parsable", content = [Content(schema = Schema())])
    @PostMapping(
            path = [ "/{context}"],
            consumes = [MediaType.TEXT_PLAIN_VALUE, MediaType.APPLICATION_OCTET_STREAM_VALUE],
            produces = [MediaType.APPLICATION_JSON_VALUE]
    )
    fun postApi(@PathVariable("context") context: String, @RequestBody request: String) = ApiDefinitions(request).let { apiDefinitions ->
        Mono.defer { Mono.just(apiRegistrationService.saveApiDefinitions(context, apiDefinitions)) }
                .filter { it }
                .map { ResponseEntity.created(URI.create("/$context")).body(apiDefinitions) }
                .switchIfEmpty(Mono.defer { Mono.just(ResponseEntity.unprocessableEntity().build()) })
    }

    @Operation(summary = "Delete API definition", description = "Deletes the API definition of the {context}")
    @ApiResponse(responseCode = "204", description = "The API is deleted", content = [Content(schema = Schema())])
    @ApiResponse(responseCode = "04", description = "The {context} does not exist", content = [Content(schema = Schema())])
    @DeleteMapping(path = [ "/{context}"])
    fun deleteApi(@PathVariable("context") context: String) =
            Mono.defer { Mono.just(apiRegistrationService.deleteApiDefinitions(context)) }
                    .filter { it }
                    .map { ResponseEntity.noContent().build<Unit>() }
                    .switchIfEmpty(Mono.defer { Mono.just(ResponseEntity.notFound().build()) })

    @Operation(summary = "Get API options", description = "Get API options of the {context}")
    @ApiResponse(responseCode = "200", description = "Get the API options")
    @ApiResponse(responseCode = "404", description = "Specified context is not found", content = [Content(schema = Schema())])
    @GetMapping(path = [ "/{context}/options"], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun getOptions(@PathVariable("context") context: String) = getApiDefinitionsProperty(context, ApiDefinitions::options)

    @Operation(summary = "Update API options", description = "Updates API options of the {context}")
    @ApiResponse(responseCode = "200", description = "The API options are updated")
    @ApiResponse(responseCode = "404", description = "Specified context is not found", content = [Content(schema = Schema())])
    @PutMapping(path = [ "/{context}/options"], consumes = [MediaType.APPLICATION_JSON_VALUE], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun putOptions(@PathVariable("context") context: String, @RequestBody request: PutApiOptionsRequest) = updateApiDefinitionsProperty(context) {
        def -> def.updateApiOptions(request)
    }

    @Operation(summary = "Delete API options", description = "Delete API options of the {context}")
    @ApiResponse(responseCode = "200", description = "The API options are deleted")
    @ApiResponse(responseCode = "404", description = "Specified context is not found", content = [Content(schema = Schema())])
    @DeleteMapping(path = [ "/{context}/options"], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun deleteOptions(@PathVariable("context") context: String) = updateApiDefinitionsProperty(context) {
        def -> def.updateApiOptions(ApiOptions())
    }

    @Operation(summary = "Get API configurations", description = "Get API configurations of the {context}")
    @ApiResponse(responseCode = "200", description = "Get the API configurations")
    @ApiResponse(responseCode = "404", description = "Specified context is not found", content = [Content(schema = Schema())])
    @GetMapping(path = [ "/{context}/configurations"], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun getConfigurations(@PathVariable("context") context: String) = getApiDefinitionsProperty(context, ApiDefinitions::configurations)

    @Operation(summary = "Update API configurations", description = "Updates API configurations of the {context}")
    @ApiResponse(responseCode = "200", description = "The API configurations are updated")
    @ApiResponse(
            responseCode = "404",
            description = "Specified context is not found and/or API paths are not matching to the specification",
            content = [Content(schema = Schema())]
    )
    @PutMapping(path = [ "/{context}/configurations"], consumes = [MediaType.APPLICATION_JSON_VALUE], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun putConfigurations(@PathVariable("context") context: String, @RequestBody request: Map<String, ApiConfiguration>) = updateApiDefinitionsProperty(context) {
        def -> def.updateApiConfigurations(request)
    }

    @Operation(summary = "Delete API configurations", description = "Delete API configurations of the {context}")
    @ApiResponse(responseCode = "200", description = "The API configurations are deleted")
    @ApiResponse(responseCode = "404", description = "Specified context is not found", content = [Content(schema = Schema())])
    @DeleteMapping(path = [ "/{context}/configurations"], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun deleteConfigurations(@PathVariable("context") context: String) = updateApiDefinitionsProperty(context) {
        def -> def.updateApiConfigurations(mapOf())
    }

    @Operation(summary = "Get API headers", description = "Get API headers of the {context}")
    @ApiResponse(responseCode = "200", description = "Get the API headers")
    @ApiResponse(responseCode = "404", description = "Specified context is not found", content = [Content(schema = Schema())])
    @GetMapping(path = [ "/{context}/headers"], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun getHeaders(@PathVariable("context") context: String) = getApiDefinitionsProperty(context, ApiDefinitions::headers)

    @Operation(summary = "Update API headers", description = "Updates API headers of the {context}")
    @ApiResponse(responseCode = "200", description = "The API headers are updated")
    @ApiResponse(responseCode = "404", description = "Specified context is not found", content = [Content(schema = Schema())])
    @PutMapping(path = [ "/{context}/headers"], consumes = [MediaType.APPLICATION_JSON_VALUE], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun putHeaders(@PathVariable("context") context: String, @RequestBody request: ApiHeaders) = updateApiDefinitionsProperty(context) {
        def -> def.updateApiHeaders(request)
    }

    @Operation(summary = "Delete API headers", description = "Deletes API headers of the {context}")
    @ApiResponse(responseCode = "200", description = "The API headers are deleted")
    @ApiResponse(responseCode = "404", description = "Specified context is not found", content = [Content(schema = Schema())])
    @DeleteMapping(path = [ "/{context}/headers"], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun deleteHeaders(@PathVariable("context") context: String) = updateApiDefinitionsProperty(context) {
        def -> def.updateApiHeaders(ApiHeaders())
    }

    @Operation(summary = "Get API data", description = "Get API data of the {context}")
    @ApiResponse(responseCode = "200", description = "Get the API data")
    @ApiResponse(responseCode = "404", description = "Specified context is not found", content = [Content(schema = Schema())])
    @GetMapping(path = [ "/{context}/data"], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun getData(@PathVariable("context") context: String) = getApiDefinitionsProperty(context, ApiDefinitions::data)

    @Operation(summary = "Update API data", description = "Updates API data of the {context}")
    @ApiResponse(responseCode = "200", description = "The API data are updated")
    @ApiResponse(responseCode = "404", description = "Specified context is not found", content = [Content(schema = Schema())])
    @PutMapping(path = [ "/{context}/data"], consumes = [MediaType.APPLICATION_JSON_VALUE], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun putData(@PathVariable("context") context: String, @RequestBody request: ApiData) = updateApiDefinitionsProperty(context) {
        def -> def.updateApiData(request)
    }

    @Operation(summary = "Delete API data", description = "Deletes API data of the {context}")
    @ApiResponse(responseCode = "200", description = "The API data are deleted")
    @ApiResponse(responseCode = "404", description = "Specified context is not found", content = [Content(schema = Schema())])
    @DeleteMapping(path = [ "/{context}/data"], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun deleteData(@PathVariable("context") context: String) = updateApiDefinitionsProperty(context) {
        def -> def.updateApiData(ApiData())
    }

    private fun getApiDefinitions(context: String): Mono<ApiDefinitions> =
            Mono.defer {
                Mono.justOrEmpty(apiRegistrationService.getApiDefinitions(context))
            }

    private fun <T> getApiDefinitionsProperty(context: String, retriever: (ApiDefinitions) -> T): Mono<ResponseEntity<T>> =
            getApiDefinitions(context)
                    .map(retriever)
                    .map { v -> ResponseEntity.ok().body(v) }
                    .switchIfEmpty(Mono.defer { Mono.just(ResponseEntity.notFound().build()) })

    private fun updateApiDefinitionsProperty(context: String, updator: (ApiDefinitions) -> ApiDefinitions) =
            getApiDefinitions(context)
                    .map(updator)
                    .flatMap { def -> Mono.justOrEmpty(if (apiRegistrationService.saveApiDefinitions(context, def)) def else null) }
                    .map { def -> ResponseEntity.ok().body(def) }
                    .switchIfEmpty(Mono.defer { Mono.just(ResponseEntity.notFound().build()) })

}

@Admin
@RestController
@Tag(name = "Single API", description = "Single API CRUD")
class ConfigurationsController(private val apiRegistrationService: ApiRegistrationService) {

    @Operation(summary = "Update API plugin configuration", description = "Update plugin of the API")
    @ApiResponse(responseCode = "200", description = "The API is updated")
    @ApiResponse(responseCode = "404", description = "Specified context or API is not found", content = [Content(schema = Schema())])
    @PutMapping(
            path = [ "/{context}/configurations/plugins/groovy"],
            consumes = [MediaType.APPLICATION_OCTET_STREAM_VALUE, MediaType.TEXT_PLAIN_VALUE],
            produces = [MediaType.APPLICATION_JSON_VALUE]
    )
    fun putPlugin(@PathVariable("context") context: String, @RequestParam(name = "api") api: URI, @RequestBody request: String) = getApiDefinitions(context)
            .map { v -> PluginDefinition(type = PluginType.GROOVY, script = request).let {
                val decodedApi = URLDecoder.decode(api.toString(), StandardCharsets.UTF_8)
                v.updateApiConfiguration(decodedApi, v.configurations[decodedApi]
                        ?.updatePlugin(it)
                        ?: ApiConfiguration(plugin = it)) }
            }
            .flatMap { v -> Mono.justOrEmpty(if (apiRegistrationService.saveApiDefinitions(context, v)) v else null) }
            .map { ResponseEntity.ok().body(it) }
            .switchIfEmpty(Mono.defer { Mono.just(ResponseEntity.notFound().build()) })

    @Operation(summary = "Delete API plugin configuration", description = "Deletes the plugin of the API if exists")
    @ApiResponse(responseCode = "204", description = "The API plugin is deleted", content = [Content(schema = Schema())])
    @ApiResponse(responseCode = "404", description = "Specified context or API (plugin) is not found", content = [Content(schema = Schema())])
    @DeleteMapping(path = [ "/{context}/configurations/plugins/groovy"])
    fun deletePlugin(@PathVariable("context") context: String, @RequestParam(name = "api") api: URI) = getApiDefinitions(context)
            .flatMap { v ->
                val decodedApi = URLDecoder.decode(api.toString(), StandardCharsets.UTF_8)
                Mono.justOrEmpty(v.configurations[decodedApi]?.updatePlugin(null)?.let { v.updateApiConfiguration(decodedApi, it) })
            }
            .flatMap { v -> Mono.justOrEmpty(if (apiRegistrationService.saveApiDefinitions(context, v)) v else null) }
            .map { ResponseEntity.noContent().build<Unit>() }
            .switchIfEmpty(Mono.defer { Mono.just(ResponseEntity.notFound().build()) })

    @Operation(summary = "Update API configuration", description = "Updates an API configuration of the {context}")
    @ApiResponse(responseCode = "200", description = "The API configuration is updated")
    @ApiResponse(responseCode = "404", description = "Specified context is not found", content = [Content(schema = Schema())])
    @PutMapping(path = [ "/{context}/configurations/headers"], consumes = [MediaType.APPLICATION_JSON_VALUE], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun putConfiguration(@PathVariable("context") context: String, @RequestParam(name = "api") api: URI, @RequestBody request: ApiHeaders) = getApiDefinitions(context)
            .map { def ->
                val decodedApi = URLDecoder.decode(api.toString(), StandardCharsets.UTF_8)
                def.updateApiConfiguration(decodedApi, def.configurations[decodedApi]
                        ?.updateHeaders(request)
                        ?: ApiConfiguration(headers = request))
            }
            .doOnNext { def -> apiRegistrationService.saveApiDefinitions(context, def)}
            .map { def -> ResponseEntity.ok().body(def) }
            .switchIfEmpty(Mono.defer { Mono.just(ResponseEntity.notFound().build()) })

    private fun getApiDefinitions(context: String): Mono<ApiDefinitions> =
            Mono.defer { Mono.justOrEmpty(apiRegistrationService.getApiDefinitions(context)) }
}
