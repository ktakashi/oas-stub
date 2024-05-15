package io.github.ktakashi.oas.engine.apis.json

import com.fasterxml.jackson.core.io.NumberInput
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.BooleanNode
import com.fasterxml.jackson.databind.node.DecimalNode
import com.fasterxml.jackson.databind.node.DoubleNode
import com.fasterxml.jackson.databind.node.IntNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.databind.node.TextNode
import io.github.ktakashi.oas.engine.data.regexp.RegexpDataGenerator
import io.swagger.v3.oas.models.media.Schema
import java.math.BigDecimal
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.UUID
import kotlin.math.min
import org.slf4j.LoggerFactory

internal typealias NodePopulator = (Schema<*>) -> JsonNode

private val logger = LoggerFactory.getLogger("io.github.ktakashi.oas.engine.apis.json.PopulatorUtils")

private val regexpDataGenerator = RegexpDataGenerator()

@Suppress("kotlin:S6518") // impossible to make it
internal fun populateObjectNode(schema: Schema<*>, objectMapper: ObjectMapper, populateNode: NodePopulator): JsonNode = handleExample(schema) { v ->
    if (v is String) {
        objectMapper.readTree(v)
    } else {
        objectMapper.valueToTree(v)
    }
} ?: ObjectNode(objectMapper.nodeFactory).also { me ->
    schema.properties?.forEach { (k, v) -> me.set<JsonNode>(k, populateNode(v)) }
}

internal fun populateArrayNode(schema: Schema<*>, objectMapper: ObjectMapper, populateNode: NodePopulator): JsonNode = handleExample(schema) { v ->
    if (v is String) {
        objectMapper.readTree(v)
    } else {
        objectMapper.valueToTree(v)
    }
} ?: handleArray(schema, objectMapper, populateNode)

internal fun populateBooleanNode(schema: Schema<*>): JsonNode = handleExample(schema) { v -> BooleanNode.valueOf(v.toString().toBoolean()) }
    ?: BooleanNode.valueOf(true)

internal fun populateIntNode(schema: Schema<*>): JsonNode = handleExample(schema) { v -> IntNode.valueOf(NumberInput.parseInt(v.toString())) }
    ?: handleRange(schema)

internal fun populateDoubleNode(schema: Schema<*>): JsonNode = handleExample(schema) { v -> DoubleNode.valueOf(NumberInput.parseDouble(v.toString(), false)) }
    ?: handleRange(schema)

internal fun populateTextNode(schema: Schema<*>): JsonNode = handleExample(schema) { v ->  TextNode.valueOf(v.toString()) }
    ?: handleText(schema)

private fun handleExample(schema: Schema<*>, generator: (Any) -> JsonNode) = getExample(schema)?.let {
    try {
        generator(it)
    } catch (e: Exception) {
        logger.debug("Failed to deserialize example: $it", e)
        null
    }
}

private fun getExample(schema: Schema<*>) = schema.examples?.get(0) ?: schema.example

private fun handleArray(schema: Schema<*>, objectMapper: ObjectMapper, populateNode: NodePopulator): ArrayNode {
    fun getCount(schema: Schema<*>): Int = schema.minItems
        ?: schema.maxItems?.let { min(it, 10) }
        ?: 1

    val count = getCount(schema)
    return ArrayNode(objectMapper.nodeFactory, count).also { me ->
        repeat(count) { me.add(populateNode(schema.items)) }
    }
}

private fun handleText(schema: Schema<*>): JsonNode =
    when (schema.format) {
        "uuid" -> TextNode.valueOf(UUID.randomUUID().toString())
        "date-time" -> TextNode.valueOf(OffsetDateTime.now(ZoneOffset.UTC).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME))
        "date" -> TextNode.valueOf(LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE))
        "email" -> TextNode.valueOf("example@example.com")
        else -> handlePattern(schema)
    }
private fun handlePattern(schema: Schema<*>): JsonNode =
    schema.pattern?.let {
        TextNode.valueOf(generateMatchingString(it))
    } ?: TextNode.valueOf(if (schema.enum != null && schema.enum.isNotEmpty()) schema.enum[0].toString() else "string")

private fun handleRange(schema: Schema<*>): JsonNode = schema.minimum?.let { v -> DecimalNode(v) }
    ?: schema.maximum?.let { v -> when (v.signum()) {
        0 -> IntNode(-1)
        -1 -> DecimalNode(v)
        1 -> if (v < BigDecimal.ONE) DecimalNode(v) else IntNode(1)
        // what?
        else -> IntNode(1)
    } }
    ?: IntNode(0) // no minimum nor no maximum

private fun generateMatchingString(pattern: String): String = regexpDataGenerator.generate(pattern)
