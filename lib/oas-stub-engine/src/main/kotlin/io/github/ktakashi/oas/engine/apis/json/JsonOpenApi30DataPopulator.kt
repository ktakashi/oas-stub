package io.github.ktakashi.oas.engine.apis.json

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.NullNode
import io.github.ktakashi.oas.engine.apis.AbstractApiDataPopulator
import io.github.ktakashi.oas.engine.apis.ApiAnyDataPopulator
import io.swagger.v3.oas.models.SpecVersion
import io.swagger.v3.oas.models.media.ArraySchema
import io.swagger.v3.oas.models.media.BooleanSchema
import io.swagger.v3.oas.models.media.ComposedSchema
import io.swagger.v3.oas.models.media.IntegerSchema
import io.swagger.v3.oas.models.media.NumberSchema
import io.swagger.v3.oas.models.media.Schema

class JsonOpenApi30DataPopulator(private val objectMapper: ObjectMapper): JsonMediaSupport, ApiAnyDataPopulator, AbstractApiDataPopulator(SpecVersion.V30) {
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
        "string" -> populateTextNode(schema)
        "number" -> populateDoubleNode(schema as NumberSchema)
        "integer" -> populateIntNode(schema as IntegerSchema)
        "boolean" -> populateBooleanNode(schema as BooleanSchema)
        "array" -> populateArray(schema as ArraySchema)
        "object" -> populateObject(schema)
        else -> NullNode.instance
    }

    private fun populateObject(schema: Schema<*>): JsonNode = populateObjectNode(schema, objectMapper, ::populateNode)

    private fun populateArray(schema: ArraySchema): JsonNode = populateArrayNode(schema, objectMapper, ::populateNode)
}

