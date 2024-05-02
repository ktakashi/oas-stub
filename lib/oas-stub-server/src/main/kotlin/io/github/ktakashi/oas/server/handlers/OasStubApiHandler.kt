package io.github.ktakashi.oas.server.handlers

import io.github.ktakashi.oas.engine.apis.ApiContext
import io.github.ktakashi.oas.engine.apis.ApiDelayService
import io.github.ktakashi.oas.engine.apis.ApiExecutionService
import io.github.ktakashi.oas.engine.apis.monitor.ApiObserver
import io.github.ktakashi.oas.engine.models.ModelPropertyUtils
import io.github.ktakashi.oas.model.ApiCommonConfigurations
import io.github.ktakashi.oas.model.ApiMetric
import io.github.ktakashi.oas.plugin.apis.HttpRequest
import io.github.ktakashi.oas.plugin.apis.HttpResponse
import io.github.ktakashi.oas.plugin.apis.ResponseContext
import io.netty.buffer.ByteBuf
import io.netty.handler.codec.http.HttpHeaderNames
import io.netty.handler.codec.http.HttpHeaderValues
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.net.HttpCookie
import java.net.URI
import java.time.Duration
import java.time.OffsetDateTime
import java.util.concurrent.atomic.AtomicReference
import java.util.function.BiFunction
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.reactivestreams.Publisher
import org.slf4j.LoggerFactory
import reactor.core.publisher.Mono
import reactor.netty.http.server.HttpServerRequest
import reactor.netty.http.server.HttpServerResponse

private val logger = LoggerFactory.getLogger(OasStubApiHandler::class.java)

class OasStubApiHandler: KoinComponent, BiFunction<HttpServerRequest, HttpServerResponse, Publisher<Void>> {
    private val apiExecutionService by inject<ApiExecutionService>()
    private val apiObserver by inject<ApiObserver>()
    private val apiDelayService by inject<ApiDelayService>()

    override fun apply(request: HttpServerRequest, response: HttpServerResponse): Publisher<Void> {
        logger.debug("Request {} {}", request.method(), request.uri())
        val now = OffsetDateTime.now()
        val ref = AtomicReference<ApiContext>()
        val outputStream = ByteArrayOutputStream()
        val newResponse = ServerHttpResponse(response, outputStream)

        return request.receiveContent()
            .map { content -> content.content() }
            .collectList()
            .map { body -> ServerHttpRequest(request, ByteBufListInputStream(body)) }
            .switchIfEmpty(Mono.defer { Mono.just(ServerHttpRequest(request, InputStream.nullInputStream())) })
            .flatMap { newRequest ->
                apiExecutionService.getApiContext(newRequest)
                    .flatMap { context ->
                        ref.set(context)
                        apiExecutionService.executeApi(context, newRequest, newResponse)
                            .transform { execution -> apiDelayService.delayMono(context, execution) }
                    }.flatMap { responseContext ->
                        responseContext.emitResponse(newResponse)
                        Mono.just(response.sendHeaders().sendByteArray(Mono.just(outputStream.toByteArray())))
                            .doOnSuccess {
                                ref.get()?.let { context ->
                                    report(context, request, responseContext, now)
                                }
                            }.doOnError { e ->
                                ref.get()?.let { context ->
                                    report(context, request, responseContext, now, e)
                                }
                            }
                    }.switchIfEmpty(Mono.defer { Mono.just(response.status(404).sendHeaders()) })
                    .flatMap { outbound -> outbound.then() }
            }
    }
    private fun report(context: ApiContext, request: HttpServerRequest, response: ResponseContext, start: OffsetDateTime, e: Throwable? = null) {
        fun rec() {
            val end = OffsetDateTime.now()
            val metric = ApiMetric(start, Duration.between(start, end), context.apiPath, request.method().name(), response.status, e)
            apiObserver.addApiMetric(context.context, context.apiPath, metric)
        }
        logger.debug("Response {}", response.status, e)
        ModelPropertyUtils.mergeProperty(context.apiPath, context.apiDefinitions, ApiCommonConfigurations<*>::options)?.let { options ->
            if (options.shouldMonitor == null || options.shouldMonitor == true) {
                rec()
            }
        } ?: rec()
    }
}

private class ServerHttpRequest(private val request: HttpServerRequest, override val inputStream: InputStream): HttpRequest {
    override val requestURI: String
        get() = request.uri()
    override val method: String
        get() = request.method().name()
    override val contentType: String?
        get() = request.requestHeaders()[HttpHeaderNames.CONTENT_TYPE]
    override val cookies: List<HttpCookie>
        get() = request.cookies().flatMap { cookies ->
            cookies.value.map { HttpCookie(it.name(), it.value()) }
        }
    override val queryString: String?
        get() = URI.create(request.uri()).query

    override fun getHeader(name: String): String? = request.requestHeaders()[name]

    override fun getHeaders(name: String): List<String> = request.requestHeaders().getAll(name)

    override val headerNames: Collection<String>
        get() = request.requestHeaders().names()
}

private class ServerHttpResponse(private val response: HttpServerResponse, override val outputStream: OutputStream): HttpResponse {
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

private class ByteBufListInputStream(private val bytebufs: List<ByteBuf>): InputStream() {
    private var index = 0;
    private var pos = 0

    // TODO implement read(byte[], int, int)
    override fun read(): Int = readInternal()

    private fun readInternal(): Int = if (index < bytebufs.size) {
        val buf = bytebufs[index]
        if (pos < buf.readerIndex()) {
            buf.getByte(pos++).toInt()
        } else {
            index++
            pos = 0
            readInternal()
        }
    } else {
        -1
    }
}
