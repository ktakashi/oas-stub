package io.github.ktakashi.oas.engine.data.regexp

import io.github.ktakashi.peg.Parser
import io.github.ktakashi.peg.SuccessResult
import io.github.ktakashi.peg.any
import io.github.ktakashi.peg.bind
import io.github.ktakashi.peg.debug
import io.github.ktakashi.peg.defer
import io.github.ktakashi.peg.eq
import io.github.ktakashi.peg.many
import io.github.ktakashi.peg.or
import io.github.ktakashi.peg.peek
import io.github.ktakashi.peg.result
import io.github.ktakashi.peg.satisfy
import io.github.ktakashi.peg.seq
import java.text.ParseException
import java.util.EnumSet

sealed interface RegexpNode

sealed interface RegexpNodeContainer: RegexpNode {
    val regexp: RegexpNode
}
sealed interface RegexpRange: RegexpNode {
    val min: Int
    val max: Int
}

data object RegexpNullSeq: RegexpNode
data object RegexpStartAnchor: RegexpNode
data object RegexpEndAnchor: RegexpNode
data object RegexpWordBoundary: RegexpNode
data object RegexpNonWordBoundary: RegexpNode
data object RegexpAny: RegexpNode
data class RegexpPatternChar(val char: Char): RegexpNode
data class RegexpCharSet(val min: Char, val max: Char): RegexpNode
data class RegexpAlter(val regexps: List<RegexpNode>): RegexpNode {
    constructor(): this(listOf())
    companion object {
        @JvmField
        val EMPTY = RegexpAlter()
        @JvmStatic
        fun of(vararg regexp: RegexpNode) = regexp.toList().filter { v -> v != RegexpNullSeq }.let {
            when (it.size) {
                0 -> RegexpNullSeq
                1 -> it[0]
                else -> RegexpAlter(it)
            }
        }
    }
    operator fun plus(regexp: RegexpNode) = RegexpAlter(regexps + regexp)
    fun unshift(regexp: RegexpNode) = RegexpAlter(listOf(regexp) + regexps)
}

data class RegexpSequence(val regexps: List<RegexpNode>): RegexpNode {
    companion object {
        @JvmField
        val EMPTY = RegexpSequence(listOf())
        @JvmStatic
        fun of(vararg regexp: RegexpNode) = regexp.toList().filter { v -> v != RegexpNullSeq }.let {
            when (it.size) {
                0 -> RegexpNullSeq
                1 -> it[0]
                else -> RegexpSequence(it)
            }
        }
    }
    operator fun plus(regexp: RegexpNode) = RegexpSequence(regexps + regexp)
    fun unshift(regexp: RegexpNode) = RegexpSequence(listOf(regexp) + regexps)
}

data class RegexpComplement(override val regexp: RegexpNode): RegexpNodeContainer
data class RegexpLookAhead(override val regexp: RegexpNode): RegexpNodeContainer
data class RegexpNegativeLookAhead(override val regexp: RegexpNode): RegexpNodeContainer
data class RegexpRepetition(override val regexp: RegexpNode, override val min: Int, override val max: Int): RegexpNodeContainer, RegexpRange
data class RegexpNonGreedyRepetition(override val regexp: RegexpNode, override val min: Int, override val max: Int): RegexpNodeContainer, RegexpRange

// Maybe for future?
enum class RegexpParserOption {
    DOTALL,
    UNICODE,
    MULTILINE
}

private fun regexpAlt(vararg regexps: RegexpNode): RegexpNode =
    regexps.fold(RegexpAlter.EMPTY) { acc, regexp ->  acc + regexp }

private const val REGEXP_SPECIAL_CHARACTERS = "^\$\\.*+?()[]{}|"
private val REGEXP_DIGIT_SET = RegexpCharSet('0', '9')
private val REGEXP_NON_DIGIT_SET = RegexpComplement(REGEXP_DIGIT_SET)
private val REGEXP_WORD_SET = regexpAlt(RegexpCharSet('a', 'z'), RegexpCharSet('A', 'Z'), REGEXP_DIGIT_SET, RegexpPatternChar('_'))
private val REGEXP_NON_WORD_SET = RegexpComplement(REGEXP_WORD_SET)
// space, \t, \r, \n and \f = \u000C
private val REGEXP_SPACE_SET = regexpAlt(RegexpPatternChar(' '), RegexpPatternChar('\t'), RegexpPatternChar('\r'), RegexpPatternChar('\n'), RegexpPatternChar('\u000C'))
private val REGEXP_NON_SPACE_SET = RegexpComplement(REGEXP_SPACE_SET)

// BNF:
//  - https://www.cs.sfu.ca/~cameron/Teaching/384/99-3/regexp-plg.html
//  - https://262.ecma-international.org/5.1/#sec-15.10.1
//  - https://www.brics.dk/automaton/doc/dk/brics/automaton/RegExp.html
class RegexpParser(private val options: EnumSet<RegexpParserOption>) {
    constructor(): this(EnumSet.noneOf(RegexpParserOption::class.java))

    fun parse(pattern: String): RegexpNode = when (val r = disjunction(pattern.asSequence())) {
        is SuccessResult -> r.value
        else -> throw ParseException("Failed to parse $pattern", 0)
    }

    private val assertion = or(
        seq(eq('^'), result(RegexpStartAnchor)), // TODO check multiline, maybe future?
        seq(eq('$'), result(RegexpEndAnchor)),
        seq(eq('\\'), eq('b'), result(RegexpWordBoundary)),
        seq(eq('\\'), eq('B'), result(RegexpNonWordBoundary)),
        bind(seq(eq('('), eq('?'), eq('=')), defer { disjunction }, eq(')')) { _, n, _ -> result(RegexpLookAhead(n)) },
        bind(seq(eq('('), eq('?'), eq('!')), defer { disjunction }, eq(')')) { _, n, _ -> result(RegexpNegativeLookAhead(n)) }
    )

