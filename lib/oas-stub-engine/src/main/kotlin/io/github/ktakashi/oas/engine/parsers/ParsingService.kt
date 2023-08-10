package io.github.ktakashi.oas.engine.parsers

import io.swagger.v3.core.util.Yaml
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.parser.OpenAPIV3Parser
import io.swagger.v3.parser.converter.SwaggerConverter
import io.swagger.v3.parser.core.extensions.SwaggerParserExtension
import io.swagger.v3.parser.core.models.ParseOptions
import jakarta.inject.Named
import jakarta.inject.Singleton
import java.util.Optional

private val parseOption = ParseOptions().apply {
    isResolve = true
    isResolveFully = true
}

private enum class OasVersions(val provider: () -> SwaggerParserExtension) {
    V3({ OpenAPIV3Parser() }), // V3 first, otherwise we'd get something weird
    V2({ SwaggerConverter() })
}

@Named @Singleton
class ParsingService {
    fun parse(content: String): Optional<OpenAPI> =
        Optional.ofNullable(OasVersions.entries
                .asSequence()
                .map { v -> v.provider().readContents(content, null, parseOption)?.openAPI }
                .firstOrNull { v -> v != null })

    fun sanitize(content: String): Optional<String> = parse(content).map(Yaml::pretty)
}
