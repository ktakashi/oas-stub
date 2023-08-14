package io.github.ktakashi.oas.engine.apis

import io.github.ktakashi.oas.engine.parsers.ParsingService
import io.github.ktakashi.oas.engine.readStringContent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

class ApiServicesTest {
    private val parsingService = ParsingService()

    @ParameterizedTest
    @CsvSource(value = [
        "/schema/validation_3.0.3.yaml,/object,/object",
        "/schema/validation_3.0.3.yaml,/v1/object,/object",
        "/schema/validation_3.0.3.yaml,/v1/objects,/objects",
        "/schema/validation_3.0.3.yaml,/v1/object/1,/object/1",
        "/schema/servers_test0_3.0.3.yaml,/object,/object",
        "/schema/servers_test0_3.0.3.yaml,/v2/beta/object,/object",
        "/schema/servers_test0_3.0.3.yaml,/v3/object,/v3/object",
    ])
    fun adjustBasePathTest(schema: String, path: String, expected: String) {
        val openApi = parsingService.parse(readStringContent(schema)).orElseThrow()
        val r = adjustBasePath(path, openApi)
        if ("<null>" == expected) {
            assertTrue(r.isEmpty)
        } else {
            assertTrue(r.isPresent)
            assertEquals(expected, r.get())
        }
    }
}
