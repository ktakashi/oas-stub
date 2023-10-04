package io.github.ktakashi.oas.engine.apis.json

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.BooleanNode
import com.fasterxml.jackson.databind.node.NullNode
import com.fasterxml.jackson.databind.node.NumericNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.databind.node.TextNode
import io.github.ktakashi.oas.engine.apis.AbstractApiDataValidator
import io.github.ktakashi.oas.engine.apis.ApiValidationResult
import io.github.ktakashi.oas.engine.apis.failedResult
import io.github.ktakashi.oas.engine.apis.success
import io.github.ktakashi.oas.engine.validators.StringValidationContext
import io.github.ktakashi.oas.engine.validators.Validator
import io.swagger.v3.oas.models.SpecVersion
import io.swagger.v3.oas.models.media.JsonSchema
import io.swagger.v3.oas.models.media.Schema
import jakarta.inject.Inject
import jakarta.inject.Named
import jakarta.inject.Singleton
import java.io.IOException
import java.util.Optional
import java.util.regex.Pattern

@Named @Singleton
class JsonOpenApi31DataValidator
@Inject constructor(private val objectMapper: ObjectMapper,
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
            else -> checkJsonSchema(value, rootProperty, schema)
        }
        else -> failedResult("[BUG] schema is not JsonSchema", rootProperty)
    }

    private fun checkJsonSchema(value: JsonNode, property: String, schema: JsonSchema): ApiValidationResult = when (value) {
        is ObjectNode -> checkObject(value, property, schema)
        is ArrayNode -> checkArray(value, property, schema)
        is TextNode -> checkString(value, property, schema)
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

    private fun checkString(value: TextNode, property: String, schema: JsonSchema): ApiValidationResult = if (hasType("string", schema)) {
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

    private fun hasType(type: String, schema: JsonSchema) = checkType(type, schema) || checkTypes(type, schema)

    private fun checkTypes(type: String, schema: JsonSchema): Boolean = schema.types?.contains(type) ?: false

    private fun checkType(type: String, schema: JsonSchema): Boolean = schema.type?.let {
        it == type
    } ?: false

}
