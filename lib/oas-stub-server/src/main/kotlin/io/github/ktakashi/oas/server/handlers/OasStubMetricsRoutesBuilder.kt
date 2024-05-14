package io.github.ktakashi.oas.server.handlers

import io.github.ktakashi.oas.engine.apis.monitor.ApiObserver
import io.github.ktakashi.oas.server.options.OasStubStubOptions
import io.netty.handler.codec.http.HttpHeaderNames
import io.netty.handler.codec.http.HttpHeaderValues
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import reactor.core.publisher.Mono

class OasStubMetricsRoutesBuilder(private val options: OasStubStubOptions): OasStubRoutesBuilder, KoinComponent {
    private val apiObserver by inject<ApiObserver>()

    override fun build(routes: OasStubRoutes) {
        if (options.enableMetrics) {
            routes.get("${options.adminPath}${options.metricsPath}/$PATH_SEGMENT") { request ->
                Mono.just(request.param(PATH_VARIABLE_NAME)?.let { context ->
                    apiObserver.getApiMetrics(context).map { metrics ->
                        request.responseBuilder()
                            .ok()
                            .header(HttpHeaderNames.CONTENT_TYPE.toString(), HttpHeaderValues.APPLICATION_JSON.toString())
                            .body(metrics)
                    }.orElseGet { request.responseBuilder().notFound().build() }
                } ?: request.responseBuilder().notFound().build())
            }.delete("${options.adminPath}${options.metricsPath}") { request ->
                apiObserver.clearApiMetrics()
                Mono.just(request.responseBuilder().noContent().build())
            }
        }
    }
}