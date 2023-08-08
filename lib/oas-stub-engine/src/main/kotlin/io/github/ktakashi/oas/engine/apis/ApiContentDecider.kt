package io.github.ktakashi.oas.engine.apis

import com.fasterxml.jackson.databind.ObjectMapper
import io.github.ktakashi.oas.plugin.apis.ResponseContext
import io.swagger.v3.oas.models.Operation
import io.swagger.v3.oas.models.media.Content
import jakarta.inject.Inject
import jakarta.inject.Named
import jakarta.inject.Singleton
import org.apache.http.HttpStatus

sealed interface ContentDecision
data class ContentFound(val status: Int, val content: Content): ContentDecision
data class ContentNotFound(val responseContext: ResponseContext): ContentDecision
@Named @Singleton
class ApiContentDecider
@Inject constructor(private val validators: Set<ApiRequestValidator>,
                    private val objectMapper: ObjectMapper){
    fun decideContent(requestContext: ApiContextAwareRequestContext, operation: Operation): ContentDecision {
        val result = validate(requestContext, operation)
        val baseStatus = if (result.isValid) HttpStatus.SC_OK else HttpStatus.SC_BAD_REQUEST
        return operation.responses.map { (k, _) -> k }
                .filter { k -> "default" != k && Integer.parseInt(k) >= baseStatus }
                .minWithOrNull { k0, k1 -> k0.compareTo(k1) }
                ?.let { status ->
                    operation.responses[status]?.let { ContentFound(status.toInt(), it.content) }
                            ?: ContentNotFound(ResponseContext(status.toInt(), result.toJsonBytes(objectMapper)))
                } ?: ContentNotFound(ResponseContext(HttpStatus.SC_BAD_REQUEST, result.toJsonBytes(objectMapper)))
    }

    private fun validate(requestContext: ApiContextAwareRequestContext, operation: Operation): ApiValidationResult {
        if (!requestContext.apiOptions.shouldValidate) {
            return success
        }
        return validators.map { v -> v.validate(requestContext, operation) }
                .fold(success) { a, b -> a.merge(b) }
    }
}
