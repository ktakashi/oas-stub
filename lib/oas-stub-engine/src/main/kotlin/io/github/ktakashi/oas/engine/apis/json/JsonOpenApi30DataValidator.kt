package io.github.ktakashi.oas.engine.apis.json

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import io.github.ktakashi.oas.engine.apis.AbstractApiDataValidator
import io.github.ktakashi.oas.engine.apis.ApiValidationResult
import io.github.ktakashi.oas.engine.apis.failedResult
import io.github.ktakashi.oas.engine.apis.success
import io.github.ktakashi.oas.engine.validators.StringValidationContext
import io.github.ktakashi.oas.engine.validators.Validator
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
import java.util.Optional
import java.util.regex.Pattern

@Named @Singleton
class JsonOpenApi30DataValidator
@Inject constructor(private val objectMapper: ObjectMapper,
                    private val validators: Set<Validator<Any>>) : AbstractApiDataValidator<JsonNode>(SpecVersion.V30), JsonMediaSupport {
    override fun validate(input: ByteArray, schema: Schema<*>): ApiValidationResult = try {
        checkSchema(objectMapper.readTree(input), "$", schema)
    } catch (e: IOException) {
        failedResult(e.message as String)
    }

    override fun checkSchema(value: JsonNode, rootProperty: String, schema: Schema<*>): ApiValidationResult = when(schema) {
        is ComposedSchema -> when {
            schema.anyOf != null -> schema.anyOf.firstOrNull { s -> checkSchema(value, rootProperty, s).isValid }
                    ?.let { success }
                    ?: failedResult("$value must satisfy at least one of ${toPrettyString(schema.anyOf)}", rootProperty)
            schema.oneOf != null -> schema.anyOf.filter { s -> checkSchema(value, rootProperty, s).isValid }
                    .let { v ->
                        if (v.size == 1) {
                            success
                        } else {
                            failedResult("$value must satisfy only one of ${toPrettyString(schema.oneOf)}, but satisfied ${v.size}", rootProperty)
                        }
                    }
            schema.allOf != null -> if (schema.allOf.all { s -> checkSchema(value, rootProperty, s).isValid }) {
                success
            } else failedResult("$value must satisfy all of ${toPrettyString(schema.allOf)}", rootProperty)
            else -> failedResult("[API specification error] Invalid composed schema", rootProperty)
        }
        else -> checkSingleSchema(schema, value, rootProperty)
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

    private fun checkMapLike(value: JsonNode, property: String, schema: Schema<Any>): ApiValidationResult = if (value.isObject) {
        val required = checkRequired(value, property, schema)
        val result = checkProperties(value, property, schema, ::checkSchema)
        required.merge(result)
    } else failedResult("Not an object '$value'", property)

    private fun checkArray(value: JsonNode, property: String, schema: ArraySchema): ApiValidationResult = if (value.isArray) {
        checkElements(value, property, schema, ::checkSchema)
    } else {
        failedResult("Not an array '$value'", property)
    }

    @Suppress("UNUSED_PARAMETER")
    private fun checkBoolean(value: JsonNode, property: String, schema: BooleanSchema): ApiValidationResult = if (value.isBoolean) {
        success
    } else failedResult("Not a boolean '$value'", property)

    private fun checkInteger(value: JsonNode, property: String, schema: IntegerSchema): ApiValidationResult = if (value.isIntegralNumber) {
        checkNumberRange(value, property, schema)
    } else failedResult("Not an integer '$value'", property)

    private fun checkNumber(value: JsonNode, property: String, schema: NumberSchema): ApiValidationResult = if (value.isNumber) {
        checkNumberRange(value, property, schema)
    } else failedResult("Not a number '$value'", property)

    private fun checkString(value: JsonNode, property: String, schema: Schema<*>): ApiValidationResult = if (value.isTextual) {
        checkText(value, property, schema, validators) { _, textValue ->
            when (schema) {
                is UUIDSchema, is DateSchema, is DateTimeSchema, is EmailSchema -> StringValidationContext(textValue, schema.format)
                else -> StringValidationContext(textValue, "format", Optional.ofNullable(schema.pattern).map(Pattern::compile).orElse(null))
            }
        }
    } else {
        failedResult("Not a string '$value'", property)
    }

}
