package io.github.ktakashi.oas.plugin.apis

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import java.util.Optional

interface RequestContext {
    val applicationName: String
    val apiPath: String
    val method: String
    val content: Optional<ByteArray>
    val contentType: Optional<String>
    val attributes: Map<String, Any>
    val headers: Map<String, List<String>>
    val queryParameters: Map<String, List<String?>>
    fun getQueryParameters(key: String) = queryParameters.getOrDefault(key, listOf())
    val rawRequest: HttpServletRequest
    val rawResponse: HttpServletResponse
}
