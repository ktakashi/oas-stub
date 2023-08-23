package io.github.ktakashi.oas.controllers

import io.github.ktakashi.oas.annotations.Delayable
import java.util.concurrent.CompletableFuture
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@RestController
@RequestMapping("/custom")
class CustomController {
    @Delayable(context = "custom", path = "/ok1")
    @GetMapping(path = ["/ok1"])
    fun getMono1() = Mono.defer {
        Mono.just(ResponseEntity.ok().body("OK"))
    }

    @Delayable(context = "custom", path = "/ok2")
    @GetMapping(path = ["/ok2"])
    fun getFlux1() = Flux.fromIterable(listOf("OK", "OK", "OK"))

    @Delayable(context = "custom", path = "/ok3")
    @GetMapping(path = ["/ok3"])
    fun getCompletionStage1() = CompletableFuture.completedStage(ResponseEntity.ok().body("OK"))


    @Delayable(context = "custom", path = "/ok4")
    @GetMapping(path = ["/ok4"])
    fun get1() = ResponseEntity.ok().body("OK")
}
