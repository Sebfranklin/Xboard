package com.example.ui.keyboard

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.KeyboardReturn
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.EmojiEmotions
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.KeyboardCapslock
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ZoomOutMap
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ClipboardItemEntity
import com.example.data.KeyboardSettings
import com.example.data.OneHandedMode
import com.example.data.SnippetEntity
import com.example.engine.SpatialLayoutModel
import com.example.engine.Suggestion
import com.example.engine.SuggestionReason
import com.example.engine.TouchSample
import com.example.ui.theme.KeyboardPalette
import com.example.ui.theme.PlusJakartaFamily
import com.example.ui.theme.SpaceGroteskFamily
import com.example.ui.theme.StudioEmerald
import com.example.ui.theme.toPalette
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.hypot

@Composable
fun KeyCraftKeyboardView(
    controller: KeyboardController,
    settings: KeyboardSettings,
    snippets: List<SnippetEntity>,
    clipboardItems: List<ClipboardItemEntity>,
    modifier: Modifier = Modifier,
    onToggleOneHanded: (OneHandedMode) -> Unit = {},
    onToggleClipboardPin: (ClipboardItemEntity) -> Unit = {},
    onDeleteClipboard: (Int) -> Unit = {},
    onClearUnpinnedClipboard: () -> Unit = {},
    onOpenSettings: (() -> Unit)? = null
) {
    val uiState by controller.uiState.collectAsState()
    val palette = settings.themeStyle.toPalette()
    val keyHeight = (46f * settings.keyboardScale).dp
    val numberRowHeight = (36f * settings.keyboardScale).dp

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(palette.trayBackground)
            .padding(bottom = 4.dp)
            .testTag("keycraft_keyboard_root")
    ) {
        SuggestionAndToolbarStrip(
            uiState = uiState,
            palette = palette,
            oneHandedMode = settings.oneHandedMode,
            onSuggestionClick = { controller.onSuggestionTapped(it, settings, snippets) },
            onUndoAutoCorrect = { controller.onUndoAutoCorrectTapped(settings, snippets) },
            onToggleEmoji = {
                val next = if (uiState.layoutMode == KeyboardLayoutMode.EMOJI) KeyboardLayoutMode.QWERTY else KeyboardLayoutMode.EMOJI
                controller.setLayoutMode(next)
            },
            onToggleClipboard = {
                val next = if (uiState.layoutMode == KeyboardLayoutMode.CLIPBOARD) KeyboardLayoutMode.QWERTY else KeyboardLayoutMode.CLIPBOARD
                controller.setLayoutMode(next)
            },
            onCycleOneHanded = {
                val next = when (settings.oneHandedMode) {
                    OneHandedMode.NONE -> OneHandedMode.RIGHT
                    OneHandedMode.RIGHT -> OneHandedMode.LEFT
                    OneHandedMode.LEFT -> OneHandedMode.NONE
                }
                onToggleOneHanded(next)
            },
            onOpenSettings = onOpenSettings
        )

        val panelBodyHeight = (keyHeight * 4) + if (settings.showNumberRow) numberRowHeight else 0.dp

        Row(modifier = Modifier.fillMaxWidth()) {
            if (settings.oneHandedMode == OneHandedMode.RIGHT) {
                OneHandedGutter(
                    palette = palette,
                    isLeftGutter = true,
                    height = panelBodyHeight,
                    onExpand = { onToggleOneHanded(OneHandedMode.NONE) },
                    onSwapHand = { onToggleOneHanded(OneHandedMode.LEFT) }
                )
            }

            Box(modifier = Modifier.weight(1f)) {
                when (uiState.layoutMode) {
                    KeyboardLayoutMode.EMOJI -> EmojiPickerPanel(
                        palette = palette,
                        panelHeight = panelBodyHeight,
                        onEmojiTap = { controller.onEmojiSelected(it, settings, snippets) },
                        onBackspace = { controller.onBackspacePressed(settings, snippets) },
                        onBackToLetters = { controller.setLayoutMode(KeyboardLayoutMode.QWERTY) }
                    )

                    KeyboardLayoutMode.CLIPBOARD -> ClipboardTrayPanel(
                        items = clipboardItems,
                        palette = palette,
                        panelHeight = panelBodyHeight,
                        onPaste = { controller.onClipboardPaste(it, settings, snippets) },
                        onTogglePin = onToggleClipboardPin,
                        onDelete = onDeleteClipboard,
                        onClearUnpinned = onClearUnpinnedClipboard,
                        onBackToLetters = { controller.setLayoutMode(KeyboardLayoutMode.QWERTY) }
                    )

                    KeyboardLayoutMode.SYMBOLS_1, KeyboardLayoutMode.SYMBOLS_2 -> SymbolsKeyboardGrid(
                        isPageTwo = uiState.layoutMode == KeyboardLayoutMode.SYMBOLS_2,
                        palette = palette,
                        keyHeight = keyHeight,
                        settings = settings,
                        enterAction = uiState.enterAction,
                        onKey = { controller.onCharacterPressed(it, settings, snippets) },
                        onBackspace = { controller.onBackspacePressed(settings, snippets) },
                        onSwipeDeleteWord = { controller.onDeleteWordSwipe(settings, snippets) },
                        onSpace = { controller.onSpacePressed(settings, snippets) },
                        onCursorScrub = { controller.onCursorScrub(it) },
                        onEnter = { controller.onEnterPressed(settings, snippets) },
                        onToggleSymbolsPage = {
                            val next = if (uiState.layoutMode == KeyboardLayoutMode.SYMBOLS_1) {
                                KeyboardLayoutMode.SYMBOLS_2
                            } else {
                                KeyboardLayoutMode.SYMBOLS_1
                            }
                            controller.setLayoutMode(next)
                        },
                        onBackToLetters = { controller.setLayoutMode(KeyboardLayoutMode.QWERTY) }
                    )

                    KeyboardLayoutMode.QWERTY -> QwertyKeyboardGrid(
                        palette = palette,
                        keyHeight = keyHeight,
                        numberRowHeight = numberRowHeight,
                        settings = settings,
                        uiState = uiState,
                        onLetterCommit = { ch, sample -> controller.onCharacterPressed(ch, settings, snippets, sample) },
                        onGlideComplete = { pts, centers ->
                            controller.onGlideCompleted(pts, centers, settings, snippets)
                        },
                        onPopupChange = { controller.setActivePopup(it) },
                        onShiftTap = { controller.onShiftTapped() },
                        onBackspace = { controller.onBackspacePressed(settings, snippets) },
                        onSwipeDeleteWord = { controller.onDeleteWordSwipe(settings, snippets) },
                        onSwitchSymbols = { controller.setLayoutMode(KeyboardLayoutMode.SYMBOLS_1) },
                        onSwitchGlobe = { controller.switchToNextIme() },
                        onPunctuation = { controller.onPunctuationPressed(it, settings, snippets) },
                        onSpace = { controller.onSpacePressed(settings, snippets) },
                        onCursorScrub = { controller.onCursorScrub(it) },
                        onEnter = { controller.onEnterPressed(settings, snippets) }
                    )
                }
            }

            if (settings.oneHandedMode == OneHandedMode.LEFT) {
                OneHandedGutter(
                    palette = palette,
                    isLeftGutter = false,
                    height = panelBodyHeight,
                    onExpand = { onToggleOneHanded(OneHandedMode.NONE) },
                    onSwapHand = { onToggleOneHanded(OneHandedMode.RIGHT) }
                )
            }
        }
    }
}

