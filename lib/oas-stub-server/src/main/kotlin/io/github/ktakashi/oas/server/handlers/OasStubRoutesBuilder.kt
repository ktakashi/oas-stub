package io.github.ktakashi.oas.server.handlers

import com.fasterxml.jackson.databind.ObjectMapper
import reactor.netty.http.server.HttpServerRoutes

class OasStubRoutes(private val routes: HttpServerRoutes,
                    private val objectMapper: ObjectMapper) {

}

fun interface OasStubRoutesBuilder {
    fun build(routes: OasStubRoutes)
}