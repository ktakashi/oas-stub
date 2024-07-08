package io.github.ktakashi.oas.server.api

import io.github.ktakashi.oas.api.http.HttpRequest
import io.github.ktakashi.oas.server.options.OasStubStubOptions
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

/**
 * API name resolver.
 *
 * This resolver is used when the `forwardingPath` option
 * is provided.
 */
fun interface OasStubApiForwardingResolver {
    /**
     * Resolve the stub name.
     *
     * [requestUri]: the original request URI
     * [request]: an [HttpRequest] instance.
     * [options]: Stub options
     *
     * NOTE: Calling `request.requestURI` causes infinite loop. If you need
     * the original URI, then use the [requestUri].
     */
    fun resolveRequestUri(requestUri: String, request: HttpRequest, options: OasStubStubOptions): String?
}

/**
 * Header value forwarding resolver.
 *
 * The value of the [headerName] will be the stub name.
 */
class OasStubApiByHeaderForwardingResolver(private val headerName: String) : OasStubApiForwardingResolver {
    override fun resolveRequestUri(requestUri: String, request: HttpRequest, options: OasStubStubOptions): String? = request.getHeader(headerName)?.let { name ->
        // simply append
        "${options.stubPath}/${URLEncoder.encode(name, StandardCharsets.UTF_8)}$requestUri"
    }
}