@Composable
private fun SuggestionAndToolbarStrip(
    uiState: KeyboardUiState,
    palette: KeyboardPalette,
    oneHandedMode: OneHandedMode,
    onSuggestionClick: (Suggestion) -> Unit,
    onUndoAutoCorrect: () -> Unit,
    onToggleEmoji: () -> Unit,
    onToggleClipboard: () -> Unit,
    onCycleOneHanded: () -> Unit,
    onOpenSettings: (() -> Unit)?
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
            .background(palette.stripBackground)
            .padding(horizontal = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left compact utility icons
        Row(
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            StripIconButton(
                active = uiState.layoutMode == KeyboardLayoutMode.EMOJI,
                palette = palette,
                contentDesc = "Emoji Picker",
                testTag = "kb_btn_emoji",
                onClick = onToggleEmoji
            ) {
                Icon(
                    imageVector = Icons.Default.EmojiEmotions,
                    contentDescription = "Emoji",
                    tint = if (uiState.layoutMode == KeyboardLayoutMode.EMOJI) palette.accentFace else palette.hintText,
                    modifier = Modifier.size(18.dp)
                )
            }
            StripIconButton(
                active = uiState.layoutMode == KeyboardLayoutMode.CLIPBOARD,
                palette = palette,
                contentDesc = "Clipboard Vault",
                testTag = "kb_btn_clipboard",
                onClick = onToggleClipboard
            ) {
                Icon(
                    imageVector = Icons.Default.ContentPaste,
                    contentDescription = "Clipboard",
                    tint = if (uiState.layoutMode == KeyboardLayoutMode.CLIPBOARD) palette.accentFace else palette.hintText,
                    modifier = Modifier.size(17.dp)
                )
            }
        }

        Box(
            modifier = Modifier
                .padding(horizontal = 4.dp)
                .width(1.dp)
                .height(22.dp)
                .background(palette.dividerColor)
        )

        // Undo autocorrect quick chip OR suggestion pills
        Row(
            modifier = Modifier
                .weight(1f)
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (uiState.lastAutoCorrectOriginal != null) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(palette.suggestionAutoBg)
                        .border(1.dp, palette.accentFace.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                        .clickable { onUndoAutoCorrect() }
                        .padding(horizontal = 8.dp, vertical = 5.dp)
                        .testTag("kb_undo_autocorrect"),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Undo,
                        contentDescription = "Undo autocorrect",
                        tint = palette.accentFace,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = uiState.lastAutoCorrectOriginal,
                        color = palette.suggestionText,
                        fontSize = 12.sp,
                        fontFamily = PlusJakartaFamily,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            uiState.suggestions.forEachIndexed { idx, suggestion ->
                val isPrimary = suggestion.isAutoCorrectTarget || idx == 0
                val chipBg = if (suggestion.isAutoCorrectTarget) palette.suggestionAutoBg else palette.suggestionChipBg
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(9.dp))
                        .background(chipBg)
                        .then(
                            if (suggestion.isAutoCorrectTarget) {
                                Modifier.border(1.dp, palette.accentFace.copy(alpha = 0.65f), RoundedCornerShape(9.dp))
                            } else Modifier
                        )
                        .clickable { onSuggestionClick(suggestion) }
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                        .testTag("kb_suggestion_$idx"),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val dotColor = when (suggestion.reason) {
                        SuggestionReason.SNIPPET -> StudioEmerald
                        SuggestionReason.AUTOCORRECT -> palette.accentFace
                        SuggestionReason.PERSONAL -> palette.glideTrailColor
                        else -> null
                    }
                    if (dotColor != null) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(dotColor)
                        )
                        Spacer(Modifier.width(5.dp))
                    }
                    Text(
                        text = suggestion.displayText,
                        color = palette.suggestionText,
                        fontSize = 13.sp,
                        fontFamily = PlusJakartaFamily,
                        fontWeight = if (isPrimary) FontWeight.SemiBold else FontWeight.Normal,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        // Right toolbar actions: One-handed & Settings
        Row(
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            StripIconButton(
                active = oneHandedMode != OneHandedMode.NONE,
                palette = palette,
                contentDesc = "Toggle one-handed mode",
                testTag = "kb_btn_one_handed",
                onClick = onCycleOneHanded
            ) {
                Icon(
                    imageVector = Icons.Default.ZoomOutMap,
                    contentDescription = "One-handed mode",
                    tint = if (oneHandedMode != OneHandedMode.NONE) palette.accentFace else palette.hintText,
                    modifier = Modifier.size(16.dp)
                )
            }
            if (onOpenSettings != null) {
                StripIconButton(
                    active = false,
                    palette = palette,
                    contentDesc = "Keyboard settings",
                    testTag = "kb_btn_settings",
                    onClick = onOpenSettings
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Settings",
                        tint = palette.hintText,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun StripIconButton(
    active: Boolean,
    palette: KeyboardPalette,
    contentDesc: String,
    testTag: String,
    onClick: () -> Unit,
    content: @Composable () -> Unit
) {
    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(if (active) palette.accentFace.copy(alpha = 0.18f) else palette.stripBackground)
            .clickable { onClick() }
            .testTag(testTag),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

@Composable
private fun OneHandedGutter(
    palette: KeyboardPalette,
    isLeftGutter: Boolean,
    height: Dp,
    onExpand: () -> Unit,
    onSwapHand: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(50.dp)
            .height(height)
            .padding(vertical = 8.dp, horizontal = 4.dp),
        verticalArrangement = Arrangement.SpaceEvenly,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(palette.modifierFace)
                .clickable { onExpand() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.ZoomOutMap,
                contentDescription = "Expand keyboard",
                tint = palette.keyText,
                modifier = Modifier.size(18.dp)
            )
        }
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(palette.modifierFace)
                .clickable { onSwapHand() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isLeftGutter) Icons.AutoMirrored.Filled.KeyboardArrowLeft else Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = "Switch one-handed side",
                tint = palette.accentFace,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

@Composable
private fun QwertyKeyboardGrid(
    palette: KeyboardPalette,
    keyHeight: Dp,
    numberRowHeight: Dp,
    settings: KeyboardSettings,
    uiState: KeyboardUiState,
    onLetterCommit: (String, TouchSample?) -> Unit,
    onGlideComplete: (List<Offset>, Map<Char, Offset>) -> Unit,
    onPopupChange: (ActiveKeyPopup?) -> Unit,
    onShiftTap: () -> Unit,
    onBackspace: () -> Unit,
    onSwipeDeleteWord: () -> Unit,
    onSwitchSymbols: () -> Unit,
    onSwitchGlobe: () -> Unit,
    onPunctuation: (String) -> Unit,
    onSpace: () -> Unit,
    onCursorScrub: (Int) -> Unit,
    onEnter: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()

    val row1 = remember { listOf('q', 'w', 'e', 'r', 't', 'y', 'u', 'i', 'o', 'p') }
    val row2 = remember { listOf('a', 's', 'd', 'f', 'g', 'h', 'j', 'k', 'l') }
    val row3 = remember { listOf('z', 'x', 'c', 'v', 'b', 'n', 'm') }

    // Bounds relative to the QWERTY 3-row letter Box
    val keyBounds = remember { mutableStateMapOf<Char, Rect>() }
    var gridOriginInRoot by remember { mutableStateOf(Offset.Zero) }
    var gridWidthPx by remember { mutableStateOf(0f) }

    var pressedChar by remember { mutableStateOf<Char?>(null) }
    val glidePoints = remember { mutableStateListOf<Offset>() }
    var isGliding by remember { mutableStateOf(false) }

    val glideSlopPx = with(density) { 18.dp.toPx() }
    val altStepPx = with(density) { 32.dp.toPx() }

    fun displayChar(c: Char): String {
        return if (uiState.shiftState != ShiftState.OFF) c.uppercase() else c.toString()
    }

    fun findNearestLetter(pos: Offset): Char? {
        keyBounds.entries.firstOrNull { it.value.contains(pos) }?.let { return it.key }
        return keyBounds.entries.minByOrNull { (_, rect) ->
            val cx = rect.center.x
            val cy = rect.center.y
            hypot((pos.x - cx).toDouble(), (pos.y - cy).toDouble())
        }?.takeIf { (_, rect) ->
            hypot((pos.x - rect.center.x).toDouble(), (pos.y - rect.center.y).toDouble()) < with(density) { 48.dp.toPx() }
        }?.key
    }

    fun updateKeyBound(ch: Char, coords: LayoutCoordinates) {
        val posInRoot = coords.positionInRoot()
        val relTopLeft = posInRoot - gridOriginInRoot
        val size = coords.size
        keyBounds[ch] = Rect(
            left = relTopLeft.x,
            top = relTopLeft.y,
            right = relTopLeft.x + size.width,
            bottom = relTopLeft.y + size.height
        )
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 3.dp, vertical = 2.dp)
    ) {
        if (settings.showNumberRow) {
            Row(modifier = Modifier.fillMaxWidth()) {
                listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0").forEach { digit ->
                    TactileActionKeycap(
                        palette = palette,
                        keyHeight = numberRowHeight,
                        contentDescriptionText = "Number $digit",
                        modifier = Modifier.weight(1f),
                        hapticEnabled = settings.hapticFeedback,
                        onClick = { onLetterCommit(digit, null) }
                    ) {
                        Text(
                            text = digit,
                            color = palette.keyText,
                            fontSize = 15.sp,
                            fontFamily = SpaceGroteskFamily,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }

        // Unified Touch + Long-Press Alternates + Glide Canvas over the 3 letter rows
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .onGloballyPositioned { coords ->
                    gridOriginInRoot = coords.positionInRoot()
                    gridWidthPx = coords.size.width.toFloat()
                }
                .pointerInput(settings.glideTypingEnabled, settings.keyPopups, settings.hapticFeedback, uiState.shiftState) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        val startPos = down.position
                        val startChar = findNearestLetter(startPos) ?: return@awaitEachGesture

                        pressedChar = startChar
                        isGliding = false
                        glidePoints.clear()
                        glidePoints.add(startPos)

                        if (settings.hapticFeedback) {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        }

                        val startRect = keyBounds[startChar]
                        if (settings.keyPopups && startRect != null) {
                            onPopupChange(
                                ActiveKeyPopup(
                                    baseChar = startChar,
                                    label = displayChar(startChar),
                                    centerX = startRect.center.x,
                                    topY = startRect.top,
                                    alternates = SpatialLayoutModel.longPressAlternates[startChar].orEmpty(),
                                    showAlternates = false
                                )
                            )
                        }

                        var alternatesOpen = false
                        var selectedAltIdx = 0
                        val alts = SpatialLayoutModel.longPressAlternates[startChar].orEmpty()
                        var longPressJob: Job? = if (alts.isNotEmpty()) {
                            scope.launch {
                                delay(360L)
                                if (!isGliding && pressedChar == startChar) {
                                    alternatesOpen = true
                                    if (settings.hapticFeedback) {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    }
                                    if (startRect != null) {
                                        onPopupChange(
                                            ActiveKeyPopup(
                                                baseChar = startChar,
                                                label = displayChar(startChar),
                                                centerX = startRect.center.x,
                                                topY = startRect.top,
                                                alternates = alts,
                                                showAlternates = true,
                                                selectedAlternateIndex = 0
                                            )
                                        )
                                    }
                                }
                            }
                        } else null

                        var activePointerId = down.id
                        var currentDownPos = startPos
                        var currentTouchPos = startPos
                        var currentHoverChar = startChar

                        try {
                            while (true) {
                                val event = awaitPointerEvent()
                                // Fast two-thumb rollover: if a second finger goes down while tapping (not gliding/alternates),
                                // commit the first key immediately and transfer tracking to the new pointer.
                                val newFingerDown = event.changes.firstOrNull {
                                    it.id != activePointerId && it.pressed && !it.previousPressed
                                }
                                if (newFingerDown != null && !isGliding && !alternatesOpen) {
                                    val nextChar = findNearestLetter(newFingerDown.position)
                                    if (nextChar != null) {
                                        longPressJob?.cancel()
                                        val rolloverSample = SpatialLayoutModel.toNormalizedTouch(
                                            pos = currentTouchPos,
                                            keyBounds = keyBounds,
                                            fallbackChar = currentHoverChar
                                        )
                                        onLetterCommit(displayChar(currentHoverChar), rolloverSample)
                                        activePointerId = newFingerDown.id
                                        currentDownPos = newFingerDown.position
                                        currentTouchPos = newFingerDown.position
                                        currentHoverChar = nextChar
                                        pressedChar = nextChar
                                        glidePoints.clear()
                                        glidePoints.add(newFingerDown.position)
                                        if (settings.hapticFeedback) {
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        }
                                        val nextRect = keyBounds[nextChar]
                                        if (settings.keyPopups && nextRect != null) {
                                            onPopupChange(
                                                ActiveKeyPopup(
                                                    baseChar = nextChar,
                                                    label = displayChar(nextChar),
                                                    centerX = nextRect.center.x,
                                                    topY = nextRect.top,
                                                    alternates = SpatialLayoutModel.longPressAlternates[nextChar].orEmpty(),
                                                    showAlternates = false
                                                )
                                            )
                                        }
                                        newFingerDown.consume()
                                        continue
                                    }
                                }

                                val change = event.changes.firstOrNull { it.id == activePointerId } ?: break
                                currentTouchPos = change.position
                                if (!change.pressed) {
                                    change.consume()
                                    break
                                }
                                val pos = change.position
                                val distFromStart = hypot((pos.x - currentDownPos.x).toDouble(), (pos.y - currentDownPos.y).toDouble()).toFloat()

                                if (alternatesOpen && alts.isNotEmpty()) {
                                    val dxFromStart = pos.x - currentDownPos.x
                                    val offsetSteps = (dxFromStart / altStepPx).toInt()
                                    val newIdx = offsetSteps.coerceIn(0, alts.lastIndex)
                                    if (newIdx != selectedAltIdx) {
                                        selectedAltIdx = newIdx
                                        if (settings.hapticFeedback) {
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        }
                                        val rectForAlt = keyBounds[currentHoverChar]
                                        if (rectForAlt != null) {
                                            onPopupChange(
                                                ActiveKeyPopup(
                                                    baseChar = currentHoverChar,
                                                    label = displayChar(currentHoverChar),
                                                    centerX = rectForAlt.center.x,
                                                    topY = rectForAlt.top,
                                                    alternates = alts,
                                                    showAlternates = true,
                                                    selectedAlternateIndex = selectedAltIdx
                                                )
                                            )
                                        }
                                    }
                                } else if (settings.glideTypingEnabled && distFromStart > glideSlopPx) {
                                    val hovered = findNearestLetter(pos)
                                    if (!isGliding && (hovered != currentHoverChar || distFromStart > glideSlopPx * 1.8f)) {
                                        isGliding = true
                                        longPressJob?.cancel()
                                        onPopupChange(null)
                                    }
                                    if (isGliding) {
                                        glidePoints.add(pos)
                                        pressedChar = hovered
                                    }
                                } else if (!isGliding) {
                                    val hovered = findNearestLetter(pos)
                                    if (hovered != null && hovered != currentHoverChar) {
                                        currentHoverChar = hovered
                                        pressedChar = hovered
                                        longPressJob?.cancel()
                                        val r = keyBounds[hovered]
                                        if (settings.keyPopups && r != null) {
                                            onPopupChange(
                                                ActiveKeyPopup(
                                                    baseChar = hovered,
                                                    label = displayChar(hovered),
                                                    centerX = r.center.x,
                                                    topY = r.top,
                                                    alternates = SpatialLayoutModel.longPressAlternates[hovered].orEmpty(),
                                                    showAlternates = false
                                                )
                                            )
                                        }
                                    }
                                }
                                change.consume()
                            }
                        } finally {
                            longPressJob?.cancel()
                        }

                        when {
                            alternatesOpen && alts.isNotEmpty() -> {
                                val chosen = alts[selectedAltIdx]
                                val finalChar = if (uiState.shiftState != ShiftState.OFF && chosen.length == 1 && chosen[0].isLetter()) {
                                    chosen.uppercase()
                                } else chosen
                                onLetterCommit(finalChar, null)
                            }
                            isGliding && glidePoints.size >= 4 -> {
                                val centers = keyBounds.mapValues { it.value.center }
                                onGlideComplete(glidePoints.toList(), centers)
                            }
                            else -> {
                                val tapSample = SpatialLayoutModel.toNormalizedTouch(
                                    pos = currentTouchPos,
                                    keyBounds = keyBounds,
                                    fallbackChar = currentHoverChar
                                )
                                onLetterCommit(displayChar(currentHoverChar), tapSample)
                            }
                        }

                        pressedChar = null
                        isGliding = false
                        glidePoints.clear()
                        onPopupChange(null)
                    }
                }
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Row 1: Q - P
                Row(modifier = Modifier.fillMaxWidth()) {
                    row1.forEach { ch ->
                        VisualLetterKeycap(
                            label = displayChar(ch),
                            hint = if (!settings.showNumberRow) SpatialLayoutModel.topRowNumberHints[ch] else null,
                            palette = palette,
                            isPressed = pressedChar == ch,
                            keyHeight = keyHeight,
                            modifier = Modifier
                                .weight(1f)
                                .onGloballyPositioned { updateKeyBound(ch, it) }
                        )
                    }
                }

                // Row 2: A - L (centered with 0.5 key side padding)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    row2.forEach { ch ->
                        VisualLetterKeycap(
                            label = displayChar(ch),
                            hint = null,
                            palette = palette,
                            isPressed = pressedChar == ch,
                            keyHeight = keyHeight,
                            modifier = Modifier
                                .weight(1f)
                                .onGloballyPositioned { updateKeyBound(ch, it) }
                        )
                    }
                }

                // Row 3: Shift + Z-M + Backspace
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TactileActionKeycap(
                        palette = palette,
                        keyHeight = keyHeight,
                        contentDescriptionText = "Shift",
                        isActiveToggle = uiState.shiftState != ShiftState.OFF,
                        hapticEnabled = settings.hapticFeedback,
                        modifier = Modifier
                            .weight(1.45f)
                            .testTag("kb_key_shift"),
                        onClick = onShiftTap
                    ) {
                        Icon(
                            imageVector = if (uiState.shiftState == ShiftState.CAPS_LOCK) {
                                Icons.Default.KeyboardCapslock
                            } else {
                                Icons.Default.KeyboardArrowUp
                            },
                            contentDescription = "Shift",
                            tint = if (uiState.shiftState != ShiftState.OFF) palette.accentFace else palette.keyText,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    row3.forEach { ch ->
                        VisualLetterKeycap(
                            label = displayChar(ch),
                            hint = null,
                            palette = palette,
                            isPressed = pressedChar == ch,
                            keyHeight = keyHeight,
                            modifier = Modifier
                                .weight(1f)
                                .onGloballyPositioned { updateKeyBound(ch, it) }
                        )
                    }

                    BackspaceKeycap(
                        icon = Icons.AutoMirrored.Filled.Backspace,
                        palette = palette,
                        keyHeight = keyHeight,
                        hapticEnabled = settings.hapticFeedback,
                        modifier = Modifier
                            .weight(1.45f)
                            .testTag("kb_key_backspace"),
                        onDeleteChar = onBackspace,
                        onSwipeDeleteWord = onSwipeDeleteWord
                    )
                }
            }

            // Glide ribbon trail Canvas overlay
            if (isGliding && glidePoints.size >= 2) {
                Canvas(modifier = Modifier.matchParentSize()) {
                    val path = Path()
                    path.moveTo(glidePoints.first().x, glidePoints.first().y)
                    for (i in 1 until glidePoints.size) {
                        path.lineTo(glidePoints[i].x, glidePoints[i].y)
                    }
                    drawPath(
                        path = path,
                        color = palette.glideTrailColor.copy(alpha = 0.35f),
                        style = Stroke(width = 18f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                    )
                    drawPath(
                        path = path,
                        color = palette.glideTrailColor,
                        style = Stroke(width = 7f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                    )
                }
            }

            // Floating key preview / long-press alternates overlay
            uiState.activePopup?.let { popup ->
                FloatingKeyPopupOverlay(
                    popup = popup,
                    palette = palette,
                    parentWidthPx = gridWidthPx
                )
            }
        }

        // Row 4: ?123, (Globe or Comma), Spacebar, Period, Enter
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TactileActionKeycap(
                palette = palette,
                keyHeight = keyHeight,
                contentDescriptionText = "Symbols and numbers",
                hapticEnabled = settings.hapticFeedback,
                modifier = Modifier
                    .weight(1.4f)
                    .testTag("kb_key_symbols"),
                onClick = onSwitchSymbols
            ) {
                Text(
                    text = "?123",
                    color = palette.keyText,
                    fontSize = 14.sp,
                    fontFamily = SpaceGroteskFamily,
                    fontWeight = FontWeight.SemiBold
                )
            }

            if (uiState.showGlobeKey) {
                TactileActionKeycap(
                    palette = palette,
                    keyHeight = keyHeight,
                    contentDescriptionText = "Switch keyboard",
                    hapticEnabled = settings.hapticFeedback,
                    modifier = Modifier.weight(1f),
                    onClick = onSwitchGlobe
                ) {
                    Icon(
                        imageVector = Icons.Default.Language,
                        contentDescription = "Switch keyboard",
                        tint = palette.keyText,
                        modifier = Modifier.size(19.dp)
                    )
                }
            } else {
                TactileActionKeycap(
                    palette = palette,
                    keyHeight = keyHeight,
                    contentDescriptionText = "Comma",
                    hapticEnabled = settings.hapticFeedback,
                    modifier = Modifier.weight(1f),
                    onClick = { onPunctuation(",") }
                ) {
                    Text(
                        text = ",",
                        color = palette.keyText,
                        fontSize = 18.sp,
                        fontFamily = SpaceGroteskFamily
                    )
                }
            }

            SpacebarKeycap(
                palette = palette,
                keyHeight = keyHeight,
                hapticEnabled = settings.hapticFeedback,
                label = "KeyCraft · Offline",
                modifier = Modifier
                    .weight(4.6f)
                    .testTag("kb_key_space"),
                onSpaceTap = onSpace,
                onCursorScrub = onCursorScrub
            )

            TactileActionKeycap(
                palette = palette,
                keyHeight = keyHeight,
                contentDescriptionText = "Period",
                hapticEnabled = settings.hapticFeedback,
                modifier = Modifier.weight(1f),
                onClick = { onPunctuation(".") },
                onLongClick = { onPunctuation("?") }
            ) {
                Text(
                    text = ".",
                    color = palette.keyText,
                    fontSize = 18.sp,
                    fontFamily = SpaceGroteskFamily,
                    fontWeight = FontWeight.Bold
                )
            }

            TactileActionKeycap(
                palette = palette,
                keyHeight = keyHeight,
                contentDescriptionText = "Enter",
                isAccent = true,
                hapticEnabled = settings.hapticFeedback,
                modifier = Modifier
                    .weight(1.6f)
                    .testTag("kb_key_enter"),
                onClick = onEnter
            ) {
                val enterIcon = when (uiState.enterAction) {
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
