package io.github.ktakashi.oas.engine.data.charset

import java.util.NavigableMap
import java.util.TreeMap
import javax.xml.stream.events.Characters

/**
 * Scheme like charset.
 *
 * Java's [java.nio.charset.Charset] is encoding charset, but
 * we need a set of characters.
 *
 * See: https://srfi.schemers.org/srfi-14/
 *
 * The charset doesn't support other than ASCII
 */
class CharSet {
    companion object {
        const val MAX_CODE_POINT = 0x10FFFF // for some reason, this doesn't exist or I'm missing then
        const val SMALL_CHARS = 128

        @JvmStatic
        fun empty() = CharSet()

        @JvmStatic
        fun fromString(charset: String): CharSet {
            val cset = CharSet()
            charset.codePoints().forEach { cp -> cset.addRange(cp, cp) }
            return cset
        }

        @JvmStatic
        fun fromRange(from: Char, to: Char): CharSet {
            val cset = CharSet()
            cset.addRange(from, to)
            return cset
        }
    }
    // For performance reason, we separate commonly used chars (ASCII) and others
    // It might be better to use bitwise operation, but I'm lazy so maybe later
    private val smalls = BooleanArray(SMALL_CHARS)            // characters within 0 and 128
    // The entry represents range of chars.
    // The key represents the lower bound of the range, and the value represents
    // the upper bound of the range.
    // e.g. e = {'a': 'z'}, this means this entry represents range of [a-z]
    private val large: NavigableMap<Int, Int> = TreeMap()    // characters larger than 128

    fun contains(element: Int): Boolean = if (element < 0) false // obvious case
    else if (element < SMALL_CHARS) smalls[element]
    else large[element]?.let {
        true
    } ?: large.lowerEntry(element)?.let {
        it.value >= element
    } ?: false

    /**
     * Returns number of characters
     */
    fun count(): Int {
        val smallCount = smalls.count { it }
        val largeCount = large.entries.fold(0) { acc, e ->
            acc + (e.value - e.key + 1)
        }
        return smallCount + largeCount
    }

    fun ranges(): List<CharRange> {
        val r = mutableListOf<CharRange>()
        var begin = 0
        var prev = false
        smalls.forEachIndexed { index, b ->
            if (b && !prev) begin = index
            if (prev && !b) {
                r.add(CharRange(begin, index - 1))
            }
            prev = b
        }
        if (prev) {
            r.add(CharRange(begin, SMALL_CHARS - 1))
        }
        large.entries.forEach { (key, value) ->
            r.add(CharRange(key, value))
        }
        return r
    }

    fun complement(): CharSet {
        for (i in 0 until SMALL_CHARS) {
            smalls[i] = !smalls[i]
        }
        var last = SMALL_CHARS - 1
        while (true) {
            val e = large.higherEntry(last) ?: break
            large.remove(e.key)
            if (last < e.key - 1) {
                large[last + 1] = e.key - 1
            }
            last = e.value
        }
        if (last < MAX_CODE_POINT) {
            large[last + 1] = MAX_CODE_POINT
        }
        return this
    }

    fun add(char: Char) = addRange(char, char)

    fun add(codePoint: Int) = addRange(codePoint, codePoint)

    fun add(charset: CharSet): CharSet {
        charset.smalls.forEachIndexed { index, b ->
            if (b) {
                smalls[index] = true
            }
        }
        charset.large.forEach { (k, v) ->
            addRange(k, v)
        }
        return this
    }

    fun addRange(from: Char, to: Char) = addRange(from.code, to.code)

    fun addRange(from: Int, to: Int): CharSet {
        // invalid range. should we raise an error here?
        if (to < from) return this
        val newFrom = if (from < SMALL_CHARS) {
            if (to < SMALL_CHARS) {
                fillSmall(from, to + 1)
                return this // done just return
            }
            fillSmall(from, SMALL_CHARS)
            SMALL_CHARS
        } else {
            from
        }
        val e = large.lowerEntry(newFrom)?.let { e ->
            if (e.value < newFrom - 1) {
                newEntry(newFrom)
            } else {
                e
            }
        } ?: newEntry(newFrom)

        if (e.value >= to) return this // it's already in range
        var hi = e
        while (true) {
            hi = large.higherEntry(hi.key) ?: break
            if (hi.key > to + 1) {
                large[hi.key] = to
                return this
            }
            val v = hi.value
            large.remove(hi.key)
            if (v > to) {
                large[e.key] = v
                return this
            }
        }
        large[e.key] = to
        return this
    }

    private fun fillSmall(from: Int, to: Int) {
        for (i in from until to) {
            smalls[i] = true
        }
    }

    private fun newEntry(from: Int): Map.Entry<Int, Int> {
        large[from] = 0
        return large.lowerEntry(from + 1) // get the newly created entry
    }

    private fun toRegexpString(): String {
        fun codePointToString(codePoint: Int): String = when {
            codePoint in 0 until 20 || codePoint in 0x7F .. 0xFF -> String.format("\\x%02x", codePoint)
            !Character.isLetterOrDigit(codePoint) -> String.format("\\u%04x", codePoint)
            else -> Character.toString(codePoint)
        }
        val sb = StringBuilder()
        sb.append("[")
        ranges().forEach { (min, max) ->
            sb.append(codePointToString(min))
            if (min != max) {
                sb.append("-").append(codePointToString(max))
            }
        }
        sb.append("]")
        return sb.toString()
    }

    override fun toString(): String {
        return "CharSet(${toRegexpString()})"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as CharSet

        if (!smalls.contentEquals(other.smalls)) return false
        if (large != other.large) return false

        return true
    }

    override fun hashCode(): Int {
        var result = smalls.contentHashCode()
        result = 31 * result + large.hashCode()
        return result
    }

    data class CharRange(val min: Int, val max: Int)
}