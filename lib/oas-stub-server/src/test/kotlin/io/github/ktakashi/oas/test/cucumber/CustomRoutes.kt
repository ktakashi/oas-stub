package io.github.ktakashi.oas.test.cucumber

import io.github.ktakashi.oas.server.handlers.OasStubRoutes
import io.github.ktakashi.oas.server.handlers.OasStubRoutesBuilder
import java.util.concurrent.CompletableFuture
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

class CustomRoutes: OasStubRoutesBuilder {
    override fun build(routes: OasStubRoutes) {
        routes {
            context("custom") {
                get("/ok1") { _ -> Mono.just("OK") }
                get("/ok2") { _ -> Flux.fromIterable(listOf("OK", "OK", "OK")) }
                get("/ok3") { _ -> CompletableFuture.completedFuture("OK") }
                get("/ok4") { _ -> "OK" }
            }
        }
    }
}