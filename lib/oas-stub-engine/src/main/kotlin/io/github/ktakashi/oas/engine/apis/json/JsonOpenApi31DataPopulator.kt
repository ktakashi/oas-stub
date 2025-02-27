package io.github.ktakashi.oas.engine.apis.json

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.NullNode
import com.fasterxml.jackson.databind.node.ObjectNode
import io.github.ktakashi.oas.engine.apis.ApiAnyDataPopulator
import io.swagger.v3.oas.models.SpecVersion
import io.swagger.v3.oas.models.media.JsonSchema
import io.swagger.v3.oas.models.media.Schema

class JsonOpenApi31DataPopulator(objectMapper: ObjectMapper): JsonMediaSupport, ApiAnyDataPopulator, JsonApiDataPopulator(objectMapper, SpecVersion.V31) {
    override fun populateNode(schema: Schema<*>): JsonNode = when {
        schema.anyOf != null && schema.anyOf.isNotEmpty() -> populateNode(schema.anyOf[0] as JsonSchema)
        schema.oneOf != null && schema.oneOf.isNotEmpty() -> populateNode(schema.oneOf[0] as JsonSchema)
        schema.allOf != null && schema.allOf.isNotEmpty() -> NullNode.instance // TODO Implement me, it's problematic...
        else -> populateNodeSimple(schema)
    }

    private fun populateNodeSimple(schema: Schema<*>): JsonNode = when (guessType(schema)) {
        "string" -> populateTextNode(schema)
        "number" -> populateDoubleNode(schema)
        "integer" -> populateIntNode(schema)
        "boolean" -> populateBooleanNode(schema)
        "array" -> populateArrayNode(schema)
        "object" -> populateObjectNode(schema)
        "any" -> ObjectNode(objectMapper.nodeFactory) // let's make it an empty object then
        else -> NullNode.instance
    }
}
