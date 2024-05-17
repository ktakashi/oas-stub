package io.github.ktakashi.oas.server.handlers

import com.fasterxml.jackson.databind.ObjectMapper
import io.github.ktakashi.oas.api.http.ResponseContext
import io.github.ktakashi.oas.engine.apis.ApiContext
import io.github.ktakashi.oas.engine.apis.ApiDelayService
import io.github.ktakashi.oas.engine.apis.ApiExecutionService
import io.github.ktakashi.oas.engine.apis.monitor.ApiObserver
import io.github.ktakashi.oas.engine.models.ModelPropertyUtils
import io.github.ktakashi.oas.model.ApiCommonConfigurations
import io.github.ktakashi.oas.model.ApiMetric
import io.github.ktakashi.oas.server.http.OasStubServerHttpRequest
import io.github.ktakashi.oas.server.http.OasStubServerHttpResponse
import io.netty.handler.codec.http.HttpResponseStatus
import java.io.ByteArrayOutputStream
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
    private val objectMapper by inject<ObjectMapper>()

    override fun apply(request: HttpServerRequest, response: HttpServerResponse): Publisher<Void> {
        logger.debug("Request {} {}", request.method(), request.uri())
        val now = OffsetDateTime.now()
        val ref = AtomicReference<ApiContext>()
        val newRequest = OasStubServerHttpRequest(request, response, objectMapper)
        val newResponse = OasStubServerHttpResponse(response)

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

