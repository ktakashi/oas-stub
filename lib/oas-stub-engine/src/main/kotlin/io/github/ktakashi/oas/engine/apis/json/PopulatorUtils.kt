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
import dk.brics.automaton.RegExp
import dk.brics.automaton.State
import io.swagger.v3.oas.models.media.Schema
import java.math.BigDecimal
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.UUID
import kotlin.math.min
import kotlin.random.Random
import org.slf4j.LoggerFactory

internal typealias NodePopulator = (Schema<*>) -> JsonNode

private val logger = LoggerFactory.getLogger("io.github.ktakashi.oas.engine.apis.json.PopulatorUtils")

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

internal fun populateDoubleNode(schema: Schema<*>): JsonNode = handleExample(schema) { v -> DoubleNode.valueOf(NumberInput.parseDouble(v.toString())) }
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

private fun generateMatchingString(pattern: String): String {
    val random = Random.Default
    tailrec fun generate(sb: StringBuilder, state: State, minLength: Int, maxLength: Int): String {
        if (state.isAccept) {
            if (sb.length in minLength..maxLength) {
                return sb.toString()
            }
            if (random.nextInt() > 0.3 * Int.MAX_VALUE && sb.length >= minLength) {
                return sb.toString()
            }
        }
        val transitions = state.getSortedTransitions(false)
        if (transitions.isEmpty()) {
            return sb.toString()
        }
        val next = random.nextInt(transitions.size)
        val randomTransition = transitions[next]
        val diff = randomTransition.max - randomTransition.min + 1
        val offset = if (diff > 0) random.nextInt(diff) else diff
        return generate(sb.append(randomTransition.min + offset), randomTransition.dest, minLength, maxLength)
    }
    val regexp = RegExp(normalize(pattern))
    val automaton = regexp.toAutomaton()
    val state = automaton.initialState
    val sb = StringBuilder()
    return generate(sb, state, 1, Int.MAX_VALUE)
}

private const val SPECIAL_CHARS = ".^$*+?(){|[\\@"
private fun normalize(pattern: String): String {
    fun quote(sb: StringBuilder, start: Int): Int {
        var i = start
        sb.append("\\Q")
        while (i < pattern.length) {
            val c = pattern[i++]
            if (SPECIAL_CHARS.indexOf(c) > 0) {
                sb.append('\\')
            }
            sb.append(c)
        }
        sb.append("\\E")
        return i
    }
    fun appendRange(sb: StringBuilder, range: String, inCharSet: Boolean) {
        if (!inCharSet) sb.append('[')
        sb.append(range)
        if (!inCharSet) sb.append(']')
    }
    val sb = StringBuilder()
    var i = 0
    var inCharset = false
    while (i < pattern.length) {
        when (val c = pattern[i++]) {
            '\\' -> when (val c1 = pattern[i++]) {
                'd' -> sb.append("[0-9]")
                'D' -> sb.append("[^0-9]")
                's' -> sb.append("[ \t\n\r]")
                'S' -> sb.append("[^ \t\n\r]")
                'w' -> sb.append("[0-9a-zA-Z]")
                'W' -> sb.append("[^0-9a-zA-Z]")
                'Q' -> i = quote(sb, i)
                else -> sb.append(c).append(c1)
            }
            '[' -> {
                inCharset = true
                sb.append(c)
            }
            ']' -> {
                inCharset = false
                sb.append(c)
            }
            else -> sb.append(c)
        }
    }
    return sb.toString()
}
