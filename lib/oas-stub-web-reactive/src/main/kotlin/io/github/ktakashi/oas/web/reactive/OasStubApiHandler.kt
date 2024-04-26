package io.github.ktakashi.oas.web.reactive

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
import jakarta.inject.Inject
import jakarta.inject.Singleton
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.net.HttpCookie
import java.time.Duration
import java.time.OffsetDateTime
import java.util.concurrent.atomic.AtomicReference
import org.springframework.core.io.buffer.DataBuffer
import org.springframework.core.io.buffer.DataBufferFactory
import org.springframework.core.io.buffer.DataBufferUtils
import org.springframework.core.io.buffer.DefaultDataBufferFactory
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.BodyExtractors
import org.springframework.web.reactive.function.BodyInserters
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import reactor.core.publisher.Mono

@Singleton
class OasStubApiHandler
@Inject constructor(private val apiExecutionService: ApiExecutionService,
                    private val apiDelayService: ApiDelayService,
                    private val apiObserver: ApiObserver) {
    private val dataBufferFactory = DefaultDataBufferFactory()
    fun handleStubApiExecution(request: ServerRequest): Mono<ServerResponse> {
        // TODO delay and observer
        val outputStream = ByteArrayOutputStream()
        val response = ReactiveHttpResponse(outputStream = outputStream)
        val now = OffsetDateTime.now()
        val ref = AtomicReference<ApiContext>(null)
        return request.body(BodyExtractors.toMono(DataBuffer::class.java))
            .switchIfEmpty(Mono.defer { Mono.just(dataBufferFactory.allocateBuffer(0)) })
            .map { ReactiveHttpRequest(request, it.asInputStream()) }
            .flatMap { httpRequest ->
                Mono.justOrEmpty(apiExecutionService.getApiContext(httpRequest))
                    .flatMap { context ->
                        Mono.deferContextual { view ->
                            val holder = view.get("api_context") as AtomicReference<ApiContext>
                            holder.set(context)
                            Mono.just(apiExecutionService.executeApi(context, httpRequest, response))
                        }
                    }
            }.flatMap { responseContext -> ServerResponse.status(responseContext.status)
                .contentType(responseContext.contentType.map(MediaType::parseMediaType).orElse(MediaType.APPLICATION_JSON))
                .headers { headers -> headers.putAll(responseContext.headers) }
                .body(BodyInserters.fromPublisher(Mono.fromSupplier {
                    // TODO make a pipe output stream to DataBuffer or something to emit body
                    responseContext.emitResponse(response)
                    outputStream.toByteArray()
                }, ByteArray::class.java))
                .doOnSuccess {
                    ref.get()?.let { context ->
                        report(context, request, responseContext, now)
                    }
                }.doOnError { e ->
                    ref.get()?.let { context ->
                        report(context, request, responseContext, now, e)
                    }
                }
            }.contextWrite { context ->
                context.put("api_context", ref)
            }
    }

    private fun report(context: ApiContext, request: ServerRequest, response: ResponseContext, start: OffsetDateTime, e: Throwable? = null) {
        fun rec() {
            val end = OffsetDateTime.now()
            val metric = ApiMetric(start, Duration.between(start, end), context.apiPath, request.method().name(), response.status, e)
            apiObserver.addApiMetric(context.context, context.apiPath, metric)
        }
        ModelPropertyUtils.mergeProperty(context.apiPath, context.apiDefinitions, ApiCommonConfigurations<*>::options)?.let { options ->
            if (options.shouldMonitor == null || options.shouldMonitor == true) {
                rec()
            }
        } ?: rec()
    }
}

private class ReactiveHttpRequest(private val request: ServerRequest, private val body: InputStream): HttpRequest {
    override val requestURI: String
        get() = request.uri().path
    override val method: String
        get() = request.method().name()
    override val contentType: String?
        get() = request.headers().contentType().map { it.toString() }.orElse(null)
    override val cookies: List<HttpCookie>
        get() = request.cookies().flatMap { c ->
            c.value.map { v ->
                HttpCookie(v.name, v.value)
            }
        }
    override val queryString: String?
        get() = request.uri().query
    override val inputStream: InputStream
        get() = body

    override fun getHeader(name: String): String? = request.headers().firstHeader(name)

    override fun getHeaders(name: String): List<String> = request.headers().header(name)

    override val headerNames: Collection<String> = request.headers().asHttpHeaders().keys
}

private class ReactiveHttpResponse(override var status: Int = 200,
                                   override var contentType: String = MediaType.APPLICATION_OCTET_STREAM_VALUE,
                                   override val outputStream: OutputStream,
                                   val headers: MutableMap<String, MutableList<String>> = mutableMapOf() ): HttpResponse {

    override fun addHeader(name: String, value: String) {
        if (headers[name]?.add(value) != true) {
            headers[name] = mutableListOf(value)
        }
    }
}
