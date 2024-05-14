package io.github.ktakashi.oas.server.http

import io.github.ktakashi.oas.api.http.HttpRequest
import io.github.ktakashi.oas.api.http.HttpResponse

/**
 * Router HTTP request.
 *
 * This is passed to custom routes
 */
interface RouterHttpRequest: HttpRequest {
    /**
     * Retrieves path variable of [name]
     */
    fun param(name: String): String?

    /**
     * Returns a [HttpRouterResponseBuilder]
     */
    fun responseBuilder(): HttpRouterResponseBuilder
}

/**
 * Router HTTP response.
 */
interface RouterHttpResponse: HttpResponse {
    var body: Any?
}

/**
 * Router HTTP response builder.
 *
 * ```Kotlin
 * request.responseBuilder()
 *   .ok()
 *   .body("OK")
 * ```
 */
interface HttpRouterResponseBuilder {
    /**
     * HTTP OK
     */
    fun ok() = status(200)

    /**
     * HTTP Created
     */
    fun created(path: String) = status(201).header("Location", path)

    /**
     * HTTP No Content
     */
    fun noContent() = status(204)

    /**
     * HTTP Not found
     */
    fun notFound() = status(404)

    /**
     * HTTP Method Not Allowed
     */
    fun methodNotAllowed() = status(405)

    /**
     * Returns [HttpRouterResponseHeaderBuilder] with [status]
     */
    fun status(status: Int): HttpRouterResponseHeaderBuilder
}

interface HttpRouterResponseHeaderBuilder: HttpRouterResponseBodyBuilder {
    /**
     * Add HTTP header of name [name] and value [value]
     */
    fun header(name: String, value: String): HttpRouterResponseHeaderBuilder

    /**
     * Builds [RouterHttpResponse] without content
     */
    fun build(): RouterHttpResponse
}

fun interface HttpRouterResponseBodyBuilder {
    /**
     * Builds a [RouterHttpResponse] with content of [body]
     */
    fun body(body: Any?): RouterHttpResponse
}