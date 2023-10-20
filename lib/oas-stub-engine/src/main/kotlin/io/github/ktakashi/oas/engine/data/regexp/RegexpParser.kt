package io.github.ktakashi.oas.engine.data.regexp

import io.github.ktakashi.oas.engine.data.charset.CharSet
import io.github.ktakashi.peg.Parser
import io.github.ktakashi.peg.SuccessResult
import io.github.ktakashi.peg.any
import io.github.ktakashi.peg.bind
import io.github.ktakashi.peg.defer
import io.github.ktakashi.peg.eq
import io.github.ktakashi.peg.many
import io.github.ktakashi.peg.optional
import io.github.ktakashi.peg.or
import io.github.ktakashi.peg.peek
import io.github.ktakashi.peg.repeat
import io.github.ktakashi.peg.result
import io.github.ktakashi.peg.satisfy
import io.github.ktakashi.peg.seq
import io.github.ktakashi.peg.token
import java.text.ParseException
import java.util.EnumSet
import java.util.Optional


// Maybe for future?
enum class RegexpParserOption {
    DOTALL,
    UNICODE,
    MULTILINE
}

private fun regexpAlt(vararg regexps: RegexpNode): RegexpNode =
    regexps.fold(RegexpAlter.EMPTY) { acc, regexp ->  acc + regexp }

private const val REGEXP_SPECIAL_CHARACTERS = "^\$\\.*+?()[]{}|"
private val REGEXP_DIGIT_SET = RegexpCharSet(CharSet.fromRange('0', '9'))
private val REGEXP_NON_DIGIT_SET = RegexpComplement(REGEXP_DIGIT_SET)
private val REGEXP_WORD_SET = RegexpCharSet(CharSet.fromRange('a', 'z').addRange('A', 'Z').add(REGEXP_DIGIT_SET.charset).add('_'))
private val REGEXP_NON_WORD_SET = RegexpComplement(REGEXP_WORD_SET)
// space, \t, \r, \n \v = \u000B and \f = \u000C
private val REGEXP_SPACE_SET = regexpAlt(RegexpPatternChar(' '),
    // should be make this range of \u0009 - \u000C?
    RegexpPatternChar('\t'),
    RegexpPatternChar('\n'),
    RegexpPatternChar('\u000B'),
    RegexpPatternChar('\u000C'),
    RegexpPatternChar('\r')
)
private val REGEXP_NON_SPACE_SET = RegexpComplement(REGEXP_SPACE_SET)

