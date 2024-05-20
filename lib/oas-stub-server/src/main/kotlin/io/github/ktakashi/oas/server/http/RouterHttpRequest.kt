package io.github.ktakashi.oas.server.http

import io.github.ktakashi.oas.api.http.HttpRequest
import io.github.ktakashi.oas.api.http.HttpResponse
import java.net.HttpURLConnection

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
    fun ok() = status(HttpURLConnection.HTTP_OK)

    /**
     * HTTP Created
     */
    fun created(path: String) = status(HttpURLConnection.HTTP_CREATED).header("Location", path)

    /**
     * HTTP Accepted
     */
    fun accepted() = status(HttpURLConnection.HTTP_ACCEPTED)

    /**
     * HTTP No Content
     */
    fun noContent() = status(HttpURLConnection.HTTP_NO_CONTENT)

    /**
     * HTTP Moved permanently
     */
    fun movedPermanently(location: String) = status(HttpURLConnection.HTTP_MOVED_PERM).header("Location", location)

    /**
     * HTTP Moved temporary
     */
    fun movedTemporary(location: String) = status(HttpURLConnection.HTTP_MOVED_TEMP).header("Location", location)

    /**
     * HTTP See Other
     */
    fun seeOther(location: String) = status(HttpURLConnection.HTTP_SEE_OTHER).header("Location", location)


    /**
     * HTTP Bad request
     */
    fun badRequest() = status(HttpURLConnection.HTTP_BAD_REQUEST)

    /**
     * HTTP unauthorized
     */
    fun unauthorized() = status(HttpURLConnection.HTTP_UNAUTHORIZED)

    /**
     * HTTP forbidden
     */
    fun forbidden() = status(HttpURLConnection.HTTP_FORBIDDEN)

    /**
     * HTTP Not found
     */
    fun notFound() = status(HttpURLConnection.HTTP_NOT_FOUND)

    /**
     * HTTP Method Not Allowed
     */
    fun methodNotAllowed() = status(HttpURLConnection.HTTP_BAD_METHOD)

    /**
     * HTTP Not Acceptable
     */
    fun notAcceptable() = status(HttpURLConnection.HTTP_NOT_ACCEPTABLE)

    /**
     * HTTP Internal Server Error
     */
    fun internalServerError() = status(HttpURLConnection.HTTP_INTERNAL_ERROR)

    /**
     * HTTP Bad Gateway
     */
    fun badGateway() = status(HttpURLConnection.HTTP_BAD_GATEWAY)

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