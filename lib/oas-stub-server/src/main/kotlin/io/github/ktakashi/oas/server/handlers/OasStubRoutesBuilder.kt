package io.github.ktakashi.oas.server.handlers

import com.fasterxml.jackson.databind.ObjectMapper
import io.github.ktakashi.oas.server.http.OasStubServerHttpRequest
import io.github.ktakashi.oas.server.http.RouterHttpMethod
import io.github.ktakashi.oas.server.http.RouterHttpRequest
import io.github.ktakashi.oas.server.http.RouterHttpResponse
import java.util.function.BiFunction
import java.util.function.Function
import org.reactivestreams.Publisher
import reactor.core.publisher.Mono
import reactor.netty.http.server.HttpServerRequest
import reactor.netty.http.server.HttpServerResponse
import reactor.netty.http.server.HttpServerRoutes

fun interface OasStubRouteHandler: Function<RouterHttpRequest, Mono<RouterHttpResponse>>
fun interface OasStubPathRouteHandler: BiFunction<RouterHttpMethod, RouterHttpRequest, Mono<RouterHttpResponse>>

class OasStubRoutes(private val routes: HttpServerRoutes,
                    private val objectMapper: ObjectMapper): (OasStubRoutes.() -> Unit) -> Unit {
    fun get(path: String, handler: OasStubRouteHandler) = apply { routes.get(path, adjustHandler(handler)) }
    fun post(path: String, handler: OasStubRouteHandler) = apply { routes.post(path, adjustHandler(handler)) }
    fun put(path: String, handler: OasStubRouteHandler) = apply { routes.put(path, adjustHandler(handler)) }
    fun delete(path: String, handler: OasStubRouteHandler) = apply { routes.delete(path, adjustHandler(handler)) }

    fun path(path: String, handler: OasStubPathRouteHandler) = routes.also {
        it.get(path, adjustHandlerWithMethod(handler))
        it.post(path, adjustHandlerWithMethod(handler))
        it.put(path, adjustHandlerWithMethod(handler))
        it.delete(path, adjustHandlerWithMethod(handler))
    }

    private fun adjustHandler(handler: OasStubRouteHandler): BiFunction<in HttpServerRequest, in HttpServerResponse, Publisher<Void>> {
        return BiFunction { request: HttpServerRequest, response: HttpServerResponse ->
            handler.apply(OasStubServerHttpRequest(request, response, objectMapper)).flatMap { resp ->
                // TODO delay
                val content = resp.body?.let {
                    Mono.just(objectMapper.writeValueAsBytes(it))
                } ?: Mono.empty()
                response.sendByteArray(content).then()
            }
        }
    }

    private fun adjustHandlerWithMethod(handler: OasStubPathRouteHandler): BiFunction<in HttpServerRequest, in HttpServerResponse, Publisher<Void>> {
        return BiFunction { request: HttpServerRequest, response: HttpServerResponse ->
            handler.apply(RouterHttpMethod.valueOf(request.method().name()), OasStubServerHttpRequest(request, response, objectMapper)).flatMap { resp ->
                // TODO delay
                val content = resp.body?.let {
                    Mono.just(objectMapper.writeValueAsBytes(it))
                } ?: Mono.empty()
                response.sendByteArray(content).then()
            }
        }
    }

    override fun invoke(init: OasStubRoutes.() -> Unit) {
        init()
    }

}

fun interface OasStubRoutesBuilder {
    fun build(routes: OasStubRoutes)
}