package io.github.ktakashi.oas.engine.parsers

import io.swagger.v3.core.util.Yaml
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.parser.OpenAPIV3Parser
import io.swagger.v3.parser.converter.SwaggerConverter
import io.swagger.v3.parser.core.models.ParseOptions
import jakarta.inject.Named
import jakarta.inject.Singleton
import java.util.Optional
import org.slf4j.LoggerFactory

private val parseOption = ParseOptions().apply {
    isResolve = true
    isResolveFully = true
}

private val swaggerParsers = listOf(::OpenAPIV3Parser, ::SwaggerConverter)

private val logger = LoggerFactory.getLogger(ParsingService::class.java)

@Named @Singleton
class ParsingService {
    fun parse(content: String): Optional<OpenAPI> =
        Optional.ofNullable(swaggerParsers
                .asSequence()
                .map { v ->
                    try {
                        val result = v().readContents(content, null, parseOption)
                        if (result.messages != null && result.messages.isNotEmpty()) {
                            logger.warn("Parsing message(s): {}", result.messages)
                        }
                        result.openAPI
                    } catch (e: Throwable) {
                        logger.error("Failed to parse: {}", e.message, e)
                        null
                    }
                }
                .firstOrNull { v -> v != null })

    fun sanitize(content: String): Optional<String> = parse(content).map(Yaml::pretty)

    fun toYaml(openAPI: OpenAPI): String = Yaml.pretty(openAPI)
}
