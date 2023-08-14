package io.github.ktakashi.oas.controllers.admin

import io.github.ktakashi.oas.annotations.Admin
import io.github.ktakashi.oas.engine.apis.ApiRegistrationService
import io.github.ktakashi.oas.model.ApiConfiguration
import io.github.ktakashi.oas.model.ApiDefinitions
import io.github.ktakashi.oas.models.CreateApiRequest
import io.github.ktakashi.oas.models.PutApiOptionsRequest
import io.github.ktakashi.oas.models.PutPluginRequest
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import java.net.URI
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
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
    fun createApi(@PathVariable("context") context: String, @RequestBody request: CreateApiRequest) =
            Mono.defer { Mono.just(apiRegistrationService.saveApiDefinitions(context, request)) }
                    .filter { it }
                    .map { ResponseEntity.created(URI.create("/$context")).body(request) }
                    .switchIfEmpty(Mono.defer { Mono.just(ResponseEntity.unprocessableEntity().build()) })

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
class PluginController(private val apiRegistrationService: ApiRegistrationService) {

    @PutMapping(path = [ "/{context}/{path}/plugins"], consumes = [MediaType.APPLICATION_JSON_VALUE], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun putPlugin(@PathVariable("context") context: String,
                  @PathVariable("path") path: URI,
                  @RequestBody request: PutPluginRequest) =
            Mono.defer { Mono.justOrEmpty(apiRegistrationService.getApiDefinitions(context)) }
                    .map { v -> v.updateApiConfiguration(path.toString(), v.configurations[path.toString()]
                            ?.updatePlugin(request)
                            ?: ApiConfiguration(plugin = request)) }
                    .doOnNext { v -> apiRegistrationService.saveApiDefinitions(context, v) }
                    .map { ResponseEntity.ok().build<Unit>() }
                    .switchIfEmpty(Mono.defer { Mono.just(ResponseEntity.notFound().build()) })
}
