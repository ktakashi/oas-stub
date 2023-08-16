package io.github.ktakashi.oas.engine.paths

import java.util.Optional
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class PathMatcherTest {

    @Test
    fun findMatchingPath() {
        val data = mapOf(
                "/path" to true,
                "/path/{val}" to true,
                "/path2/{val}/" to true
        )

        assertEquals(Optional.of(true), findMatchingPathValue("/path", data))
        assertEquals(Optional.of(true), findMatchingPathValue("/path/1", data))
        assertEquals(Optional.empty<Boolean>(), findMatchingPathValue("/path/1/", data))
        assertEquals(Optional.empty<Boolean>(), findMatchingPathValue("/path/1/val", data))
        assertEquals(Optional.of(true), findMatchingPathValue("/path2/2/", data))
        assertEquals(Optional.empty<Boolean>(), findMatchingPathValue("/path2/2", data))
    }
}
