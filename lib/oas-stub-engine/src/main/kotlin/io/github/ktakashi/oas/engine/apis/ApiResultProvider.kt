package io.github.ktakashi.oas.engine.apis

import io.github.ktakashi.oas.api.http.RequestContext
import io.github.ktakashi.oas.api.http.ResponseContext
import io.swagger.v3.oas.models.Operation
import io.swagger.v3.oas.models.PathItem
import io.swagger.v3.oas.models.media.Content
import io.swagger.v3.oas.models.media.Schema
import jakarta.ws.rs.core.MediaType
import java.util.Optional
import org.apache.http.HttpStatus
import org.slf4j.LoggerFactory
import reactor.core.publisher.Mono

private val logger = LoggerFactory.getLogger(ApiResultProvider::class.java)

class ApiResultProvider(private val contentDecider: ApiContentDecider,
                        private val populators: Set<ApiDataPopulator>,
                        private val anyPopulators: Set<ApiAnyDataPopulator>) {
    fun provideResult(path: PathItem, operation: Operation, requestContext: ApiContextAwareRequestContext): Mono<ResponseContext> = Mono.just(when (val decision = contentDecider.decideContent(requestContext, path, operation)) {
        is ContentFound -> decision.content.map { content -> toResponseContext(requestContext, decision.status, content) }
                .orElseGet { DefaultResponseContext(status = decision.status) }
        is ContentNotFound -> decision.responseContext
    })

    private fun toResponseContext(requestContext: ApiContextAwareRequestContext, status: Int, content: Content): ResponseContext {
        if (content.isEmpty()) {
            return DefaultResponseContext(status = status)
        }
        return getMostExpectedMedia(content, requestContext)
                .map { mediaType ->
                    content[mediaType]?.schema?.let { schema ->
                        val data = populate(mediaType, schema)
                        DefaultResponseContext(status = status, content = Optional.ofNullable(data), contentType = Optional.of(mediaType))
                    }
                }.orElseGet {
                    DefaultResponseContext(status = HttpStatus.SC_INTERNAL_SERVER_ERROR)
                }
    }

    private fun populate(mediaType: String, schema: Schema<*>): ByteArray? = MediaType.valueOf(mediaType).let { mt ->
        logger.debug("MediaType: {}, Schema spec: {}, populators: {}, anyPopulators: {}", mt, schema.specVersion, populators, anyPopulators)
        populators.firstOrNull { p -> p.supports(mt) && p.supports(schema) }?.populate(schema)
            ?: anyPopulators.firstOrNull { p -> p.supports(schema) }?.populate(schema)
    }
}

private fun getMostExpectedMedia(content: Content, request: RequestContext): Optional<String> = getAccepts(request)
    .map { m -> "${m.type}/${m.subtype}" }
    .firstOrNull { m -> content.containsKey(m) }
    ?.let { m -> Optional.of(m) }
    ?: if (content.containsKey(MediaType.APPLICATION_JSON)) {
        Optional.of(MediaType.APPLICATION_JSON)
    } else {
        Optional.ofNullable(content.keys.firstOrNull())
    }

private val DELIMITER = Regex("\\s*,\\s*")
private fun getAccepts(request: RequestContext): List<MediaType> = request.headers["Accept"]
    ?.flatMap { value -> value.split(DELIMITER) }
    ?.map { v ->
        // MediaType#valueOf can't handle '*', so handle it manually
        if (v == "*") {
            MediaType.WILDCARD_TYPE
        } else {
            MediaType.valueOf(v)
        }
    }
    ?: listOf()
