package io.github.ktakashi.oas.controllers.admin

import io.github.ktakashi.oas.annotations.Admin
import io.github.ktakashi.oas.engine.apis.ApiRegistrationService
import io.github.ktakashi.oas.model.ApiConfiguration
import io.github.ktakashi.oas.model.ApiDefinitions
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

    @Operation(summary = "Create API definition", description = "Creates an API definition of the {context}")
    @ApiResponse(responseCode = "201", description = "The API is created")
    @ApiResponse(responseCode = "422", description = "The uploaded OAS specification is not parsable", content = [Content(schema = Schema())])
    @PostMapping(path = [ "/{context}"], consumes = [MediaType.APPLICATION_JSON_VALUE], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun postApi(@PathVariable("context") context: String, @RequestBody request: CreateApiRequest) =
            Mono.defer { Mono.just(apiRegistrationService.saveApiDefinitions(context, request)) }
                    .filter { it }
                    .map { ResponseEntity.created(URI.create("/$context")).body(request) }
                    .switchIfEmpty(Mono.defer { Mono.just(ResponseEntity.unprocessableEntity().build()) })

    @Operation(summary = "Delete API definition", description = "Deletes an API definition of the {context}")
    @ApiResponse(responseCode = "204", description = "The API is deleted", content = [Content(schema = Schema())])
    @ApiResponse(responseCode = "04", description = "The {context} does not exist", content = [Content(schema = Schema())])
    @DeleteMapping(path = [ "/{context}"])
    fun deleteApi(@PathVariable("context") context: String) =
            Mono.defer { Mono.just(apiRegistrationService.deleteApiDefinitions(context)) }
                    .filter { it }
                    .map { ResponseEntity.noContent().build<Unit>() }
                    .switchIfEmpty(Mono.defer { Mono.just(ResponseEntity.notFound().build()) })

    @Operation(summary = "Update API options", description = "Updates an API options of the {context}")
    @ApiResponse(responseCode = "200", description = "The API is updated")
    @ApiResponse(responseCode = "404", description = "Specified context is not found", content = [Content(schema = Schema())])
    @PutMapping(path = [ "/{context}/options"], consumes = [MediaType.APPLICATION_JSON_VALUE], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun putOptions(@PathVariable("context") context: String, @RequestBody request: PutApiOptionsRequest) = getApiDefinitions(context)
            .map { def -> def.updateApiOptions(request) }
            .doOnNext { def -> apiRegistrationService.saveApiDefinitions(context, def)}
            .map { def -> ResponseEntity.ok().body(def) }
            .switchIfEmpty(Mono.defer { Mono.just(ResponseEntity.notFound().build()) })

    private fun getApiDefinitions(context: String): Mono<ApiDefinitions> =
            Mono.defer {
                Mono.justOrEmpty(apiRegistrationService.getApiDefinitions(context))
            }
}

@Admin
@RestController
@Tag(name = "Single API", description = "Single API CRUD")
class ConfigurationsController(private val apiRegistrationService: ApiRegistrationService) {

    @Operation(summary = "Update API plugin configuration", description = "Update plugin of the API")
    @ApiResponse(responseCode = "200", description = "The API is updated")
    @ApiResponse(responseCode = "404", description = "Specified context or API is not found", content = [Content(schema = Schema())])
    @PutMapping(path = [ "/{context}/configurations/plugins/groovy"], consumes = [MediaType.APPLICATION_OCTET_STREAM_VALUE], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun putPlugin(@PathVariable("context") context: String,
                  @RequestParam(name = "api") api: URI,
                  @RequestBody request: String) =
            Mono.defer { Mono.justOrEmpty(apiRegistrationService.getApiDefinitions(context)) }
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
    fun deletePlugin(@PathVariable("context") context: String, @RequestParam(name = "api") api: URI) =
            Mono.defer { Mono.justOrEmpty(apiRegistrationService.getApiDefinitions(context)) }
                    .flatMap { v ->
                        val decodedApi = URLDecoder.decode(api.toString(), StandardCharsets.UTF_8)
                        Mono.justOrEmpty(v.configurations[decodedApi]?.updatePlugin(null)?.let { v.updateApiConfiguration(decodedApi, it) })
                    }
                    .flatMap { v -> Mono.justOrEmpty(if (apiRegistrationService.saveApiDefinitions(context, v)) v else null) }
                    .map { ResponseEntity.noContent().build<Unit>() }
                    .switchIfEmpty(Mono.defer { Mono.just(ResponseEntity.notFound().build()) })
}
