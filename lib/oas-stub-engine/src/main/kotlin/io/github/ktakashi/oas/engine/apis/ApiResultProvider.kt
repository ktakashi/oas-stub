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
@Inject constructor(private val contentDecider: ApiContentDecider) {
    fun provideResult(operation: Operation, requestContext: ApiContextAwareRequestContext): ResponseContext = when (val decision = contentDecider.decideContent(requestContext, operation)) {
        is ContentFound -> toResponseContext(requestContext, decision.status, decision.content)
        is ContentNotFound -> decision.responseContext
    }

    private fun toResponseContext(requestContext: ApiContextAwareRequestContext, status: Int, content: Content): ResponseContext {
        if (content.isEmpty()) {
            return ResponseContext(status)
        }
        return getMostExpectedMedia(content, requestContext)
                .map { mediaType ->
                    content[mediaType]?.schema?.let { schema ->
                        val data = populate(mediaType, schema)
                        ResponseContext(status, content = Optional.of(data.toByteArray()), contentType = Optional.of(mediaType))
                    }
                }.orElseGet {
                    ResponseContext(HttpStatus.SC_INTERNAL_SERVER_ERROR)
                }
    }

    private fun populate(mediaType: String, schema: Schema<*>): String {
        TODO("")
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
