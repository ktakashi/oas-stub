package io.github.ktakashi.oas.engine.data.regexp

import io.github.ktakashi.oas.engine.data.charset.CharSet

sealed interface RegexpNode

sealed interface RegexpNodeContainer: RegexpNode {
    val regexp: RegexpNode
}
sealed interface RegexpRange: RegexpNode {
    val min: Int
    val max: Int
}

// Need better name...
sealed interface RegexpSingle: RegexpNode

data object RegexpNullSeq: RegexpNode
data object RegexpStartAnchor: RegexpNode
data object RegexpEndAnchor: RegexpNode
data object RegexpWordBoundary: RegexpNode
data object RegexpNonWordBoundary: RegexpNode
data object RegexpAny: RegexpNode
data class RegexpPatternChar(val char: Char): RegexpSingle
data class RegexpBackreference(val reference: Int): RegexpNode
data class RegexpCharSet(val charset: CharSet): RegexpSingle
data class RegexpAlter(val regexps: List<RegexpNode>): RegexpNode {
    companion object {
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
data class RegexpCapture(override val regexp: RegexpNode): RegexpNodeContainer
data class RegexpComplement(override val regexp: RegexpNode): RegexpNodeContainer
data class RegexpLookAhead(override val regexp: RegexpNode): RegexpNodeContainer
data class RegexpNegativeLookAhead(override val regexp: RegexpNode): RegexpNodeContainer
data class RegexpRepetition(override val regexp: RegexpNode, override val min: Int, override val max: Int): RegexpNodeContainer, RegexpRange
data class RegexpNonGreedyRepetition(override val regexp: RegexpNode, override val min: Int, override val max: Int): RegexpNodeContainer, RegexpRange
