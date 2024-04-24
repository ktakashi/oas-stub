package io.github.ktakashi.oas.web.reactive

import org.springframework.web.reactive.function.server.RequestPredicates
import org.springframework.web.reactive.function.server.RouterFunction
import org.springframework.web.reactive.function.server.RouterFunctions
import org.springframework.web.reactive.function.server.RouterFunctions.Builder
import org.springframework.web.reactive.function.server.ServerResponse

fun interface RouterFunctionBuilder {
    fun build(builder: Builder): Builder
}

class RouterFunctionFactory(private val oasStubApiHandler: OasStubApiHandler) {
    fun buildRouterFunction(path: String, builders: Set<RouterFunctionBuilder>): RouterFunction<ServerResponse> = RouterFunctions.route()
        .add(RouterFunctions.route(RequestPredicates.path(path)) { request ->
            oasStubApiHandler.handleStubApiExecution(request)
        }).also { builder ->
            builders.fold(builder) { acc, b -> b.build(acc) }
        }.build()
}
