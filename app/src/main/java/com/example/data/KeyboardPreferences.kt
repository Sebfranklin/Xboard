package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.ui.theme.KeyboardThemeStyle
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.keyboardDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "keycraft_settings"
)

enum class AutoCorrectLevel(val label: String, val description: String) {
    OFF("Off", "Suggestions only, never auto-replace on space"),
    BALANCED("Balanced", "Fixes clear typos & contractions automatically"),
    ASSERTIVE("Assertive", "Wider spatial radius for fast thumb typing")
}

enum class OneHandedMode(val label: String) {
    NONE("Full"),
    LEFT("Left"),
    RIGHT("Right")
}

data class KeyboardSettings(
    val themeStyle: KeyboardThemeStyle = KeyboardThemeStyle.TACTILE_DARK,
    val showNumberRow: Boolean = false,
    val hapticFeedback: Boolean = true,
    val keyPopups: Boolean = true,
    val autoCapitalize: Boolean = true,
    val doubleSpacePeriod: Boolean = true,
    val autoCorrectLevel: AutoCorrectLevel = AutoCorrectLevel.BALANCED,
    val glideTypingEnabled: Boolean = true,
    val oneHandedMode: OneHandedMode = OneHandedMode.NONE,
    val keyboardScale: Float = 1.0f
)

class KeyboardPreferences(private val context: Context) {

    private object Keys {
        val THEME = stringPreferencesKey("theme_style")
        val NUMBER_ROW = booleanPreferencesKey("show_number_row")
        val HAPTIC = booleanPreferencesKey("haptic_feedback")
        val POPUPS = booleanPreferencesKey("key_popups")
        val AUTO_CAP = booleanPreferencesKey("auto_capitalize")
        val DOUBLE_SPACE = booleanPreferencesKey("double_space_period")
        val AUTOCORRECT = stringPreferencesKey("autocorrect_level")
        val GLIDE = booleanPreferencesKey("glide_typing")
        val ONE_HANDED = stringPreferencesKey("one_handed_mode")
        val SCALE = floatPreferencesKey("keyboard_scale")
    }

    val settingsFlow: Flow<KeyboardSettings> = context.keyboardDataStore.data.map { prefs ->
        KeyboardSettings(
            themeStyle = prefs[Keys.THEME]?.let { runCatching { KeyboardThemeStyle.valueOf(it) }.getOrNull() }
                ?: KeyboardThemeStyle.TACTILE_DARK,
            showNumberRow = prefs[Keys.NUMBER_ROW] ?: false,
            hapticFeedback = prefs[Keys.HAPTIC] ?: true,
            keyPopups = prefs[Keys.POPUPS] ?: true,
            autoCapitalize = prefs[Keys.AUTO_CAP] ?: true,
            doubleSpacePeriod = prefs[Keys.DOUBLE_SPACE] ?: true,
            autoCorrectLevel = prefs[Keys.AUTOCORRECT]?.let { runCatching { AutoCorrectLevel.valueOf(it) }.getOrNull() }
                ?: AutoCorrectLevel.BALANCED,
            glideTypingEnabled = prefs[Keys.GLIDE] ?: true,
            oneHandedMode = prefs[Keys.ONE_HANDED]?.let { runCatching { OneHandedMode.valueOf(it) }.getOrNull() }
                ?: OneHandedMode.NONE,
            keyboardScale = (prefs[Keys.SCALE] ?: 1.0f).coerceIn(0.88f, 1.15f)
        )
    }

    suspend fun setTheme(style: KeyboardThemeStyle) {
        context.keyboardDataStore.edit { it[Keys.THEME] = style.name }
    }

    suspend fun setShowNumberRow(enabled: Boolean) {
        context.keyboardDataStore.edit { it[Keys.NUMBER_ROW] = enabled }
    }

    suspend fun setHapticFeedback(enabled: Boolean) {
        context.keyboardDataStore.edit { it[Keys.HAPTIC] = enabled }
    }

    suspend fun setKeyPopups(enabled: Boolean) {
        context.keyboardDataStore.edit { it[Keys.POPUPS] = enabled }
    }

    suspend fun setAutoCapitalize(enabled: Boolean) {
        context.keyboardDataStore.edit { it[Keys.AUTO_CAP] = enabled }
    }

    suspend fun setDoubleSpacePeriod(enabled: Boolean) {
        context.keyboardDataStore.edit { it[Keys.DOUBLE_SPACE] = enabled }
    }

    suspend fun setAutoCorrectLevel(level: AutoCorrectLevel) {
        context.keyboardDataStore.edit { it[Keys.AUTOCORRECT] = level.name }
    }

    suspend fun setGlideTypingEnabled(enabled: Boolean) {
        context.keyboardDataStore.edit { it[Keys.GLIDE] = enabled }
    }

    suspend fun setOneHandedMode(mode: OneHandedMode) {
        context.keyboardDataStore.edit { it[Keys.ONE_HANDED] = mode.name }
    }

    suspend fun setKeyboardScale(scale: Float) {
        context.keyboardDataStore.edit { it[Keys.SCALE] = scale.coerceIn(0.88f, 1.15f) }
    }
}
