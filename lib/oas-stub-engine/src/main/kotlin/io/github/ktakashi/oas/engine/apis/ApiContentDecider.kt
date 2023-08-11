package io.github.ktakashi.oas.engine.apis

import com.fasterxml.jackson.databind.ObjectMapper
import io.github.ktakashi.oas.plugin.apis.ResponseContext
import io.swagger.v3.oas.models.Operation
import io.swagger.v3.oas.models.media.Content
import jakarta.inject.Inject
import jakarta.inject.Named
import jakarta.inject.Singleton
import java.util.Optional
import org.apache.http.HttpStatus
import org.slf4j.LoggerFactory

sealed interface ContentDecision
// 201 can be defined without any content, so content can be optional
data class ContentFound(val status: Int, val content: Optional<Content>): ContentDecision
data class ContentNotFound(val responseContext: ResponseContext): ContentDecision

private val logger = LoggerFactory.getLogger(ApiContentDecider::class.java)
private val contentType = Optional.of(APPLICATION_PROBLEM_JSON)

@Named @Singleton
class ApiContentDecider
@Inject constructor(private val validators: Set<ApiRequestValidator>,
                    private val objectMapper: ObjectMapper) {
    fun decideContent(requestContext: ApiContextAwareRequestContext, operation: Operation): ContentDecision {
        val result = validate(requestContext, operation)
        if (!result.isValid) {
            logger.info("Validation failed: {}", result)
        }
        val baseStatus = when (result.resultType) {
            ApiValidationResultType.SUCCESS -> HttpStatus.SC_OK
            ApiValidationResultType.VALIDATION_ERROR -> HttpStatus.SC_BAD_REQUEST
            ApiValidationResultType.SECURITY -> HttpStatus.SC_UNAUTHORIZED
        }

        return operation.responses.map { (k, _) -> k }
                .filter { k -> "default" != k && Integer.parseInt(k) >= baseStatus }
                .minWithOrNull { k0, k1 -> k0.compareTo(k1) }
                ?.let { status ->
                    operation.responses[status]
                            ?.let { ContentFound(status.toInt(), Optional.ofNullable(it.content)) }
                            ?: ContentNotFound(ResponseContext(status.toInt(), result.toJsonProblemDetails(status.toInt(), objectMapper), contentType))
                } ?: operation.responses["default"]?.let { ContentFound(baseStatus, Optional.ofNullable(it.content)) }
        ?: ContentNotFound(ResponseContext(HttpStatus.SC_BAD_REQUEST, result.toJsonProblemDetails(baseStatus, objectMapper), contentType))
    }

    private fun validate(requestContext: ApiContextAwareRequestContext, operation: Operation): ApiValidationResult {
        if (!requestContext.apiOptions.shouldValidate) {
            return success
        }
        return validators.map { v -> v.validate(requestContext, operation) }
                .fold(success) { a, b -> a.merge(b) }
    }
}
