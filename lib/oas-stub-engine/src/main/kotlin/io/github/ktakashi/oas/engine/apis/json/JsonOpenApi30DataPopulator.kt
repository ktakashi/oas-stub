package io.github.ktakashi.oas.engine.apis.json

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.core.io.NumberInput
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.BooleanNode
import com.fasterxml.jackson.databind.node.DecimalNode
import com.fasterxml.jackson.databind.node.DoubleNode
import com.fasterxml.jackson.databind.node.IntNode
import com.fasterxml.jackson.databind.node.NullNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.databind.node.TextNode
import io.github.ktakashi.oas.engine.apis.AbstractApiDataPopulator
import io.github.ktakashi.oas.engine.apis.ApiAnyDataPopulator
import io.swagger.v3.oas.models.SpecVersion
import io.swagger.v3.oas.models.media.ArraySchema
import io.swagger.v3.oas.models.media.BooleanSchema
import io.swagger.v3.oas.models.media.ComposedSchema
import io.swagger.v3.oas.models.media.DateSchema
import io.swagger.v3.oas.models.media.DateTimeSchema
import io.swagger.v3.oas.models.media.EmailSchema
import io.swagger.v3.oas.models.media.IntegerSchema
import io.swagger.v3.oas.models.media.NumberSchema
import io.swagger.v3.oas.models.media.ObjectSchema
import io.swagger.v3.oas.models.media.Schema
import io.swagger.v3.oas.models.media.StringSchema
import io.swagger.v3.oas.models.media.UUIDSchema
import jakarta.inject.Inject
import jakarta.inject.Named
import jakarta.inject.Singleton
import java.math.BigDecimal
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Optional
import java.util.UUID
import kotlin.math.min

@Named @Singleton
class JsonOpenApi30DataPopulator
@Inject constructor(private val objectMapper: ObjectMapper): JsonMediaSupport, ApiAnyDataPopulator, AbstractApiDataPopulator(SpecVersion.V30) {
    override fun populate(schema: Schema<*>): ByteArray = try {
        val node = populateNode(schema)
        objectMapper.writeValueAsBytes(node)
    } catch (e: JsonProcessingException) {
        "null".toByteArray()
    }

    private fun populateNode(schema: Schema<*>): JsonNode = when (schema) {
        is ComposedSchema -> when {
            schema.anyOf != null && schema.anyOf.isNotEmpty() -> populateNode(schema.anyOf[0])
            schema.oneOf != null && schema.oneOf.isNotEmpty() -> populateNode(schema.oneOf[0])
            schema.allOf != null && schema.allOf.isNotEmpty() -> NullNode.instance // TODO Implement me, it's problematic...
            else -> throw IllegalArgumentException("This mustn't happen")
        }
        else -> populateNodeSimple(schema)
    }

    private fun populateNodeSimple(schema: Schema<*>): JsonNode = when (schema.type) {
        "string" -> populateString(schema)
        "number" -> populateNumber(schema as NumberSchema)
        "integer" -> populateInteger(schema as IntegerSchema)
        "boolean" -> populateBoolean(schema as BooleanSchema)
        "array" -> populateArray(schema as ArraySchema)
        "object" -> populateObject(schema)
        else -> NullNode.instance
    }

    private fun populateObject(schema: Schema<*>): JsonNode = ObjectNode(objectMapper.nodeFactory).also { me ->
        if (schema is ObjectSchema) {
            schema.properties?.forEach { (k, v) -> me.set<JsonNode>(k, populateNode(v)) }
        }
    }

    private fun populateArray(schema: ArraySchema): JsonNode {
        fun getCount(schema: ArraySchema): Int = schema.minItems
                ?: schema.maxItems?.let { min(it, 10) }
                ?: 1
        val count = getCount(schema)
        return ArrayNode(objectMapper.nodeFactory, count).also { me ->
            repeat(count) { me.add(populateNode(schema.items)) }
        }
    }

    private fun populateBoolean(schema: BooleanSchema): JsonNode =
            handleExample(schema) { v -> BooleanNode.valueOf(v.toBoolean()) }
                    .orElseGet { BooleanNode.valueOf(true) }

    private fun populateInteger(schema: IntegerSchema): JsonNode =
            handleExample(schema) { v -> IntNode.valueOf(NumberInput.parseInt(v)) }
                    .orElseGet { handleRange(schema) }

    private fun populateNumber(schema: NumberSchema): JsonNode =
            handleExample(schema) { v -> DoubleNode.valueOf(NumberInput.parseDouble(v)) }
                    .orElseGet { handleRange(schema) }

    private fun populateString(schema: Schema<*>): JsonNode = handleExample(schema, TextNode::valueOf)
            .orElseGet { when (schema) {
                is UUIDSchema -> TextNode.valueOf(UUID.randomUUID().toString())
                is DateTimeSchema -> TextNode.valueOf(OffsetDateTime.now(ZoneOffset.UTC).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME))
                is DateSchema -> TextNode.valueOf(LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE))
                is EmailSchema -> TextNode.valueOf("example@example.com")
                is StringSchema -> TextNode(if (schema.enum != null && schema.enum.isNotEmpty()) schema.enum[0] else "string")
                else -> TextNode.valueOf("string")
            } }
}

private fun handleExample(schema: Schema<*>, generator: (s: String) -> JsonNode) = schema.example?.let {
    Optional.of(generator(it.toString()))
} ?: Optional.empty()

private fun handleRange(schema: Schema<*>): JsonNode = schema.minimum?.let { v -> DecimalNode(v) }
        ?: schema.maximum?.let { v -> when (v.signum()) {
            0 -> IntNode(-1)
            -1 -> DecimalNode(v)
            1 -> if (v < BigDecimal.ONE) DecimalNode(v) else IntNode(1)
            // what?
            else -> IntNode(1)
        } }
        ?: IntNode(1) // no minimum nor no maximum
