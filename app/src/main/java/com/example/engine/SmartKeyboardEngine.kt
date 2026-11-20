package com.example.engine

import androidx.compose.ui.geometry.Offset
import com.example.data.AutoCorrectLevel
import com.example.data.SnippetEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min

enum class SuggestionReason(val badgeLabel: String) {
    SNIPPET("Snippet"),
    AUTOCORRECT("Auto-Fix"),
    COMPLETION("Trie"),
    BIGRAM("Bigram"),
    TRIGRAM("Trigram"),
    PERSONAL("Learned"),
    EMOJI_TAG("Emoji"),
    GLIDE("Glide")
}

data class Suggestion(
    val text: String,
    val displayText: String = text,
    val reason: SuggestionReason,
    val score: Double,
    val isAutoCorrectTarget: Boolean = false
)

data class EngineTelemetry(
    val algorithmLabel: String = "Trie + Markov Ready",
    val latencyMicros: Long = 0L,
    val candidatesEvaluated: Int = 0
)

data class EngineResult(
    val suggestions: List<Suggestion>,
    val telemetry: EngineTelemetry
)

class SmartKeyboardEngine {

    private val trie = TrieIndex()
    val ngramPredictor = NGramPredictor()

    init {
        BuiltInLexicon.coreWords.forEach { (word, freq) ->
            trie.insert(word, freq, isCustom = false)
        }
    }

    fun loadCustomWordsAndBigrams(
        customWords: List<Triple<String, Int, Boolean>>,
        bigrams: List<Triple<String, String, Int>>
    ) {
        customWords.forEach { (word, freq, isCustom) ->
            trie.insert(word, 2200 + freq * 25, isCustom = isCustom)
        }
        ngramPredictor.loadLearned(
            unigrams = customWords.map { it.first to it.second },
            bigrams = bigrams
        )
    }

    fun resetLearnedMemory() {
        ngramPredictor.reset()
    }

    fun recordCommittedWord(
        word: String,
        prevWord: String?,
        prevPrevWord: String? = null
    ) {
        val clean = word.trim()
        if (clean.length < 2 && clean.lowercase() != "i") return
        val lower = clean.lowercase()
        ngramPredictor.recordSequence(clean, prevWord, prevPrevWord)
        val existingFreq = trie.getEntry(lower)?.frequency ?: 2200
        trie.insert(
            word = clean,
            frequency = existingFreq + 25,
            isCustom = !BuiltInLexicon.coreWords.any { it.first == lower }
        )
    }

