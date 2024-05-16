package io.github.ktakashi.oas.engine.apis

import io.github.ktakashi.oas.api.http.HttpRequest
import io.github.ktakashi.oas.api.http.ResponseContext
import io.github.ktakashi.oas.model.ApiConnectionError
import io.github.ktakashi.oas.model.ApiHttpError
import io.github.ktakashi.oas.model.ApiOptions
import io.github.ktakashi.oas.model.ApiProtocolFailure
import reactor.core.publisher.Mono

class ApiFailureService {
    fun checkFailure(requestContext: ApiContextAwareRequestContext, headers: Map<String, List<String>> = mapOf()): Mono<ResponseContext> {
        return when (val f = requestContext.apiOptions?.failure) {
            is ApiProtocolFailure -> Mono.just(DefaultResponseContext(1000)) // mustn't return out of range of [100, 500)
            is ApiHttpError -> Mono.just(DefaultResponseContext(f.status, headers = headers))
            is ApiConnectionError -> close(requestContext.rawRequest)
            // None, won't fail then
            else -> Mono.empty()
        }
    }

    fun checkConnectionError(apiOptions: ApiOptions?, request: HttpRequest): Mono<HttpRequest> {
        return when (apiOptions?.failure) {
            is ApiConnectionError -> close(request)
            else -> Mono.just(request)
        }
    }

    private fun <T> close(request: HttpRequest): Mono<T> {
        request.connection.close()
        return Mono.error { throw Exception() }
    }
}