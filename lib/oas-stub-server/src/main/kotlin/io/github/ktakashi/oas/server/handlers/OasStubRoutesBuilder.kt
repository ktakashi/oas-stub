package io.github.ktakashi.oas.server.handlers

import com.fasterxml.jackson.databind.ObjectMapper
import io.github.ktakashi.oas.engine.apis.ApiDelayService
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
import reactor.core.CorePublisher
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.netty.http.server.HttpServerRequest
import reactor.netty.http.server.HttpServerResponse
import reactor.netty.http.server.HttpServerRoutes

private typealias ReactorResponseHandler = BiFunction<HttpServerRequest, HttpServerResponse, Publisher<Void>>

fun interface OasStubRouteHandler: Function<RouterHttpRequest, Any>

class OasStubRoutes(internal val routes: HttpServerRoutes): (OasStubRoutes.() -> Unit) -> Unit, KoinComponent {
    private val objectMapper: ObjectMapper by inject()
    fun get(path: String, handler: OasStubRouteHandler) = apply { routes.get(path, adjustHandler(handler, objectMapper)) }
    fun post(path: String, handler: OasStubRouteHandler) = apply { routes.post(path, adjustHandler(handler, objectMapper)) }
    fun put(path: String, handler: OasStubRouteHandler) = apply { routes.put(path, adjustHandler(handler, objectMapper)) }
    fun delete(path: String, handler: OasStubRouteHandler) = apply { routes.delete(path, adjustHandler(handler, objectMapper)) }

    fun path(path: String, handler: OasStubRouteHandler) = apply {
        routes.also {
            it.get(path, adjustHandler(handler, objectMapper))
            it.post(path, adjustHandler(handler, objectMapper))
            it.put(path, adjustHandler(handler, objectMapper))
            it.delete(path, adjustHandler(handler, objectMapper))
        }
    }

    fun context(context: String) = ContextOasStubRoutes(context, this)
    fun context(context: String, init: ContextOasStubRoutes.() -> Unit) = ContextOasStubRoutes(context, this).invoke(init)

    override fun invoke(init: OasStubRoutes.() -> Unit) {
        init()
    }
}

class ContextOasStubRoutes(private val context: String,
                           private val routes: OasStubRoutes)
    : (ContextOasStubRoutes.() -> Unit) -> Unit, KoinComponent {
    private val apiRegistrationService by inject<ApiRegistrationService>()
    private val objectMapper by inject<ObjectMapper>()
    private val delayService by inject<ApiDelayService>()

    fun get(path: String, handler: OasStubRouteHandler) = apply { routes.routes.get(adjustPath(path), optionAwareHandler(path, handler)) }
    fun post(path: String, handler: OasStubRouteHandler) = apply { routes.routes.post(adjustPath(path), optionAwareHandler(path, handler)) }
    fun put(path: String, handler: OasStubRouteHandler) = apply { routes.routes.put(adjustPath(path), optionAwareHandler(path, handler)) }
    fun delete(path: String, handler: OasStubRouteHandler) = apply { routes.routes.delete(adjustPath(path), optionAwareHandler(path, handler)) }

    fun path(path: String, handler: OasStubRouteHandler) = apply {
        routes.routes.also {
            it.get(adjustPath(path), optionAwareHandler(path, handler))
            it.post(adjustPath(path), optionAwareHandler(path, handler))
            it.put(adjustPath(path), optionAwareHandler(path, handler))
            it.delete(adjustPath(path), optionAwareHandler(path, handler))
        }
    }

    fun build(): OasStubRoutes {
        saveContext()
        return routes
    }

    private fun adjustPath(path: String): String = "/$context$path"
    private fun optionAwareHandler(path: String, handler: OasStubRouteHandler): ReactorResponseHandler {
        return adjustHandler(handler, objectMapper) { execution ->
            delayService.delayMono(context, path, execution)
        }
    }

    override fun invoke(init: ContextOasStubRoutes.() -> Unit) {
        init()
        saveContext()
    }

    private fun saveContext() {
        apiRegistrationService.saveApiDefinitions(context, ApiDefinitions()).subscribe()
    }
}

private fun <T> defaultDelayTransformer(corePublisher: Mono<CorePublisher<T>>): Mono<CorePublisher<T>> = corePublisher
private typealias ResponseTransformer = (Mono<CorePublisher<ByteArray>>) -> Mono<CorePublisher<ByteArray>>

private fun adjustHandler(handler: OasStubRouteHandler, objectMapper: ObjectMapper, delayTransformer: ResponseTransformer = ::defaultDelayTransformer)
        : ReactorResponseHandler {
    return BiFunction { request: HttpServerRequest, response: HttpServerResponse ->
        val newRequest = OasStubServerHttpRequest(request, response, objectMapper)
        (when (val r = handler.apply(newRequest)) {
            is Mono<*> -> r.map { v -> ensureResponse(newRequest, v) }
            else -> Mono.just(ensureResponse(newRequest, r))

        }).map { resp -> toMonoByteArray(resp, objectMapper)}
            .transform(delayTransformer)
            .flatMap { resp -> emitResponse(resp, response) }
    }
}

private fun emitResponse(content: CorePublisher<ByteArray>, response: HttpServerResponse): Mono<Void> {
    return response.sendByteArray(content).then()
}

private fun toMonoByteArray(resp: RouterHttpResponse, objectMapper: ObjectMapper): CorePublisher<ByteArray> = resp.body?.let {
    when (it) {
        is Mono<*> -> it.mapNotNull { body -> toByteArray(body, objectMapper) }
        is Flux<*> -> it.mapNotNull { body -> toByteArray(body, objectMapper) }
        is CompletionStage<*> -> Mono.fromFuture(it.toCompletableFuture())
            .mapNotNull { body -> toByteArray(body, objectMapper) }

        else -> Mono.justOrEmpty(toByteArray(it, objectMapper))
    }
} ?: Mono.empty()

private fun toByteArray(body: Any?, objectMapper: ObjectMapper): ByteArray? = when (body) {
    is ByteArray -> body
    is String -> body.toByteArray()
    else -> objectMapper.writeValueAsBytes(body)
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