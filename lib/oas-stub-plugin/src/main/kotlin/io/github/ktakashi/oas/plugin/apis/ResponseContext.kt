package io.github.ktakashi.oas.plugin.apis

import java.util.Optional

data class ResponseContext(val status: Int,
                           val content: Optional<ByteArray> = Optional.empty(),
                           val contentType: Optional<String> = Optional.empty(),
                           val headers: Map<String, List<String>> = mapOf()) {
    fun from() = ResponseContextBuilder(status, content, contentType, headers)

    data class ResponseContextBuilder(val status: Int,
                                      val content: Optional<ByteArray>,
                                      val contentType: Optional<String>,
                                      val headers: Map<String, List<String>>) {
        fun status(status: Int) = ResponseContextBuilder(status, content, contentType, headers)
        fun content(content: ByteArray?) = ResponseContextBuilder(status, Optional.ofNullable(content), contentType, headers)
        fun contentType(contentType: String?) = ResponseContextBuilder(status, content, Optional.ofNullable(contentType), headers)
        fun headers(headers: Map<String, List<String>>) = ResponseContextBuilder(status, content, contentType, headers)

        fun build() = ResponseContext(status, content, contentType, headers)
    }
}
