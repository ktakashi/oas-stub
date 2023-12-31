package io.github.ktakashi.oas.engine.apis.json

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import io.github.ktakashi.oas.engine.apis.ApiDataValidator
import io.github.ktakashi.oas.engine.readBinaryContent
import io.github.ktakashi.oas.engine.readStringContent
import io.swagger.v3.core.util.Yaml
import io.swagger.v3.parser.OpenAPIV3Parser
import io.swagger.v3.parser.converter.SwaggerConverter
import io.swagger.v3.parser.core.models.ParseOptions
import java.util.stream.IntStream
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.extension.ParameterContext
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.aggregator.AggregateWith
import org.junit.jupiter.params.aggregator.ArgumentsAccessor
import org.junit.jupiter.params.aggregator.ArgumentsAggregationException
import org.junit.jupiter.params.aggregator.ArgumentsAggregator
import org.junit.jupiter.params.provider.CsvSource
import org.junit.platform.commons.util.Preconditions

class JsonApiDataValidatorTest {
    private val jsonApi30DataValidator = JsonOpenApi30DataValidator(ObjectMapper(), setOf())
    private val jsonApi31DataValidator = JsonOpenApi31DataValidator(ObjectMapper(), setOf())
    private val openApiV3Parser = OpenAPIV3Parser()
    private val swaggerConverter = SwaggerConverter()
    private val parserOptions = ParseOptions().also {
        it.isResolve = true
        it.isResolveFully = true
    }
    @ParameterizedTest
    @CsvSource(value = [
        "v3,/schema/validation_3.0.3.yaml,/json/valid_input0.json,true",
        "v3,/schema/validation_3.0.3.yaml,/json/valid_input1.json,true",
        "v3,/schema/validation_3.0.3.yaml,/json/invalid_input0.json,false,$.id,$.values[0]",
    ])
    fun validate30(type: String, schemaFile: String, inputFile: String, isValid: Boolean,
                 @AggregateWith(VarargsAggregator::class) vararg properties: String) {
        validate(jsonApi30DataValidator, type, schemaFile, inputFile, isValid, *properties)
    }

    @ParameterizedTest
    @CsvSource(value = [
        "v3,/schema/validation_3.1.0.yaml,/json/valid_input0.json,true",
        "v3,/schema/validation_3.1.0.yaml,/json/valid_input1.json,true",
        "v3,/schema/validation_3.1.0.yaml,/json/invalid_input0.json,false,$.id,$.values[0]",
        "v3,/schema/validation_3.1.0.yaml,/json/valid_input2.json,true",
    ])
    fun validate31(type: String, schemaFile: String, inputFile: String, isValid: Boolean,
                 @AggregateWith(VarargsAggregator::class) vararg properties: String) {
        validate(jsonApi31DataValidator, type, schemaFile, inputFile, isValid, *properties)
    }

    private fun validate(validator: ApiDataValidator<JsonNode>, type: String, schemaFile: String, inputFile: String, isValid: Boolean, vararg properties: String) {
        val openAPI = when (type) {
            "v3" -> openApiV3Parser.readContents(readStringContent(schemaFile), null, parserOptions).openAPI
            "v2" -> swaggerConverter.readContents(readStringContent(schemaFile), null, parserOptions).let {
                val v3Content = Yaml.pretty(it.openAPI)
                openApiV3Parser.readContents(v3Content, null, parserOptions).openAPI
            }
            else -> throw IllegalArgumentException("$type is not supported... obviously")
        }
        val schema = openAPI.paths["/object"]?.post?.requestBody?.content?.get("application/json")?.schema ?: throw IllegalArgumentException("Watch out")
        val result = validator.validate(readBinaryContent(inputFile), schema)
        assertEquals(isValid, result.isValid)
        assertEquals(properties.toList(), result.validationDetails.map { v -> v.property.get() })
    }
}

@Suppress("UNCHECKED_CAST")
internal class VarargsAggregator : ArgumentsAggregator {
    @Throws(ArgumentsAggregationException::class)
    override fun aggregateArguments(accessor: ArgumentsAccessor, context: ParameterContext): Any {
        val parameterType = context.parameter.type
        Preconditions.condition(parameterType.isArray) { "must be an array type, but was $parameterType" }
        val componentType = parameterType.componentType
        return IntStream.range(context.index, accessor.size())
                .mapToObj { index: Int -> accessor[index, componentType] }
                .toArray<Any?> { size: Int -> java.lang.reflect.Array.newInstance(componentType, size) as Array<Any?>? }
    }
}
