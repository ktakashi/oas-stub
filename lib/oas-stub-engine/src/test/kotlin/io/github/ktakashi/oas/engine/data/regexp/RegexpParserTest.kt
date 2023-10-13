package io.github.ktakashi.oas.engine.data.regexp

import io.github.ktakashi.oas.engine.data.charset.CharSet
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

class RegexpParserTest {
    private val parser = RegexpParser()

    @ParameterizedTest
    @CsvSource(value = [
        "\\d\\w/seq(cset(0, 9), union(cset(a, z), cset(A, Z), cset(0, 9), cset(_, _)))",
        "\\d/cset(0, 9)",
        "a|b/alt(char(a), char(b))",
        "\\D\\W/seq(comp(cset(0, 9)), comp(union(cset(a, z), cset(A, Z), cset(0, 9), cset(_, _))))",
        "./any()",
        "^abc$/seq(start(), char(a), char(b), char(c), end())",
        "a+/rep(char(a), 1)",
        "a*/rep(char(a), 0)",
        "a?/rep(char(a), 0, 1)",
        "a{2}/rep(char(a), 2, 2)",
        "a{1,}/rep(char(a), 1)",
        "a{1,2}/rep(char(a), 1, 2)",
        "[abc]/cset(a, c)",
        "[a-z]/cset(a, z)",
        "[a-z\\d]/union(cset(a, z), cset(0, 9))",
        "a+?/ngrep(char(a), 1)",
        "[\\q]/cset(q, q)",
        "[\\n]/cset(\\n, \\n)",
        "[\\t]/cset(\\t, \\t)",
        "[\\v]/cset(\\v, \\v)",
        "[\\f]/cset(\\f, \\f)",
        "\\n/char(\\n)",
        "\\t/char(\\t)",
        "\\v/char(\\v)",
        "\\f/char(\\f)",
        "[a-\\dz]/union(cset(a), cset(0, 9)))",
        "\\?/char(?)",
        "[\\cA]/cset(\\x01, \\x01)",
        "[\\ca]/cset(\\x01, \\x01)",
        "[\\cJ]/cset(\\n, \\n)",
        "[\\cj]/cset(\\n, \\n)",
        "\\cA/char(\\x01)",
        "\\ca/char(\\x01)",
        "\\cJ/char(\\n)",
        "\\cj/char(\\n)",
        "\\x61/char(a)",
        "\\u0061/char(a)",
        "[\\b]/cset(\\x08, \\x08)",
        "\\0/char(\\x00)",
        "\\1/backref(1)",
        "\\01/char(\\x01)",
        "(a)/cap(char(a))",
        "(?=a)/lookahead(char(a))",
        "(?!a)/nlookahead(char(a))",
    ], delimiter = '/')
    fun parsePattern(pattern: String, ast: String) {
        val result = parser.parse(pattern)
        // println(result)
        assertEquals(constructAst(ast), result)
    }
}

private fun constructAst(ast: String): RegexpNode {
    fun parseNameAndContent(ast: String): Pair<String, String> {
        val index = ast.indexOf('(')
        val name = ast.substring(0, index)
        val rindex = ast.lastIndexOf(')')
        return name.trim() to ast.substring(index + 1, rindex)
    }
    fun parseContent1(content: String, start: Int = 0): Pair<String, Int> {
        fun search(start: Int): Int {
            var depth = 0
            var index = content.indexOf('(', start) + 1
            while (true) {
                when (content[index++]) {
                    ')' -> if (depth == 0) {
                        return index
                    } else {
                        depth--
                    }
                    '(' -> depth++
                    else -> continue
                }
            }
        }
        val index = search(start)
        return content.substring(start, index) to index
    }
    fun parseContent(content: String): List<String> {
        if (content.isEmpty()) return listOf()
        var start = 0
        val r = mutableListOf<String>()
        do {
            val (ast1, index) = parseContent1(content, start)
            r.add(ast1)
            start = index + 2
        } while (start < content.length)
        return r
    }
    fun handleChar(s: String) = when (val c = s[0]) {
        '\\' -> when (val c1 = s[1]) {
            'f' -> '\u000c'
            'n' -> '\n'
            'r' -> '\r'
            't' -> '\t'
            'v' -> '\u000b'
            'x' -> s.substring(2).toInt(16).toChar()
            else -> c1
        }
        else -> c
    }
    // format of test ast
    // name(value, ...)
    // where name = type, value is constructor argument
    // e.g. seq(cset(0, 9), alt(cset(a, z), cset(A, Z), cset(0, 9), char(_))) == \d\w
    val (name, content) = parseNameAndContent(ast)
    return when (name) {
        "seq" -> RegexpSequence(parseContent(content).map { constructAst(it) })
        "alt" -> RegexpAlter(parseContent(content).map { constructAst(it) })
        "cset" -> content.split(",").map { it.trim() }.let {
            if (it.size == 1) {
                RegexpCharSet(CharSet.fromRange(handleChar(it[0]), Char.MAX_VALUE))
            } else {
                RegexpCharSet(CharSet.fromRange(handleChar(it[0]), handleChar(it[1])))
            }
        }
        "union" -> RegexpCharSet(parseContent(content).map { constructAst(it) }
            .filterIsInstance<RegexpCharSet>()
            .map { it.charset }
            .fold(CharSet.empty()) { acc, charSet -> acc.add(charSet) })
        "char" -> RegexpPatternChar(handleChar(content))
        "comp" -> RegexpComplement(constructAst(content))
        "any" -> RegexpAny
        "start" -> RegexpStartAnchor
        "end" -> RegexpEndAnchor
        "rep", "ngrep" -> parseContent1(content).let { (ast, rx) ->
            val n = content.substring(rx + 1).split(",")
            if (name == "rep") {
                RegexpRepetition(constructAst(ast), n[0].trim().toInt(), if (n.size == 1) Int.MAX_VALUE else n[1].trim().toInt())
            } else {
                RegexpNonGreedyRepetition(constructAst(ast), n[0].trim().toInt(), if (n.size == 1) Int.MAX_VALUE else n[1].trim().toInt())
            }
        }
        "backref" -> RegexpBackreference(content.toInt())
        "cap" -> RegexpCapture(constructAst(content))
        "lookahead", "nlookahead" -> if (name == "lookahead") {
            RegexpLookAhead(constructAst(content))
        } else {
            RegexpNegativeLookAhead(constructAst(content))
        }
        else -> throw IllegalArgumentException("$name not supported")
    }
}