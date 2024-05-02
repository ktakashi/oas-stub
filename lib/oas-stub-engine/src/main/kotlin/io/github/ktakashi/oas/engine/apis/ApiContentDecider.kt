package io.github.ktakashi.oas.engine.apis

import com.fasterxml.jackson.databind.ObjectMapper
import io.github.ktakashi.oas.plugin.apis.ResponseContext
import io.swagger.v3.oas.models.Operation
import io.swagger.v3.oas.models.PathItem
import io.swagger.v3.oas.models.media.Content
import java.util.Optional
import org.apache.http.HttpStatus
import org.slf4j.LoggerFactory

sealed interface ContentDecision
// 201 can be defined without any content, so content can be optional
data class ContentFound(val status: Int, val content: Optional<Content>): ContentDecision
data class ContentNotFound(val responseContext: ResponseContext): ContentDecision

private val logger = LoggerFactory.getLogger(ApiContentDecider::class.java)
private val contentType = Optional.of(APPLICATION_PROBLEM_JSON)

class ApiContentDecider(private val validators: Set<ApiRequestValidator>,
                        private val objectMapper: ObjectMapper) {
    fun decideContent(requestContext: ApiContextAwareRequestContext, path: PathItem, operation: Operation): ContentDecision {
        val r = decideContentImpl(requestContext, path, operation)
        logger.debug("Request {}: decision -> {}", requestContext.apiPath, r)
        return r
    }

    private fun decideContentImpl(requestContext: ApiContextAwareRequestContext, path: PathItem, operation: Operation): ContentDecision {
        val result = validate(requestContext, path, operation)
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
                            ?: ContentNotFound(DefaultResponseContext(
                                    status = status.toInt(),
                                    content = result.toJsonProblemDetails(status.toInt(), objectMapper),
                                    contentType = contentType))
                } ?: operation.responses["default"]?.let { ContentFound(baseStatus, Optional.ofNullable(it.content)) }
        ?: ContentNotFound(DefaultResponseContext(
                status = HttpStatus.SC_BAD_REQUEST,
                content = result.toJsonProblemDetails(baseStatus, objectMapper),
                contentType = contentType))
    }

    private fun validate(requestContext: ApiContextAwareRequestContext, path: PathItem, operation: Operation): ApiValidationResult {
        if (requestContext.skipValidation) {
            return success
        }
        return validators.map { v -> v.validate(requestContext, path, operation) }
                .fold(success) { a, b -> a.merge(b) }
    }
}