    fun suggest(
        currentWord: String,
        prevWord: String?,
        prevPrevWord: String?,
        snippets: List<SnippetEntity>,
        autoCorrectLevel: AutoCorrectLevel = AutoCorrectLevel.BALANCED,
        touchSamples: List<TouchSample> = emptyList()
    ): EngineResult {
        val startNs = System.nanoTime()
        var evaluated = 0

        // 1. Check deterministic semicolon snippet trigger (e.g. ;omw or ;date)
        if (currentWord.startsWith(";") && currentWord.length >= 2) {
            val lower = currentWord.lowercase()
            val matched = snippets.filter { it.isEnabled && it.trigger.lowercase().startsWith(lower) }
            if (matched.isNotEmpty()) {
                val results = matched.take(3).map { snippet ->
                    val expanded = resolveDynamicMacro(snippet.expansion)
                    val exact = snippet.trigger.equals(lower, ignoreCase = true)
                    Suggestion(
                        text = expanded,
                        displayText = "${snippet.trigger} → ${expanded.take(18)}",
                        reason = SuggestionReason.SNIPPET,
                        score = if (exact) 10_000.0 else 5_000.0,
                        isAutoCorrectTarget = exact
                    )
                }
                val elapsedUs = (System.nanoTime() - startNs) / 1000L
                return EngineResult(
                    suggestions = results,
                    telemetry = EngineTelemetry("Deterministic Macro Match", elapsedUs, matched.size)
                )
            }
        }

        // 2. Check :emoji keyword tag shortcut (e.g. :fire, :coffee)
        if (currentWord.startsWith(":") && currentWord.length >= 2) {
            val query = currentWord.drop(1).lowercase()
            val emojiMatches = BuiltInLexicon.emojiCatalog.filter { entry ->
                entry.keywords.any { it.startsWith(query) }
            }.take(4)
            if (emojiMatches.isNotEmpty()) {
                val results = emojiMatches.mapIndexed { i, e ->
                    Suggestion(
                        text = e.glyph,
                        displayText = "${e.glyph} ${e.keywords.firstOrNull().orEmpty()}",
                        reason = SuggestionReason.EMOJI_TAG,
                        score = 9000.0 - i,
                        isAutoCorrectTarget = false
                    )
                }
                val elapsedUs = (System.nanoTime() - startNs) / 1000L
                return EngineResult(
                    suggestions = results,
                    telemetry = EngineTelemetry("Emoji Keyword Index", elapsedUs, emojiMatches.size)
                )
            }
        }

        val cleanCurrent = currentWord.trim()

        // 3. Empty current word -> Trigram + Bigram + Unigram N-Gram predictor
        if (cleanCurrent.isEmpty()) {
            val predictions = ngramPredictor.predictNext(prevWord, prevPrevWord, limit = 4)
            val elapsedUs = (System.nanoTime() - startNs) / 1000L
            val label = when {
                !prevPrevWord.isNullOrBlank() && !prevWord.isNullOrBlank() -> "N-Gram Trigram / Bigram"
                !prevWord.isNullOrBlank() -> "N-Gram Bigram Backoff"
                else -> "Zipf Unigram Baseline"
            }
            return EngineResult(
                suggestions = predictions,
                telemetry = EngineTelemetry(label, elapsedUs, predictions.size)
            )
        }

        val lower = cleanCurrent.lowercase()
        val results = ArrayList<Suggestion>(8)
        val seenLower = HashSet<String>()

        // 4. Deterministic common typo / contraction lookup
        val knownTypoFix = BuiltInLexicon.commonTypos[lower]
        if (knownTypoFix != null && autoCorrectLevel != AutoCorrectLevel.OFF) {
            val cased = applyCasePattern(knownTypoFix, cleanCurrent)
            results.add(
                Suggestion(
                    text = cased,
                    reason = SuggestionReason.AUTOCORRECT,
                    score = 9_500.0,
                    isAutoCorrectTarget = true
                )
            )
            seenLower.add(cased.lowercase())
            evaluated++
        }

        // 5. Exact word match in dictionary
        val exactEntry = trie.getEntry(lower)
        if (exactEntry != null) {
            val casedExact = applyCasePattern(exactEntry.word, cleanCurrent)
            if (seenLower.add(casedExact.lowercase())) {
                results.add(
                    Suggestion(
                        text = casedExact,
                        reason = if (exactEntry.isCustom) SuggestionReason.PERSONAL else SuggestionReason.COMPLETION,
                        score = 8_500.0 + exactEntry.frequency,
                        isAutoCorrectTarget = false
                    )
                )
            }
        }

        // 6. Prefix completions from Trie boosted by N-gram context
        val prefixes = trie.prefixCompletions(lower, limit = 6)
        evaluated += prefixes.size
        for (entry in prefixes) {
            val cased = applyCasePattern(entry.word, cleanCurrent)
            if (seenLower.add(cased.lowercase())) {
                val contextBoost = ngramPredictor.contextualBoost(prevWord, prevPrevWord, entry.word)
                results.add(
                    Suggestion(
                        text = cased,
                        reason = if (entry.isCustom) SuggestionReason.PERSONAL else SuggestionReason.COMPLETION,
                        score = 5_000.0 + entry.frequency + contextBoost,
                        isAutoCorrectTarget = false
                    )
                )
            }
        }

        // 7. Coordinate-Aware Spatial Damerau-Levenshtein Fuzzy Autocorrect
        val hasTouchTrace = touchSamples.size == cleanCurrent.length
        val shouldRunFuzzy = exactEntry == null ||
            (autoCorrectLevel == AutoCorrectLevel.ASSERTIVE && cleanCurrent.length >= 3)

        if (shouldRunFuzzy && autoCorrectLevel != AutoCorrectLevel.OFF && cleanCurrent.length >= 2) {
            val maxDist = when (autoCorrectLevel) {
                AutoCorrectLevel.OFF -> 0.0f
                AutoCorrectLevel.BALANCED -> if (cleanCurrent.length <= 4) 1.45f else 2.05f
                AutoCorrectLevel.ASSERTIVE -> if (cleanCurrent.length <= 4) 1.75f else 2.45f
            }

            val allWords = trie.snapshotWords()
            val fuzzyCandidates = ArrayList<Suggestion>(6)
            for (entry in allWords) {
                val wLower = entry.word.lowercase()
                if (wLower == lower) continue
                if (abs(wLower.length - lower.length) > 2) continue
                evaluated++
                val dist = if (hasTouchTrace) {
                    spatialTouchDamerauLevenshtein(lower, touchSamples, wLower)
                } else {
                    spatialDamerauLevenshtein(lower, wLower)
                }
                if (dist <= maxDist) {
                    val contextBoost = ngramPredictor.contextualBoost(prevWord, prevPrevWord, wLower)
                    val freqNorm = entry.frequency.toDouble()
                    val candidateScore = 7_500.0 + freqNorm + contextBoost - (dist * 1_650.0)
                    val cased = applyCasePattern(entry.word, cleanCurrent)
                    fuzzyCandidates.add(
                        Suggestion(
                            text = cased,
                            reason = SuggestionReason.AUTOCORRECT,
                            score = candidateScore,
                            isAutoCorrectTarget = exactEntry == null && dist <= 1.55f
                        )
                    )
                }
            }

            fuzzyCandidates.sortByDescending { it.score }
            for (cand in fuzzyCandidates.take(3)) {
                if (seenLower.add(cand.text.lowercase())) {
                    results.add(cand)
                }
            }
        }

        val sorted = results.sortedWith(
            compareByDescending<Suggestion> { it.isAutoCorrectTarget }
                .thenByDescending { it.score }
        ).take(4)

        val finalized = if (exactEntry != null && knownTypoFix == null) {
            sorted.map { it.copy(isAutoCorrectTarget = false) }
        } else {
            var marked = false
            sorted.map { item ->
                if (item.isAutoCorrectTarget && !marked) {
                    marked = true
                    item
                } else {
                    item.copy(isAutoCorrectTarget = false)
                }
            }
        }

        val elapsedUs = (System.nanoTime() - startNs) / 1000L
        val algoName = when {
            finalized.any { it.reason == SuggestionReason.AUTOCORRECT } ->
                if (hasTouchTrace) "Touch-Coord Spatial Edit" else "Spatial Edit + Trie"
            finalized.any { it.reason == SuggestionReason.PERSONAL } -> "Personal Lexicon Trie"
            else -> "Prefix Trie + N-Gram"
        }
        return EngineResult(
            suggestions = finalized,
            telemetry = EngineTelemetry(algoName, elapsedUs, evaluated)
        )
    }