private const val ALPHABETS = "abcdefghijklmnopqrstuvwxyz"

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
        token("\\b".asSequence()) { RegexpWordBoundary },
        token("\\B".asSequence()) { RegexpNonWordBoundary },
        bind(token("(?=".asSequence()), defer { disjunction }, eq(')')) { _, n, _ -> result(RegexpLookAhead(n)) },
        bind(token("(?!".asSequence()), defer { disjunction }, eq(')')) { _, n, _ -> result(RegexpNegativeLookAhead(n)) }
    )

    private val hexDigit = satisfy { c: Char -> Character.digit(c, 16) != -1 }
    private val decimalDigit = satisfy { c: Char -> c.isDigit() }
    private val decimalDigits = bind(many(decimalDigit)) { digits ->
        result(digits.joinToString("").toInt())
    }

    private val quantifierPostfix = or(
        seq(eq('}'), result(Optional.empty())),
        token(",}".asSequence()) { Optional.of(Int.MAX_VALUE) },
        bind(eq(','), decimalDigits, eq('}')) { _, n, _ ->
            result(Optional.of(n))
        }
    )

    private val quantifierPrefix = or(
        seq(eq('*'), result(Quantifier(0, Int.MAX_VALUE))),
        seq(eq('+'), result(Quantifier(1, Int.MAX_VALUE))),
        seq(eq('?'), result(Quantifier(0, 1))),
        bind(eq('{'), decimalDigits, quantifierPostfix) { _, n, m -> result(Quantifier(n, m.orElse(n))) },
    )

    private val quantifier = bind(quantifierPrefix, optional(eq('?'))) { q, o -> result(q to o.isPresent) }

    // Slightly defers from the ECMA specification for our convenience
    private val characterClassEscape = or(
        token("\\w".asSequence()) { REGEXP_WORD_SET },
        token("\\W".asSequence()) { REGEXP_NON_WORD_SET },
        token("\\s".asSequence()) { REGEXP_SPACE_SET },
        token("\\S".asSequence()) { REGEXP_NON_SPACE_SET },
        token("\\d".asSequence()) { REGEXP_DIGIT_SET },
        token("\\D".asSequence()) { REGEXP_NON_DIGIT_SET }
    )

    private val characterEscape: Parser<Char, RegexpNode> = seq(eq('\\'), or(
        bind(satisfy { c: Char -> "fnrtv".indexOf(c) >= 0 }) { c ->
            result(RegexpPatternChar(when (c) {
                'f' -> '\u000c'
                'n' -> '\n'
                'r' -> '\r'
                't' -> '\t'
                else -> '\u000b' // vtab
            }))
        },
        seq(eq('c'), bind(satisfy { c: Char -> ALPHABETS.indexOf(c.lowercaseChar()) >= 0 }) { c ->
            result(RegexpPatternChar((ALPHABETS.indexOf(c.lowercaseChar()) + 1).toChar()))
        }),
        seq(eq('x'), bind(repeat(hexDigit, 2)) { hs ->
            // (hi << 4) | lo
            val v = hs.map { Character.digit(it, 16) }.fold(0) { acc, d ->
                (acc shl 4) or d
            }
            result(RegexpPatternChar(v.toChar()))
        }),
        seq(eq('u'), bind(repeat(hexDigit, 4)) { hs ->
            // (u3 << 12) | (u2 << 8) | (u1 << 4) | u0
            val v = hs.map { Character.digit(it, 16) }.fold(0) { acc, d ->
                (acc shl 4) or d
            }
            result(RegexpPatternChar(v.toChar()))
        })
    ))

    private val decimalEscape = or(
        bind(token("\\0".asSequence()), many(decimalDigit)) { _, ds ->
            result(when {
                ds.isEmpty() -> RegexpPatternChar('\u0000')
                else -> RegexpPatternChar(ds.joinToString("").toInt().toChar()) // decimal or octal? got for decimal as ECMA says for now
            })
        },
        bind(eq('\\'), decimalDigit) { _, d -> result(RegexpBackreference(Character.digit(d, 10))) }
    )

    private val classAtomNoDash = or(
        bind(satisfy { c: Char -> "\\]-".indexOf(c) < 0 }) { c -> result(RegexpPatternChar(c)) },
        characterClassEscape,
        characterEscape,
        // decimalEscape, In charset, `[]`, why do we need decimal escape? so remove it for my laziness
        seq(token("\\b".asSequence()), result(RegexpPatternChar('\b'))),
        bind(eq('\\'), ::any) { _, c: Char -> result(RegexpPatternChar(c)) }
    )

    private val classAtom = or(
        seq(eq('-'), result(RegexpPatternChar('-'))),
        classAtomNoDash
    )

    private val classRanges: Parser<Char, RegexpCharSet> by lazy {
        // here classAtom returns only the blow types
        // - RegexpPatternChar
        // - RegexpCharSet
        or(
            bind(classAtom, eq('-'), classAtom, defer { classRanges }) { s, _, e, n ->
                result(when (s) {
                    is RegexpPatternChar -> when (e) {
                        is RegexpPatternChar -> RegexpCharSet(n.charset.addRange(s.char, e.char))
                        // [a-\d] or so, we interpret this as charset('a', '-', [0-9])
                        is RegexpCharSet -> RegexpCharSet(n.charset.add(s.char).add('-').add(e.charset))
                        else -> error("[BUG] Unexpected type `e`: $e")
                    }
                    // [\w-whatever] case, '-' will be treated as a char
                    is RegexpCharSet -> when (e) {
                        is RegexpPatternChar -> RegexpCharSet(n.charset.add(s.charset).add(e.char))
                        // [a-\d] or so? not sure how it should be handled, but let's do some
                        // meaningful interpretation. (a- inf) | \d
                        is RegexpCharSet -> RegexpCharSet(n.charset.add(s.charset).add(e.charset))
                        else -> error("[BUG] Unexpected type `e`: $e")
                    }
                    else -> error("[BUG] Unexpected type `s`: $s")
                })
            },
            bind(classAtom, defer { classRanges }) { a, r ->
                result(when (a) {
                    is RegexpPatternChar -> RegexpCharSet(r.charset.add(a.char))
                    is RegexpCharSet -> RegexpCharSet(r.charset.add(a.charset))
                    else -> error("[BUG] Unexpected type `a`: $a")
                })
            },
            seq(peek(eq(']')), result { RegexpCharSet(CharSet.empty()) })
        )
    }

    private val characterClass = or(
        bind(token("[^".asSequence()), classRanges, eq(']')) { _, range, _ -> result(RegexpComplement(range)) },
        bind(eq('['), classRanges, eq(']')) { _, range, _ -> result(range) }
    )

    private val atom = or(
        seq(eq('.'), result(RegexpAny)),

        characterClassEscape,
        characterEscape,
        characterClass,
        decimalEscape,
        bind(eq('('), defer { disjunction }, eq(')')) { _, ex, _ -> result(RegexpCapture(ex)) },
        bind(token("(?:".asSequence()), defer { disjunction }, eq(')')) { _, ex, _ -> result(ex) },

        bind(satisfy { c: Char -> REGEXP_SPECIAL_CHARACTERS.indexOf(c) < 0 }) { c -> result(RegexpPatternChar(c)) },
        bind(eq('\\'), ::any) { _, c: Char -> result(RegexpPatternChar(c)) }
    )

    private val term: Parser<Char, RegexpNode> = or(
        assertion,
        bind(atom, optional(quantifier)) { r, o ->
            result(o.map { q ->
                // optinisation
                // e.g. \d+ -> \d\d*, so the repetition would always have minimum 0
                val rep = q.first.min
                val max = q.first.max - (if (q.first.max == Int.MAX_VALUE) 0 else rep)
                val rp = if (q.second) {
                    RegexpNonGreedyRepetition(r, 0, max)
                } else {
                    RegexpRepetition(r, 0, max)
                }
                if (q.first.min == 0) {
                    rp
                } else {
                    val rs = Array(rep) { r }
                    if (max == 0) {
                        RegexpSequence.of(*rs)
                    } else {
                        RegexpSequence.of(*rs, rp)
                    }
                }
            }.orElse(r))
        }
    )

    private val alternative: Parser<Char, RegexpNode> by lazy {
        bind(term, optional(defer { alternative })) { t, o ->
            result(o.map { a ->
                when (a) {
                    is RegexpNullSeq -> t
                    is RegexpSequence -> a.unshift(t)
                    else -> RegexpSequence.of(t, a)
                }
            }.orElse(t))
        }
    }

    private val disjunction: Parser<Char, RegexpNode> by lazy {
        bind(alternative, optional(seq(eq('|'), defer { disjunction }))) { a, o ->
            result(o.map { d ->
                when (d) {
                    is RegexpAlter -> d.unshift(a)
                    else -> RegexpAlter.of(a, d)
                }
            }.orElse(a))
        }
    }

    private data class Quantifier(val min: Int, val max: Int)

}