    private val decimalDigits = bind(many(satisfy { c: Char -> Character.isDigit(c) })) { digits ->
        result(digits.joinToString("").toInt())
    }

    private val quantifierPrefix = or(
        seq(eq('*'), result(Quantifier(0, Int.MAX_VALUE))),
        seq(eq('+'), result(Quantifier(1, Int.MAX_VALUE))),
        seq(eq('?'), result(Quantifier(0, 1))),
        bind(eq('{'), decimalDigits, eq('}')) { _, n, _ -> result(Quantifier(n, n)) },
        bind(eq('{'), decimalDigits, seq(eq(','), eq('}'))) { _, n, _ -> result(Quantifier(n, Int.MAX_VALUE)) },
        bind(eq('{'), decimalDigits, eq(','), decimalDigits, eq('}')) { _, n0, _, n1, _ -> result(Quantifier(n0, n1)) }
    )

    private val quantifier: Parser<Char, Pair<Quantifier, Boolean>> = or(
        bind(quantifierPrefix) { q -> result(q to false) },
        bind(quantifierPrefix, eq('?')) { q, _ -> result(q to true) }
    )

    // Slightly defers from the ECMA specification for our convenience
    private val characterClassEscape = or(
        seq(eq('\\'), eq('w'), result(REGEXP_WORD_SET)),
        seq(eq('\\'), eq('W'), result(REGEXP_NON_WORD_SET)),
        seq(eq('\\'), eq('s'), result(REGEXP_SPACE_SET)),
        seq(eq('\\'), eq('S'), result(REGEXP_NON_SPACE_SET)),
        seq(eq('\\'), eq('d'), result(REGEXP_DIGIT_SET)),
        seq(eq('\\'), eq('D'), result(REGEXP_NON_DIGIT_SET))
    )

    private val classAtomNoDash = or(
        bind(satisfy { c: Char -> "\\]-".indexOf(c) < 0 }) { c -> result(RegexpPatternChar(c)) },
        characterClassEscape,
        bind(eq('\\'), satisfy { c: Char -> "fnrtv".indexOf(c) >= 0 }) { _, c ->
            result(RegexpPatternChar(when (c) {
                'f' -> '\u000c'
                'n' -> '\n'
                'r' -> '\r'
                't' -> '\t'
                else -> '\u000b' // vtab
            }))
        },
        bind(eq('\\'), ::any) { _, c: Char -> result(RegexpPatternChar(c)) }
    )

    private val classAtom = or(
        seq(eq('-'), result(RegexpPatternChar('-'))),
        classAtomNoDash
    )

    private val classRanges: Parser<Char, RegexpNode> by lazy {
        or(
            bind(classAtom, eq('-'), classAtom, defer { classRanges }) { s, _, e, n ->
                result(when (s) {
                    is RegexpPatternChar -> when (e) {
                        is RegexpPatternChar -> RegexpAlter.of(RegexpCharSet(s.char, e.char), n)
                        // [a-\d] or so? not sure how it should be handled, but let's do some
                        // meaningful interpretation. (a- inf) | \d
                        else -> RegexpAlter.of(RegexpCharSet(s.char, Char.MAX_VALUE), e, n)
                    }
                    // [\w-whatever] case, '-' will be treated as a char
                    else -> RegexpAlter.of(s, RegexpPatternChar('-'), e, n)
                })
            },
            bind(classAtom, defer { classRanges }) { a, r ->
                result(if (r is RegexpAlter) {
                    r.unshift(a)
                } else {
                    RegexpAlter.of(a, r)
                })
            },
            seq(peek(eq(']')), result(RegexpNullSeq))
        )
    }

    private val characterClass = or(
        bind(seq(eq('['), eq('^')), classRanges, eq(']')) { _, range, _ -> result(RegexpComplement(range)) },
        bind(eq('['), classRanges, eq(']')) { _, range, _ -> result(range) }
    )

    private val atom = or(
        seq(eq('.'), result(RegexpAny)),
        characterClassEscape,
        characterClass,
        bind(eq('('), defer { disjunction }, eq(')')) { _, ex, _ -> result(ex) }, // capture, but we ignore, for now?
        bind(seq(eq('('), eq('?'), eq(':')), defer { disjunction }, eq(')')) { _, ex, _ -> result(ex) },

        bind(satisfy { c: Char -> REGEXP_SPECIAL_CHARACTERS.indexOf(c) < 0 }) { c -> result(RegexpPatternChar(c)) },
        bind(eq('\\'), ::any) { _, c: Char -> result(RegexpPatternChar(c)) }
    )

    private val term: Parser<Char, RegexpNode> = or(
        assertion,
        bind(atom, quantifier) { r, q ->
            if (q.second) {
                result(RegexpNonGreedyRepetition(r, q.first.min, q.first.max))
            } else {
                result(RegexpRepetition(r, q.first.min, q.first.max))
            }
        },
        atom
    )

    private val alternative: Parser<Char, RegexpNode> by lazy {
        or(
            bind(term, defer { alternative }) { t, a ->
                result(when (a) {
                    is RegexpNullSeq -> t
                    is RegexpSequence -> a.unshift(t)
                    else -> RegexpSequence.of(t, a)
                })
            },
            term
        )
    }

    private val disjunction: Parser<Char, RegexpNode> by lazy {
        or(
            bind(alternative, eq('|'), defer { disjunction }) { a, _, d ->
                result(when (d) {
                    is RegexpAlter -> d.unshift(a)
                    else -> RegexpAlter.of(a, d)
                })
            },
            alternative
        )
    }

    private data class Quantifier(val min: Int, val max: Int)

}