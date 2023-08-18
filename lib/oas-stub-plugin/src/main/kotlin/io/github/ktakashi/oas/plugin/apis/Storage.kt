package io.github.ktakashi.oas.plugin.apis

import java.time.Duration
import java.util.Optional

/**
 * Storage interface.
 *
 * This interface provides methods to access session storage of the API.
 * A storage can be implemented out-side of the library. So, the behaviour
 * of storages may vary.
 *
 * The storage implementation must follow the *must*, *should*, *should not*
 * and *must not* keywords.
 */
interface Storage {
    /**
     * Puts [value] associating to [key] into the storage.
     *
     * The [value] *must* be retrievable via [Storage.get] method with [key].
     *
     * The stored value *should* not expire unless the storage has hard expiration period.
     */
    fun <T> put(key: String, value: T) = put(key, value, Duration.ZERO)


    /**
     * Puts [value] associating to [key] into the storage.
     *
     * The [value] *must* be retrievable via [Storage.get] method with [key].
     *
     * If [ttl] is equivalent to 0, then the stored value should not expire.
     * Otherwise, it *should* expire in [ttl] period.
     */
    fun <T> put(key: String, value: T, ttl: Duration): Boolean

    /**
     * Retrieves the value associated to [key].
     *
     * The returning value *must* be deserialized to [type] instance.
     */
    fun <T: Any> get(key: String, type: Class<T>): Optional<T>

    /**
     * Deletes the value associated to [key] if exists.
     */
    fun delete(key: String): Boolean
}
