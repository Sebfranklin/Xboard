package com.example.ui.keyboard

import com.example.engine.EngineTelemetry
import com.example.engine.Suggestion

enum class KeyboardLayoutMode {
    QWERTY,
    SYMBOLS_1,
    SYMBOLS_2,
    EMOJI,
    CLIPBOARD
}

enum class ShiftState {
    OFF,
    ONCE,
    CAPS_LOCK
}

enum class EnterActionType {
    DONE,
    GO,
    SEARCH,
    SEND,
    NEXT,
    NEWLINE
}

data class ActiveKeyPopup(
    val baseChar: Char,
    val label: String,
    val centerX: Float,
    val topY: Float,
    val alternates: List<String> = emptyList(),
    val showAlternates: Boolean = false,
    val selectedAlternateIndex: Int = 0
)

data class KeyboardUiState(
    val layoutMode: KeyboardLayoutMode = KeyboardLayoutMode.QWERTY,
    val shiftState: ShiftState = ShiftState.ONCE,
    val suggestions: List<Suggestion> = emptyList(),
    val telemetry: EngineTelemetry = EngineTelemetry(),
    val lastAutoCorrectOriginal: String? = null,
    val activePopup: ActiveKeyPopup? = null,
    val enterAction: EnterActionType = EnterActionType.NEWLINE,
    val showGlobeKey: Boolean = false,
    val isPasswordField: Boolean = false,
    val disableAutoCorrectForField: Boolean = false
)
