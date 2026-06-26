package io.github.ktakashi.oas.api.plugin

import io.github.ktakashi.oas.api.http.RequestContext
import io.github.ktakashi.oas.api.http.ResponseContext
import io.github.ktakashi.oas.api.storage.Storage
import java.util.Optional
import tools.jackson.databind.ObjectReader
import tools.jackson.databind.ObjectWriter

/**
 * Plugin context
 */
interface PluginContext {
    /**
     * Returns request context
     */
    val requestContext: RequestContext

    /**
     * Returns response context
     */
    val responseContext: ResponseContext

    /**
     * Retrieves stub data associated to [label] as [clazz] if exists
     */
    fun <T> getApiData(label: String, clazz: Class<T>): Optional<T & Any>

    /**
     * Returns session storage.
     */
    val sessionStorage: Storage

    /**
     * Default [ObjectReader]
     */
    val objectReader: ObjectReader

    /**
     * Default [ObjectWriter]
     */
    val objectWriter: ObjectWriter
}