    fun decodeGlidePath(
        points: List<Offset>,
        keyCenters: Map<Char, Offset>,
        prevWord: String?,
        prevPrevWord: String? = null
    ): EngineResult {
        val startNs = System.nanoTime()
        if (points.size < 3 || keyCenters.isEmpty()) {
            return EngineResult(emptyList(), EngineTelemetry("Glide Cancelled", 0L, 0))
        }

        val resampled = resamplePath(points, 16)
        val startPt = resampled.first()
        val endPt = resampled.last()

        val startKeys = keyCenters.entries
            .sortedBy { hypot((it.value.x - startPt.x).toDouble(), (it.value.y - startPt.y).toDouble()) }
            .take(3)
            .map { it.key }
            .toSet()

        val endKeys = keyCenters.entries
            .sortedBy { hypot((it.value.x - endPt.x).toDouble(), (it.value.y - endPt.y).toDouble()) }
            .take(3)
            .map { it.key }
            .toSet()

        val allWords = trie.snapshotWords()
        var evaluated = 0
        val candidates = ArrayList<Suggestion>(8)

        for (entry in allWords) {
            val w = entry.word.lowercase().filter { it in 'a'..'z' }
            if (w.length < 2) continue
            if (w.first() !in startKeys || w.last() !in endKeys) continue
            evaluated++

            val idealPoints = w.mapNotNull { keyCenters[it] }
            if (idealPoints.size != w.length) continue
            val idealResampled = resamplePath(idealPoints, 16)

            var totalDist = 0.0
            for (i in 0 until 16) {
                val p1 = resampled[i]
                val p2 = idealResampled[i]
                totalDist += hypot((p1.x - p2.x).toDouble(), (p1.y - p2.y).toDouble())
            }
            val avgDist = totalDist / 16.0

            val qPos = keyCenters['q']
            val pPos = keyCenters['p']
            val keyWidth = if (qPos != null && pPos != null) abs(pPos.x - qPos.x) / 9f else 90f
            val normalizedError = avgDist / max(keyWidth, 24f)

            if (normalizedError <= 1.35) {
                val contextBoost = ngramPredictor.contextualBoost(prevWord, prevPrevWord, w)
                val score = 10_000.0 - (normalizedError * 3_200.0) + entry.frequency * 0.6 + contextBoost
                candidates.add(
                    Suggestion(
                        text = entry.word,
                        reason = SuggestionReason.GLIDE,
                        score = score,
                        isAutoCorrectTarget = false
                    )
                )
            }
        }

        val ranked = candidates.sortedByDescending { it.score }.take(3)
        val elapsedUs = (System.nanoTime() - startNs) / 1000L
        return EngineResult(
            suggestions = ranked,
            telemetry = EngineTelemetry("Geometric Path Alignment", elapsedUs, evaluated)
        )
    }

