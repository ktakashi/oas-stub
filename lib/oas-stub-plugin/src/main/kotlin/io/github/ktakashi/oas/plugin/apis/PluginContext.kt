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
     * Retrieves stub data associated to [label] if exists
     */
    fun getApiDate(label: String): Optional<ByteArray>

    /**
     * Retrieves stub data associated to [label] if exists, otherwise returns [defaultValue]
     */
    fun getStubData(label: String, defaultValue: ByteArray): ByteArray {
        return getApiDate(label).orElse(defaultValue)
    }

    /**
     * Returns session storage.
     */
    val sessionStorage: Storage

    /**
     * Returns persistent storage
     */
    val persistentStorage: Storage
}
