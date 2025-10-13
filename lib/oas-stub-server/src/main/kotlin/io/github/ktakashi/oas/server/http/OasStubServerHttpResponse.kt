package io.github.ktakashi.oas.server.http

import io.netty.handler.codec.http.HttpHeaderNames
import io.netty.handler.codec.http.HttpHeaderValues
import reactor.netty.http.server.HttpServerResponse

internal class OasStubServerHttpResponse(private val response: HttpServerResponse,
                                         override var body: Any? = null): RouterHttpResponse {
    override var status: Int
        get() = response.status().code()
        set(value) {
            response.status(value)
        }
    override var contentType: String
        get() = HttpHeaderValues.APPLICATION_JSON.toString()
        set(value) {
            response.addHeader(HttpHeaderNames.CONTENT_TYPE, value)
        }

    override fun addHeader(name: String, value: String) {
        response.addHeader(name, value)
    }
}
