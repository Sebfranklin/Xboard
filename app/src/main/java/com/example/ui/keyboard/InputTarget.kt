package com.example.ui.keyboard

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue

interface InputTarget {
    fun getTextBeforeCursor(maxChars: Int): String
    fun commitText(text: String)
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
    initialText: String = "Try typing teh, recieve, ;omw, or swipe a word!"
) : InputTarget {

    var value by mutableStateOf(
        TextFieldValue(
            text = initialText,
            selection = TextRange(initialText.length)
        )
    )
        private set

    fun updateFromExternal(newValue: TextFieldValue) {
        value = newValue
    }

    fun clearAll() {
        value = TextFieldValue(text = "", selection = TextRange(0))
    }

    override fun getTextBeforeCursor(maxChars: Int): String {
        val cursor = value.selection.start.coerceIn(0, value.text.length)
        val start = (cursor - maxChars).coerceAtLeast(0)
        return value.text.substring(start, cursor)
    }

    override fun commitText(text: String) {
        val current = value
        val start = minOf(current.selection.start, current.selection.end).coerceIn(0, current.text.length)
        val end = maxOf(current.selection.start, current.selection.end).coerceIn(0, current.text.length)
        val updated = buildString {
            append(current.text, 0, start)
            append(text)
            append(current.text, end, current.text.length)
        }
        val newCursor = start + text.length
        value = TextFieldValue(text = updated, selection = TextRange(newCursor))
    }

    override fun deleteBeforeCursor(count: Int) {
        if (count <= 0) return
        val current = value
        val start = minOf(current.selection.start, current.selection.end).coerceIn(0, current.text.length)
        val end = maxOf(current.selection.start, current.selection.end).coerceIn(0, current.text.length)
        if (start != end) {
            val updated = current.text.removeRange(start, end)
            value = TextFieldValue(text = updated, selection = TextRange(start))
            return
        }
        if (start == 0) return
        val deleteFrom = (start - count).coerceAtLeast(0)
        val updated = current.text.removeRange(deleteFrom, start)
        value = TextFieldValue(text = updated, selection = TextRange(deleteFrom))
    }

    override fun moveCursor(delta: Int) {
        val current = value
        val next = (current.selection.start + delta).coerceIn(0, current.text.length)
        value = current.copy(selection = TextRange(next))
    }

    override fun performEnterAction(action: EnterActionType) {
        commitText("\n")
    }
}
