package io.github.ktakashi.oas.server.http

import com.fasterxml.jackson.databind.ObjectMapper
import io.github.ktakashi.oas.api.http.Connection
import io.netty.handler.codec.http.HttpHeaderNames
import io.netty.handler.codec.http.QueryStringDecoder
import java.io.InputStream
import java.net.HttpCookie
import java.net.URI
import java.util.concurrent.CompletableFuture
import org.reactivestreams.Publisher
import reactor.core.publisher.Mono
import reactor.netty.http.server.HttpServerRequest
import reactor.netty.http.server.HttpServerResponse

private typealias NConnection = reactor.netty.Connection

internal class OasStubServerConnection(private val connection: NConnection): Connection {
    override fun close() {
        connection.dispose()
    }
}

internal class OasStubServerHttpRequest(private val request: HttpServerRequest,
                                        private val response: HttpServerResponse,
                                        private val objectMapper: ObjectMapper): RouterHttpRequest {
    private val uri = URI.create(request.uri())
    private val qp by lazy { QueryStringDecoder(request.uri()).parameters() }
    override val connection by lazy { OasStubServerConnection(request as NConnection) }

    override val requestURI: String
        get() = uri.path
    override val method: String
        get() = request.method().name()
    override val contentType: String?
        get() = request.requestHeaders()[HttpHeaderNames.CONTENT_TYPE]
    override val cookies: List<HttpCookie>
        get() = request.cookies().flatMap { cookies ->
            cookies.value.map { HttpCookie(it.name(), it.value()) }
        }
    override val queryString: String?
        get() = uri.query
    override val queryParameters: Map<String, List<String>>
        get() = qp

    override fun getHeader(name: String): String? = request.requestHeaders()[name]

    override fun getHeaders(name: String): List<String> = request.requestHeaders().getAll(name)

    override val headerNames: Collection<String>
        get() = request.requestHeaders().names()

    override fun param(name: String) = request.param(name)

    override fun bodyToInputStream(): Mono<InputStream> = request.receive()
        .aggregate()
        .asInputStream()
        .switchIfEmpty(Mono.just(InputStream.nullInputStream()))

    override fun <T> bodyToMono(type: Class<T>): Mono<T> = bodyToInputStream()
        .map { objectMapper.readValue(it, type) }

    override fun responseBuilder(): HttpRouterResponseBuilder = OasStubServerResponseBuilder(response)
}

internal class OasStubServerResponseBuilder(response: HttpServerResponse)
    : HttpRouterResponseBuilder, HttpRouterResponseHeaderBuilder, HttpRouterResponseBodyBuilder {
    private val buildingResponse = OasStubServerHttpResponse(response)
    override fun status(status: Int): HttpRouterResponseHeaderBuilder = apply { buildingResponse.status = status }

    override fun header(name: String, value: String): HttpRouterResponseHeaderBuilder = apply { buildingResponse.addHeader(name, value) }

    override fun build(): RouterHttpResponse = buildingResponse

    override fun body(body: Any?): RouterHttpResponse = buildingResponse.apply {
        this.body = body
    }


}