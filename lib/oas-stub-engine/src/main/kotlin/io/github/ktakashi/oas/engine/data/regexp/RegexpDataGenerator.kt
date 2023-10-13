package io.github.ktakashi.oas.engine.data.regexp

class RegexpDataGenerator(private val parser: RegexpParser) {
    constructor(): this(RegexpParser())
    fun generate(pattern: String) = generate(parser.parse(pattern))

    private fun generate(pattern: RegexpNode): String = TODO()
}