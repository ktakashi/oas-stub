package io.github.ktakashi.oas.engine.apis

import io.github.ktakashi.oas.engine.parsers.ParsingService
import io.github.ktakashi.oas.engine.readStringContent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

class ApiServicesTest {
    private val parsingService = ParsingService()
    private val openApi = parsingService.parse(readStringContent("/schema/validation_3.0.3.yaml")).orElseThrow()
    @ParameterizedTest
    @CsvSource(value = [
        "/object,/object",
        "/v1/object,/object"
    ])
    fun adjustBasePathTest(path: String, expected: String) {
        val r = adjustBasePath(path, openApi)
        assertTrue(r.isPresent)
        assertEquals(expected, r.get())
    }
}
