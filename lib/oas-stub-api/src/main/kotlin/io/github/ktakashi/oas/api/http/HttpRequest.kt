package io.github.ktakashi.oas.api.http

import java.io.InputStream
import java.net.HttpCookie
import reactor.core.publisher.Mono

/**
 * Wrapper of native Http request
 */
interface HttpRequest {
    /**
     * request URI
     */
    val requestURI: String

    /**
     * request method
     */
    val method: String

    /**
     * Content-Type header value
     */
    val contentType: String?

    /**
     * Cookies
     */
    val cookies: List<HttpCookie>

    /**
     * Raw query string
     */
    val queryString: String?

    /**
     * Parsed query parameters
     */
    val queryParameters: Map<String, List<String>>

    /**
     * Retrieve the first [name] HTTP header value
     */
    fun getHeader(name: String): String?

    /**
     * Retrieve all the [name] HTTP header values
     */
    fun getHeaders(name: String): List<String>

    /**
     * Retrieve all HTTP header names
     */
    val headerNames: Collection<String>

    /**
     * Convert request body to [Mono] of [InputStream]
     */
    fun bodyToInputStream(): Mono<InputStream>

    /**
     * Convert request body to [Mono] of [T]
     */
    fun <T> bodyToMono(type: Class<T>): Mono<T>

    /**
     * Connection
     */
    val connection: Connection
}

inline fun <reified T> HttpRequest.body() = this.bodyToMono(T::class.java)