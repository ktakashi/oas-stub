package io.github.ktakashi.oas.test.cucumber

import io.github.ktakashi.oas.server.handlers.OasStubRoutes
import io.github.ktakashi.oas.server.handlers.OasStubRoutesBuilder
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

class CustomRoutes: OasStubRoutesBuilder {
    override fun build(routes: OasStubRoutes) {
        routes {
            get("/custom/ok1") { request ->
                Mono.just(request.responseBuilder().ok().body("OK"))
            }
            get("/custom/ok2") { request ->
                Mono.just(request.responseBuilder().ok()
                    .body(Flux.fromIterable(listOf("OK", "OK", "OK"))))
            }
            get("/custom/ok3") { request ->
                Mono.just(request.responseBuilder().ok().body("OK"))
            }
            get("/custom/ok4") { request ->
                Mono.just(request.responseBuilder().ok().body("OK"))
            }
        }
    }
}