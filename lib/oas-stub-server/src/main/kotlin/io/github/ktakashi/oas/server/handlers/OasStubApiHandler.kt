package io.github.ktakashi.oas.server.handlers

import io.github.ktakashi.oas.api.http.HttpRequest
import io.github.ktakashi.oas.api.http.HttpResponse
import io.github.ktakashi.oas.api.http.ResponseContext
import io.github.ktakashi.oas.engine.apis.ApiContext
import io.github.ktakashi.oas.engine.apis.ApiDelayService
import io.github.ktakashi.oas.engine.apis.ApiExecutionService
import io.github.ktakashi.oas.engine.apis.monitor.ApiObserver
import io.github.ktakashi.oas.engine.models.mergeProperty
import io.github.ktakashi.oas.model.ApiCommonConfigurations
import io.github.ktakashi.oas.model.ApiMetric
import io.github.ktakashi.oas.server.api.OasStubApiForwardingResolver
import io.github.ktakashi.oas.server.http.OasStubServerHttpRequest
import io.github.ktakashi.oas.server.http.OasStubServerHttpResponse
import io.github.ktakashi.oas.server.options.OasStubStubOptions
import io.netty.handler.codec.http.HttpResponseStatus
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
import tools.jackson.databind.json.JsonMapper

private val logger = LoggerFactory.getLogger(OasStubApiHandler::class.java)

open class OasStubApiHandler: KoinComponent, BiFunction<HttpServerRequest, HttpServerResponse, Publisher<Void>> {
    private val apiExecutionService by inject<ApiExecutionService>()
    private val apiObserver by inject<ApiObserver>()
    private val apiDelayService by inject<ApiDelayService>()
    private val jsonMapper by inject<JsonMapper>()

    override fun apply(request: HttpServerRequest, response: HttpServerResponse): Publisher<Void> {
        logger.debug("Request {} {}", request.method(), request.uri())
        val now = OffsetDateTime.now()
        val ref = AtomicReference<ApiContext>()
        val newRequest = newOasStubServerHttpRequest(request, response, jsonMapper)
        val newResponse = newOasStubServerHttpResponse(response)

        return apiExecutionService.getApiContext(newRequest)
            .flatMap { context ->
                ref.set(context)
                apiExecutionService.executeApi(context, newRequest, newResponse)
                    .transform { execution -> apiDelayService.delayMono(context, execution) }
            }.flatMap { responseContext ->
                logger.debug("Response: {}", responseContext)
                val content = responseContext.emitResponse(newResponse)
                Mono.just(response.sendHeaders().sendByteArray(content))
                    .doOnSuccess {
                        ref.get()?.let { context ->
                            report(context, request, responseContext, now)
                        }
                    }.doOnError { e ->
                        ref.get()?.let { context ->
                            report(context, request, responseContext, now, e)
                        }
                    }
            }.switchIfEmpty(Mono.defer { Mono.just(response.status(HttpResponseStatus.NOT_FOUND).sendHeaders()) })
            .flatMap { outbound -> outbound.then() }
            // Return 500 if the connection is still alive (we don't have to check)
            .onErrorResume { response.status(HttpResponseStatus.INTERNAL_SERVER_ERROR).send() }
    }

    protected open fun newOasStubServerHttpResponse(response: HttpServerResponse): HttpResponse =
        OasStubServerHttpResponse(response)

    protected open fun newOasStubServerHttpRequest(request: HttpServerRequest, response: HttpServerResponse, jsonMapper: JsonMapper): HttpRequest =
        OasStubServerHttpRequest(request, response, jsonMapper)

    private fun report(context: ApiContext, request: HttpServerRequest, response: ResponseContext, start: OffsetDateTime, e: Throwable? = null) {
        fun rec() {
            val end = OffsetDateTime.now()
            val metric = ApiMetric(start, Duration.between(start, end), context.apiPath, request.method().name(), response.status, e)
            apiObserver.addApiMetric(context.context, context.apiPath, metric)
        }
        logger.debug("Response {}", response.status, e)
        context.mergeProperty(ApiCommonConfigurations<*>::options)?.let { options ->
            if (options.shouldMonitor == null || options.shouldMonitor == true) {
                rec()
            }
        } ?: rec()
    }
}

class OasStubForwardingApiHandler(private val options: OasStubStubOptions): OasStubApiHandler() {
    override fun newOasStubServerHttpRequest(request: HttpServerRequest, response: HttpServerResponse, jsonMapper: JsonMapper): HttpRequest =
        OasStubServerForwardingHttpRequest(options, request, response, jsonMapper)
}

internal class OasStubServerForwardingHttpRequest(private val options: OasStubStubOptions,
                                                  request: HttpServerRequest,
                                                  response: HttpServerResponse,
                                                  jsonMapper: JsonMapper): OasStubServerHttpRequest(request, response, jsonMapper) {
    private val resolvedURI: String? by lazy {
        val context = ForwardingContext(this, super.requestURI, options.stubPath)
        options.forwardingResolvers.asSequence().mapNotNull { it.resolveRequestUri(context) }.firstOrNull()
    }
    override val requestURI: String
        get() = resolvedURI ?: super.requestURI

    private class ForwardingContext(private val request: HttpRequest,
                                    override val requestUri: String,
                                    override val stubPrefix: String): OasStubApiForwardingResolver.Context {
        override val method: String
            get() = request.method
        override fun getHeader(name: String): String? = request.getHeader(name)
    }
}