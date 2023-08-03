package io.github.ktakashi.oas.engine.apis.json

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import io.github.ktakashi.oas.engine.apis.ApiDataValidator
import io.github.ktakashi.oas.engine.apis.ApiValidationResult
import io.github.ktakashi.oas.engine.apis.failedResult
import io.github.ktakashi.oas.engine.apis.success
import io.github.ktakashi.oas.engine.validators.StringValidationContext
import io.github.ktakashi.oas.engine.validators.ValidationContext
import io.github.ktakashi.oas.engine.validators.Validator
import io.swagger.v3.core.util.Yaml
import io.swagger.v3.oas.models.SpecVersion
import io.swagger.v3.oas.models.media.ArraySchema
import io.swagger.v3.oas.models.media.BooleanSchema
import io.swagger.v3.oas.models.media.ComposedSchema
import io.swagger.v3.oas.models.media.DateSchema
import io.swagger.v3.oas.models.media.DateTimeSchema
import io.swagger.v3.oas.models.media.EmailSchema
import io.swagger.v3.oas.models.media.IntegerSchema
import io.swagger.v3.oas.models.media.MapSchema
import io.swagger.v3.oas.models.media.NumberSchema
import io.swagger.v3.oas.models.media.ObjectSchema
import io.swagger.v3.oas.models.media.Schema
import io.swagger.v3.oas.models.media.UUIDSchema
import jakarta.inject.Inject
import jakarta.inject.Named
import jakarta.inject.Singleton
import java.io.IOException
import java.math.BigDecimal
import java.util.Optional
import java.util.regex.Pattern

