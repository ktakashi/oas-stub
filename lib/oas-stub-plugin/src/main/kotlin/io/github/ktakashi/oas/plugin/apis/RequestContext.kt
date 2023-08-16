package io.github.ktakashi.oas.plugin.apis

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import java.net.HttpCookie
import java.util.Optional

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
     * Raw [jakarta.servlet.http.HttpServletRequest].
     */
    val rawRequest: HttpServletRequest
    /**
     * Raw [jakarta.servlet.http.HttpServletResponse].
     *
     * CAUTION: Modifying the response will affect the result. e.g. write value to [HttpServletResponse.getOutputStream]
     */
    val rawResponse: HttpServletResponse
}
