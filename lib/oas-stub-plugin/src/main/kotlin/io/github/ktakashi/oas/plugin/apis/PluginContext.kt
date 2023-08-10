package io.github.ktakashi.oas.plugin.apis

import java.util.Optional

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
}
