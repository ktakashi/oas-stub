package io.github.ktakashi.oas.engine.data.regexp

import io.github.ktakashi.oas.engine.data.charset.CharSet
import java.io.StringWriter
import java.io.Writer
import kotlin.random.Random

class RegexpDataGenerator(private val parser: RegexpParser,
                          private val random: Random) {
    constructor(): this(RegexpParser(), Random.Default)
    fun generate(pattern: String) = generate(parser.parse(pattern))

    private fun generate(pattern: RegexpNode): String = StringWriter().use {
        generate(it, pattern)
        it.toString()
    }

    private fun generate(sink: Writer, pattern: RegexpNode) {
        when (pattern) {
            is RegexpAny -> fromCharSet(CharSet.PRINTABLE, sink)
            is RegexpCharSet -> fromCharSet(pattern.charset, sink)
            is RegexpAlter -> generate(sink, pattern.regexps[0])
            is RegexpSequence -> pattern.regexps.forEach { generate(sink, it) }
            is RegexpPatternChar -> sink.append(pattern.char)
            is RegexpComplement -> when (pattern.regexp) {
                is RegexpCharSet -> fromCharSet(pattern.regexp.charset.complement(), sink)
                else -> Unit // Something is wrong, at least not the one constructed by the parser
            }
            is RegexpRepetition -> randomizedRepetition(pattern, sink)
            else -> Unit // do nothing, e.g. null sequence
        }
    }

    private fun randomizedRepetition(pattern: RegexpRepetition, sink: Writer) {
        // NOTE: Repetition is optimised, so we only have 0 or more
        val offset = pattern.max - pattern.min
        val count = pattern.min + (if (offset > 0) random.nextInt(offset) else offset)
        for (i in 0 until count) {
            generate(sink, pattern.regexp)
            if (!random.nextBoolean()) break // this should prevent extremely long string
        }
    }

    private fun fromCharSet(charset: CharSet, sink: Writer) {
        val range = charset.ranges()
        val x = random.nextInt(range.size)
        val t = range[x]
        val diff = t.max - t.min
        val offset = if (diff > 0) random.nextInt(diff) else diff
        val cp = t.min + offset
        sink.write(Character.toString(cp))
    }
}