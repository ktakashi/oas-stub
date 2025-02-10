package io.github.ktakashi.oas.engine.apis

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import io.swagger.v3.oas.models.SpecVersion
import io.swagger.v3.oas.models.media.Schema

interface ApiDataValidator<T>: MediaSupport, OpenApiVersionSupport {
    fun validate(input: ByteArray, schema: Schema<*>): ApiValidationResult

    fun checkSchema(value: T, rootProperty: String, schema: Schema<*>): ApiValidationResult
}

interface ApiDataPopulator: MediaSupport, OpenApiVersionSupport {
    val objectMapper: ObjectMapper
    fun populate(schema: Schema<*>): ByteArray
}

interface ApiAnyDataPopulator: ApiDataPopulator

abstract class ApiDataProcessor(private val version: SpecVersion): MediaSupport, OpenApiVersionSupport {
    override fun supports(version: SpecVersion): Boolean = this.version == version
}

abstract class AbstractApiDataValidator<T>(version: SpecVersion): ApiDataProcessor(version), ApiDataValidator<T>

abstract class AbstractApiDataPopulator(override val objectMapper: ObjectMapper, version: SpecVersion): ApiDataProcessor(version), ApiDataPopulator {
    override fun populate(schema: Schema<*>): ByteArray = try {
        val node = populateNode(schema)
        objectMapper.writeValueAsBytes(node)
    } catch (e: JsonProcessingException) {
        "null".toByteArray()
    }
    abstract fun populateNode(schema: Schema<*>): JsonNode
}
