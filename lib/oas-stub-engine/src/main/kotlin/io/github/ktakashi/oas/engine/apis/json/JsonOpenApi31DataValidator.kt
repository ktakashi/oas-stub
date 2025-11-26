package io.github.ktakashi.oas.engine.apis.json

import io.github.ktakashi.oas.engine.apis.AbstractApiDataValidator
import io.github.ktakashi.oas.engine.apis.ApiValidationResult
import io.github.ktakashi.oas.engine.apis.failedResult
import io.github.ktakashi.oas.engine.apis.success
import io.github.ktakashi.oas.engine.validators.StringValidationContext
import io.github.ktakashi.oas.engine.validators.Validator
import io.swagger.v3.oas.models.SpecVersion
import io.swagger.v3.oas.models.media.JsonSchema
import io.swagger.v3.oas.models.media.Schema
import java.io.IOException
import java.util.Optional
import java.util.regex.Pattern
import tools.jackson.databind.JsonNode
import tools.jackson.databind.ObjectMapper
import tools.jackson.databind.node.ArrayNode
import tools.jackson.databind.node.BooleanNode
import tools.jackson.databind.node.NullNode
import tools.jackson.databind.node.NumericNode
import tools.jackson.databind.node.ObjectNode
import tools.jackson.databind.node.StringNode

class JsonOpenApi31DataValidator(private val objectMapper: ObjectMapper,
                                 private val validators: Set<Validator<Any>>) : AbstractApiDataValidator<JsonNode>(SpecVersion.V31), JsonMediaSupport {

    override fun validate(input: ByteArray, schema: Schema<*>): ApiValidationResult = try {
        checkSchema(objectMapper.readTree(input), "$", schema as JsonSchema)
    } catch (e: IOException) {
        failedResult(e.message as String)
    }
    override fun checkSchema(value: JsonNode, rootProperty: String, schema: Schema<*>): ApiValidationResult = when (schema) {
        is JsonSchema -> when {
            schema.anyOf != null -> schema.anyOf.firstOrNull { s -> checkSchema(value, rootProperty, s).isValid }
                ?.let { success }
                ?: failedResult("$value must satisfy at least one of ${toPrettyString(schema.anyOf)}", rootProperty)
            schema.oneOf != null -> when (val size = schema.oneOf.filter { s -> checkSchema(value, rootProperty, s).isValid }.size) {
                1 -> success
                else -> failedResult("$value must satisfy only one of ${toPrettyString(schema.oneOf)}, but satisfied $size", rootProperty)
            }
            schema.allOf != null -> if (schema.allOf.all { s -> checkSchema(value, rootProperty, s).isValid }) {
                success
            } else failedResult("$value must satisfy all of ${toPrettyString(schema.allOf)}", rootProperty)
            else -> checkJsonSchema(value, rootProperty, schema)
        }
        else -> failedResult("[BUG] schema is not JsonSchema", rootProperty)
    }

    private fun checkJsonSchema(value: JsonNode, property: String, schema: JsonSchema): ApiValidationResult = when (value) {
        is ObjectNode -> checkObject(value, property, schema)
        is ArrayNode -> checkArray(value, property, schema)
        is StringNode -> checkString(value, property, schema)
        is BooleanNode -> checkBoolean(value, property, schema)
        is NumericNode -> checkNumber(value, property, schema)
        is NullNode -> checkNull(value, property, schema)
        else -> failedResult("Unknown value: $value", property)
    }

    private fun checkNull(value: NullNode, property: String, schema: JsonSchema): ApiValidationResult = if (hasType("null", schema)) {
        success
    } else failedResult("Not a null '$value'", property)

    private fun checkNumber(value: NumericNode, property: String, schema: JsonSchema): ApiValidationResult = if (hasType("integer", schema) || hasType("number", schema)) {
        checkNumberRange(value, property, schema)
    } else failedResult("Not a number/integer '$value'", property)

    private fun checkBoolean(value: BooleanNode, property: String, schema: JsonSchema): ApiValidationResult = if (hasType("boolean", schema)) {
        success
    } else failedResult("Not a boolean '$value'", property)

    private fun checkString(value: StringNode, property: String, schema: JsonSchema): ApiValidationResult = if (hasType("string", schema)) {
        checkText(value, property, schema, validators) { _, textValue ->
            schema.format?.let {
                StringValidationContext(textValue, it)
            } ?: StringValidationContext(textValue, "format", Optional.ofNullable(schema.pattern).map(Pattern::compile).orElse(null))
        }
    } else {
        failedResult("Not a string '$value'", property)
    }

    private fun checkArray(value: ArrayNode, property: String, schema: JsonSchema): ApiValidationResult = if (hasType("array", schema)) {
        checkElements(value, property, schema, ::checkSchema)
    } else failedResult("Not an array '$value'", property)

    private fun checkObject(value: ObjectNode, property: String, schema: JsonSchema): ApiValidationResult = if (hasType("object", schema)) {
        val required = checkRequired(value, property, schema)
        val result = checkProperties(value, property, schema, ::checkSchema)
        required.merge(result)
    } else {
        failedResult("Not an object '$value'", property)
    }

    private fun hasType(type: String, schema: JsonSchema) = checkTypes(type, schema) || guessType(schema) == type

    private fun checkTypes(type: String, schema: JsonSchema): Boolean = schema.types?.contains(type) ?: false

}
