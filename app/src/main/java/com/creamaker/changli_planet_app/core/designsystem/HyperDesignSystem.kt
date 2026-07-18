package com.creamaker.changli_planet_app.core.designsystem

import android.graphics.BlurMaskFilter
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Text
import com.creamaker.changli_planet_app.core.theme.AppTheme

/** Shared dimensions from the Hyper-Neumorphic design system. */
object HyperSpacing {
    val pageHorizontal = 20.dp
    val cardGap = 12.dp
    val cardRadius = 24.dp
    val cardPadding = 14.dp
    val cardPaddingHorizontal = 16.dp
    val cardPaddingVertical = 12.dp
    val topBarRadius = 24.dp
    val topBarContentHeight = 44.dp
    val controlRadius = 16.dp
    val pillRadius = 999.dp
}

/**
 * Raised surface: top-left highlight plus bottom-right shadow.
 * It deliberately avoids Material elevation so light and dark skins render consistently.
 */
@Composable
fun Modifier.hyperConvex(
    cornerRadius: Dp = HyperSpacing.cardRadius,
    elevation: Dp = 6.dp,
    surfaceColor: Color = AppTheme.colors.bgCardColor
): Modifier {
    val dark = AppTheme.colors.bgPrimaryColor.red < .25f
    val highlight = if (dark) Color(0xFF2A2D33) else Color.White
    val shadow = if (dark) Color(0xFF080A0E) else Color(0xFFD1D9E6)
    return drawBehind {
        drawIntoCanvas { canvas ->
            val blur = BlurMaskFilter(elevation.toPx(), BlurMaskFilter.Blur.NORMAL)
            val paint = Paint().apply {
                asFrameworkPaint().apply {
                    isAntiAlias = true
                    maskFilter = blur
                }
            }
            val offset = elevation.toPx() * .55f
            paint.color = shadow.copy(alpha = .82f)
            canvas.drawRoundRect(offset, offset, size.width + offset, size.height + offset,
                cornerRadius.toPx(), cornerRadius.toPx(), paint)
            paint.color = highlight.copy(alpha = if (dark) .48f else .92f)
            canvas.drawRoundRect(-offset, -offset, size.width - offset, size.height - offset,
                cornerRadius.toPx(), cornerRadius.toPx(), paint)
        }
    }.clip(RoundedCornerShape(cornerRadius)).background(surfaceColor)
}

/** Selected/pressed surface with a restrained inset-like light treatment. */
@Composable
fun Modifier.hyperConcave(
    cornerRadius: Dp = HyperSpacing.controlRadius,
    surfaceColor: Color = AppTheme.colors.bgButtonLowlightColor
): Modifier {
    val outline = AppTheme.colors.outlineLowContrastColor.copy(alpha = .42f)
    return clip(RoundedCornerShape(cornerRadius))
        .background(surfaceColor)
        .border(1.dp, outline, RoundedCornerShape(cornerRadius))
}

/** No-ripple HyperOS press animation with haptic feedback. */
fun Modifier.hyperTap(
    enabled: Boolean = true,
    onClick: () -> Unit
): Modifier = composed {
    var pressed by remember { mutableStateOf(false) }
    // pointerInput keeps the coroutine created for its keys. Always invoke the
    // latest callback so state-dependent actions (such as expand/collapse)
    // don't keep replaying the value captured on the first composition.
    val currentOnClick by rememberUpdatedState(onClick)
    val scale by animateFloatAsState(
        targetValue = if (pressed && enabled) .95f else 1f,
        animationSpec = tween(150, easing = FastOutSlowInEasing),
        label = "hyper_press"
    )
    val haptic = LocalHapticFeedback.current
    graphicsLayer { scaleX = scale; scaleY = scale }
        .pointerInput(enabled) {
            if (enabled) detectTapGestures(
                onPress = { pressed = true; tryAwaitRelease(); pressed = false },
                onTap = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    currentOnClick()
                }
            )
        }
}

/** Drop-in card for existing screens while they migrate off Material Surface elevation. */
@Composable
fun HyperSurface(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(HyperSpacing.cardRadius),
    color: Color = AppTheme.colors.bgCardColor,
    border: BorderStroke? = null,
    shadowElevation: Dp = 0.dp,
    tonalElevation: Dp = 0.dp,
    content: @Composable BoxScope.() -> Unit
) {
    @Suppress("UNUSED_VARIABLE") val materialElevation = shadowElevation + tonalElevation
    Box(
        modifier = modifier
            .hyperConvex(surfaceColor = color)
            .then(if (border != null) Modifier.border(border, shape) else Modifier),
        contentAlignment = Alignment.TopStart,
        content = content
    )
}

@Composable
fun HyperIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    minSize: Dp = 44.dp,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .sizeIn(minWidth = minSize, minHeight = minSize)
            .then(if (selected) Modifier.hyperConcave(18.dp) else Modifier.hyperConvex(18.dp, 3.dp))
            .hyperTap(onClick = onClick),
        contentAlignment = Alignment.Center,
        content = content
    )
}

@Composable
fun HyperButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    primary: Boolean = true,
    enabled: Boolean = true
) {
    val colors = AppTheme.colors
    Box(
        modifier = modifier
            .height(52.dp)
            .hyperConvex(
                cornerRadius = 26.dp,
                elevation = 5.dp,
                surfaceColor = if (primary) colors.bgButtonLowlightColor else colors.bgPrimaryColor
            )
            .hyperTap(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = if (enabled) {
                if (primary) colors.functionalTextColor else colors.secondaryTextColor
            } else colors.disabledTextColor,
            fontSize = 15.sp,
            fontWeight = if (primary) FontWeight.Bold else FontWeight.Medium
        )
    }
}
