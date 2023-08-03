package io.github.ktakashi.oas.engine.validators

import jakarta.inject.Named
import jakarta.inject.Singleton
import jakarta.mail.internet.AddressException
import jakarta.mail.internet.InternetAddress
import java.time.DateTimeException
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeParseException
import java.util.regex.Pattern

data class StringValidationContext(override val target: String, val type: String, val format: Pattern? = null): ValidationContext<String>

abstract class StringFormatValidator(private val pattern: Pattern?, private val formatType: String): Validator<String> {
    override fun validate(context: ValidationContext<String>): Boolean = pattern?.matcher(context.target)?.matches() ?: false

    override fun shouldValidate(context: ValidationContext<*>): Boolean = if (context is StringValidationContext) {
        formatType == context.type
    } else {
        false
    }
}

@Named @Singleton
class FormatValidator: StringFormatValidator(null, "format") {
    override fun validate(context: ValidationContext<String>): Boolean = if (context is StringValidationContext) {
        context.format?.matcher(context.target)?.matches() ?: true
    } else {
        false
    }
}

abstract class DateValidator<T>(private val parser: (v: String) -> T, type: String): StringFormatValidator(null, type) {
    override fun validate(context: ValidationContext<String>): Boolean = if (context is StringValidationContext) {
        try {
            parser(context.target)
            true
        } catch (e: DateTimeException) {
            false
        }
    } else {
        false
    }
}

@Named @Singleton
class LocalDateValidator: DateValidator<LocalDate>(LocalDate::parse, "date")

private fun parseOffsetDateTime(s: String): OffsetDateTime = try {
    OffsetDateTime.parse(s)
} catch (e: DateTimeParseException) {
    LocalDateTime.parse(s).atOffset(ZoneOffset.UTC)
}
@Named @Singleton
class OffsetDateValidator: DateValidator<OffsetDateTime>(::parseOffsetDateTime, "date-time")

private val uuidPattern = Pattern.compile("^[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$")
@Named @Singleton
class UUIDValidator: StringFormatValidator(uuidPattern, "uuid")

@Named @Singleton
class EmailValidator: StringFormatValidator(null, "email") {
    override fun validate(context: ValidationContext<String>): Boolean = if (context is StringValidationContext) {
        try {
            val emailAddr = InternetAddress(context.target)
            emailAddr.validate()
            true
        } catch (e: AddressException) {
            false
        }
    } else false
}
