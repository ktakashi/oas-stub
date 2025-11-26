package io.github.ktakashi.oas.engine.apis.json

import com.fasterxml.jackson.core.JsonProcessingException
import com.github.benmanes.caffeine.cache.CacheLoader
import com.github.benmanes.caffeine.cache.Caffeine
import io.github.ktakashi.oas.engine.apis.AbstractApiDataPopulator
import io.github.ktakashi.oas.engine.data.regexp.RegexpDataGenerator
import io.swagger.v3.oas.models.SpecVersion
import io.swagger.v3.oas.models.media.Schema
import java.math.BigDecimal
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.UUID
import java.util.concurrent.TimeUnit
import kotlin.math.min
import org.slf4j.LoggerFactory
import tools.jackson.core.JsonGenerator
import tools.jackson.core.io.NumberInput
import tools.jackson.databind.JsonNode
import tools.jackson.databind.SerializationContext
import tools.jackson.databind.json.JsonMapper
import tools.jackson.databind.module.SimpleModule
import tools.jackson.databind.module.SimpleSerializers
import tools.jackson.databind.node.ArrayNode
import tools.jackson.databind.node.BooleanNode
import tools.jackson.databind.node.DecimalNode
import tools.jackson.databind.node.DoubleNode
import tools.jackson.databind.node.IntNode
import tools.jackson.databind.node.NullNode
import tools.jackson.databind.node.ObjectNode
import tools.jackson.databind.node.POJONode
import tools.jackson.databind.node.StringNode
import tools.jackson.databind.ser.std.StdSerializer
import com.fasterxml.jackson.databind.JsonNode as J2JsonNode

private val logger = LoggerFactory.getLogger("io.github.ktakashi.oas.engine.apis.json.PopulatorUtils")

private val regexpDataGenerator = RegexpDataGenerator()

private fun interface JsonNodeSupplier: () -> JsonNode
private object JsonNodeSupplerSerializer: StdSerializer<JsonNodeSupplier>(JsonNodeSupplier::class.java) {
    override fun serialize(value: JsonNodeSupplier, gen: JsonGenerator, provider: SerializationContext) {
        val node = value()
        gen.writeString(node.toString())
    }
}

private class JsonNodeSerializerModule: SimpleModule("JsonNodeSerializerModule") {
    override fun setupModule(context: SetupContext) {
        super.setupModule(context)
        val sers = SimpleSerializers()
        sers.addSerializer(JsonNodeSupplerSerializer)
        context.addSerializers(sers)
    }
}

abstract class JsonApiDataPopulator(sourceJsonMapper: JsonMapper, version: SpecVersion): AbstractApiDataPopulator(version) {
    protected val jsonMapper: JsonMapper = sourceJsonMapper.rebuild().addModule(JsonNodeSerializerModule()).build()
    private val cache = Caffeine.newBuilder()
        .expireAfterWrite(1, TimeUnit.HOURS)
        .build(CacheLoader<Schema<*>, JsonNode> { schema ->
            populateCache(schema)
        })

    override fun populate(schema: Schema<*>): ByteArray = jsonMapper.writeValueAsBytes(cache[schema])

    private fun populateCache(schema: Schema<*>): JsonNode = try {
        populateNode(schema)
    } catch (_: JsonProcessingException) {
        NullNode.instance
    }

    abstract fun populateNode(schema: Schema<*>): JsonNode

    @Suppress("kotlin:S6518") // impossible to make it
    protected fun populateObjectNode(schema: Schema<*>): JsonNode = handleExample(schema) { v ->
        if (v is String) {
            jsonMapper.readTree(v)
        } else {
            jsonMapper.valueToTree(v)
        }
    } ?: ObjectNode(jsonMapper.nodeFactory).also { me ->
        schema.properties?.forEach { (k, v) -> me.set(k, populateNode(v)) }
    }

    protected fun populateArrayNode(schema: Schema<*>): JsonNode = handleExample(schema) { v ->
        if (v is String) {
            jsonMapper.readTree(v)
        } else {
            jsonMapper.valueToTree(v)
        }
    } ?: handleArray(schema)

    protected fun populateBooleanNode(schema: Schema<*>): JsonNode = handleExample(schema) { v -> BooleanNode.valueOf(v.toString().toBoolean()) }
        ?: BooleanNode.valueOf(true)

    protected fun populateIntNode(schema: Schema<*>): JsonNode = handleExample(schema) { v -> IntNode.valueOf(
        NumberInput.parseInt(v.toString())) }
        ?: handleRange(schema)

    protected fun populateDoubleNode(schema: Schema<*>): JsonNode = handleExample(schema) { v -> DoubleNode.valueOf(NumberInput.parseDouble(v.toString(), false)) }
        ?: handleRange(schema)

    protected fun populateTextNode(schema: Schema<*>): JsonNode = handleExample(schema) { v ->  StringNode.valueOf(v.toString()) }
        ?: handleText(schema)

    private fun handleExample(schema: Schema<*>, generator: (Any) -> JsonNode) = schema.selectExample()?.let {
        try {
            if (it is J2JsonNode) {
                generator(it.convert(jsonMapper))
            } else {
                generator(it)
            }
        } catch (e: Exception) {
            logger.debug("Failed to deserialize example: {}", it, e)
            null
        }
    }

    private fun Schema<*>.selectExample() = examples?.get(0) ?: example

    private fun handleArray(schema: Schema<*>): ArrayNode {
        fun getCount(schema: Schema<*>): Int = schema.minItems
            ?: schema.maxItems?.let { min(it, 10) }
            ?: 1

        val count = getCount(schema)
        return ArrayNode(jsonMapper.nodeFactory, count).also { me ->
            repeat(count) { me.add(populateNode(schema.items)) }
        }
    }

    private fun handleText(schema: Schema<*>): JsonNode =
        when (schema.format) {
            "uuid" -> POJONode(JsonNodeSupplier { StringNode.valueOf(UUID.randomUUID().toString()) })
            "date-time" -> POJONode(JsonNodeSupplier { StringNode.valueOf(OffsetDateTime.now(ZoneOffset.UTC).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)) } )
            "date" -> POJONode(JsonNodeSupplier { StringNode.valueOf(LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)) })
            "email" -> StringNode.valueOf("example@example.com")
            else -> handlePattern(schema)
        }

    private fun handlePattern(schema: Schema<*>): JsonNode =
        schema.pattern?.let {
            StringNode.valueOf(generateMatchingString(it))
        } ?: StringNode.valueOf(if (schema.enum != null && schema.enum.isNotEmpty()) schema.enum[0].toString() else "string")

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

}

private fun J2JsonNode.convert(jsonMapper: JsonMapper): JsonNode = when {
    isValueNode -> when {
        this.isNull -> NullNode.instance
        this.isNumber -> if (isIntegralNumber) IntNode.valueOf(asInt()) else DoubleNode.valueOf(asDouble())
        this.isBoolean -> BooleanNode.valueOf(asBoolean())
        this.isTextual -> StringNode.valueOf(asText())
        else -> StringNode.valueOf(asText())
    }
    isContainerNode -> when {
        isObject -> ObjectNode(jsonMapper.nodeFactory).also { objectNode ->
            fieldNames().forEachRemaining { fieldName ->
                objectNode[fieldName] = this[fieldName].convert(jsonMapper)
            }
        }
        isArray -> ArrayNode(JsonMapper().nodeFactory).also { arrayNode ->
            for (element in this) {
                arrayNode.add(element.convert(jsonMapper))
            }
        }
        else -> NullNode.instance
    }
    else -> NullNode.instance
}