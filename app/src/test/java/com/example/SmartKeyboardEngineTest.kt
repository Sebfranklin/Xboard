package com.example

import androidx.compose.ui.geometry.Offset
import com.example.data.AutoCorrectLevel
import com.example.data.SnippetEntity
import com.example.engine.SmartKeyboardEngine
import com.example.engine.SpatialLayoutModel
import com.example.engine.SuggestionReason
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
}
