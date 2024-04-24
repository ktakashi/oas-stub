package io.github.ktakashi.oas.plugin.apis

import java.io.InputStream
import java.io.OutputStream
import java.net.HttpCookie
import java.util.Optional

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
    val inputStream: InputStream

    fun getHeader(name: String): String?
    fun getHeaders(name: String): List<String>
    val headerNames: Collection<String>
}

/**
 * Wrapper of native Http response
 */
interface HttpResponse {
    var status: Int
    var contentType: String
    fun addHeader(name: String, value: String)
    val outputStream: OutputStream
}


/**
 * Request context.
 *
 * This interface represents HTTP requests of defined APIs
 */
interface RequestContext {
    /**
     * Application name
     */
    val applicationName: String

    /**
     * The request path.
     *
     * The value doesn't contain application's prefix.
     */
    val apiPath: String

    /**
     * Http method name
     */
    val method: String

    /**
     * Request body if exists
     */
    val content: Optional<ByteArray>

    /**
     * Content type header if exists
     */
    val contentType: Optional<String>

    /**
     * Request headers map.
     */
    val headers: Map<String, List<String>>

    /**
     * Cookies
     */
    val cookies: Map<String, HttpCookie>

    /**
     * Query parameters
     */
    val queryParameters: Map<String, List<String?>>

    /**
     * Raw request
     */
    val rawRequest: HttpRequest
    /**
     * Raw response
     *
     * CAUTION: Modifying the response will affect the result. e.g. write value to [HttpServletResponse.getOutputStream]
     */
    val rawResponse: HttpResponse
}
