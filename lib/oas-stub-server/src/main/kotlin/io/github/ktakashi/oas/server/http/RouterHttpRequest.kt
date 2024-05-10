package io.github.ktakashi.oas.server.http

import io.github.ktakashi.oas.api.http.HttpRequest
import io.github.ktakashi.oas.api.http.HttpResponse

interface RouterHttpRequest: HttpRequest {
    fun param(name: String): String?
    fun responseBuilder(): HttpRouterResponseBuilder
}

interface RouterHttpResponse: HttpResponse {
    var body: Any?
}

interface HttpRouterResponseBuilder {
    fun ok() = status(200)
    fun created(path: String) = status(201).header("Location", path)
    fun noContent() = status(204)

    fun notFound() = status(404)
    fun methodNotAllowed() = status(405)

    fun status(status: Int): HttpRouterResponseHeaderBuilder
}

interface HttpRouterResponseHeaderBuilder: HttpRouterResponseBodyBuilder {
    fun header(name: String, value: String): HttpRouterResponseHeaderBuilder
    fun build(): RouterHttpResponse
}

fun interface HttpRouterResponseBodyBuilder {
    fun body(body: Any?): RouterHttpResponse
}