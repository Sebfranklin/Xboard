package com.example.ui.keyboard

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ClipboardItemEntity
import com.example.engine.BuiltInLexicon
import com.example.engine.EmojiCategory
import com.example.ui.theme.KeyboardPalette
import com.example.ui.theme.PlusJakartaFamily
import com.example.ui.theme.SpaceGroteskFamily

@Composable
fun EmojiPickerPanel(
    palette: KeyboardPalette,
    panelHeight: Dp,
    onEmojiTap: (String) -> Unit,
    onBackspace: () -> Unit,
    onBackToLetters: () -> Unit
) {
    var selectedCategory by remember { mutableStateOf(EmojiCategory.SMILEYS) }
    var activeFilterTag by remember { mutableStateOf<String?>(null) }

    val quickTags = listOf("smile", "love", "fire", "check", "work", "coffee", "party")

    val filteredEmojis = remember(selectedCategory, activeFilterTag) {
        if (activeFilterTag != null) {
            BuiltInLexicon.emojiCatalog.filter { entry ->
                entry.keywords.any { it.contains(activeFilterTag!!, ignoreCase = true) }
            }
        } else {
            BuiltInLexicon.emojiCatalog.filter { it.category == selectedCategory }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(panelHeight)
            .background(palette.trayBackground)
            .padding(horizontal = 6.dp, vertical = 4.dp)
    ) {
        // Offline keyword tag filter row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            quickTags.forEach { tag ->
                val selected = activeFilterTag == tag
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (selected) palette.accentFace else palette.modifierFace)
                        .clickable { activeFilterTag = if (selected) null else tag }
                        .padding(horizontal = 9.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "#$tag",
                        color = if (selected) palette.onAccentText else palette.hintText,
                        fontSize = 11.sp,
                        fontFamily = PlusJakartaFamily,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        LazyVerticalGrid(
            columns = GridCells.Fixed(7),
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(2.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(filteredEmojis, key = { it.glyph }) { entry ->
                Box(
                    modifier = Modifier
                        .height(40.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(palette.keyFace.copy(alpha = 0.45f))
                        .clickable { onEmojiTap(entry.glyph) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = entry.glyph,
                        fontSize = 22.sp
                    )
                }
            }
        }

        // Bottom category + ABC/Backspace bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            TactileActionKeycap(
                palette = palette,
                keyHeight = 38.dp,
                contentDescriptionText = "Back to letters",
                modifier = Modifier.width(56.dp),
                onClick = onBackToLetters
            ) {
                Text(
                    text = "ABC",
                    color = palette.keyText,
                    fontSize = 13.sp,
                    fontFamily = SpaceGroteskFamily,
                    fontWeight = FontWeight.Bold
                )
            }

            EmojiCategory.entries.forEach { category ->
                val isSelected = activeFilterTag == null && selectedCategory == category
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(38.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSelected) palette.accentFace.copy(alpha = 0.22f) else palette.modifierFace)
                        .then(
                            if (isSelected) Modifier.border(1.dp, palette.accentFace, RoundedCornerShape(8.dp))
                            else Modifier
                        )
                        .clickable {
                            activeFilterTag = null
                            selectedCategory = category
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = category.iconGlyph, fontSize = 17.sp)
                }
            }

            TactileActionKeycap(
                palette = palette,
                keyHeight = 38.dp,
                contentDescriptionText = "Backspace",
                modifier = Modifier.width(52.dp),
                onClick = onBackspace
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Backspace,
                    contentDescription = "Backspace",
                    tint = palette.keyText,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
fun ClipboardTrayPanel(
    items: List<ClipboardItemEntity>,
    palette: KeyboardPalette,
    panelHeight: Dp,
    onPaste: (String) -> Unit,
    onTogglePin: (ClipboardItemEntity) -> Unit,
    onDelete: (Int) -> Unit,
    onClearUnpinned: () -> Unit,
    onBackToLetters: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(panelHeight)
            .background(palette.trayBackground)
            .padding(horizontal = 8.dp, vertical = 6.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.ContentPaste,
                    contentDescription = null,
                    tint = palette.accentFace,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = "Local Clipboard Vault",
                    color = palette.keyText,
                    fontSize = 13.sp,
                    fontFamily = SpaceGroteskFamily,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(palette.modifierFace)
                        .clickable { onClearUnpinned() }
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.DeleteSweep,
                            contentDescription = "Clear unpinned",
                            tint = palette.hintText,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = "Clear",
                            color = palette.hintText,
                            fontSize = 11.sp,
                            fontFamily = PlusJakartaFamily
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(palette.accentFace)
                        .clickable { onBackToLetters() }
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Text(
                        text = "ABC",
                        color = palette.onAccentText,
                        fontSize = 11.sp,
                        fontFamily = SpaceGroteskFamily,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        if (items.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Copied text will appear here locally for 1-tap paste.",
                    color = palette.hintText,
                    fontSize = 13.sp,
                    fontFamily = PlusJakartaFamily
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(items, key = { it.id }) { clip ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(palette.keyFace)
                            .border(
                                width = 1.dp,
                                color = if (clip.isPinned) palette.accentFace.copy(alpha = 0.55f) else palette.dividerColor,
                                shape = RoundedCornerShape(10.dp)
                            )
                            .clickable { onPaste(clip.content) }
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = clip.content,
                            color = palette.keyText,
                            fontSize = 13.sp,
                            fontFamily = PlusJakartaFamily,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = { onTogglePin(clip) },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = if (clip.isPinned) Icons.Filled.PushPin else Icons.Outlined.PushPin,
                                contentDescription = if (clip.isPinned) "Unpin clip" else "Pin clip",
                                tint = if (clip.isPinned) palette.accentFace else palette.hintText,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        IconButton(
                            onClick = { onDelete(clip.id) },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Delete clip",
                                tint = palette.hintText,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
