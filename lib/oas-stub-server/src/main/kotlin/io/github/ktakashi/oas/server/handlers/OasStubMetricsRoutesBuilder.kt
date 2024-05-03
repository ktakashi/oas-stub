package io.github.ktakashi.oas.server.handlers

import io.github.ktakashi.oas.engine.apis.monitor.ApiObserver
import io.github.ktakashi.oas.server.options.OasStubServerOptions
import io.netty.handler.codec.http.HttpHeaderNames
import io.netty.handler.codec.http.HttpHeaderValues
import io.netty.handler.codec.http.HttpResponseStatus
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import reactor.core.publisher.Mono
import reactor.netty.http.server.HttpServerRoutes

class OasStubMetricsRoutesBuilder(private val options: OasStubServerOptions): KoinComponent {
    private val apiObserver by inject<ApiObserver>()

    fun build(routes: HttpServerRoutes) {
        if (options.enableMetrics) {
            routes.get("${options.metricsPath}/$PATH_SEGMENT") { request, response ->
                request.param(PATH_VARIABLE_NAME)?.let { context ->
                    Mono.justOrEmpty(apiObserver.getApiMetrics(context))
                        .map {
                            response.status(HttpResponseStatus.OK)
                                .header(HttpHeaderNames.CONTENT_TYPE, HttpHeaderValues.APPLICATION_JSON)
                                .sendObject(it as Any)
                        }.switchIfEmpty(Mono.defer { Mono.just(notFound(response)) })
                        .flatMap { r -> r.then() }
                } ?: sendNotFound(response)
            }.delete(options.metricsPath) { _, response ->
                apiObserver.clearApiMetrics()
                sendNoContent(response)
            }
        }
    }
}