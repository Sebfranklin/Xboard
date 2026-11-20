package com.example.engine

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import kotlin.math.exp
import kotlin.math.hypot
import kotlin.math.max

/**
 * Physical center of a key on a normalized 10x3 staggered QWERTY grid.
 */
data class KeyCoord(val char: Char, val x: Float, val y: Float)

/**
 * A single touch event mapped into the normalized 10x3 QWERTY layout coordinate space.
 */
data class TouchSample(
    val tappedChar: Char,
    val normX: Float,
    val normY: Float
)

/**
 * Ranked proximity candidate from a touch point to a physical character key.
 */
data class KeyProximityMatch(
    val char: Char,
    val distance: Float,
    val likelihood: Double
)

object SpatialLayoutModel {
    private val row1 = "qwertyuiop"
    private val row2 = "asdfghjkl"
    private val row3 = "zxcvbnm"

    val keyCoords: Map<Char, KeyCoord> = buildMap {
        row1.forEachIndexed { idx, c -> put(c, KeyCoord(c, idx + 0.5f, 0.5f)) }
        row2.forEachIndexed { idx, c -> put(c, KeyCoord(c, idx + 1.0f, 1.5f)) }
        row3.forEachIndexed { idx, c -> put(c, KeyCoord(c, idx + 1.85f, 2.5f)) }
    }

    /**
     * Maps a raw screen coordinate [pos] inside the rendered QWERTY key grid into the
     * normalized 10x3 layout space, allowing spatial distance calculations independent of
     * screen density or one-handed width.
     */
    fun toNormalizedTouch(
        pos: Offset,
        keyBounds: Map<Char, Rect>,
        fallbackChar: Char? = null
    ): TouchSample? {
        val qRect = keyBounds['q']
        val pRect = keyBounds['p']
        val zRect = keyBounds['z']
        val tapped = fallbackChar?.lowercaseChar() ?: nearestKeyForScreenOffset(pos, keyBounds, Float.MAX_VALUE)

        if (qRect != null && pRect != null && zRect != null) {
            val leftEdge = qRect.left
            val rightEdge = pRect.right
            val topEdge = qRect.top
            val bottomEdge = zRect.bottom
            val gridW = max(rightEdge - leftEdge, 1f)
            val gridH = max(bottomEdge - topEdge, 1f)
            val normX = ((pos.x - leftEdge) / gridW) * 10f
            val normY = ((pos.y - topEdge) / gridH) * 3f
            val resolvedChar = tapped ?: nearestKeys(normX, normY, limit = 1).firstOrNull()?.char ?: 'a'
            return TouchSample(resolvedChar, normX.coerceIn(0f, 10f), normY.coerceIn(0f, 3f))
        }

        val coord = tapped?.let { keyCoords[it] } ?: return null
        return TouchSample(tapped, coord.x, coord.y)
    }

    /**
     * Finds the nearest rendered key to a raw screen coordinate within [maxRadiusPx].
     */
    fun nearestKeyForScreenOffset(
        pos: Offset,
        keyBounds: Map<Char, Rect>,
        maxRadiusPx: Float
    ): Char? {
        keyBounds.entries.firstOrNull { it.value.contains(pos) }?.let { return it.key }
        val closest = keyBounds.entries.minByOrNull { (_, rect) ->
            hypot((pos.x - rect.center.x).toDouble(), (pos.y - rect.center.y).toDouble())
        } ?: return null
        val dist = hypot((pos.x - closest.value.center.x).toDouble(), (pos.y - closest.value.center.y).toDouble()).toFloat()
        return if (dist <= maxRadiusPx) closest.key else null
    }

    /**
     * Returns the top [limit] character keys nearest to normalized coordinate ([normX], [normY]),
     * scored by Euclidean distance and a 2D Gaussian spatial likelihood (sigma = 0.65 key widths).
     */
    fun nearestKeys(normX: Float, normY: Float, limit: Int = 4): List<KeyProximityMatch> {
        val twoSigmaSq = 2.0 * 0.65 * 0.65
        return keyCoords.values
            .map { kc ->
                val d = hypot((normX - kc.x).toDouble(), (normY - kc.y).toDouble()).toFloat()
                val likelihood = exp(-(d * d) / twoSigmaSq)
                KeyProximityMatch(kc.char, d, likelihood)
            }
            .sortedBy { it.distance }
            .take(limit)
    }

    fun spatialDistance(a: Char, b: Char): Float {
        val ca = keyCoords[a.lowercaseChar()] ?: return 2.5f
        val cb = keyCoords[b.lowercaseChar()] ?: return 2.5f
        return hypot((ca.x - cb.x).toDouble(), (ca.y - cb.y).toDouble()).toFloat()
    }

    /**
     * Coordinate-aware substitution cost from an actual [TouchSample] to candidate [targetChar].
     * Returns 0f for an exact char hit, and scales smoothly from ~0.22f..1.0f based on the
     * actual Euclidean distance between the finger's touch coordinate and [targetChar]'s key center.
     */
    fun touchSubstitutionCost(sample: TouchSample, targetChar: Char): Float {
        val target = targetChar.lowercaseChar()
        if (sample.tappedChar.lowercaseChar() == target) return 0f
        val kc = keyCoords[target] ?: return 1.0f
        val dist = hypot((sample.normX - kc.x).toDouble(), (sample.normY - kc.y).toDouble()).toFloat()
        return when {
            dist <= 0.72f -> 0.22f // finger landed right on the border of targetChar!
            dist <= 1.15f -> 0.35f
            dist <= 1.65f -> 0.55f
            dist <= 2.30f -> 0.80f
            else -> 1.0f
        }
    }

    /**
     * Key-center fallback substitution cost when raw touch coordinates are unavailable.
     */
    fun substitutionCost(a: Char, b: Char): Float {
        val la = a.lowercaseChar()
        val lb = b.lowercaseChar()
        if (la == lb) return 0f
        val dist = spatialDistance(la, lb)
        return when {
            dist <= 1.15f -> 0.35f
            dist <= 1.65f -> 0.55f
            dist <= 2.30f -> 0.80f
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
