package com.example.ui.keyboard

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.ui.theme.KeyboardPalette
import com.example.ui.theme.PlusJakartaFamily
import com.example.ui.theme.SpaceGroteskFamily
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun VisualLetterKeycap(
    label: String,
    hint: String?,
    palette: KeyboardPalette,
    isPressed: Boolean,
    keyHeight: Dp,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(7.dp)
    Box(
        modifier = modifier
            .height(keyHeight)
            .padding(horizontal = 2.dp, vertical = 2.5.dp)
            .semantics { contentDescription = "Key $label" }
    ) {
        // Tactile bottom bevel shadow
        Box(
            modifier = Modifier
                .fillMaxSize()
                .offset(y = 2.dp)
                .clip(shape)
                .background(palette.keyBevelShadow)
        )
        // Keycap top surface
        Box(
            modifier = Modifier
                .fillMaxSize()
                .offset(y = if (isPressed) 1.5.dp else 0.dp)
                .clip(shape)
                .background(if (isPressed) palette.keyPressedFace else palette.keyFace),
            contentAlignment = Alignment.Center
        ) {
            if (hint != null) {
                Text(
                    text = hint,
                    color = palette.hintText,
                    fontSize = 9.sp,
                    fontFamily = SpaceGroteskFamily,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 2.dp, end = 4.dp)
                )
            }
            Text(
                text = label,
                color = palette.keyText,
                fontSize = 20.sp,
                fontFamily = SpaceGroteskFamily,
                fontWeight = FontWeight.Medium,
                maxLines = 1
            )
        }
    }
}

@Composable
fun TactileActionKeycap(
    palette: KeyboardPalette,
    keyHeight: Dp,
    contentDescriptionText: String,
    modifier: Modifier = Modifier,
    isAccent: Boolean = false,
    isActiveToggle: Boolean = false,
    hapticEnabled: Boolean = true,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    var isPressed by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(7.dp)

    val faceColor = when {
        isAccent && isPressed -> palette.accentPressedFace
        isAccent -> palette.accentFace
        isActiveToggle -> palette.accentFace.copy(alpha = 0.24f)
        isPressed -> palette.modifierPressedFace
        else -> palette.modifierFace
    }

    Box(
        modifier = modifier
            .height(keyHeight)
            .padding(horizontal = 2.dp, vertical = 2.5.dp)
            .semantics { contentDescription = contentDescriptionText }
            .pointerInput(hapticEnabled, onLongClick) {
                detectTapGestures(
                    onPress = {
                        isPressed = true
                        if (hapticEnabled) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        tryAwaitRelease()
                        isPressed = false
                    },
                    onTap = { onClick() },
                    onLongPress = if (onLongClick != null) { { onLongClick() } } else null
                )
            }
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .offset(y = 2.dp)
                .clip(shape)
                .background(palette.keyBevelShadow)
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .offset(y = if (isPressed) 1.5.dp else 0.dp)
                .clip(shape)
                .background(faceColor)
                .then(
                    if (isActiveToggle && !isAccent) {
                        Modifier.border(1.dp, palette.accentFace.copy(alpha = 0.6f), shape)
                    } else Modifier
                ),
            contentAlignment = Alignment.Center
        ) {
            content()
        }
    }
}

@Composable
fun BackspaceKeycap(
    icon: ImageVector,
    palette: KeyboardPalette,
    keyHeight: Dp,
    hapticEnabled: Boolean,
    modifier: Modifier = Modifier,
    onDeleteChar: () -> Unit,
    onSwipeDeleteWord: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    var isPressed by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(7.dp)
    val swipeThresholdPx = with(density) { 42.dp.toPx() }

    Box(
        modifier = modifier
            .height(keyHeight)
            .padding(horizontal = 2.dp, vertical = 2.5.dp)
            .semantics { contentDescription = "Backspace" }
            .pointerInput(hapticEnabled) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    isPressed = true
                    if (hapticEnabled) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)

                    var swipedWord = false
                    var totalDx = 0f
                    var lastX = down.position.x

                    val repeatJob = scope.launch {
                        delay(380L)
                        while (isPressed && !swipedWord) {
                            onDeleteChar()
                            if (hapticEnabled) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            delay(65L)
                        }
                    }

                    try {
                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == down.id } ?: break
                            if (!change.pressed) {
                                change.consume()
                                break
                            }
                            val dx = change.position.x - lastX
                            lastX = change.position.x
                            totalDx += dx
                            if (!swipedWord && totalDx < -swipeThresholdPx) {
                                swipedWord = true
                                repeatJob.cancel()
                                onSwipeDeleteWord()
                                if (hapticEnabled) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            }
                            change.consume()
                        }
                    } finally {
                        repeatJob.cancel()
                        isPressed = false
                    }
                    if (!swipedWord) {
                        onDeleteChar()
                    }
                }
            }
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .offset(y = 2.dp)
                .clip(shape)
                .background(palette.keyBevelShadow)
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .offset(y = if (isPressed) 1.5.dp else 0.dp)
                .clip(shape)
                .background(if (isPressed) palette.modifierPressedFace else palette.modifierFace),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = "Backspace",
                tint = palette.keyText,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
