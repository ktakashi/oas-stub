package io.github.ktakashi.oas.engine.parsers

import io.github.ktakashi.oas.engine.readStringContent
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

class ParsingServiceTest {

    private val parsingService = ParsingService()

    @ParameterizedTest
    @CsvSource(value = [
        //"/schema/v3/petstore.yaml",
        "/schema/v2/petstore.yaml"
    ])
    fun parse(schema: String) {
        val r = parsingService.parse(readStringContent(schema))
        assertTrue(r.isPresent)
    }
}
