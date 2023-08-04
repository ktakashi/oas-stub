package io.github.ktakashi.oas.engine.apis

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.ObjectMapper
import io.swagger.v3.oas.models.Operation
import jakarta.inject.Inject
import jakarta.inject.Named
import jakarta.inject.Singleton
import jakarta.ws.rs.core.MediaType
import java.util.Optional

data class ValidationDetail(val message: String, val property: Optional<String> = Optional.empty())
data class ApiValidationResult(val isValid: Boolean,
                               val validationDetails: List<ValidationDetail> = listOf()) {
    fun merge(other: ApiValidationResult) =
            ApiValidationResult(isValid && other.isValid,validationDetails + other.validationDetails)
    fun toJsonBytes(objectMapper: ObjectMapper): Optional<ByteArray> = try {
        Optional.of(objectMapper.writeValueAsBytes(this))
    } catch (e: JsonProcessingException) {
        Optional.empty()
    }
}

internal val success = ApiValidationResult(true)

internal fun failedResult(message: String, property: String? = null) = ApiValidationResult(false, listOf(ValidationDetail(message, Optional.ofNullable(property))))

interface ApiRequestValidator {
    fun validate(requestContext: ApiContextAwareRequestContext, operation: Operation): ApiValidationResult
}

@Named @Singleton
class ApiRequestBodyValidator
@Inject constructor(private val validators: Set<ApiDataValidator>): ApiRequestValidator {
    override fun validate(requestContext: ApiContextAwareRequestContext, operation: Operation): ApiValidationResult = when (requestContext.method) {
        "GET", "HEAD", "OPTIONS", "DELETE" -> success // nobody
        else -> {
            val content = requestContext.content
            val mediaType = requestContext.contentType.map { v -> MediaType.valueOf(v) }.orElse(MediaType.APPLICATION_JSON_TYPE)
            val contentType = "${mediaType.type}/${mediaType.subtype}"
            val requestMediaType = Optional.ofNullable(operation.requestBody?.content?.get(contentType))
            if (requestMediaType.isEmpty) {
                content.map { operation.requestBody.required ?: false }
                        .map { required -> if (required) success else failedResult("Body with undefined content-type") }
                        .orElse(success)
            } else {
                val schema = requestMediaType.get().schema
                content.map { value -> validators.filter { v -> v.supports(mediaType) && v.supports(schema.specVersion) }
                        .map { v -> v.validate(value, schema) }
                        .fold(success) { a, b -> a.merge(b) } }
                        .orElseGet { failedResult("Empty body") }
            }
        }
    }
}
