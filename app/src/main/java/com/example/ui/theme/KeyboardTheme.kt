package com.example.ui.theme

import androidx.compose.ui.graphics.Color

enum class KeyboardThemeStyle(val displayName: String, val subtitle: String) {
    TACTILE_DARK("Tactile Obsidian", "Crisp dark slate with terracotta switch"),
    ALABASTER_LIGHT("Alabaster Desk", "Warm ivory keycaps with cobalt accent"),
    NORDIC_SLATE("Nordic Steel", "Cool midnight blue with crisp contrast"),
    AMBER_STUDIO("Espresso Amber", "Warm charcoal wood & copper glow")
}

data class KeyboardPalette(
    val trayBackground: Color,
    val stripBackground: Color,
    val keyFace: Color,
    val keyPressedFace: Color,
    val keyBevelShadow: Color,
    val modifierFace: Color,
    val modifierPressedFace: Color,
    val accentFace: Color,
    val accentPressedFace: Color,
    val onAccentText: Color,
    val keyText: Color,
    val hintText: Color,
    val suggestionChipBg: Color,
    val suggestionAutoBg: Color,
    val suggestionText: Color,
    val glideTrailColor: Color,
    val popupBg: Color,
    val popupText: Color,
    val dividerColor: Color
)

fun KeyboardThemeStyle.toPalette(): KeyboardPalette = when (this) {
    KeyboardThemeStyle.TACTILE_DARK -> KeyboardPalette(
        trayBackground = TactileDarkTray,
        stripBackground = ObsidianSurface,
        keyFace = TactileDarkKey,
        keyPressedFace = ObsidianSurfaceVariant,
        keyBevelShadow = TactileDarkKeyShadow,
        modifierFace = TactileDarkModKey,
        modifierPressedFace = TactileDarkKey,
        accentFace = StudioTerracotta,
        accentPressedFace = StudioTerracottaDark,
        onAccentText = PaperWhite,
        keyText = TactileDarkText,
        hintText = TactileDarkHint,
        suggestionChipBg = TactileDarkModKey,
        suggestionAutoBg = StudioTerracotta.copy(alpha = 0.22f),
        suggestionText = TactileDarkText,
        glideTrailColor = StudioTerracotta,
        popupBg = ObsidianSurfaceVariant,
        popupText = PaperWhite,
        dividerColor = ObsidianOutline
    )

    KeyboardThemeStyle.ALABASTER_LIGHT -> KeyboardPalette(
        trayBackground = AlabasterTray,
        stripBackground = AlabasterSurfaceVariant,
        keyFace = AlabasterKey,
        keyPressedFace = AlabasterSurfaceVariant,
        keyBevelShadow = AlabasterKeyShadow,
        modifierFace = AlabasterModKey,
        modifierPressedFace = AlabasterOutline,
        accentFace = StudioCobalt,
        accentPressedFace = InkCharcoal,
        onAccentText = AlabasterSurface,
        keyText = AlabasterText,
        hintText = AlabasterHint,
        suggestionChipBg = AlabasterSurface,
        suggestionAutoBg = StudioCobaltSoft,
        suggestionText = AlabasterText,
        glideTrailColor = StudioCobalt,
        popupBg = AlabasterSurface,
        popupText = InkCharcoal,
        dividerColor = AlabasterOutline
    )

    KeyboardThemeStyle.NORDIC_SLATE -> KeyboardPalette(
        trayBackground = NordicTray,
        stripBackground = Color(0xFF161E2B),
        keyFace = NordicKey,
        keyPressedFace = Color(0xFF36465E),
        keyBevelShadow = NordicKeyShadow,
        modifierFace = NordicModKey,
        modifierPressedFace = NordicKey,
        accentFace = StudioCobalt,
        accentPressedFace = Color(0xFF1D3EB5),
        onAccentText = NordicText,
        keyText = NordicText,
        hintText = NordicHint,
        suggestionChipBg = NordicModKey,
        suggestionAutoBg = StudioCobalt.copy(alpha = 0.28f),
        suggestionText = NordicText,
        glideTrailColor = StudioCobaltLight,
        popupBg = Color(0xFF314056),
        popupText = NordicText,
        dividerColor = Color(0xFF334157)
    )

    KeyboardThemeStyle.AMBER_STUDIO -> KeyboardPalette(
        trayBackground = AmberTray,
        stripBackground = Color(0xFF1D1815),
        keyFace = AmberKey,
        keyPressedFace = Color(0xFF473B35),
        keyBevelShadow = AmberKeyShadow,
        modifierFace = AmberModKey,
        modifierPressedFace = AmberKey,
        accentFace = StudioAmber,
        accentPressedFace = StudioTerracottaDark,
        onAccentText = PaperWhite,
        keyText = AmberText,
        hintText = AmberHint,
        suggestionChipBg = AmberModKey,
        suggestionAutoBg = StudioAmber.copy(alpha = 0.25f),
        suggestionText = AmberText,
        glideTrailColor = StudioAmber,
        popupBg = Color(0xFF453A34),
        popupText = AmberText,
        dividerColor = Color(0xFF4A3D36)
    )
}