    fun resolveDynamicMacro(raw: String): String {
        val now = Date()
        return raw
            .replace("{date}", SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(now))
            .replace("{time}", SimpleDateFormat("h:mm a", Locale.getDefault()).format(now))
            .replace("{iso}", SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(now))
    }

    /**
     * Touch-coordinate-aware Damerau-Levenshtein distance using recorded [TouchSample] coordinates
     * at each character index.
     */
    fun spatialTouchDamerauLevenshtein(
        typed: String,
        touchSamples: List<TouchSample>,
        candidate: String
    ): Float {
        if (touchSamples.size != typed.length) return spatialDamerauLevenshtein(typed, candidate)
        val m = typed.length
        val n = candidate.length
        if (m == 0) return n.toFloat()
        if (n == 0) return m.toFloat()

        val dp = Array(m + 1) { FloatArray(n + 1) }
        for (i in 0..m) dp[i][0] = i.toFloat()
        for (j in 0..n) dp[0][j] = j.toFloat()

        for (i in 1..m) {
            for (j in 1..n) {
                val subCost = SpatialLayoutModel.touchSubstitutionCost(touchSamples[i - 1], candidate[j - 1])
                var best = min(
                    dp[i - 1][j] + 0.9f,
                    min(
                        dp[i][j - 1] + 0.9f,
                        dp[i - 1][j - 1] + subCost
                    )
                )
                if (i > 1 && j > 1 && typed[i - 1] == candidate[j - 2] && typed[i - 2] == candidate[j - 1]) {
                    best = min(best, dp[i - 2][j - 2] + 0.45f)
                }
                dp[i][j] = best
            }
        }
        return dp[m][n]
    }

    /**
     * Weighted Optimal String Alignment (Damerau-Levenshtein) incorporating QWERTY spatial proximity.
     */
    fun spatialDamerauLevenshtein(a: String, b: String): Float {
        val m = a.length
        val n = b.length
        if (m == 0) return n.toFloat()
        if (n == 0) return m.toFloat()

        val dp = Array(m + 1) { FloatArray(n + 1) }
        for (i in 0..m) dp[i][0] = i.toFloat()
        for (j in 0..n) dp[0][j] = j.toFloat()

        for (i in 1..m) {
            for (j in 1..n) {
                val subCost = SpatialLayoutModel.substitutionCost(a[i - 1], b[j - 1])
                var best = min(
                    dp[i - 1][j] + 0.9f,
                    min(
                        dp[i][j - 1] + 0.9f,
                        dp[i - 1][j - 1] + subCost
                    )
                )
                if (i > 1 && j > 1 && a[i - 1] == b[j - 2] && a[i - 2] == b[j - 1]) {
                    best = min(best, dp[i - 2][j - 2] + 0.45f)
                }
                dp[i][j] = best
            }
        }
        return dp[m][n]
    }

    private fun applyCasePattern(target: String, typed: String): String {
        if (typed.isEmpty()) return target
        if (target == "I" || target.startsWith("I'")) return target
        val letters = typed.filter { it.isLetter() }
        if (letters.isEmpty()) return target
        return when {
            letters.length > 1 && letters.all { it.isUpperCase() } -> target.uppercase()
            letters.first().isUpperCase() -> target.replaceFirstChar {
                if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
            }
            else -> target
        }
    }

    private fun resamplePath(points: List<Offset>, targetCount: Int): List<Offset> {
        if (points.size == 1) return List(targetCount) { points.first() }
        val cumulative = FloatArray(points.size)
        var totalLength = 0f
        for (i in 1 until points.size) {
            totalLength += hypot(
                (points[i].x - points[i - 1].x).toDouble(),
                (points[i].y - points[i - 1].y).toDouble()
            ).toFloat()
            cumulative[i] = totalLength
        }
        if (totalLength < 1f) return List(targetCount) { points.first() }

        val out = ArrayList<Offset>(targetCount)
        val step = totalLength / (targetCount - 1)
        var segIdx = 0
        for (k in 0 until targetCount) {
            val targetDist = k * step
            while (segIdx < cumulative.size - 2 && cumulative[segIdx + 1] < targetDist) {
                segIdx++
            }
            val segStart = cumulative[segIdx]
            val segEnd = cumulative[segIdx + 1]
            val segLen = max(segEnd - segStart, 0.001f)
            val t = ((targetDist - segStart) / segLen).coerceIn(0f, 1f)
            val pA = points[segIdx]
            val pB = points[segIdx + 1]
            out.add(
                Offset(
                    x = pA.x + (pB.x - pA.x) * t,
                    y = pA.y + (pB.y - pA.y) * t
                )
            )
        }
        return out
    }
}
