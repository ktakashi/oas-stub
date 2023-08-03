package io.github.ktakashi.oas.engine.apis.json

import io.github.ktakashi.oas.engine.apis.MediaSupport
import jakarta.ws.rs.core.MediaType

interface JsonMediaSupport: MediaSupport {
    override fun supports(mediaType: MediaType): Boolean = MediaType.APPLICATION_JSON_TYPE == mediaType
}
