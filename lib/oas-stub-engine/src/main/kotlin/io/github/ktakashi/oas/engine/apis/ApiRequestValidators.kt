package io.github.ktakashi.oas.engine.apis

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.JsonNodeFactory
import io.github.ktakashi.oas.engine.apis.json.guessType
import io.github.ktakashi.oas.engine.paths.findMatchingPath
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.Operation
import io.swagger.v3.oas.models.PathItem
import io.swagger.v3.oas.models.media.Schema
import io.swagger.v3.oas.models.parameters.Parameter
import io.swagger.v3.oas.models.security.SecurityRequirement
import io.swagger.v3.oas.models.security.SecurityScheme
import jakarta.ws.rs.core.MediaType
import java.math.BigDecimal
import java.math.BigInteger
import java.util.Optional
import org.glassfish.jersey.uri.UriTemplate

data class ValidationDetail(val message: String, val property: Optional<String> = Optional.empty())
enum class ApiValidationResultType {
    SUCCESS,
    VALIDATION_ERROR,
    SECURITY;

    fun merge(other: ApiValidationResultType) = when (this) {
        SECURITY -> this
        VALIDATION_ERROR -> when (other) {
            SECURITY -> other
            else -> this
        }
        else -> other
    }
}
data class ApiValidationResult(val resultType: ApiValidationResultType,
                               val validationDetails: List<ValidationDetail> = listOf()) {
    val isValid: Boolean
        get() = resultType == ApiValidationResultType.SUCCESS
    fun merge(other: ApiValidationResult) =
            ApiValidationResult(resultType.merge(other.resultType),validationDetails + other.validationDetails)
    fun toJsonProblemDetails(status: Int, objectMapper: ObjectMapper): Optional<ByteArray> = try {
        if (isValid) {
            Optional.empty()
        } else {
            val invalidParams = validationDetails.map { v -> InvalidParams(v.property.orElse("N/A"), v.message) }
            val details = JsonProblemDetails("validation-error", "Validation error", status, invalidParams)
            Optional.of(objectMapper.writeValueAsBytes(details))
        }
    } catch (e: JsonProcessingException) {
        Optional.empty()
    }
}
private data class InvalidParams(val name: String, val reason: String)
private data class JsonProblemDetails(val type: String,
                                      val title: String,
                                      val status: Int,
                                      val errors: List<InvalidParams>)

internal val success = ApiValidationResult(ApiValidationResultType.SUCCESS)

internal fun failedResult(message: String, property: String? = null, type: ApiValidationResultType = ApiValidationResultType.VALIDATION_ERROR) =
        ApiValidationResult(type, listOf(ValidationDetail(message, Optional.ofNullable(property))))

fun interface ApiRequestValidator {
    fun validate(requestContext: ApiContextAwareRequestContext, path: PathItem, operation: Operation): ApiValidationResult
}

private fun getParameter(operation: Operation, path: PathItem) = operation.parameters ?: path.parameters

class ApiRequestBodyValidator(private val validators: Set<ApiDataValidator<JsonNode>>): ApiRequestValidator {
    override fun validate(requestContext: ApiContextAwareRequestContext, path: PathItem, operation: Operation): ApiValidationResult = when (requestContext.method) {
        "GET", "HEAD", "OPTIONS", "DELETE" -> success // nobody
        else -> {
            val content = requestContext.content
            val mediaType = requestContext.contentType.map { v -> MediaType.valueOf(v) }.orElse(MediaType.APPLICATION_JSON_TYPE)
            val operationContent = operation.requestBody?.content
            // We ignore parameters for now
            val contentType = operationContent?.keys?.find {
                val mt = MediaType.valueOf(it)
                mt.type == mediaType.type && mt.subtype == mediaType.subtype
            }
            val requestMediaType = Optional.ofNullable(operationContent?.get(contentType))
            if (requestMediaType.isEmpty) {
                content.map { operation.requestBody.required ?: false }
                        .map { required -> if (required) success else failedResult("Body with undefined content-type") }
                        .orElse(success)
            } else {
                val schema = requestMediaType.get().schema
                content.map { value -> validators.filter { v -> v.supports(mediaType) && v.supports(schema) }
                        .map { v -> v.validate(value, schema) }
                        .fold(success) { a, b -> a.merge(b) } }
                        .orElseGet { failedResult("Empty body") }
            }
        }
    }
}

class ApiRequestParameterValidator(private val validators: Set<ApiDataValidator<JsonNode>>): ApiRequestValidator {
    override fun validate(requestContext: ApiContextAwareRequestContext, path: PathItem, operation: Operation): ApiValidationResult =
            getParameter(operation, path)
                    ?.map { p -> validate(requestContext, p) }
                    ?.fold(success) { a, b -> a.merge(b) }
                    ?: success
    private fun validate(requestContext: ApiContextAwareRequestContext, parameter: Parameter) = when (parameter.`in`) {
        "header" -> validateHeader(requestContext, parameter)
        "query" -> validateQuery(requestContext, parameter)
        // "path" is done in ApiService
        else -> success
    }

    private fun validateQuery(requestContext: ApiContextAwareRequestContext, parameter: Parameter): ApiValidationResult =
            validateParameterList(parameter, requestContext.queryParameters.getOrDefault(parameter.name, listOf()), "Query parameter '${parameter.name}' is required")

    private fun validateHeader(requestContext: ApiContextAwareRequestContext, parameter: Parameter): ApiValidationResult =
            validateParameterList(parameter, requestContext.headers.getOrDefault(parameter.name, listOf()), "Header '${parameter.name}' is required")

