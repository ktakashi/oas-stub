package io.github.ktakashi.oas.plugin.apis

import jakarta.servlet.http.HttpServletResponse
import java.util.Optional

interface ResponseContext {
    val status: Int
    val content: Optional<ByteArray>
    val contentType: Optional<String>
    val headers: Map<String, List<String>>
    fun from(): ResponseContextBuilder
    fun emitResponse(response: HttpServletResponse)

    interface ResponseContextBuilder {
        fun status(status: Int): ResponseContextBuilder
        fun content(content: ByteArray?): ResponseContextBuilder
        fun contentType(contentType: String?): ResponseContextBuilder
        fun headers(headers: Map<String, List<String>>): ResponseContextBuilder

        fun build(): ResponseContext
    }
}
