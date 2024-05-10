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
    val contentType: String?
    val cookies: List<HttpCookie>
    val queryString: String?
    val queryParameters: Map<String, List<String>>

    fun getHeader(name: String): String?
    fun getHeaders(name: String): List<String>
    val headerNames: Collection<String>

    fun bodyToInputStream(): Mono<InputStream>
    fun <T> bodyToMono(type: Class<T>): Mono<T>
}