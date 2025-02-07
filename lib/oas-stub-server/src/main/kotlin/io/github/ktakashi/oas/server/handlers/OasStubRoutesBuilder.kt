package io.github.ktakashi.oas.server.handlers

import com.fasterxml.jackson.databind.ObjectMapper
import io.github.ktakashi.oas.engine.apis.ApiConnectionException
import io.github.ktakashi.oas.engine.apis.ApiDelayService
import io.github.ktakashi.oas.engine.apis.ApiFailureService
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
import org.koin.core.Koin
import org.reactivestreams.Publisher
import org.slf4j.LoggerFactory
import reactor.core.CorePublisher
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.netty.http.server.HttpServerRequest
import reactor.netty.http.server.HttpServerResponse
import reactor.netty.http.server.HttpServerRoutes

private typealias ReactorResponseHandler = BiFunction<HttpServerRequest, HttpServerResponse, Publisher<Void>>

fun interface OasStubRouteHandler: Function<RouterHttpRequest, Any>

private val logger = LoggerFactory.getLogger(OasStubRoutes::class.java)

/**
 * OAS Stub routes.
 *
 * A route is passed to builders
 */
class OasStubRoutes
    internal constructor(internal val routes: HttpServerRoutes,
                         private val koin: Koin): (OasStubRoutes.() -> Unit) -> Unit {
    private val objectMapper = koin.get<ObjectMapper>()

    /**
     * GET route
     */
    fun get(path: String, handler: OasStubRouteHandler) = apply { routes.get(path, adjustHandler(handler, objectMapper)) }

    /**
     * POST route
     */
    fun post(path: String, handler: OasStubRouteHandler) = apply { routes.post(path, adjustHandler(handler, objectMapper)) }

    /**
     * PUT route
     */
    fun put(path: String, handler: OasStubRouteHandler) = apply { routes.put(path, adjustHandler(handler, objectMapper)) }

    /**
     * DELETE route
     */
    fun delete(path: String, handler: OasStubRouteHandler) = apply { routes.delete(path, adjustHandler(handler, objectMapper)) }

    /**
     * Handling `GET`, `POST`, `PUT` and `DELETE` method in the given [path]
     */
    fun path(path: String, handler: OasStubRouteHandler) = apply {
        routes.also {
            it.get(path, adjustHandler(handler, objectMapper))
            it.post(path, adjustHandler(handler, objectMapper))
            it.put(path, adjustHandler(handler, objectMapper))
            it.delete(path, adjustHandler(handler, objectMapper))
        }
    }

    /**
     * Creating a configurable route named [context]
     *
     * This method is more for Java
     *
     * Example:
     * ```Java
     * routes.context("custom")
     *   .get("/ok", request -> request.responseBuilder().ok().build("OK"))
     *   .build()
     *   .context("custom2")
     *   .get("/ok", request -> Mono.just("OK"))
     *   .build()
     * ```
     */
    fun context(context: String) = ContextOasStubRoutes(context, this, koin)
    /**
     * Creating a configurable route named [context]
     *
     * This is a DSL for Kotlin.
     */
    fun context(context: String, init: ContextOasStubRoutes.() -> Unit) = ContextOasStubRoutes(context, this, koin).invoke(init)

    override fun invoke(init: OasStubRoutes.() -> Unit) {
        init()
    }
}

/**
 * Configurable custom route
 */
