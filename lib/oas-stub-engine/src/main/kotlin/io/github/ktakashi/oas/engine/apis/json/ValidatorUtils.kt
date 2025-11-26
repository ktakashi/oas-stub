package io.github.ktakashi.oas.engine.apis.json

import io.github.ktakashi.oas.engine.apis.ApiValidationResult
import io.github.ktakashi.oas.engine.apis.failedResult
import io.github.ktakashi.oas.engine.apis.success
import io.github.ktakashi.oas.engine.validators.ValidationContext
import io.github.ktakashi.oas.engine.validators.Validator
import io.swagger.v3.core.util.Yaml
import io.swagger.v3.oas.models.media.Schema
import java.math.BigDecimal
import tools.jackson.databind.JsonNode
import tools.jackson.databind.node.ArrayNode

internal fun toPrettyString(schema: List<Schema<*>>): String = Yaml.pretty(schema)
internal fun checkRequired(value: JsonNode, property: String, schema: Schema<Any>) = schema.required
    ?.filter { r -> !value.has(r) }
    ?.map { r -> failedResult("Missing required field", "$property.$r") }
    ?.fold(success) { a, b -> a.merge(b) }
    ?: success

internal fun checkProperties(value: JsonNode, property: String, schema: Schema<Any>, checkSchema: (JsonNode, String, Schema<*>) -> ApiValidationResult): ApiValidationResult {
    val props: Map<String, Schema<Any>> = schema.properties
    val additionalProperties = schema.additionalProperties
    return value.properties().asSequence()
        .map { e -> checkProperty(e, property, props, additionalProperties, checkSchema) }
        .fold(success) { a, b -> a.merge(b) }
}

@Suppress("UNCHECKED_CAST")
private fun checkProperty(e: Map.Entry<String, JsonNode>,
                          property: String,
                          props: Map<String, Schema<Any>>,
                          additionalProperties: Any?,
                          checkSchema: (JsonNode, String, Schema<*>) -> ApiValidationResult): ApiValidationResult {
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

internal fun checkElements(value: ArrayNode, property: String, schema: Schema<Any>, checkSchema: (JsonNode, String, Schema<*>) -> ApiValidationResult): ApiValidationResult {
    val max = schema.maxItems
    if (max != null && value.size() > max) {
        return failedResult("At most $max elements are allowed", property)
    }
    val min = schema.minItems
    if (min != null && value.size() < min) {
        return failedResult("At least $min elements are required", property)
    }
    val items = schema.items
    return value.elements().asSequence()
        .mapIndexed { i, v -> checkSchema(v, "$property[$i]", items) }
        .fold(success) { a, b -> a.merge(b) }
}

internal fun checkText(value: JsonNode, property: String, schema: Schema<*>, validators: Set<Validator<Any>>, makeContext: (Schema<*>, String) -> ValidationContext<Any>): ApiValidationResult {
    val textValue = value.asString()
    val context = makeContext(schema, textValue)
    val result = validators.map { v -> v.tryValidate(context) }.fold(true) { a, b -> a && b }
    return if (!result) {
        failedResult("Format error, format: ${schema.format}, pattern: ${schema.pattern}")
    } else if (schema.enum != null && !schema.enum.contains(textValue)) {
        failedResult("Value must be one of the ${schema.enum}", property)
    } else {
        success
    }
}

internal fun checkNumberRange(value: JsonNode, property: String, schema: Schema<*>): ApiValidationResult {
    val max = schema.maximum
    val min = schema.minimum
    val v = BigDecimal(value.asString())
    return if (max != null && v > max) {
        failedResult("Maximum value is $max", property)
    } else if (min != null && v < min) {
        failedResult("Minimum value is $min", property)
    } else success
}