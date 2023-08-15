package io.github.ktakashi.oas.engine.apis

import io.github.ktakashi.oas.plugin.apis.RequestContext
import io.github.ktakashi.oas.plugin.apis.ResponseContext
import io.swagger.v3.oas.models.Operation
import io.swagger.v3.oas.models.media.Content
import io.swagger.v3.oas.models.media.Schema
import jakarta.inject.Inject
import jakarta.inject.Named
import jakarta.inject.Singleton
import jakarta.ws.rs.core.MediaType
import java.util.Optional
import org.apache.http.HttpStatus

@Named @Singleton
class ApiResultProvider
@Inject constructor(private val contentDecider: ApiContentDecider,
                    private val populators: Set<ApiDataPopulator>,
                    private val anyPopulators: Set<ApiAnyDataPopulator>) {
    fun provideResult(operation: Operation, requestContext: ApiContextAwareRequestContext): ResponseContext = when (val decision = contentDecider.decideContent(requestContext, operation)) {
        is ContentFound -> decision.content.map { content -> toResponseContext(requestContext, decision.status, content) }
                .orElseGet { DefaultResponseContext(status = decision.status) }
        is ContentNotFound -> decision.responseContext
    }

    private fun toResponseContext(requestContext: ApiContextAwareRequestContext, status: Int, content: Content): ResponseContext {
        if (content.isEmpty()) {
            return DefaultResponseContext(status = status)
        }
        return getMostExpectedMedia(content, requestContext)
                .map { mediaType ->
                    content[mediaType]?.schema?.let { schema ->
                        val data = populate(mediaType, schema)
                        DefaultResponseContext(status = status, content = Optional.of(data), contentType = Optional.of(mediaType))
                    }
                }.orElseGet {
                    DefaultResponseContext(status = HttpStatus.SC_INTERNAL_SERVER_ERROR)
                }
    }

    private fun populate(mediaType: String, schema: Schema<*>): ByteArray = MediaType.valueOf(mediaType).let { mt ->
        populators.firstOrNull { p -> p.supports(mt) && p.supports(schema) }
                ?.populate(schema)
                ?: anyPopulators.firstOrNull { p -> p.supports(schema) }
                        ?.populate(schema)
                ?: byteArrayOf()
    }
}

private fun getMostExpectedMedia(content: Content, request: RequestContext): Optional<String> {
    return getAccept(request).map { m -> "${m.type}/${m.subtype}" }
            .filter { m -> content.containsKey(m) }
            .or {
                if (content.containsKey(MediaType.APPLICATION_JSON)) {
                    Optional.of(MediaType.APPLICATION_JSON)
                } else {
                    Optional.ofNullable(content.keys.firstOrNull())
                }
            }

}

fun getAccept(request: RequestContext): Optional<MediaType> = request.headers["Accept"]?.let { v ->
    if (v.isEmpty()) {
        Optional.empty()
    } else {
        Optional.of(MediaType.valueOf(v[0]))
    }
} ?: Optional.empty()
