package io.github.ktakashi.oas.server.api

import io.github.ktakashi.oas.api.http.HttpRequest
import io.github.ktakashi.oas.server.options.OasStubStubOptions
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

fun interface OasStubApiForwardingResolver {
    fun resolveRequestUri(requestUri: String, request: HttpRequest, options: OasStubStubOptions): String?
}

class OasStubApiByHeaderForwardingResolver(private val headerName: String) : OasStubApiForwardingResolver {
    override fun resolveRequestUri(requestUri: String, request: HttpRequest, options: OasStubStubOptions): String? = request.getHeader(headerName)?.let { name ->
        // simply append
        "${options.stubPath}/${URLEncoder.encode(name, StandardCharsets.UTF_8)}$requestUri"
    }
}