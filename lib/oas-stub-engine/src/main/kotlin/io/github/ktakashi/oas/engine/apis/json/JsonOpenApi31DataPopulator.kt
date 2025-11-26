package io.github.ktakashi.oas.engine.apis.json

import io.github.ktakashi.oas.engine.apis.ApiAnyDataPopulator
import io.swagger.v3.oas.models.SpecVersion
import io.swagger.v3.oas.models.media.JsonSchema
import io.swagger.v3.oas.models.media.Schema
import tools.jackson.databind.JsonNode
import tools.jackson.databind.json.JsonMapper
import tools.jackson.databind.node.NullNode
import tools.jackson.databind.node.ObjectNode

class JsonOpenApi31DataPopulator(jsonMapper: JsonMapper): JsonMediaSupport, ApiAnyDataPopulator, JsonApiDataPopulator(jsonMapper, SpecVersion.V31) {
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
        "any" -> ObjectNode(jsonMapper.nodeFactory) // let's make it an empty object then
        else -> NullNode.instance
    }
}
