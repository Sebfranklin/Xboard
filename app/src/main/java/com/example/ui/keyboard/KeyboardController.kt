package com.example.ui.keyboard

import androidx.compose.ui.geometry.Offset
import com.example.data.AutoCorrectLevel
import com.example.data.KeyCraftRepository
import com.example.data.KeyboardSettings
import com.example.data.SnippetEntity
import com.example.engine.EngineTelemetry
import com.example.engine.SpatialLayoutModel
import com.example.engine.Suggestion
import com.example.engine.SuggestionReason
import com.example.engine.TouchSample
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.Locale

class KeyboardController(
    private val repository: KeyCraftRepository,
    private val target: InputTarget
) {
    private val _uiState = MutableStateFlow(KeyboardUiState())
    val uiState: StateFlow<KeyboardUiState> = _uiState.asStateFlow()

    private var lastSpaceTapMs: Long = 0L
    private var lastShiftTapMs: Long = 0L
    private var lastGlideCommittedLength: Int = 0
    private var composingBuffer: String = ""
    private val composingTouchSamples = mutableListOf<TouchSample>()
    private var undoAutoCorrectState: AutoCorrectUndo? = null

    private data class AutoCorrectUndo(
        val originalToken: String,
        val replacedWith: String,
        val separator: String
    )

    fun setEnvironmentFlags(
        enterAction: EnterActionType,
        showGlobe: Boolean,
        isPasswordField: Boolean = false,
        disableAutoCorrectForField: Boolean = false,
        initialLayoutMode: KeyboardLayoutMode = KeyboardLayoutMode.QWERTY,
        initialShiftState: ShiftState? = null
    ) {
        composingBuffer = ""
        composingTouchSamples.clear()
        undoAutoCorrectState = null
        lastGlideCommittedLength = 0
        _uiState.update {
            it.copy(
                enterAction = enterAction,
                showGlobeKey = showGlobe,
                isPasswordField = isPasswordField,
                disableAutoCorrectForField = disableAutoCorrectForField,
                layoutMode = initialLayoutMode,
                shiftState = initialShiftState ?: it.shiftState
            )
        }
    }

    fun resetComposingState() {
        if (composingBuffer.isNotEmpty()) {
            target.finishComposingText()
            composingBuffer = ""
        }
        composingTouchSamples.clear()
    }

    fun refreshContext(settings: KeyboardSettings, snippets: List<SnippetEntity>) {
        val stateSnapshot = _uiState.value
        if (stateSnapshot.isPasswordField) {
            _uiState.update {
                it.copy(
                    suggestions = emptyList(),
                    telemetry = EngineTelemetry("Password Privacy Guard", 0L, 0),
                    lastAutoCorrectOriginal = null
                )
            }
            return
        }

        val before = target.getTextBeforeCursor(80)
        val (currentToken, prevWord, prevPrevWord) = extractTokens(before)
        if (currentToken != composingBuffer) {
            // Keep composing buffer synchronized when cursor moves externally
            composingBuffer = if (currentToken.isEmpty()) "" else currentToken
            if (currentToken.isEmpty() || composingTouchSamples.size != currentToken.length) {
                composingTouchSamples.clear()
            }
        }

        val effectiveAutoCorrect = if (stateSnapshot.disableAutoCorrectForField) {
            AutoCorrectLevel.OFF
        } else {
            settings.autoCorrectLevel
        }

        val alignedSamples = if (composingTouchSamples.size == currentToken.length && currentToken.isNotEmpty()) {
            composingTouchSamples.toList()
        } else {
            emptyList()
        }

        val result = repository.engine.suggest(
            currentWord = currentToken,
            prevWord = prevWord,
            prevPrevWord = prevPrevWord,
            snippets = snippets,
            autoCorrectLevel = effectiveAutoCorrect,
            touchSamples = alignedSamples
        )

        val shouldAutoCap = settings.autoCapitalize && shouldCapitalizeAfter(before)
        _uiState.update { state ->
            val nextShift = when {
                state.shiftState == ShiftState.CAPS_LOCK -> ShiftState.CAPS_LOCK
                currentToken.isEmpty() && shouldAutoCap -> ShiftState.ONCE
                currentToken.isEmpty() && !shouldAutoCap -> ShiftState.OFF
                else -> state.shiftState
            }
            state.copy(
                shiftState = nextShift,
                suggestions = result.suggestions,
                telemetry = result.telemetry,
                lastAutoCorrectOriginal = undoAutoCorrectState?.originalToken
            )
        }
    }

    fun onCharacterPressed(
        rawChar: String,
        settings: KeyboardSettings,
        snippets: List<SnippetEntity>,
        touchSample: TouchSample? = null
    ) {
        undoAutoCorrectState = null
        lastGlideCommittedLength = 0
        val currentState = _uiState.value
        val currentShift = currentState.shiftState
        val textToInsert = if (currentShift != ShiftState.OFF && rawChar.length == 1 && rawChar[0].isLetter()) {
            rawChar.uppercase(Locale.getDefault())
        } else {
            rawChar
        }

        val isWordChar = !currentState.isPasswordField &&
            textToInsert.length == 1 &&
            (textToInsert[0].isLetterOrDigit() || textToInsert[0] == '\'' || textToInsert[0] == ';' || textToInsert[0] == ':')

        if (isWordChar) {
            val before = target.getTextBeforeCursor(80)
            val (existingToken, _, _) = extractTokens(before)
            if (existingToken != composingBuffer) {
                composingTouchSamples.clear()
            }
            composingBuffer = existingToken + textToInsert
            if (composingBuffer.length == composingTouchSamples.size + 1) {
                val lowerChar = textToInsert[0].lowercaseChar()
                val resolvedSample = touchSample ?: SpatialLayoutModel.keyCoords[lowerChar]?.let { kc ->
                    TouchSample(lowerChar, kc.x, kc.y)
                } ?: TouchSample(lowerChar, -1f, -1f)
                composingTouchSamples.add(resolvedSample)
            }
            target.setComposingText(composingBuffer)
        } else {
            composingTouchSamples.clear()
            if (composingBuffer.isNotEmpty()) {
                target.finishComposingText()
                composingBuffer = ""
            }
            target.commitText(textToInsert)
        }

        if (currentShift == ShiftState.ONCE) {
            _uiState.update { it.copy(shiftState = ShiftState.OFF) }
        }
        refreshContext(settings, snippets)
    }

    fun onSpacePressed(settings: KeyboardSettings, snippets: List<SnippetEntity>) {
        val now = System.currentTimeMillis()
        val before = target.getTextBeforeCursor(80)
        val isPassword = _uiState.value.isPasswordField
        lastGlideCommittedLength = 0

        if (isPassword) {
            composingBuffer = ""
            composingTouchSamples.clear()
            target.commitText(" ")
            return
        }

        // Double-space period shortcut
        if (settings.doubleSpacePeriod &&
            now - lastSpaceTapMs < 550L &&
            before.length >= 2 &&
            before.endsWith(" ") &&
            before[before.length - 2].isLetterOrDigit()
        ) {
            composingBuffer = ""
            composingTouchSamples.clear()
            target.finishComposingText()
            target.deleteBeforeCursor(1)
            target.commitText(". ")
            undoAutoCorrectState = null
            lastSpaceTapMs = 0L
            refreshContext(settings, snippets)
            return
        }

        lastSpaceTapMs = now
        val (currentToken, prevWord, prevPrevWord) = extractTokens(before)

        val autoTarget = _uiState.value.suggestions.firstOrNull { it.isAutoCorrectTarget }
        if (currentToken.isNotEmpty() && autoTarget != null) {
            if (composingBuffer == currentToken) {
                // Commit directly over the active composing span
                target.commitText(autoTarget.text + " ")
            } else {
                target.deleteBeforeCursor(currentToken.length)
                target.commitText(autoTarget.text + " ")
            }
            composingBuffer = ""
            composingTouchSamples.clear()

            if (autoTarget.reason == SuggestionReason.SNIPPET) {
                snippets.firstOrNull { it.trigger.equals(currentToken, ignoreCase = true) }?.let {
                    repository.incrementSnippetUseAsync(it.id)
                }
                undoAutoCorrectState = AutoCorrectUndo(currentToken, autoTarget.text, " ")
            } else {
                repository.recordCommittedWordAsync(autoTarget.text, prevWord, prevPrevWord)
                undoAutoCorrectState = AutoCorrectUndo(currentToken, autoTarget.text, " ")
            }
        } else {
            if (currentToken.isNotEmpty() && !currentToken.startsWith(";") && !currentToken.startsWith(":")) {
                repository.recordCommittedWordAsync(currentToken, prevWord, prevPrevWord)
            }
            undoAutoCorrectState = null
            composingTouchSamples.clear()
            if (composingBuffer.isNotEmpty()) {
                target.finishComposingText()
                composingBuffer = ""
            }
            target.commitText(" ")
        }

        refreshContext(settings, snippets)
    }

    fun onPunctuationPressed(
        symbol: String,
        settings: KeyboardSettings,
        snippets: List<SnippetEntity>
    ) {
        lastGlideCommittedLength = 0
        if (_uiState.value.isPasswordField) {
            composingBuffer = ""
            composingTouchSamples.clear()
            target.commitText(symbol)
            return
        }

        val before = target.getTextBeforeCursor(80)
        val (currentToken, prevWord, prevPrevWord) = extractTokens(before)

        val autoTarget = _uiState.value.suggestions.firstOrNull {
            it.isAutoCorrectTarget && it.reason == SuggestionReason.AUTOCORRECT
        }
        if (currentToken.isNotEmpty() && autoTarget != null) {
            if (composingBuffer == currentToken) {
                target.commitText(autoTarget.text + symbol)
            } else {
                target.deleteBeforeCursor(currentToken.length)
                target.commitText(autoTarget.text + symbol)
            }
            composingBuffer = ""
            composingTouchSamples.clear()
            repository.recordCommittedWordAsync(autoTarget.text, prevWord, prevPrevWord)
            undoAutoCorrectState = AutoCorrectUndo(currentToken, autoTarget.text, symbol)
        } else {
            if (currentToken.isNotEmpty()) {
                repository.recordCommittedWordAsync(currentToken, prevWord, prevPrevWord)
            }
            undoAutoCorrectState = null
            composingTouchSamples.clear()
            if (composingBuffer.isNotEmpty()) {
                target.finishComposingText()
                composingBuffer = ""
            }
            target.commitText(symbol)
        }
        refreshContext(settings, snippets)
    }

    fun onBackspacePressed(settings: KeyboardSettings, snippets: List<SnippetEntity>) {
        // Immediate 1-tap revert of autocorrection or snippet expansion
        val undo = undoAutoCorrectState
        if (undo != null) {
            val expected = undo.replacedWith + undo.separator
            val before = target.getTextBeforeCursor(expected.length + 4)
            if (before.endsWith(expected)) {
                target.deleteBeforeCursor(expected.length)
                composingBuffer = undo.originalToken
                composingTouchSamples.clear()
                target.setComposingText(composingBuffer)
                undoAutoCorrectState = null
                refreshContext(settings, snippets)
                return
            }
        }
        undoAutoCorrectState = null
        lastGlideCommittedLength = 0

        if (composingBuffer.length > 1) {
            composingBuffer = composingBuffer.dropLast(1)
            if (composingTouchSamples.isNotEmpty()) {
                composingTouchSamples.removeAt(composingTouchSamples.lastIndex)
            }
            target.setComposingText(composingBuffer)
        } else if (composingBuffer.length == 1) {
            composingBuffer = ""
            composingTouchSamples.clear()
            target.commitText("")
        } else {
            composingTouchSamples.clear()
            target.deleteBeforeCursor(1)
        }
        refreshContext(settings, snippets)
    }

    fun onDeleteWordSwipe(settings: KeyboardSettings, snippets: List<SnippetEntity>) {
        undoAutoCorrectState = null
        lastGlideCommittedLength = 0
        composingBuffer = ""
        composingTouchSamples.clear()
        target.finishComposingText()
        val before = target.getTextBeforeCursor(64)
        if (before.isEmpty()) return
        var idx = before.length
        while (idx > 0 && before[idx - 1].isWhitespace()) idx--
        while (idx > 0 && !before[idx - 1].isWhitespace()) idx--
        val deleteCount = before.length - idx
        if (deleteCount > 0) {
            target.deleteBeforeCursor(deleteCount)
        }
        refreshContext(settings, snippets)
    }

    fun onSuggestionTapped(
        suggestion: Suggestion,
        settings: KeyboardSettings,
        snippets: List<SnippetEntity>
    ) {
        if (_uiState.value.isPasswordField) return
        undoAutoCorrectState = null
        val before = target.getTextBeforeCursor(80)
        val (currentToken, prevWord, prevPrevWord) = extractTokens(before)

        if (suggestion.reason == SuggestionReason.GLIDE && lastGlideCommittedLength > 0) {
            composingBuffer = ""
            composingTouchSamples.clear()
            if (before.endsWith(" ")) {
                target.deleteBeforeCursor(lastGlideCommittedLength)
            } else {
                target.deleteBeforeCursor(currentToken.length)
            }
            val cased = applyShiftToWord(suggestion.text)
            target.commitText("$cased ")
            lastGlideCommittedLength = cased.length + 1
            repository.recordCommittedWordAsync(cased, prevWord, prevPrevWord)
            refreshContext(settings, snippets)
            return
        }

        val insert = if (suggestion.reason == SuggestionReason.EMOJI_TAG) {
            suggestion.text
        } else {
            "${suggestion.text} "
        }

        if (currentToken.isNotEmpty()) {
            if (composingBuffer == currentToken) {
                target.commitText(insert)
            } else {
                target.deleteBeforeCursor(currentToken.length)
                target.commitText(insert)
            }
        } else {
            target.commitText(insert)
        }
        composingBuffer = ""
        composingTouchSamples.clear()

        if (suggestion.reason == SuggestionReason.SNIPPET) {
            snippets.firstOrNull { it.trigger.equals(currentToken, ignoreCase = true) }?.let {
                repository.incrementSnippetUseAsync(it.id)
            }
        } else if (suggestion.reason != SuggestionReason.EMOJI_TAG) {
            repository.recordCommittedWordAsync(suggestion.text, prevWord, prevPrevWord)
        }
        refreshContext(settings, snippets)
    }

    fun onUndoAutoCorrectTapped(settings: KeyboardSettings, snippets: List<SnippetEntity>) {
        onBackspacePressed(settings, snippets)
    }

    fun onGlideCompleted(
        points: List<Offset>,
        keyCenters: Map<Char, Offset>,
        settings: KeyboardSettings,
        snippets: List<SnippetEntity>
    ) {
        if (_uiState.value.isPasswordField) return
        composingBuffer = ""
        composingTouchSamples.clear()
        target.finishComposingText()

        val before = target.getTextBeforeCursor(80)
        val (_, prevWord, prevPrevWord) = extractTokens(before)
        val result = repository.engine.decodeGlidePath(points, keyCenters, prevWord, prevPrevWord)
        val top = result.suggestions.firstOrNull()
        if (top != null) {
            val needsLeadingSpace = before.isNotEmpty() && !before.last().isWhitespace() && before.last() != '(' && before.last() != '"'
            val cased = applyShiftToWord(top.text)
            val prefix = if (needsLeadingSpace) " " else ""
            val committed = "$prefix$cased "
            target.commitText(committed)
            lastGlideCommittedLength = cased.length + 1
            repository.recordCommittedWordAsync(cased, prevWord, prevPrevWord)
            if (_uiState.value.shiftState == ShiftState.ONCE) {
                _uiState.update { it.copy(shiftState = ShiftState.OFF) }
            }
            _uiState.update {
                it.copy(
                    suggestions = result.suggestions,
                    telemetry = result.telemetry
                )
            }
        } else {
            _uiState.update { it.copy(telemetry = result.telemetry) }
        }
    }

    fun onShiftTapped() {
        val now = System.currentTimeMillis()
        _uiState.update { state ->
            val next = when {
                state.shiftState == ShiftState.CAPS_LOCK -> ShiftState.OFF
                state.shiftState == ShiftState.ONCE && now - lastShiftTapMs < 400L -> ShiftState.CAPS_LOCK
                state.shiftState == ShiftState.ONCE -> ShiftState.OFF
                else -> ShiftState.ONCE
            }
            lastShiftTapMs = now
            state.copy(shiftState = next)
        }
    }

    fun onEnterPressed(settings: KeyboardSettings, snippets: List<SnippetEntity>) {
        undoAutoCorrectState = null
        lastGlideCommittedLength = 0
        if (!_uiState.value.isPasswordField) {
            val before = target.getTextBeforeCursor(80)
            val (currentToken, prevWord, prevPrevWord) = extractTokens(before)
            if (currentToken.isNotEmpty() && !currentToken.startsWith(";")) {
                repository.recordCommittedWordAsync(currentToken, prevWord, prevPrevWord)
            }
        }
        composingBuffer = ""
        composingTouchSamples.clear()
        target.finishComposingText()
        target.performEnterAction(_uiState.value.enterAction)
        refreshContext(settings, snippets)
    }

    fun onEmojiSelected(emoji: String, settings: KeyboardSettings, snippets: List<SnippetEntity>) {
        undoAutoCorrectState = null
        composingBuffer = ""
        composingTouchSamples.clear()
        target.finishComposingText()
        target.commitText(emoji)
        refreshContext(settings, snippets)
    }

    fun onClipboardPaste(content: String, settings: KeyboardSettings, snippets: List<SnippetEntity>) {
        undoAutoCorrectState = null
        composingBuffer = ""
        composingTouchSamples.clear()
        target.finishComposingText()
        target.commitText(content)
        refreshContext(settings, snippets)
    }

    fun onCursorScrub(steps: Int) {
        if (steps != 0) {
            undoAutoCorrectState = null
            composingBuffer = ""
            composingTouchSamples.clear()
            target.finishComposingText()
            target.moveCursor(steps)
        }
    }

    fun setLayoutMode(mode: KeyboardLayoutMode) {
        _uiState.update { it.copy(layoutMode = mode) }
    }

    fun setActivePopup(popup: ActiveKeyPopup?) {
        _uiState.update { it.copy(activePopup = popup) }
    }

    fun switchToNextIme() = target.switchToNextInputMethod()

    private fun applyShiftToWord(word: String): String {
        return when (_uiState.value.shiftState) {
            ShiftState.CAPS_LOCK -> word.uppercase(Locale.getDefault())
            ShiftState.ONCE -> word.replaceFirstChar {
                if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
            }
            ShiftState.OFF -> word
        }
    }

    private fun shouldCapitalizeAfter(before: String): Boolean {
        if (before.isEmpty()) return true
        if (before.endsWith("\n")) return true
        val trimmed = before.trimEnd()
        if (trimmed.isEmpty()) return true
        val lastChar = trimmed.last()
        return (lastChar == '.' || lastChar == '!' || lastChar == '?') && before.last().isWhitespace()
    }

    private fun extractTokens(before: String): Triple<String, String?, String?> {
        if (before.isEmpty()) return Triple("", null, null)
        var i = before.length - 1
        while (i >= 0 && !before[i].isWhitespace()) {
            i--
        }
        val currentToken = before.substring(i + 1)
        val preceding = before.substring(0, (i + 1).coerceAtLeast(0)).trimEnd()
        if (preceding.isEmpty()) return Triple(currentToken, null, null)

        val words = preceding
            .split(Regex("\\s+"))
            .map { it.trim { ch -> !ch.isLetterOrDigit() && ch != '\'' } }
            .filter { it.isNotEmpty() }

        val prevWord = words.lastOrNull()
        val prevPrevWord = if (words.size >= 2) words[words.size - 2] else null
        return Triple(currentToken, prevWord, prevPrevWord)
    }
}
