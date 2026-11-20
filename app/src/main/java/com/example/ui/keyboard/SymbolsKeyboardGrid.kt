package com.example.ui.keyboard

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.automirrored.filled.KeyboardReturn
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.KeyboardSettings
import com.example.ui.theme.KeyboardPalette
import com.example.ui.theme.SpaceGroteskFamily

@Composable
fun SymbolsKeyboardGrid(
    isPageTwo: Boolean,
    palette: KeyboardPalette,
    keyHeight: Dp,
    settings: KeyboardSettings,
    enterAction: EnterActionType,
    onKey: (String) -> Unit,
    onBackspace: () -> Unit,
    onSwipeDeleteWord: () -> Unit,
    onSpace: () -> Unit,
    onCursorScrub: (Int) -> Unit,
    onEnter: () -> Unit,
    onToggleSymbolsPage: () -> Unit,
    onBackToLetters: () -> Unit
) {
    val row1 = if (!isPageTwo) {
        listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0")
    } else {
        listOf("~", "`", "|", "•", "√", "π", "÷", "×", "§", "△")
    }
    val row2 = if (!isPageTwo) {
        listOf("@", "#", "$", "_", "&", "-", "+", "(", ")", "/")
    } else {
        listOf("£", "€", "¥", "^", "°", "=", "{", "}", "\\", "%")
    }
    val row3 = if (!isPageTwo) {
        listOf("*", "\"", "'", ":", ";", "!", "?")
    } else {
        listOf("©", "®", "™", "✓", "[", "]", "<")
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 3.dp, vertical = 2.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            row1.forEach { sym ->
                TactileActionKeycap(
                    palette = palette,
                    keyHeight = keyHeight,
                    contentDescriptionText = "Symbol $sym",
                    modifier = Modifier.weight(1f),
                    hapticEnabled = settings.hapticFeedback,
                    onClick = { onKey(sym) }
                ) {
                    Text(
                        text = sym,
                        color = palette.keyText,
                        fontSize = 18.sp,
                        fontFamily = SpaceGroteskFamily
                    )
                }
            }
        }

        Row(modifier = Modifier.fillMaxWidth()) {
            row2.forEach { sym ->
                TactileActionKeycap(
                    palette = palette,
                    keyHeight = keyHeight,
                    contentDescriptionText = "Symbol $sym",
                    modifier = Modifier.weight(1f),
                    hapticEnabled = settings.hapticFeedback,
                    onClick = { onKey(sym) }
                ) {
                    Text(
                        text = sym,
                        color = palette.keyText,
                        fontSize = 18.sp,
                        fontFamily = SpaceGroteskFamily
                    )
                }
            }
        }

        Row(modifier = Modifier.fillMaxWidth()) {
            TactileActionKeycap(
                palette = palette,
                keyHeight = keyHeight,
                contentDescriptionText = "More symbols",
                modifier = Modifier.weight(1.45f),
                hapticEnabled = settings.hapticFeedback,
                onClick = onToggleSymbolsPage
            ) {
                Text(
                    text = if (isPageTwo) "?123" else "=\\<",
                    color = palette.keyText,
                    fontSize = 13.sp,
                    fontFamily = SpaceGroteskFamily,
                    fontWeight = FontWeight.SemiBold
                )
            }

            row3.forEach { sym ->
                TactileActionKeycap(
                    palette = palette,
                    keyHeight = keyHeight,
                    contentDescriptionText = "Symbol $sym",
                    modifier = Modifier.weight(1f),
                    hapticEnabled = settings.hapticFeedback,
                    onClick = { onKey(sym) }
                ) {
                    Text(
                        text = sym,
                        color = palette.keyText,
                        fontSize = 18.sp,
                        fontFamily = SpaceGroteskFamily
                    )
                }
            }

            BackspaceKeycap(
                icon = Icons.AutoMirrored.Filled.Backspace,
                palette = palette,
                keyHeight = keyHeight,
                hapticEnabled = settings.hapticFeedback,
                modifier = Modifier.weight(1.45f),
                onDeleteChar = onBackspace,
                onSwipeDeleteWord = onSwipeDeleteWord
            )
        }

        Row(modifier = Modifier.fillMaxWidth()) {
            TactileActionKeycap(
                palette = palette,
                keyHeight = keyHeight,
                contentDescriptionText = "Back to letters",
                modifier = Modifier.weight(1.5f),
                hapticEnabled = settings.hapticFeedback,
                onClick = onBackToLetters
            ) {
                Text(
                    text = "ABC",
                    color = palette.keyText,
                    fontSize = 14.sp,
                    fontFamily = SpaceGroteskFamily,
                    fontWeight = FontWeight.SemiBold
                )
            }

            TactileActionKeycap(
                palette = palette,
                keyHeight = keyHeight,
                contentDescriptionText = "Semicolon macro prefix",
                modifier = Modifier.weight(1f),
                hapticEnabled = settings.hapticFeedback,
                onClick = { onKey(";") }
            ) {
                Text(
                    text = ";",
                    color = palette.accentFace,
                    fontSize = 19.sp,
                    fontFamily = SpaceGroteskFamily,
                    fontWeight = FontWeight.Bold
                )
            }

            SpacebarKeycap(
                palette = palette,
                keyHeight = keyHeight,
                hapticEnabled = settings.hapticFeedback,
                label = "Space",
                modifier = Modifier.weight(4.5f),
                onSpaceTap = onSpace,
                onCursorScrub = onCursorScrub
            )

            TactileActionKeycap(
                palette = palette,
                keyHeight = keyHeight,
                contentDescriptionText = "Period",
                modifier = Modifier.weight(1f),
                hapticEnabled = settings.hapticFeedback,
                onClick = { onKey(".") }
            ) {
                Text(
                    text = ".",
                    color = palette.keyText,
                    fontSize = 18.sp,
                    fontFamily = SpaceGroteskFamily
                )
            }

            TactileActionKeycap(
                palette = palette,
                keyHeight = keyHeight,
                contentDescriptionText = "Enter",
                isAccent = true,
                hapticEnabled = settings.hapticFeedback,
                modifier = Modifier.weight(1.6f),
                onClick = onEnter
            ) {
                val enterIcon = when (enterAction) {
                    EnterActionType.SEARCH -> Icons.Default.Search
                    EnterActionType.SEND -> Icons.AutoMirrored.Filled.Send
                    EnterActionType.DONE -> Icons.Default.Check
                    else -> Icons.AutoMirrored.Filled.KeyboardReturn
                }
                Icon(
                    imageVector = enterIcon,
                    contentDescription = "Enter",
                    tint = palette.onAccentText,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