private fun toPrettyString(schema: List<Schema<*>>): String = Yaml.pretty(schema)
@Named @Singleton
class JsonOpenApi30DataValidator
@Inject constructor(private val objectMapper: ObjectMapper,
                    private val validators: Set<Validator<Any>>) : ApiDataValidator(SpecVersion.V30), JsonMediaSupport {
    override fun validate(input: ByteArray, schema: Schema<*>): ApiValidationResult = try {
        checkSchema(objectMapper.readTree(input), "$", schema)
    } catch (e: IOException) {
        failedResult(e.message as String)
    }

    private fun checkSchema(value: JsonNode, property: String, schema: Schema<*>): ApiValidationResult = when(schema) {
        is ComposedSchema -> when {
            schema.anyOf != null -> schema.anyOf.firstOrNull { s -> checkSchema(value, property, s).isValid }
                    ?.let { success }
                    ?: failedResult("$value must satisfy at least one of ${toPrettyString(schema.anyOf)}", property)
            schema.oneOf != null -> schema.anyOf.filter { s -> checkSchema(value, property, s).isValid }
                    .let { v ->
                        if (v.size == 1) {
                            success
                        } else {
                            failedResult("$value must satisfy only one of ${toPrettyString(schema.oneOf)}, but satisfied ${v.size}", property)
                        }
                    }
            schema.allOf != null -> if (schema.allOf.all { s -> checkSchema(value, property, s).isValid }) {
                success
            } else failedResult("$value must satisfy all of ${toPrettyString(schema.allOf)}", property)
            else -> failedResult("[API specification error] Invalid composed schema", property)
        }
        else -> checkSingleSchema(schema, value, property)
    }

    private fun checkSingleSchema(schema: Schema<*>, value: JsonNode, property: String) = when (schema.type) {
        "string" -> checkString(value, property, schema)
        "number" -> checkNumber(value, property, schema as NumberSchema)
        "integer" -> checkInteger(value, property, schema as IntegerSchema)
        "boolean" -> checkBoolean(value, property, schema as BooleanSchema)
        "array" -> checkArray(value, property, schema as ArraySchema)
        "object" -> if (schema is MapSchema) {
            checkMap(value, property, schema)
        } else {
            checkObject(value, property, schema as ObjectSchema)
        }
        else -> failedResult("Unknown object type: ${schema.type}", property)
    }

    private fun checkObject(value: JsonNode, property: String, schema: ObjectSchema): ApiValidationResult = checkMapLike(value, property, schema)

    private fun checkMap(value: JsonNode, property: String, schema: MapSchema): ApiValidationResult = checkMapLike(value, property, schema)

    private fun checkMapLike(value: JsonNode, property: String, schema: Schema<Any>): ApiValidationResult {
        return if (value.isObject) {
            val required = schema.required
                    ?.filter { r -> !value.has(r) }
                    ?.map { r -> failedResult("Missing required field", "$property.$r") }
                    ?.fold(success) { a, b -> a.merge(b) }
                    ?: success
            val props = schema.properties
            val additionalProperties = schema.additionalProperties
            val result = value.fields().asSequence()
                    .map { e -> checkProperty(e, property, props, additionalProperties) }
                    .fold(success) { a, b -> a.merge(b) }
            required.merge(result)
        } else failedResult("Not an object '$value'", property)
    }

    private fun checkProperty(e: Map.Entry<String, JsonNode>, property: String, props: Map<String, Schema<Any>>, additionalProperties: Any?): ApiValidationResult {
        val p = "$property.${e.key}"
        var schema = props[e.key]
        if (schema == null) {
            when (additionalProperties) {
                null -> return failedResult("Unknown property $p", p)
                is Schema<*> -> schema = additionalProperties as Schema<Any>
                else -> return success
            }
        }
        return checkSchema(e.value, p, schema)
    }

    private fun checkArray(value: JsonNode, property: String, schema: ArraySchema): ApiValidationResult {
        return if (value.isArray) {
            val max = schema.maxItems
            if (max != null && value.size() > max) {
                return failedResult("At most $max elements are allowed", property)
            }
            val min = schema.minItems
            if (min != null && value.size() < min) {
                return failedResult("At least $min elements are required", property)
            }
            val items = schema.items
            value.elements().asSequence()
                    .mapIndexed { i, v -> checkSchema(v, "$property[$i]", items) }
                    .fold(success) { a, b -> a.merge(b) }
        } else {
            failedResult("Not an array '$value'", property)
        }
    }

    private fun checkBoolean(value: JsonNode, property: String, schema: BooleanSchema): ApiValidationResult = if (value.isBoolean) {
        success
    } else failedResult("Not a boolean '$value'", property)

    private fun checkInteger(value: JsonNode, property: String, schema: IntegerSchema): ApiValidationResult = if (value.isIntegralNumber) {
        checkNumberRange(value, property, schema)
    } else failedResult("Not an integer '$value'", property)

    private fun checkNumber(value: JsonNode, property: String, schema: NumberSchema): ApiValidationResult = if (value.isNumber) {
        checkNumberRange(value, property, schema)
    } else failedResult("Not a number '$value'", property)

    private fun <T: Number> checkNumberRange(value: JsonNode, property: String, schema: Schema<T>): ApiValidationResult {
        val max = schema.maximum
        val min = schema.minimum
        val v = BigDecimal(value.asText());
        return if (max != null && v > max) {
            failedResult("Maximum value is $max", property);
        } else if (min != null && v < min) {
            failedResult("Minimum value is $min", property);
        } else success
    }

    private fun checkString(value: JsonNode, property: String, schema: Schema<*>): ApiValidationResult = if (value.isTextual) {
        val textValue = value.asText()
        val context: ValidationContext<Any> = when (schema) {
            is UUIDSchema, is DateSchema, is DateTimeSchema, is EmailSchema -> StringValidationContext(textValue, schema.format)
            else -> StringValidationContext(textValue, "format", Optional.ofNullable(schema.pattern).map(Pattern::compile).orElse(null))
        }
        val result = validators.map { v -> v.tryValidate(context) }.fold(true) { a, b -> a && b }
        if (!result) {
            failedResult("Format error, format: ${schema.format}, pattern: ${schema.pattern}", property)
        } else {
            if (schema.enum != null && !schema.enum.contains(textValue)) {
                failedResult("Value must be one of the ${schema.enum}", property)
            } else {
                success
            }
        }
    } else {
        failedResult("Not a string '$value'", property)
    }

}
