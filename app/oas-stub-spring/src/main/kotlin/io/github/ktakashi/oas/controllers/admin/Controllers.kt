package io.github.ktakashi.oas.controllers.admin

import io.github.ktakashi.oas.engine.storages.StorageService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono

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
