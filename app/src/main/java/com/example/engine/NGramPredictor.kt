package com.example.engine

import java.util.concurrent.ConcurrentHashMap
import kotlin.math.max

/**
 * Deterministic statistical N-gram (Unigram + Bigram + Trigram) predictor with interpolated
 * backoff smoothing and personal on-device frequency adaptation. Zero AI/neural models.
 */
class NGramPredictor {

    private val learnedUnigrams = ConcurrentHashMap<String, Int>()
    private val learnedBigrams = ConcurrentHashMap<String, ConcurrentHashMap<String, Int>>()
    private val learnedTrigrams = ConcurrentHashMap<String, ConcurrentHashMap<String, Int>>()

    fun loadLearned(
        unigrams: List<Pair<String, Int>>,
        bigrams: List<Triple<String, String, Int>>,
        trigrams: List<Triple<String, String, Int>> = emptyList()
    ) {
        unigrams.forEach { (word, count) ->
            val lower = word.lowercase()
            learnedUnigrams[lower] = max(learnedUnigrams[lower] ?: 0, count)
        }
        bigrams.forEach { (prev, next, count) ->
            val p = prev.lowercase()
            val n = next.lowercase()
            val map = learnedBigrams.getOrPut(p) { ConcurrentHashMap() }
            map[n] = max(map[n] ?: 0, count)
        }
        trigrams.forEach { (ctx, next, count) ->
            val c = ctx.lowercase()
            val n = next.lowercase()
            val map = learnedTrigrams.getOrPut(c) { ConcurrentHashMap() }
            map[n] = max(map[n] ?: 0, count)
        }
    }

    fun recordSequence(word: String, prevWord: String?, prevPrevWord: String?) {
        val clean = word.trim()
        if (clean.length < 2 && clean.lowercase() != "i") return
        val lower = clean.lowercase()
        learnedUnigrams[lower] = (learnedUnigrams[lower] ?: 0) + 1

        if (!prevWord.isNullOrBlank()) {
            val prevLower = prevWord.trim().lowercase()
            val biMap = learnedBigrams.getOrPut(prevLower) { ConcurrentHashMap() }
            biMap[lower] = (biMap[lower] ?: 0) + 1

            if (!prevPrevWord.isNullOrBlank()) {
                val triKey = "${prevPrevWord.trim().lowercase()} $prevLower"
                val triMap = learnedTrigrams.getOrPut(triKey) { ConcurrentHashMap() }
                triMap[lower] = (triMap[lower] ?: 0) + 1
            }
        }
    }

    fun reset() {
        learnedUnigrams.clear()
        learnedBigrams.clear()
        learnedTrigrams.clear()
    }

    /**
     * Computes contextual N-gram boost using interpolated Trigram -> Bigram -> Unigram backoff.
     */
    fun contextualBoost(prevWord: String?, prevPrevWord: String?, candidate: String): Double {
        val candLower = candidate.lowercase()
        var boost = (learnedUnigrams[candLower] ?: 0) * 18.0

        if (!prevWord.isNullOrBlank()) {
            val prevLower = prevWord.lowercase()
            val personalBi = learnedBigrams[prevLower]?.get(candLower) ?: 0
            val builtInBi = BuiltInLexicon.bigrams[prevLower]
                ?.firstOrNull { it.first == candLower }
                ?.second ?: 0
            boost += personalBi * 260.0 + builtInBi * 14.0

            if (!prevPrevWord.isNullOrBlank()) {
                val triKey = "${prevPrevWord.lowercase()} $prevLower"
                val personalTri = learnedTrigrams[triKey]?.get(candLower) ?: 0
                val builtInTri = BuiltInLexicon.trigrams[triKey]
                    ?.firstOrNull { it.first == candLower }
                    ?.second ?: 0
                boost += personalTri * 480.0 + builtInTri * 22.0
            }
        }
        return boost
    }

    /**
     * Predicts the next word given [prevPrevWord] and [prevWord] using interpolated
     * Trigram -> Bigram -> Unigram backoff ranking.
     */
    fun predictNext(
        prevWord: String?,
        prevPrevWord: String?,
        limit: Int = 4
    ): List<Suggestion> {
        val scored = HashMap<String, Pair<SuggestionReason, Double>>()

        fun addCandidate(word: String, reason: SuggestionReason, delta: Double) {
            val key = word.lowercase()
            val existing = scored[key]
            if (existing == null) {
                scored[key] = reason to delta
            } else {
                // Keep highest-priority reason while accumulating interpolated score
                val bestReason = if (reasonPriority(reason) > reasonPriority(existing.first)) reason else existing.first
                scored[key] = bestReason to (existing.second + delta)
            }
        }

        // 1. Trigram signal (highest specificity)
        if (!prevPrevWord.isNullOrBlank() && !prevWord.isNullOrBlank()) {
            val trigramKey = "${prevPrevWord.lowercase()} ${prevWord.lowercase()}"
            learnedTrigrams[trigramKey]?.entries
                ?.sortedByDescending { it.value }
                ?.take(4)
                ?.forEach { (next, count) ->
                    addCandidate(next, SuggestionReason.PERSONAL, 5600.0 + count * 120.0)
                }

            BuiltInLexicon.trigrams[trigramKey]?.forEach { (next, weight) ->
                addCandidate(next, SuggestionReason.TRIGRAM, 5000.0 + weight * 8.0)
            }
        }

        // 2. Bigram backoff signal
        if (!prevWord.isNullOrBlank()) {
            val prevLower = prevWord.lowercase()
            learnedBigrams[prevLower]?.entries
                ?.sortedByDescending { it.value }
                ?.take(4)
                ?.forEach { (next, count) ->
                    addCandidate(next, SuggestionReason.PERSONAL, 4400.0 + count * 90.0)
                }

            BuiltInLexicon.bigrams[prevLower]?.forEach { (next, weight) ->
                addCandidate(next, SuggestionReason.BIGRAM, 3800.0 + weight * 6.0)
            }
        }

        // 3. Unigram starter fallback
        if (scored.size < limit) {
            val starters = listOf("I", "The", "Yes", "Thanks", "On", "Let's", "Sounds")
            for ((idx, starter) in starters.withIndex()) {
                if (scored.size >= limit + 2) break
                val personalBonus = (learnedUnigrams[starter.lowercase()] ?: 0) * 15.0
                addCandidate(starter, SuggestionReason.COMPLETION, 900.0 - idx * 20.0 + personalBonus)
            }
        }

        return scored.entries
            .map { (lowerKey, pair) ->
                val displayWord = restoreCanonicalCasing(lowerKey)
                Suggestion(
                    text = displayWord,
                    reason = pair.first,
                    score = pair.second
                )
            }
            .sortedByDescending { it.score }
            .take(limit)
    }

    private fun restoreCanonicalCasing(word: String): String {
        return when (word) {
            "i" -> "I"
            "i'm" -> "I'm"
            "i've" -> "I've"
            "i'll" -> "I'll"
            "i'd" -> "I'd"
            "let's" -> "Let's"
            else -> word
        }
    }

    private fun reasonPriority(reason: SuggestionReason): Int = when (reason) {
        SuggestionReason.PERSONAL -> 4
        SuggestionReason.TRIGRAM -> 3
        SuggestionReason.BIGRAM -> 2
        else -> 1
    }
}
