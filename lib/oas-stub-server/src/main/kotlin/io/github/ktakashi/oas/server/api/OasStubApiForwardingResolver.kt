package io.github.ktakashi.oas.server.api

import io.github.ktakashi.oas.api.http.HttpRequest
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

/**
 * API name resolver.
 *
 * This resolver is used when the `forwardingPath` option
 * is provided.
 */
fun interface OasStubApiForwardingResolver {
    interface Context {
        val requestUri: String
        val method: String
        val stubPrefix: String
        fun getHeader(name: String): String?
        fun buildPath(name: String) = buildPath(name, requestUri)
        fun buildPath(name: String, path: String) = "$stubPrefix/${URLEncoder.encode(name, StandardCharsets.UTF_8)}$path"
    }
    /**
     * Resolve the stub name.
     *
     * [context]: API endpoint context
     */
    fun resolveRequestUri(context: Context): String?
}

/**
 * Header value forwarding resolver.
 *
 * The value of the [headerName] will be the stub name.
 */
class OasStubApiByHeaderForwardingResolver(private val headerName: String) : OasStubApiForwardingResolver {
    override fun resolveRequestUri(context: OasStubApiForwardingResolver.Context): String? = context.getHeader(headerName)?.let { name ->
        context.buildPath(name)
    }
}