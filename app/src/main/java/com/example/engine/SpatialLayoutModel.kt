package com.example.engine

import kotlin.math.hypot

data class KeyCoord(val char: Char, val x: Float, val y: Float)

object SpatialLayoutModel {
    private val row1 = "qwertyuiop"
    private val row2 = "asdfghjkl"
    private val row3 = "zxcvbnm"

    val keyCoords: Map<Char, KeyCoord> = buildMap {
        row1.forEachIndexed { idx, c -> put(c, KeyCoord(c, idx + 0.5f, 0.5f)) }
        row2.forEachIndexed { idx, c -> put(c, KeyCoord(c, idx + 1.0f, 1.5f)) }
        row3.forEachIndexed { idx, c -> put(c, KeyCoord(c, idx + 1.85f, 2.5f)) }
    }

    fun spatialDistance(a: Char, b: Char): Float {
        val ca = keyCoords[a.lowercaseChar()] ?: return 2.5f
        val cb = keyCoords[b.lowercaseChar()] ?: return 2.5f
        return hypot((ca.x - cb.x).toDouble(), (ca.y - cb.y).toDouble()).toFloat()
    }

    /**
     * Weighted substitution cost:
     * 0f if exact match, ~0.35f for immediate physical neighbors, up to 1.0f for distant keys.
     */
    fun substitutionCost(a: Char, b: Char): Float {
        val la = a.lowercaseChar()
        val lb = b.lowercaseChar()
        if (la == lb) return 0f
        val dist = spatialDistance(la, lb)
        return when {
            dist <= 1.15f -> 0.35f
            dist <= 1.65f -> 0.55f
            dist <= 2.3f -> 0.80f
            else -> 1.0f
        }
    }

    val longPressAlternates: Map<Char, List<String>> = mapOf(
        'q' to listOf("1"),
        'w' to listOf("2"),
        'e' to listOf("3", "é", "è", "ê", "ë", "ē"),
        'r' to listOf("4"),
        't' to listOf("5", "þ"),
        'y' to listOf("6", "ý", "ÿ"),
        'u' to listOf("7", "ú", "ù", "û", "ü", "ū"),
        'i' to listOf("8", "í", "ì", "î", "ï", "ī"),
        'o' to listOf("9", "ó", "ò", "ô", "ö", "ø", "ō"),
        'p' to listOf("0"),
        'a' to listOf("@", "á", "à", "â", "ä", "æ", "å"),
        's' to listOf("$", "ß", "ś", "š"),
        'd' to listOf("ð"),
        'f' to listOf("&"),
        'g' to listOf("_"),
        'h' to listOf("-"),
        'j' to listOf("+"),
        'k' to listOf("("),
        'l' to listOf(")"),
        'z' to listOf("#", "ž", "ź"),
        'x' to listOf("*"),
        'c' to listOf("ç", "ć", "č"),
        'v' to listOf("\""),
        'b' to listOf("'"),
        'n' to listOf("ñ", "ń"),
        'm' to listOf("?")
    )

    val topRowNumberHints: Map<Char, String> = mapOf(
        'q' to "1", 'w' to "2", 'e' to "3", 'r' to "4", 't' to "5",
        'y' to "6", 'u' to "7", 'i' to "8", 'o' to "9", 'p' to "0"
    )
}
