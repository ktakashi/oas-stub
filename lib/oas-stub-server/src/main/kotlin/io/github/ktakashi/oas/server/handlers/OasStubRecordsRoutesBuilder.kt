package io.github.ktakashi.oas.server.handlers

import com.fasterxml.jackson.databind.ObjectMapper
import io.github.ktakashi.oas.engine.apis.APPLICATION_JSON
import io.github.ktakashi.oas.engine.apis.APPLICATION_PROBLEM_JSON
import io.github.ktakashi.oas.model.ApiRecord
import io.github.ktakashi.oas.server.options.OasStubStubOptions
import io.github.ktakashi.oas.storages.apis.SessionStorage
import io.netty.handler.codec.http.HttpHeaderNames
import io.netty.handler.codec.http.HttpHeaderValues
import java.util.Optional
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import reactor.core.publisher.Mono

class OasStubRecordsRoutesBuilder(private val options: OasStubStubOptions): OasStubRoutesBuilder, KoinComponent {
    private val sessionStorage by inject<SessionStorage>()
    private val objectMapper by inject<ObjectMapper>()

    override fun build(routes: OasStubRoutes) {
        if (options.enableRecord) {
            routes.get("${options.adminPath}${options.recordPath}/$PATH_SEGMENT") { request ->
                Mono.justOrEmpty(request.param(PATH_VARIABLE_NAME)?.let { context ->
                    sessionStorage.getApiRecords(context).map { records ->
                        request.responseBuilder()
                            .ok()
                            .header(HttpHeaderNames.CONTENT_TYPE.toString(), HttpHeaderValues.APPLICATION_JSON.toString())
                            .body(records.records.map { toResponse(it) })
                    }
                }).switchIfEmpty(Mono.defer { Mono.just(request.responseBuilder().notFound().build())})
            }.delete("${options.adminPath}${options.recordPath}/$PATH_SEGMENT") { request ->
                Mono.just(request.param(PATH_VARIABLE_NAME)?.let {
                    sessionStorage.clearApiRecords(it)
                    request.responseBuilder().noContent().build()
                } ?: request.responseBuilder().notFound().build())
            }
        }
    }

    private fun toResponse(record: ApiRecord): Map<String, Any> = with(record) {
        mapOf(
            "method" to method,
            "path" to path,
            "request" to with(request) {
                mapOf(
                    "contentType" to contentType,
                    "headers" to headers,
                    "cookies" to cookies,
                    "body" to contentType.map { body.tryStringify(it) }.orElse(body)
                )
            },
            "response" to with(response) {
                mapOf(
                    "status" to status,
                    "contentType" to contentType,
                    "headers" to headers,
                    "body" to contentType.map { body.tryStringify(it) }.orElse(body)
                )
            }
        )
    }

    private fun Optional<ByteArray>.tryStringify(contentType: String) = if (contentType.startsWith("text/")) {
        map { String(it) }
    } else if (contentType.startsWith(APPLICATION_JSON) || contentType.endsWith(APPLICATION_PROBLEM_JSON)) {
        map { objectMapper.readTree(it) }
    } else {
        this
    }

}
