package io.github.ktakashi.oas.engine.apis

import io.github.ktakashi.oas.api.http.HttpRequest
import io.github.ktakashi.oas.api.http.ResponseContext
import io.github.ktakashi.oas.engine.models.mergeProperty
import io.github.ktakashi.oas.model.ApiCommonConfigurations
import io.github.ktakashi.oas.model.ApiConnectionError
import io.github.ktakashi.oas.model.ApiHttpError
import io.github.ktakashi.oas.model.ApiProtocolFailure
import reactor.core.publisher.Mono

class ApiConnectionException(message: String): Exception(message)

class ApiFailureService(private val apiContextService: ApiContextService) {
    fun checkFailure(requestContext: ApiContextAwareRequestContext, headers: Map<String, List<String>> = mapOf()): Mono<ResponseContext> {
        return when (val f = requestContext.apiOptions?.failure) {
            is ApiProtocolFailure -> Mono.just(DefaultResponseContext(1000)) // mustn't return out of range of [100, 500)
            is ApiHttpError -> Mono.just(DefaultResponseContext(f.status, headers = headers))
            is ApiConnectionError -> close(requestContext.rawRequest)
            // None, won't fail then
            else -> Mono.empty()
        }
    }

    fun <T> checkFailure(context: String, path: String, request: HttpRequest, handler: (ResponseContext) -> T & Any): Mono<T & Any> {
        return apiContextService.getApiContext(context, path, request.method).flatMap { apiContext ->
            apiContext.mergeProperty(ApiCommonConfigurations<*>::options)?.let { config ->
                when (val f = config.failure) {
                    is ApiProtocolFailure -> Mono.defer { Mono.just(handler(DefaultResponseContext(1000))) }
                    is ApiHttpError -> Mono.defer { Mono.just(handler(DefaultResponseContext(f.status))) }
                    is ApiConnectionError -> Mono.error { throw ApiConnectionException("Configured connection error on /${apiContext.context}${apiContext.apiPath}") }
                    else -> Mono.empty()
                }
            } ?: Mono.empty()
        }
    }

    private fun <T: Any> close(request: HttpRequest): Mono<T> {
        request.connection.close()
        return Mono.error { throw ApiConnectionException("Configured connection error on ${request.requestURI}") }
    }
}