package com.example.data

import com.example.engine.BuiltInLexicon
import com.example.engine.SmartKeyboardEngine
import com.example.ui.theme.KeyboardThemeStyle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class KeyCraftRepository(
    private val dao: KeyCraftDao,
    val preferences: KeyboardPreferences,
    val engine: SmartKeyboardEngine
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val settingsFlow: Flow<KeyboardSettings> = preferences.settingsFlow
    val snippetsFlow: Flow<List<SnippetEntity>> = dao.observeSnippets()
    val learnedWordsFlow: Flow<List<LearnedWordEntity>> = dao.observeLearnedWords()
    val topBigramsFlow: Flow<List<BigramEntity>> = dao.observeTopBigrams()
    val clipboardFlow: Flow<List<ClipboardItemEntity>> = dao.observeClipboardItems()

    fun preloadEngineMemory() {
        scope.launch {
            val words = dao.getAllLearnedWordsOnce()
            val bigrams = dao.getAllBigramsOnce()
            engine.loadCustomWordsAndBigrams(
                customWords = words.map { Triple(it.word, it.frequency, it.isCustom) },
                bigrams = bigrams.map { Triple(it.prevWord, it.nextWord, it.count) }
            )
        }
    }

    fun recordCommittedWordAsync(word: String, prevWord: String?, prevPrevWord: String? = null) {
        val clean = word.trim()
        if (clean.length < 2 && clean.lowercase() != "i") return
        engine.recordCommittedWord(clean, prevWord, prevPrevWord)

        scope.launch {
            val lower = clean.lowercase()
            val existing = dao.getLearnedWord(lower)
            val isBuiltIn = BuiltInLexicon.coreWords.any { it.first == lower }
            if (existing != null) {
                dao.upsertLearnedWord(
                    existing.copy(
                        frequency = existing.frequency + 1,
                        lastUsedAt = System.currentTimeMillis()
                    )
                )
            } else {
                dao.upsertLearnedWord(
                    LearnedWordEntity(
                        word = lower,
                        frequency = 1,
                        isCustom = !isBuiltIn,
                        lastUsedAt = System.currentTimeMillis()
                    )
                )
            }

            if (!prevWord.isNullOrBlank()) {
                val p = prevWord.trim().lowercase()
                val existingBi = dao.getBigram(p, lower)
                dao.upsertBigram(
                    BigramEntity(
                        prevWord = p,
                        nextWord = lower,
                        count = (existingBi?.count ?: 0) + 1,
                        lastUsedAt = System.currentTimeMillis()
                    )
                )
            }
        }
    }

    suspend fun addCustomWord(word: String) {
        val clean = word.trim()
        if (clean.isEmpty()) return
        engine.loadCustomWordsAndBigrams(
            customWords = listOf(Triple(clean, 10, true)),
            bigrams = emptyList()
        )
        dao.upsertLearnedWord(
            LearnedWordEntity(
                word = clean.lowercase(),
                frequency = 10,
                isCustom = true,
                lastUsedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun deleteLearnedWord(word: String) {
        dao.deleteLearnedWord(word)
    }

    suspend fun resetAutoLearnedMemory() {
        dao.clearAutoLearnedWords()
        dao.clearAllBigrams()
        engine.resetLearnedMemory()
        val remainingCustom = dao.getAllLearnedWordsOnce()
        engine.loadCustomWordsAndBigrams(
            customWords = remainingCustom.map { Triple(it.word, it.frequency, it.isCustom) },
            bigrams = emptyList()
        )
    }

    suspend fun addOrUpdateSnippet(snippet: SnippetEntity) {
        val normalizedTrigger = if (snippet.trigger.startsWith(";")) {
            snippet.trigger.trim().lowercase()
        } else {
            ";${snippet.trigger.trim().lowercase()}"
        }
        dao.insertSnippet(snippet.copy(trigger = normalizedTrigger))
    }

    suspend fun toggleSnippet(snippet: SnippetEntity) {
        dao.updateSnippet(snippet.copy(isEnabled = !snippet.isEnabled))
    }

    suspend fun deleteSnippet(id: Int) {
        dao.deleteSnippetById(id)
    }

    fun incrementSnippetUseAsync(id: Int) {
        scope.launch { dao.incrementSnippetUse(id) }
    }

    fun recordClipboardTextAsync(text: String) {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return
        scope.launch {
            val existing = dao.findClipboardByContent(trimmed)
            if (existing != null) {
                dao.upsertClipboardItem(existing.copy(createdAt = System.currentTimeMillis()))
            } else {
                dao.upsertClipboardItem(
                    ClipboardItemEntity(
                        content = trimmed,
                        isPinned = false,
                        createdAt = System.currentTimeMillis()
                    )
                )
            }
        }
    }

    suspend fun toggleClipboardPin(item: ClipboardItemEntity) {
        dao.setClipboardPinned(item.id, !item.isPinned)
    }

    suspend fun deleteClipboardItem(id: Int) {
        dao.deleteClipboardItem(id)
    }

    suspend fun clearUnpinnedClipboard() {
        dao.clearUnpinnedClipboard()
    }

    suspend fun setTheme(style: KeyboardThemeStyle) = preferences.setTheme(style)
    suspend fun setShowNumberRow(enabled: Boolean) = preferences.setShowNumberRow(enabled)
    suspend fun setHapticFeedback(enabled: Boolean) = preferences.setHapticFeedback(enabled)
    suspend fun setKeyPopups(enabled: Boolean) = preferences.setKeyPopups(enabled)
    suspend fun setAutoCapitalize(enabled: Boolean) = preferences.setAutoCapitalize(enabled)
    suspend fun setDoubleSpacePeriod(enabled: Boolean) = preferences.setDoubleSpacePeriod(enabled)
    suspend fun setAutoCorrectLevel(level: AutoCorrectLevel) = preferences.setAutoCorrectLevel(level)
    suspend fun setGlideTypingEnabled(enabled: Boolean) = preferences.setGlideTypingEnabled(enabled)
    suspend fun setOneHandedMode(mode: OneHandedMode) = preferences.setOneHandedMode(mode)
    suspend fun setKeyboardScale(scale: Float) = preferences.setKeyboardScale(scale)
}
