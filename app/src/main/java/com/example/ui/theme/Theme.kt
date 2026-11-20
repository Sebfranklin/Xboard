package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

private val KeyCraftDarkColorScheme = darkColorScheme(
    primary = StudioTerracotta,
    onPrimary = PaperWhite,
    primaryContainer = StudioTerracottaDark,
    onPrimaryContainer = StudioTerracottaSoft,
    secondary = StudioCobaltLight,
    onSecondary = ObsidianBg,
    secondaryContainer = ObsidianSurfaceVariant,
    onSecondaryContainer = StudioCobaltSoft,
    tertiary = StudioEmerald,
    onTertiary = ObsidianBg,
    background = ObsidianBg,
    onBackground = PaperWhite,
    surface = ObsidianSurface,
    onSurface = PaperWhite,
    surfaceVariant = ObsidianSurfaceVariant,
    onSurfaceVariant = PaperMuted,
    outline = ObsidianOutline,
    outlineVariant = ObsidianSurfaceVariant
)

private val KeyCraftLightColorScheme = lightColorScheme(
    primary = StudioTerracotta,
    onPrimary = AlabasterSurface,
    primaryContainer = StudioTerracottaSoft,
    onPrimaryContainer = StudioTerracottaDark,
    secondary = StudioCobalt,
    onSecondary = AlabasterSurface,
    secondaryContainer = StudioCobaltSoft,
    onSecondaryContainer = StudioCobalt,
    tertiary = StudioEmerald,
    onTertiary = AlabasterSurface,
    background = AlabasterBg,
    onBackground = InkCharcoal,
    surface = AlabasterSurface,
    onSurface = InkCharcoal,
    surfaceVariant = AlabasterSurfaceVariant,
    onSurfaceVariant = InkMuted,
    outline = AlabasterOutline,
    outlineVariant = AlabasterSurfaceVariant
)

val KeyCraftShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(26.dp)
)

@Composable
fun KeyCraftTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> KeyCraftDarkColorScheme
        else -> KeyCraftLightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = KeyCraftTypography,
        shapes = KeyCraftShapes,
        content = content
    )
}
