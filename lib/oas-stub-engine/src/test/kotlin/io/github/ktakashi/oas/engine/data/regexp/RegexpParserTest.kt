package io.github.ktakashi.oas.engine.data.regexp

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

class RegexpParserTest {
    private val parser = RegexpParser()

    @ParameterizedTest
    @CsvSource(value = [
        "\\d\\w/seq(cset(0, 9), alt(cset(a, z), cset(A, Z), cset(0, 9), char(_)))",
        "\\d/cset(0, 9)",
        "a|b/alt(char(a), char(b))",
        "\\D\\W/seq(comp(cset(0, 9)), comp(alt(cset(a, z), cset(A, Z), cset(0, 9), char(_))))",
        "./any()",
        "^abc$/seq(start(), char(a), char(b), char(c), end())",
        "a+/rep(char(a), 1)",
        "a*/rep(char(a), 0)",
        "a?/rep(char(a), 0, 1)",
        "a{2}/rep(char(a), 2, 2)",
        "a{1,}/rep(char(a), 1)",
        "a{1,2}/rep(char(a), 1, 2)",
    ], delimiter = '/')
    fun parsePattern(pattern: String, ast: String) {
        val result = parser.parse(pattern)
        println(result)
        assertEquals(constructAst(ast), result)
    }
}

private fun constructAst(ast: String): RegexpNode {
    fun parseNameAndContent(ast: String): Pair<String, String> {
        val index = ast.indexOf('(')
        val name = ast.substring(0, index)
        val rindex = ast.lastIndexOf(')')
        return name to ast.substring(index + 1, rindex)
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
            val (ast, index) = parseContent1(content, start)
            r.add(ast)
            start = index + 2
        } while (start < content.length)
        return r
    }
    // format of test ast
    // name(value, ...)
    // where name = type, value is constructor argument
    // e.g. seq(cset(0, 9), alt(cset(a, z), cset(A, Z), cset(0, 9), char(_))) == \d\w
    val (name, content) = parseNameAndContent(ast)
    return when (name.trim()) {
        "seq" -> RegexpSequence(parseContent(content).map { constructAst(it) })
        "alt" -> RegexpAlter(parseContent(content).map { constructAst(it) })
        "cset" -> content.split(",").map { it.trim() }.let {
            RegexpCharSet(it[0][0], it[1][0])
        }
        "char" -> RegexpPatternChar(content[0])
        "comp" -> RegexpComplement(constructAst(content))
        "any" -> RegexpAny
        "start" -> RegexpStartAnchor
        "end" -> RegexpEndAnchor
        "rep" -> parseContent1(content).let { (ast, rx) ->
            val n = content.substring(rx + 1).split(",")
            RegexpRepetition(constructAst(ast), n[0].trim().toInt(), if (n.size == 1) Int.MAX_VALUE else n[1].trim().toInt())
        }
        else -> throw IllegalArgumentException("$name not supported")
    }
}