package com.example.ui.studio

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.AutoCorrectLevel
import com.example.data.KeyboardSettings
import com.example.data.OneHandedMode
import com.example.ui.theme.KeyboardThemeStyle
import com.example.ui.theme.toPalette
import kotlin.math.roundToInt

@Composable
fun TuningScreen(
    settings: KeyboardSettings,
    viewModel: StudioViewModel,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 680.dp),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Keycap Themes & Tuning",
                        style = MaterialTheme.typography.headlineMedium
                    )
                    Text(
                        text = "Customize physical keycap aesthetics, ergonomics, and deterministic engine heuristics.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Theme Picker
            item {
                SectionCard(title = "Tactile Keycap Palette") {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        KeyboardThemeStyle.entries.forEach { style ->
                            val palette = style.toPalette()
                            val selected = settings.themeStyle == style
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(MaterialTheme.shapes.medium)
                                    .background(palette.trayBackground)
                                    .border(
                                        width = if (selected) 2.dp else 1.dp,
                                        color = if (selected) palette.accentFace else palette.dividerColor,
                                        shape = MaterialTheme.shapes.medium
                                    )
                                    .clickable { viewModel.setTheme(style) }
                                    .padding(12.dp)
                                    .testTag("theme_${style.name.lowercase()}"),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Mini keycap swatch preview
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    listOf("K", "C").forEach { letter ->
                                        Box(
                                            modifier = Modifier
                                                .size(28.dp)
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(palette.keyFace),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = letter,
                                                color = palette.keyText,
                                                style = MaterialTheme.typography.labelLarge
                                            )
                                        }
                                    }
                                    Box(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(palette.accentFace),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "↵",
                                            color = palette.onAccentText,
                                            style = MaterialTheme.typography.labelLarge
                                        )
                                    }
                                }

                                Spacer(Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = style.displayName,
                                        style = MaterialTheme.typography.titleSmall,
                                        color = palette.keyText
                                    )
                                    Text(
                                        text = style.subtitle,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = palette.hintText
                                    )
                                }

                                if (selected) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = "Selected theme",
                                        tint = palette.accentFace
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Autocorrect Strength
            item {
                SectionCard(title = "Spatial Autocorrect Strength") {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            AutoCorrectLevel.entries.forEach { level ->
                                val selected = settings.autoCorrectLevel == level
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(
                                            if (selected) MaterialTheme.colorScheme.primary
                                            else MaterialTheme.colorScheme.surfaceVariant
                                        )
                                        .clickable { viewModel.setAutoCorrectLevel(level) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = level.label,
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (selected) MaterialTheme.colorScheme.onPrimary
                                        else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                        Text(
                            text = settings.autoCorrectLevel.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Ergonomics: One-handed & Scale
            item {
                SectionCard(title = "Ergonomics & Size") {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OneHandedMode.entries.forEach { mode ->
                                val selected = settings.oneHandedMode == mode
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(
                                            if (selected) MaterialTheme.colorScheme.secondary
                                            else MaterialTheme.colorScheme.surfaceVariant
                                        )
                                        .clickable { viewModel.setOneHandedMode(mode) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "${mode.label} Hand",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = if (selected) MaterialTheme.colorScheme.onSecondary
                                        else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Keycap Height Scale", style = MaterialTheme.typography.titleSmall)
                                Text(
                                    text = "${(settings.keyboardScale * 100).roundToInt()}%",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Slider(
                                value = settings.keyboardScale,
                                onValueChange = { viewModel.setKeyboardScale(it) },
                                valueRange = 0.88f..1.15f
                            )
                        }
                    }
                }
            }

            // Deterministic Engine & Input Toggles
            item {
                SectionCard(title = "Input & Deterministic Rules") {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        ToggleRow(
                            title = "Glide Swipe Typing",
                            subtitle = "Geometric touch-path matching without neural models",
                            checked = settings.glideTypingEnabled,
                            onCheckedChange = { viewModel.setGlideTypingEnabled(it) }
                        )
                        ToggleRow(
                            title = "Dedicated Number Row",
                            subtitle = "Always show 1–0 row above QWERTY keys",
                            checked = settings.showNumberRow,
                            onCheckedChange = { viewModel.setShowNumberRow(it) }
                        )
                        ToggleRow(
                            title = "Key Preview Popups",
                            subtitle = "Show floating magnified keycap & long-press accents",
                            checked = settings.keyPopups,
                            onCheckedChange = { viewModel.setKeyPopups(it) }
                        )
                        ToggleRow(
                            title = "Tactile Haptic Feedback",
                            subtitle = "Subtle vibration on keypress and cursor scrubbing",
                            checked = settings.hapticFeedback,
                            onCheckedChange = { viewModel.setHapticFeedback(it) }
                        )
                        ToggleRow(
                            title = "Auto-Capitalization",
                            subtitle = "Capitalize first letter of sentences automatically",
                            checked = settings.autoCapitalize,
                            onCheckedChange = { viewModel.setAutoCapitalize(it) }
                        )
                        ToggleRow(
                            title = "Double-Space Period",
                            subtitle = "Tap space twice quickly to insert \". \"",
                            checked = settings.doubleSpacePeriod,
                            onCheckedChange = { viewModel.setDoubleSpacePeriod(it) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionCard(
    title: String,
    content: @Composable () -> Unit
) {
    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, MaterialTheme.shapes.large)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            content()
        }
    }
}

@Composable
private fun ToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.titleSmall)
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.width(12.dp))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
