package io.github.ktakashi.oas.plugin.apis

import java.time.Duration
import java.util.Optional

interface Storage {
    fun <T> put(key: String, value: T)
    fun <T> put(key: String, value: T, ttl: Duration) {
        put(key, value)
    }
    fun <T: Any> get(key: String, type: Class<T>): Optional<T>
    fun delete(key: String): Boolean
}
