package io.github.ktakashi.oas.test.cucumber

import io.github.ktakashi.oas.api.http.body
import io.github.ktakashi.oas.server.handlers.OasStubRoutes
import io.github.ktakashi.oas.server.handlers.OasStubRoutesBuilder
import java.net.HttpURLConnection
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
                put("/status") { req ->
                    req.body<StatusRequest>().map { status ->
                        val responseBuilder = req.responseBuilder()
                        when (status.status) {
                            HttpURLConnection.HTTP_OK -> responseBuilder.ok().build()
                            HttpURLConnection.HTTP_CREATED -> responseBuilder.created("dummy").build()
                            HttpURLConnection.HTTP_ACCEPTED -> responseBuilder.accepted().build()
                            HttpURLConnection.HTTP_NO_CONTENT -> responseBuilder.noContent().build()
                            HttpURLConnection.HTTP_MOVED_PERM -> responseBuilder.movedPermanently("dummy").build()
                            HttpURLConnection.HTTP_MOVED_TEMP -> responseBuilder.movedTemporary("dummy").build()
                            HttpURLConnection.HTTP_SEE_OTHER -> responseBuilder.seeOther("dummy").build()
                            HttpURLConnection.HTTP_BAD_REQUEST -> responseBuilder.badRequest().build()
                            HttpURLConnection.HTTP_UNAUTHORIZED -> responseBuilder.unauthorized().build()
                            HttpURLConnection.HTTP_FORBIDDEN -> responseBuilder.forbidden().build()
                            HttpURLConnection.HTTP_NOT_FOUND -> responseBuilder.notFound().build()
                            HttpURLConnection.HTTP_BAD_METHOD -> responseBuilder.methodNotAllowed().build()
                            HttpURLConnection.HTTP_NOT_ACCEPTABLE -> responseBuilder.notAcceptable().build()
                            HttpURLConnection.HTTP_INTERNAL_ERROR -> responseBuilder.internalServerError().build()
                            HttpURLConnection.HTTP_BAD_GATEWAY -> responseBuilder.badGateway().build()
                            else -> responseBuilder.status(418).build()
                        }
                    }
                }
            }
        }
    }
}

data class StatusRequest(val status: Int)