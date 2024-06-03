package io.github.ktakashi.oas.engine.parsers

import io.swagger.v3.core.util.Yaml
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.parser.OpenAPIV3Parser
import io.swagger.v3.parser.converter.SwaggerConverter
import io.swagger.v3.parser.core.models.ParseOptions
import org.slf4j.LoggerFactory
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

private val parseOption = ParseOptions().apply {
    isResolve = true
    isResolveFully = true
}

private val nonResolveOption = ParseOptions()

private val swaggerParsers = listOf(::OpenAPIV3Parser, ::SwaggerConverter)

private val logger = LoggerFactory.getLogger(ParsingService::class.java)

class ParsingService {
    fun parse(content: String, resolve: Boolean = true): Mono<OpenAPI> = Flux.fromIterable(swaggerParsers)
        .flatMap { v ->
            try {
                val result = v().readContents(content, null, if (resolve) parseOption else nonResolveOption)
                if (result.messages != null && result.messages.isNotEmpty()) {
                    logger.warn("Parsing message(s): {}", result.messages)
                }
                Mono.justOrEmpty(result.openAPI)
            } catch (e: Throwable) {
                logger.error("Failed to parse: {}", e.message, e)
                Mono.empty()
            }
        }.next()


    fun toYaml(openAPI: OpenAPI): Mono<String> = Mono.defer { Mono.just(Yaml.pretty(openAPI)) }
}
