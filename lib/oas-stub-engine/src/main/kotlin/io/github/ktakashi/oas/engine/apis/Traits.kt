package io.github.ktakashi.oas.engine.apis

import io.swagger.v3.oas.models.SpecVersion
import io.swagger.v3.oas.models.media.Schema
import jakarta.ws.rs.core.MediaType

interface MediaSupport {
    fun supports(mediaType: MediaType): Boolean
}

interface OpenApiVersionSupport {
    fun supports(version: SpecVersion): Boolean
    fun supports(schema: Schema<*>) = supports(schema.specVersion)
}
