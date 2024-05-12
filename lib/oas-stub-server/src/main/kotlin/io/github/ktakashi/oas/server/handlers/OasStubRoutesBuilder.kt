package io.github.ktakashi.oas.server.handlers

import com.fasterxml.jackson.databind.ObjectMapper
import io.github.ktakashi.oas.engine.apis.ApiRegistrationService
import io.github.ktakashi.oas.model.ApiDefinitions
import io.github.ktakashi.oas.server.http.OasStubServerHttpRequest
import io.github.ktakashi.oas.server.http.RouterHttpRequest
import io.github.ktakashi.oas.server.http.RouterHttpResponse
import io.netty.handler.codec.http.HttpHeaderNames
import io.netty.handler.codec.http.HttpHeaderValues
import java.util.concurrent.CompletionStage
import java.util.function.BiFunction
import java.util.function.Function
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.reactivestreams.Publisher
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.netty.http.server.HttpServerRequest
import reactor.netty.http.server.HttpServerResponse
import reactor.netty.http.server.HttpServerRoutes

fun interface OasStubRouteHandler: Function<RouterHttpRequest, Any>

class OasStubRoutes(private val routes: HttpServerRoutes): (OasStubRoutes.() -> Unit) -> Unit, KoinComponent {
    private val objectMapper: ObjectMapper by inject()
    fun get(path: String, handler: OasStubRouteHandler) = apply { routes.get(path, adjustHandler(handler)) }
    fun post(path: String, handler: OasStubRouteHandler) = apply { routes.post(path, adjustHandler(handler)) }
    fun put(path: String, handler: OasStubRouteHandler) = apply { routes.put(path, adjustHandler(handler)) }
    fun delete(path: String, handler: OasStubRouteHandler) = apply { routes.delete(path, adjustHandler(handler)) }

    fun path(path: String, handler: OasStubRouteHandler) = routes.also {
        it.get(path, adjustHandler(handler))
        it.post(path, adjustHandler(handler))
        it.put(path, adjustHandler(handler))
        it.delete(path, adjustHandler(handler))
    }

    fun context(context: String) = ContextOasStubRoutes(context, this)
    fun context(context: String, init: ContextOasStubRoutes.() -> Unit) = ContextOasStubRoutes(context, this).invoke(init)

    private fun adjustHandler(handler: OasStubRouteHandler): BiFunction<in HttpServerRequest, in HttpServerResponse, Publisher<Void>> {
        return BiFunction { request: HttpServerRequest, response: HttpServerResponse ->
            val newRequest = OasStubServerHttpRequest(request, response, objectMapper)
            // TODO delay
            (when (val r = handler.apply(newRequest)) {
                is Mono<*> -> r.map { v -> ensureResponse(newRequest, v) }
                else -> Mono.just(ensureResponse(newRequest, r))
            }).flatMap { resp -> emitResponse(resp, response) }
        }
    }

    private fun emitResponse(resp: RouterHttpResponse, response: HttpServerResponse): Mono<Void> {
        val content = resp.body?.let {
            when (it) {
                is Mono<*> -> it.mapNotNull { body -> toByteArray(body) }
                is Flux<*> -> it.mapNotNull { body -> toByteArray(body) }
                is CompletionStage<*> -> Mono.fromFuture(it.toCompletableFuture())
                    .mapNotNull { body -> toByteArray(body) }
                else -> Mono.justOrEmpty(toByteArray(it))
            }
        } ?: Mono.empty()
        return response.sendByteArray(content).then()
    }

    private fun toByteArray(body: Any?): ByteArray? = when (body) {
        is ByteArray -> body
        is String -> body.toByteArray()
        else -> objectMapper.writeValueAsBytes(body)
    }

    override fun invoke(init: OasStubRoutes.() -> Unit) {
        init()
    }
}

class ContextOasStubRoutes(private val context: String,
                           private val routes: OasStubRoutes)
    : (ContextOasStubRoutes.() -> Unit) -> Unit, KoinComponent {
    private val apiRegistrationService by inject<ApiRegistrationService>()

    fun get(path: String, handler: OasStubRouteHandler) = apply { routes.get(adjustPath(path), handler) }
    fun post(path: String, handler: OasStubRouteHandler) = apply { routes.post(adjustPath(path), handler) }
    fun put(path: String, handler: OasStubRouteHandler) = apply { routes.put(adjustPath(path), handler) }
    fun delete(path: String, handler: OasStubRouteHandler) = apply { routes.delete(adjustPath(path), handler) }

    fun path(path: String, handler: OasStubRouteHandler) = routes.also {
        it.get(adjustPath(path), handler)
        it.post(adjustPath(path), handler)
        it.put(adjustPath(path), handler)
        it.delete(adjustPath(path), handler)
    }

    fun build(): OasStubRoutes {
        saveContext()
        return routes
    }

    private fun adjustPath(path: String): String = "/$context/$path"

    override fun invoke(init: ContextOasStubRoutes.() -> Unit) {
        init()
        saveContext()
    }

    private fun saveContext() {
        apiRegistrationService.saveApiDefinitions(context, ApiDefinitions()).subscribe()
    }
}

private fun ensureResponse(newRequest: OasStubServerHttpRequest, v: Any?): RouterHttpResponse {
    return if (v is RouterHttpResponse) {
        v
    } else {
        newRequest.responseBuilder().ok()
            .apply {
                if (v != null) {
                    when (v) {
                        is String -> header(HttpHeaderNames.CONTENT_TYPE.toString(), HttpHeaderValues.TEXT_PLAIN.toString())
                        is ByteArray -> header(HttpHeaderNames.CONTENT_TYPE.toString(), HttpHeaderValues.APPLICATION_OCTET_STREAM.toString())
                        is Publisher<*> -> header(HttpHeaderNames.CONTENT_TYPE.toString(), HttpHeaderValues.APPLICATION_OCTET_STREAM.toString())
                        else -> header(HttpHeaderNames.CONTENT_TYPE.toString(), HttpHeaderValues.APPLICATION_JSON.toString())
                    }
                }
            }
            .body(v)
    }
}

fun interface OasStubRoutesBuilder {
    fun build(routes: OasStubRoutes)
}