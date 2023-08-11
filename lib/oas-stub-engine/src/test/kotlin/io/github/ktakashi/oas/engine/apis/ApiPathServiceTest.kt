package io.github.ktakashi.oas.engine.apis

import java.util.Optional
import org.junit.jupiter.api.Test

import org.junit.jupiter.api.Assertions.*

class ApiPathServiceTest {

    private val testSubject = ApiPathService("/oas")

    @Test
    fun findMatchingPath() {
        val data = mapOf(
                "/path" to true,
                "/path/{val}" to true,
                "/path2/{val}/" to true
        )

        assertEquals(Optional.of(true), testSubject.findMatchingPath("/path", data))
        assertEquals(Optional.of(true), testSubject.findMatchingPath("/path/1", data))
        assertEquals(Optional.empty<Boolean>(), testSubject.findMatchingPath("/path/1/", data))
        assertEquals(Optional.empty<Boolean>(), testSubject.findMatchingPath("/path/1/val", data))
        assertEquals(Optional.of(true), testSubject.findMatchingPath("/path2/2/", data))
        assertEquals(Optional.empty<Boolean>(), testSubject.findMatchingPath("/path2/2", data))
    }
}
