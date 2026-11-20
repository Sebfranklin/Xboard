package com.example.ui.keyboard

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue

interface InputTarget {
    fun getTextBeforeCursor(maxChars: Int): String
    fun commitText(text: String)
    fun setComposingText(text: String) {
        commitText(text)
    }
    fun finishComposingText() {}
    fun deleteBeforeCursor(count: Int)
    fun moveCursor(delta: Int)
    fun performEnterAction(action: EnterActionType)
    fun switchToNextInputMethod() {}
    fun openAppSettings() {}
}

/**
 * In-app interactive sandbox target backing a Compose TextFieldValue so users can
 * test the exact non-AI KeyCraft keyboard directly inside the Companion Studio.
 */
class SandboxInputTarget(
    initialText: String = "Tap or glide across keys. Try typing teh, recieve, or ;omw "
) : InputTarget {

    var value by mutableStateOf(
        TextFieldValue(
            text = initialText,
            selection = TextRange(initialText.length)
        )
    )
        private set

    var composingRange by mutableStateOf<TextRange?>(null)
        private set

    fun updateFromExternal(newValue: TextFieldValue) {
        composingRange = null
        value = newValue
    }

    fun clearAll() {
        composingRange = null
        value = TextFieldValue(text = "", selection = TextRange(0))
    }

    override fun getTextBeforeCursor(maxChars: Int): String {
        val cursor = value.selection.start.coerceIn(0, value.text.length)
        val start = (cursor - maxChars).coerceAtLeast(0)
        return value.text.substring(start, cursor)
    }

    override fun commitText(text: String) {
        val current = value
        val activeComp = composingRange
        val replaceStart: Int
        val replaceEnd: Int
        if (activeComp != null && activeComp.start >= 0 && activeComp.end <= current.text.length) {
            replaceStart = minOf(activeComp.start, activeComp.end)
            replaceEnd = maxOf(activeComp.start, activeComp.end)
        } else {
            replaceStart = minOf(current.selection.start, current.selection.end).coerceIn(0, current.text.length)
            replaceEnd = maxOf(current.selection.start, current.selection.end).coerceIn(0, current.text.length)
        }
        val updated = buildString {
            append(current.text, 0, replaceStart)
            append(text)
            append(current.text, replaceEnd, current.text.length)
        }
        val newCursor = replaceStart + text.length
        composingRange = null
        value = TextFieldValue(text = updated, selection = TextRange(newCursor))
    }

    override fun setComposingText(text: String) {
        val current = value
        val activeComp = composingRange
        val replaceStart: Int
        val replaceEnd: Int
        if (activeComp != null && activeComp.start >= 0 && activeComp.end <= current.text.length) {
            replaceStart = minOf(activeComp.start, activeComp.end)
            replaceEnd = maxOf(activeComp.start, activeComp.end)
        } else {
            replaceStart = minOf(current.selection.start, current.selection.end).coerceIn(0, current.text.length)
            replaceEnd = maxOf(current.selection.start, current.selection.end).coerceIn(0, current.text.length)
        }
        val updated = buildString {
            append(current.text, 0, replaceStart)
            append(text)
            append(current.text, replaceEnd, current.text.length)
        }
        val newCursor = replaceStart + text.length
        composingRange = if (text.isNotEmpty()) TextRange(replaceStart, newCursor) else null
        value = TextFieldValue(text = updated, selection = TextRange(newCursor))
    }

    override fun finishComposingText() {
        composingRange = null
    }

    override fun deleteBeforeCursor(count: Int) {
        if (count <= 0) return
        val current = value
        val start = minOf(current.selection.start, current.selection.end).coerceIn(0, current.text.length)
        val end = maxOf(current.selection.start, current.selection.end).coerceIn(0, current.text.length)
        if (start != end) {
            val updated = current.text.removeRange(start, end)
            composingRange = null
            value = TextFieldValue(text = updated, selection = TextRange(start))
            return
        }
        if (start == 0) return
        val deleteFrom = (start - count).coerceAtLeast(0)
        val updated = current.text.removeRange(deleteFrom, start)
        composingRange = null
        value = TextFieldValue(text = updated, selection = TextRange(deleteFrom))
    }

    override fun moveCursor(delta: Int) {
        composingRange = null
        val current = value
        val next = (current.selection.start + delta).coerceIn(0, current.text.length)
        value = current.copy(selection = TextRange(next))
    }

    override fun performEnterAction(action: EnterActionType) {
        commitText("\n")
    }
}
