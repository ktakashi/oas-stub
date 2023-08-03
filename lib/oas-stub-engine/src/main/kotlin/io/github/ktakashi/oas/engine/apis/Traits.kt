package io.github.ktakashi.oas.engine.apis

import io.swagger.v3.oas.models.SpecVersion
import jakarta.ws.rs.core.MediaType

interface MediaSupport {
    fun supports(mediaType: MediaType): Boolean
}

abstract class OpenApiVersionSupport(private val supportVersion: SpecVersion) {
    fun supports(version: SpecVersion): Boolean = supportVersion == version
}