class ContextOasStubRoutes
internal constructor(private val context: String,
                     private val routes: OasStubRoutes,
                     koin: Koin): (ContextOasStubRoutes.() -> Unit) -> Unit {
    private val apiRegistrationService by koin.inject<ApiRegistrationService>()
    private val objectMapper by koin.inject<ObjectMapper>()
    private val delayService by koin.inject<ApiDelayService>()
    private val failureService by koin.inject<ApiFailureService>()

    /**
     * GET route
     */
    fun get(path: String, handler: OasStubRouteHandler) = apply { routes.routes.get(adjustPath(path), optionAwareHandler("GET", path, handler)) }
    /**
     * POST route
     */
    fun post(path: String, handler: OasStubRouteHandler) = apply { routes.routes.post(adjustPath(path), optionAwareHandler("POST", path, handler)) }
    /**
     * PUT route
     */
    fun put(path: String, handler: OasStubRouteHandler) = apply { routes.routes.put(adjustPath(path), optionAwareHandler("PUT", path, handler)) }
    /**
     * DELETE route
     */
    fun delete(path: String, handler: OasStubRouteHandler) = apply { routes.routes.delete(adjustPath(path), optionAwareHandler("DELETE", path, handler)) }

    /**
     * Handling `GET`, `POST`, `PUT` and `DELETE` method in the given [path]
     */
    fun path(path: String, handler: OasStubRouteHandler) = apply {
        routes.routes.also {
            it.get(adjustPath(path), optionAwareHandler("GET", path, handler))
            it.post(adjustPath(path), optionAwareHandler("POST", path, handler))
            it.put(adjustPath(path), optionAwareHandler("PUT", path, handler))
            it.delete(adjustPath(path), optionAwareHandler("DELETE", path, handler))
        }
    }

    /**
     * Builds the routes and returns the root route.
     */
    fun build(): OasStubRoutes {
        saveContext()
        return routes
    }

    private fun adjustPath(path: String): String = "/$context$path"
    private fun optionAwareHandler(method: String, path: String, handler: OasStubRouteHandler): ReactorResponseHandler {
        return adjustHandler(handler, objectMapper, checkFailure(path)) { execution ->
            delayService.delayMono(context, method, path, execution)
        }
    }

    private fun checkFailure(path: String): (RouterHttpRequest) -> Mono<RouterHttpResponse> = { request ->
        failureService.checkFailure(context, path, request) { responseContext ->
            request.responseBuilder().status(responseContext.status).build()
        }.doOnError(ApiConnectionException::class.java) { e ->
            logger.error(e.message)
            request.connection.close()
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

private val defaultFailureHandler: (RouterHttpRequest) -> Mono<RouterHttpResponse> = { Mono.empty() }

private fun adjustHandler(handler: OasStubRouteHandler,
                          objectMapper: ObjectMapper,
                          failureHandler: (RouterHttpRequest) -> Mono<RouterHttpResponse> = defaultFailureHandler,
                          delayTransformer: ResponseTransformer = ::defaultDelayTransformer)
        : ReactorResponseHandler {
    return BiFunction { request: HttpServerRequest, response: HttpServerResponse ->
        val newRequest = OasStubServerHttpRequest(request, response, objectMapper)
        failureHandler(newRequest).switchIfEmpty(Mono.defer { invokeHandler(handler, newRequest) })
            .map { resp -> toMonoByteArray(resp, objectMapper) }
            .switchIfEmpty(Mono.just(Mono.just(byteArrayOf())))
            .transform(delayTransformer)
            .flatMap { resp -> emitResponse(resp, response) }
    }
}

private fun invokeHandler(handler: OasStubRouteHandler, newRequest: OasStubServerHttpRequest) = (when (val r = handler.apply(newRequest)) {
    is Mono<*> -> r.map { v -> ensureResponse(newRequest, v) }
    else -> Mono.just(ensureResponse(newRequest, r))
})

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

/**
 * Custom OAS Stub server route builder
 */
fun interface OasStubRoutesBuilder {
    /**
     * The entry point of the builder.
     *
     * Below is an example of creating a `/custom` context
     * ```kotlin
     * OasStubRoutesBuilder { routes ->
     *     routes {
     *         context("context") {
     *             get("/ok") { request ->
     *                 request.responseBuilder().ok().build("OK")
     *             }
     *         }
     *     }
     * }
     * ```
     */
    fun build(routes: OasStubRoutes)
}