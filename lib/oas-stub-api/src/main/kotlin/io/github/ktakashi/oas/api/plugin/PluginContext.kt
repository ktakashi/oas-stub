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
     *
     * This is derived the stub application [JsonMapper]
     */
    val objectReader: ObjectReader

    /**
     * Default [ObjectWriter]
     *
     * This is derived the stub application [JsonMapper]
     */
    val objectWriter: ObjectWriter
}