fun SpacebarKeycap(
    palette: KeyboardPalette,
    keyHeight: Dp,
    hapticEnabled: Boolean,
    label: String,
    modifier: Modifier = Modifier,
    onSpaceTap: () -> Unit,
    onCursorScrub: (Int) -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val density = LocalDensity.current
    var isPressed by remember { mutableStateOf(false) }
    var isScrubbing by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(7.dp)
    val stepPx = with(density) { 20.dp.toPx() }
    val slopPx = with(density) { 14.dp.toPx() }

    Box(
        modifier = modifier
            .height(keyHeight)
            .padding(horizontal = 2.dp, vertical = 2.5.dp)
            .semantics { contentDescription = "Spacebar" }
            .pointerInput(hapticEnabled) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    isPressed = true
                    isScrubbing = false
                    if (hapticEnabled) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)

                    var accumDx = 0f
                    var totalMove = 0f
                    var lastX = down.position.x

                    try {
                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == down.id } ?: break
                            if (!change.pressed) {
                                change.consume()
                                break
                            }
                            val dx = change.position.x - lastX
                            lastX = change.position.x
                            totalMove += kotlin.math.abs(dx)
                            if (totalMove > slopPx) {
                                isScrubbing = true
                                accumDx += dx
                                val steps = (accumDx / stepPx).toInt()
                                if (steps != 0) {
                                    accumDx -= steps * stepPx
                                    onCursorScrub(steps)
                                    if (hapticEnabled) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                }
                            }
                            change.consume()
                        }
                    } finally {
                        val wasScrubbing = isScrubbing
                        isPressed = false
                        isScrubbing = false
                        if (!wasScrubbing) {
                            onSpaceTap()
                        }
                    }
                }
            }
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .offset(y = 2.dp)
                .clip(shape)
                .background(palette.keyBevelShadow)
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .offset(y = if (isPressed) 1.5.dp else 0.dp)
                .clip(shape)
                .background(if (isPressed) palette.keyPressedFace else palette.keyFace),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (isScrubbing) "◀  Cursor  ▶" else label,
                color = if (isScrubbing) palette.accentFace else palette.hintText,
                fontSize = 11.sp,
                fontFamily = PlusJakartaFamily,
                fontWeight = if (isScrubbing) FontWeight.SemiBold else FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun FloatingKeyPopupOverlay(
    popup: ActiveKeyPopup,
    palette: KeyboardPalette,
    parentWidthPx: Float
) {
    val density = LocalDensity.current
    if (!popup.showAlternates || popup.alternates.isEmpty()) {
        val bubbleW = with(density) { 46.dp.toPx() }
        val bubbleH = with(density) { 54.dp.toPx() }
        val xPos = (popup.centerX - bubbleW / 2f).coerceIn(4f, (parentWidthPx - bubbleW - 4f).coerceAtLeast(4f))
        val yPos = (popup.topY - bubbleH - with(density) { 6.dp.toPx() }).coerceAtLeast(-bubbleH * 0.5f)

        Box(
            modifier = Modifier
                .zIndex(20f)
                .offset { IntOffset(xPos.roundToInt(), yPos.roundToInt()) }
                .size(46.dp, 54.dp)
                .shadow(8.dp, RoundedCornerShape(10.dp))
                .clip(RoundedCornerShape(10.dp))
                .background(palette.popupBg)
                .border(1.dp, palette.dividerColor, RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = popup.label,
                color = palette.popupText,
                fontSize = 24.sp,
                fontFamily = SpaceGroteskFamily,
                fontWeight = FontWeight.Bold
            )
        }
    } else {
        val itemWidthDp = 36.dp
        val totalW = with(density) { (itemWidthDp * popup.alternates.size + 12.dp).toPx() }
        val bubbleH = with(density) { 48.dp.toPx() }
        val xPos = (popup.centerX - totalW / 2f).coerceIn(4f, (parentWidthPx - totalW - 4f).coerceAtLeast(4f))
        val yPos = (popup.topY - bubbleH - with(density) { 8.dp.toPx() }).coerceAtLeast(-bubbleH * 0.5f)

        Row(
            modifier = Modifier
                .zIndex(25f)
                .offset { IntOffset(xPos.roundToInt(), yPos.roundToInt()) }
                .height(48.dp)
                .shadow(10.dp, RoundedCornerShape(12.dp))
                .clip(RoundedCornerShape(12.dp))
                .background(palette.popupBg)
                .border(1.dp, palette.accentFace.copy(alpha = 0.7f), RoundedCornerShape(12.dp))
                .padding(horizontal = 6.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            popup.alternates.forEachIndexed { idx, alt ->
                val selected = idx == popup.selectedAlternateIndex
                Box(
                    modifier = Modifier
                        .width(34.dp)
                        .height(38.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (selected) palette.accentFace else Color.Transparent),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = alt,
                        color = if (selected) palette.onAccentText else palette.popupText,
                        fontSize = 18.sp,
                        fontFamily = SpaceGroteskFamily,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}
