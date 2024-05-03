package io.github.ktakashi.oas.server.handlers

import com.fasterxml.jackson.databind.ObjectMapper
import io.github.ktakashi.oas.engine.apis.monitor.ApiObserver
import io.github.ktakashi.oas.server.options.OasStubServerOptions
import io.netty.handler.codec.http.HttpHeaderNames
import io.netty.handler.codec.http.HttpHeaderValues
import io.netty.handler.codec.http.HttpResponseStatus
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import reactor.core.publisher.Mono
import reactor.netty.http.server.HttpServerResponse
import reactor.netty.http.server.HttpServerRoutes

class OasStubMetricsRoutesBuilder(private val options: OasStubServerOptions): KoinComponent {
    private val apiObserver by inject<ApiObserver>()
    private val objectMapper by inject<ObjectMapper>()

    fun build(routes: HttpServerRoutes) {
        if (options.enableMetrics) {
            routes.get("${options.adminPath}${options.metricsPath}/$PATH_SEGMENT") { request, response ->
                request.param(PATH_VARIABLE_NAME)?.let { context ->
                    Mono.justOrEmpty(apiObserver.getApiMetrics(context))
                        .map { ok(response, it) }
                        .switchIfEmpty(Mono.defer { Mono.just(notFound(response)) })
                        .flatMap { r -> r.then() }
                } ?: sendNotFound(response)
            }.delete("${options.adminPath}${options.metricsPath}") { _, response ->
                apiObserver.clearApiMetrics()
                sendNoContent(response)
            }
        }
    }

    private fun <T> ok(response: HttpServerResponse, v: T) = response.status(HttpResponseStatus.OK)
        .header(HttpHeaderNames.CONTENT_TYPE, HttpHeaderValues.APPLICATION_JSON)
        .sendString(Mono.fromCallable { objectMapper.writeValueAsString(v) })

}