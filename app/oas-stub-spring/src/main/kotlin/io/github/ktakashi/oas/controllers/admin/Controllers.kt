package io.github.ktakashi.oas.controllers.admin

import io.github.ktakashi.oas.annotations.Admin
import io.github.ktakashi.oas.engine.parsers.ParsingService
import io.github.ktakashi.oas.engine.storages.StorageService
import io.github.ktakashi.oas.model.ApiConfiguration
import io.github.ktakashi.oas.model.ApiDefinitions
import io.github.ktakashi.oas.model.PluginDefinition
import io.github.ktakashi.oas.models.CreateApiRequest
import io.github.ktakashi.oas.models.PutPluginRequest
import java.net.URI
import java.util.Optional
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
class ApiController(private val storageService: StorageService) {
    @GetMapping(path = [ "/{context}" ])
    fun getApi(@PathVariable("context") context: String) =
            Mono.defer { storageService.getApiDefinitions(context)
                    .map { v -> Mono.just(v) }
                    .orElseGet { Mono.empty() }
            }
                    .map { v -> ResponseEntity.ok().body(v) }
                    .switchIfEmpty(Mono.defer { Mono.just(ResponseEntity.notFound().build()) })

}

@Admin
@RestController
class CreateController(private val storageService: StorageService,
                       private val parsingService: ParsingService) {
    @PostMapping(path = [ "/{context}"] )
    fun createApi(@PathVariable("context") context: String, @RequestBody request: CreateApiRequest) =
            Mono.defer { Mono.justOrEmpty(parsingService.sanitize(request.api)) }
                    .map { v ->
                        ApiDefinitions(applicationName = context,
                                api = v,
                                apiConfigurations = request.apiConfigurations,
                                headers = request.headers,
                                apiOptions = request.apiOptions,
                                apiData = request.apiData) }
                    .doOnNext { v -> storageService.saveApiDefinitions(context, v) }
                    .map { ResponseEntity.created(URI.create("/$context")).body(request) }
                    .switchIfEmpty(Mono.defer { Mono.just(ResponseEntity.unprocessableEntity().body(request)) })
}

@Admin
@RestController
class PluginController(private val storageService: StorageService) {

    @PutMapping(path = [ "/{context}/plugins/{path}"] )
    fun putPlugin(@PathVariable("context") context: String,
                  @PathVariable("path") path: URI,
                  @RequestBody request: PutPluginRequest) =
            Mono.defer { Mono.justOrEmpty(storageService.getApiDefinitions(context)) }
                    .map { v -> v.updateApiConfiguration(path.toString(), v.apiConfigurations[path.toString()]
                            ?.updatePlugin(PluginDefinition(request.type, request.script))
                            ?: ApiConfiguration(plugin = Optional.of(PluginDefinition(request.type, request.script)))) }
                    .doOnNext { v -> storageService.saveApiDefinitions(context, v) }
                    .map { ResponseEntity.ok().build<Unit>() }
                    .switchIfEmpty(Mono.defer { Mono.just(ResponseEntity.notFound().build()) })
}
