package io.github.ktakashi.oas.api.http

import java.util.Optional
import org.reactivestreams.Publisher

/**
 * Response context interface.
 *
 * This context can be retrieved via [PluginContext].
 * The initial context contains default values of the response.
 * e.g. the [status] might be 200 or 4xx, if the request validation failed.
 *
 * To modify the context, users need to rebuild the context via [mutate] method.
 */
interface ResponseContext {
    /**
     * Responding HTTP status
     */
    val status: Int

    /**
     * Responding HTTP body if needed
     */
    val content: Optional<ByteArray>

    /**
     * Responding Content-Type if needed
     */
    val contentType: Optional<String>

    /**
     * Response headers
     */
    val headers: Map<String, List<String>>

    /**
     * Creates a [ResponseContextBuilder] to mutate response.
     */
    fun mutate(): ResponseContextBuilder

    /**
     * Emits the context to [response].
     *
     * **WARNING** this method is not meant to be called in a plugin.
     */
    fun emitResponse(response: HttpResponse): Publisher<ByteArray>

    /**
     * [ResponseContext] builder interface.
     *
     * This interface can be used to mutate default response context provided by the application.
     */
    interface ResponseContextBuilder {
        /**
         * Sets the response HTTP status
         */
        fun status(status: Int): ResponseContextBuilder

        /**
         * Sets the response content.
         *
         * If null is passed, then no content.
         *
         * NOTE: null doesn't mean status of 204. If this is needed, you need to set status manually
         */
        fun content(content: ByteArray?): ResponseContextBuilder

        /**
         * Set the response Content-Type.
         */
        fun contentType(contentType: String?): ResponseContextBuilder

        /**
         * Replace the response headers.
         */
        fun headers(headers: Map<String, List<String>>): ResponseContextBuilder

        /**
         * Builds a [ResponseContext]
         */
        fun build(): ResponseContext
    }
}
