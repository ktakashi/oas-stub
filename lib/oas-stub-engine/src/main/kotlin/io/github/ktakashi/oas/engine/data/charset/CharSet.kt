package io.github.ktakashi.oas.engine.data.charset

import java.util.NavigableMap
import java.util.TreeMap

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
sealed class CharSet {

    // For performance reason, we separate commonly used chars (ASCII) and others
    // It might be better to use bitwise operation, but I'm lazy so maybe later
    internal val smalls = BooleanArray(SMALL_CHARS)            // characters within 0 and 128
    // The entry represents range of chars.
    // The key represents the lower bound of the range, and the value represents
    // the upper bound of the range.
    // e.g. e = {'a': 'z'}, this means this entry represents range of [a-z]
    internal val large: NavigableMap<Int, Int> = TreeMap()    // characters larger than 128

    companion object {
        const val MAX_CODE_POINT = 0x10FFFF // for some reason, this doesn't exist or I'm missing then
        const val SMALL_CHARS = 128
        @JvmStatic
        fun empty(): CharSet = MutableCharSet()

        @JvmStatic
        fun fromString(charset: String): CharSet {
            val cset = MutableCharSet()
            charset.codePoints().forEach { cp -> cset.addRange(cp, cp) }
            return cset
        }

        @JvmStatic
        fun fromRange(from: Char, to: Char): CharSet {
            val cset = MutableCharSet()
            cset.addRange(from, to)
            return cset
        }

        @JvmField
        val ASCII = empty().addRange(0, 128).immutable()
        @JvmField
        val LETTER = empty().addRange('a', 'z').addRange('A', 'Z').immutable()
        @JvmField
        val DIGITS = empty().addRange('0', '9').immutable()
        @JvmField
        val PRINTABLE = empty().addRange(32, 126).immutable()
        @JvmField
        val SYMBOLS = empty().addRange(33, 47).addRange(58, 64).addRange(91, 96).addRange(123, 126).immutable()
    }

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

    open fun ranges(): List<CharRange> {
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

    abstract fun complement(): CharSet

    abstract fun add(charset: CharSet): CharSet

    fun add(char: Char) = addRange(char, char)

    fun add(codePoint: Int) = addRange(codePoint, codePoint)

    fun addRange(from: Char, to: Char) = addRange(from.code, to.code)
    abstract fun addRange(from: Int, to: Int): CharSet

    fun immutable(): CharSet {
        val r = ImmutableCharSet()
        System.arraycopy(this.smalls, 0, r.smalls, 0, this.smalls.size)
        r.large.putAll(this.large)
        return r
    }

    fun mutable(): CharSet {
        val r = MutableCharSet()
        System.arraycopy(this.smalls, 0, r.smalls, 0, this.smalls.size)
        r.large.putAll(this.large)
        return r
    }

    private fun toRegexpString(): String {
        fun isSymbol(codePoint: Int): Boolean {
            return ((1 shl Character.OTHER_SYMBOL.toInt() or
                    (1 shl Character.MATH_SYMBOL.toInt()) or
                    (1 shl Character.CURRENCY_SYMBOL.toInt()) or
                    (1 shl Character.MODIFIER_SYMBOL.toInt()) or
                    (1 shl Character.OTHER_PUNCTUATION.toInt()) or
                    (1 shl Character.DASH_PUNCTUATION.toInt()) or
                    (1 shl Character.CONNECTOR_PUNCTUATION.toInt()) or
                    (1 shl Character.END_PUNCTUATION.toInt()) or
                    (1 shl Character.START_PUNCTUATION.toInt()))
                    shr Character.getType(codePoint) and 1) != 0
        }

        fun codePointToString(codePoint: Int): String = when {
            codePoint in 0 until 20 || codePoint in 0x7F .. 0xFF -> String.format("\\x%02x", codePoint)
            !Character.isLetterOrDigit(codePoint) && !isSymbol(codePoint)-> String.format("\\u%04x", codePoint)
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

class MutableCharSet: CharSet() {
    override fun complement(): CharSet {
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

    override fun add(charset: CharSet): CharSet {
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

    override fun addRange(from: Int, to: Int): CharSet {
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
}

class ImmutableCharSet: CharSet() {
    private val range: List<CharRange> by lazy {
        super.ranges()
    }
    override fun complement(): CharSet = empty().add(this).complement()

    override fun add(charset: CharSet): CharSet = empty().add(this).add(charset)

    override fun addRange(from: Int, to: Int): CharSet = empty().add(this).addRange(from, to)

    override fun ranges(): List<CharRange> = range
}