    private fun validateParameterList(parameter: Parameter, values: List<String?>, s: String): ApiValidationResult =
            when {
                values.isEmpty() && parameter.required == false -> success
                values.isEmpty() && parameter.required == true -> failedResult(s, parameter.name)
                else -> validateParameterList(parameter, values)
            }

    fun validateParameterList(parameter: Parameter, values: List<String?>): ApiValidationResult =
            values.map { v -> convertToJsonNode(v, parameter.schema) }
                    .flatMap { v ->
                        validators.filter { validator -> validator.supports(parameter.schema) }
                                .map { validator -> validator.checkSchema(v, parameter.name, parameter.schema) }
                    }
                    .fold(success) { a, b -> a.merge(b) }
}

private fun convertToJsonNode(s: String?, schema: Schema<*>) = s?.let {
    when (guessType(schema)) {
        "integer" -> try {
            JsonNodeFactory.instance.numberNode(BigInteger(s))
        } catch (e: Exception) {
            JsonNodeFactory.instance.textNode(s)
        }
        "number" -> try {
            JsonNodeFactory.instance.numberNode(BigDecimal(s))
        } catch (e: Exception) {
            JsonNodeFactory.instance.textNode(s)
        }
        else -> JsonNodeFactory.instance.textNode(s)
    }
} ?: JsonNodeFactory.instance.nullNode()

class ApiRequestSecurityValidator: ApiRequestValidator {
    override fun validate(requestContext: ApiContextAwareRequestContext, path: PathItem, operation: Operation): ApiValidationResult =
            getSecurityRequirements(operation, requestContext.apiContext.openApi)?.map { requirement ->
                requirement.keys.mapNotNull { key -> requestContext.apiContext.openApi.components.securitySchemes?.get(key) }
                        .map { securitySchema -> validate(requestContext, securitySchema) }
                        .fold(success) { a, b -> a.merge(b) }
            } ?.fold(success) { a, b -> a.merge(b) } ?: success

    private fun validate(requestContext: ApiContextAwareRequestContext, securityScheme: SecurityScheme): ApiValidationResult =
            when (securityScheme.type) {
                SecurityScheme.Type.APIKEY -> validateApiKey(requestContext, securityScheme)
                SecurityScheme.Type.HTTP -> validateHttp(requestContext, securityScheme)
                // TODO support them
                SecurityScheme.Type.OAUTH2, SecurityScheme.Type.OPENIDCONNECT -> success
                // Hmmmm, this is a bit problematic
                SecurityScheme.Type.MUTUALTLS -> success
                else -> success // in case of null
            }

    private fun validateApiKey(requestContext: ApiContextAwareRequestContext, securityScheme: SecurityScheme): ApiValidationResult =
            when (securityScheme.`in`) {
                SecurityScheme.In.HEADER -> check(requestContext.headers, securityScheme.name, "Header")
                SecurityScheme.In.QUERY -> check(requestContext.queryParameters, securityScheme.name, "Query parameter")
                SecurityScheme.In.COOKIE -> check(requestContext.cookies, securityScheme.name, "Cookie")
                else -> success
            }

    private fun validateHttp(requestContext: ApiContextAwareRequestContext, securityScheme: SecurityScheme): ApiValidationResult =
        when (securityScheme.scheme) {
            // Only two of them from the https://www.iana.org/assignments/http-authschemes/http-authschemes.xhtml
            "basic", "Basic" -> checkAuthorization(requestContext.headers, "Basic")
            "bearer", "Bearer" -> checkAuthorization(requestContext.headers, "Bearer")
            else -> success
        }

    private fun checkAuthorization( headers: Map<String, List<String>>, key: String): ApiValidationResult =
        if (headers["Authorization"]?.firstOrNull { v -> v.startsWith(key) } != null) {
            success
        } else {
            failedResult("'Authorization: $key' header must exist", key, ApiValidationResultType.SECURITY)
        }


    private fun <T> check(map: Map<String, T>, key: String, name: String) =
            if (map.containsKey(key)) success else failedResult("$name '$key' must exist", key, ApiValidationResultType.SECURITY)

    // TODO maybe we should merge?
    private fun getSecurityRequirements(operation: Operation, openApi: OpenAPI): List<SecurityRequirement>? = operation.security ?: openApi.security
}


class ApiRequestPathVariableValidator(private val requestParameterValidator: ApiRequestParameterValidator): ApiRequestValidator {

    override fun validate(requestContext: ApiContextAwareRequestContext, path: PathItem, operation: Operation): ApiValidationResult {
        val apiContext = requestContext.apiContext
        val adjustedPath = adjustBasePath(requestContext.apiPath, apiContext.openApi)
        return adjustedPath.flatMap { v ->
            findMatchingPath(v, apiContext.openApi.paths.keys)
                .map { p -> validate(v, p, path, operation) }
        }.orElse(success)
    }
    private fun validate(path: String, template: String, pathItem: PathItem, operation: Operation): ApiValidationResult {
        val uriTemplate = UriTemplate(template)
        val matcher = uriTemplate.pattern.match(path)
        return uriTemplate.templateVariables.flatMapIndexed { i, name ->
            getParameter(operation, pathItem).map { parameter ->
                when (parameter.`in`) {
                    "path" ->
                        if (parameter.name == name) {
                            val value = matcher.group(i + 1)
                            requestParameterValidator.validateParameterList(parameter, listOf(value))
                        } else success
                    else -> success
                }
            }
        }.fold(success) { a, b -> a.merge(b) }
    }

    private fun getParameter(operation: Operation, pathItem: PathItem) = operation.parameters
        ?: pathItem.parameters
        ?: listOf()
}
