package com.example

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import com.example.data.AutoCorrectLevel
import com.example.data.SnippetEntity
import com.example.engine.NGramPredictor
import com.example.engine.SmartKeyboardEngine
import com.example.engine.SpatialLayoutModel
import com.example.engine.SuggestionReason
import com.example.engine.TouchSample
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SmartKeyboardEngineTest {

    private val engine = SmartKeyboardEngine()
    private val sampleSnippets = listOf(
        SnippetEntity(id = 1, trigger = ";omw", expansion = "On my way!"),
        SnippetEntity(id = 2, trigger = ";date", expansion = "{date}")
    )

    @Test
    fun `spatial proximity prefers adjacent key over distant key`() {
        val adjacentCost = SpatialLayoutModel.substitutionCost('e', 'r')
        val distantCost = SpatialLayoutModel.substitutionCost('e', 'p')
        assertTrue("Adjacent key cost ($adjacentCost) should be smaller than distant ($distantCost)", adjacentCost < distantCost)
    }

    @Test
    fun `autocorrects common transpositions and contractions`() {
        val tehResult = engine.suggest("teh", null, null, sampleSnippets, AutoCorrectLevel.BALANCED)
        assertEquals("the", tehResult.suggestions.first().text)
        assertTrue(tehResult.suggestions.first().isAutoCorrectTarget)

        val dontResult = engine.suggest("dont", null, null, sampleSnippets, AutoCorrectLevel.BALANCED)
        assertEquals("don't", dontResult.suggestions.first().text)
    }

    @Test
    fun `fuzzy spatial autocorrect fixes adjacent key slips`() {
        // 'helo' -> 'hello', 'hellp' -> 'hello' (p is next to o)
        val res = engine.suggest("hellp", null, null, sampleSnippets, AutoCorrectLevel.BALANCED)
        assertEquals("hello", res.suggestions.first().text)
        assertEquals(SuggestionReason.AUTOCORRECT, res.suggestions.first().reason)
        assertTrue(res.suggestions.first().isAutoCorrectTarget)
    }

    @Test
    fun `markov trigram and bigram predict next word deterministically`() {
        val trigramRes = engine.suggest("", prevWord = "my", prevPrevWord = "on", snippets = sampleSnippets)
        assertEquals("way", trigramRes.suggestions.first().text)
        assertEquals(SuggestionReason.TRIGRAM, trigramRes.suggestions.first().reason)

        val bigramRes = engine.suggest("", prevWord = "thank", prevPrevWord = null, snippets = sampleSnippets)
        assertEquals("you", bigramRes.suggestions.first().text)
    }

    @Test
    fun `snippet trigger expands deterministically`() {
        val res = engine.suggest(";omw", null, null, sampleSnippets)
        assertEquals("On my way!", res.suggestions.first().text)
        assertEquals(SuggestionReason.SNIPPET, res.suggestions.first().reason)
        assertTrue(res.suggestions.first().isAutoCorrectTarget)
    }

    @Test
    fun `emoji keyword tag surfaces matching glyph`() {
        val res = engine.suggest(":fire", null, null, sampleSnippets)
        assertEquals("🔥", res.suggestions.first().text)
        assertEquals(SuggestionReason.EMOJI_TAG, res.suggestions.first().reason)
    }

    @Test
    fun `personal word learning adapts bigram ranking offline`() {
        engine.recordCommittedWord("keycraft", "love")
        engine.recordCommittedWord("keycraft", "love")
        engine.recordCommittedWord("keycraft", "love")

        val res = engine.suggest("", prevWord = "love", prevPrevWord = null, snippets = sampleSnippets)
        assertEquals("keycraft", res.suggestions.first().text)
        assertEquals(SuggestionReason.PERSONAL, res.suggestions.first().reason)
    }

    @Test
    fun `geometric glide path decoder matches spatial word trajectory`() {
        // Construct key centers matching QWERTY spatial model scaled by 100px
        val centers = SpatialLayoutModel.keyCoords.mapValues { (_, kc) ->
            Offset(kc.x * 100f, kc.y * 100f)
        }
        // Trace path for "the": t -> h -> e
        val path = listOf(
            centers.getValue('t'),
            Offset((centers.getValue('t').x + centers.getValue('h').x) / 2f, (centers.getValue('t').y + centers.getValue('h').y) / 2f),
            centers.getValue('h'),
            Offset((centers.getValue('h').x + centers.getValue('e').x) / 2f, (centers.getValue('h').y + centers.getValue('e').y) / 2f),
            centers.getValue('e')
        )
        val res = engine.decodeGlidePath(path, centers, prevWord = null)
        assertTrue("Expected 'the' in top glide candidates, got ${res.suggestions.map { it.text }}",
            res.suggestions.any { it.text.equals("the", ignoreCase = true) })
    }

    @Test
    fun `toNormalizedTouch maps screen coordinates onto 10x3 grid and ranks nearest keys`() {
        // 1000x300 screen layout: q=(0..100,0..100), p=(900..1000,0..100), z=(135..235,200..300)
        val bounds = mapOf(
            'q' to Rect(0f, 0f, 100f, 100f),
            'w' to Rect(100f, 0f, 200f, 100f),
            'e' to Rect(200f, 0f, 300f, 100f),
            'r' to Rect(300f, 0f, 400f, 100f),
            'p' to Rect(900f, 0f, 1000f, 100f),
            'z' to Rect(135f, 200f, 235f, 300f)
        )
        // Finger lands just inside 'r' (x=305px -> normX=3.05), right on the border next to 'e' (x=2.5)
        val sample = SpatialLayoutModel.toNormalizedTouch(
            pos = Offset(305f, 50f),
            keyBounds = bounds,
            fallbackChar = 'r'
        )!!
        assertEquals('r', sample.tappedChar)
        assertEquals(3.05f, sample.normX, 0.02f)
        assertEquals(0.50f, sample.normY, 0.02f)

        val nearest = SpatialLayoutModel.nearestKeys(sample.normX, sample.normY, limit = 3)
        assertEquals('r', nearest[0].char)
        assertEquals('e', nearest[1].char)
        assertTrue(nearest[1].likelihood > nearest[2].likelihood)
    }

    @Test
    fun `touch coordinate autocorrect fixes boundary slip and reports touch telemetry`() {
        // User meant "hello" but slipped slightly right from 'e' (2.5, 0.5) onto left border of 'r' (3.05, 0.5)
        val samples = listOf(
            TouchSample('h', 6.0f, 1.5f),
            TouchSample('r', 3.05f, 0.5f), // 0.55 key units from 'e' -> low substitution cost (0.22f)
            TouchSample('l', 9.0f, 1.5f),
            TouchSample('l', 9.0f, 1.5f),
            TouchSample('o', 8.5f, 0.5f)
        )
        val touchDistToHello = engine.spatialTouchDamerauLevenshtein("hrllo", samples, "hello")
        val centerDistToHello = engine.spatialDamerauLevenshtein("hrllo", "hello")
        assertTrue(
            "Boundary touch distance ($touchDistToHello) should be smaller than key-center distance ($centerDistToHello)",
            touchDistToHello < centerDistToHello
        )

        val res = engine.suggest(
            currentWord = "hrllo",
            prevWord = null,
            prevPrevWord = null,
            snippets = sampleSnippets,
            autoCorrectLevel = AutoCorrectLevel.BALANCED,
            touchSamples = samples
        )
        assertEquals("hello", res.suggestions.first().text)
        assertEquals(SuggestionReason.AUTOCORRECT, res.suggestions.first().reason)
        assertTrue(res.suggestions.first().isAutoCorrectTarget)
        assertEquals("Touch-Coord Spatial Edit", res.telemetry.algorithmLabel)
    }

    @Test
    fun `ngram predictor performs interpolated trigram bigram unigram backoff and learns trigrams`() {
        val predictor = NGramPredictor()

        // Trigram match: "how are" -> "you"
        val trigramPreds = predictor.predictNext(prevWord = "are", prevPrevWord = "how", limit = 4)
        assertEquals("you", trigramPreds.first().text)
        assertEquals(SuggestionReason.TRIGRAM, trigramPreds.first().reason)

        // Bigram match without trigram: "see" -> "you"
        val bigramPreds = predictor.predictNext(prevWord = "see", prevPrevWord = null, limit = 4)
        assertEquals("you", bigramPreds.first().text)
        assertEquals(SuggestionReason.BIGRAM, bigramPreds.first().reason)

        // Unigram starter fallback when context is empty
        val unigramPreds = predictor.predictNext(prevWord = null, prevPrevWord = null, limit = 4)
        assertEquals(4, unigramPreds.size)
        assertEquals("I", unigramPreds.first().text)

        // Personal trigram learning overrides generic predictions for that 2-word prefix
        predictor.recordSequence("smooth", prevWord = "are", prevPrevWord = "how")
        predictor.recordSequence("smooth", prevWord = "are", prevPrevWord = "how")
        val learnedPreds = predictor.predictNext(prevWord = "are", prevPrevWord = "how", limit = 4)
        assertEquals("smooth", learnedPreds.first().text)
        assertEquals(SuggestionReason.PERSONAL, learnedPreds.first().reason)
        assertTrue(
            predictor.contextualBoost(prevWord = "are", prevPrevWord = "how", candidate = "smooth") >
                predictor.contextualBoost(prevWord = "are", prevPrevWord = "how", candidate = "potato")
        )
    }
}
