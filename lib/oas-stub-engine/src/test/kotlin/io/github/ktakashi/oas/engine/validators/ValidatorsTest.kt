package io.github.ktakashi.oas.engine.validators

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

class ValidatorsTest {
    @ParameterizedTest
    @CsvSource(value = [
        "2023-08-01,true",
        "2023-13-01,false",
        "2023-12-32,false",
        "2023-08-01T10:21:30,false",
    ])
    fun localDateValidatorTest(s: String, result: Boolean) {
        val validator = LocalDateValidator()
        assertEquals(result, validator.validate(StringValidationContext(s, "date")))
    }

    @ParameterizedTest
    @CsvSource(value = [
        "2023-12-01,false",
        "2023-08-01T10:21:30,true",
        "2023-08-01T10:21:30Z,true",
        "2023-08-01T10:21:30.000Z,true",
    ])
    fun offsetDateValidatorTest(s: String, result: Boolean) {
        val validator = OffsetDateValidator()
        assertEquals(result, validator.validate(StringValidationContext(s, "date-time")))
    }
}
