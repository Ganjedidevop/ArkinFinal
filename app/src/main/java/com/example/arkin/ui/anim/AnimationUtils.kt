package com.example.arkin.ui.anim

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Spring-based tactile bouncy clickable modifier.
 * Scales down smoothly on press and springs back on release.
 */
fun Modifier.bouncyClickable(
    pressedScale: Float = 0.93f,
    onClick: () -> Unit
): Modifier = composed {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) pressedScale else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "bouncy_scale"
    )
    this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .clickable(
            interactionSource = interactionSource,
            indication = null,
            onClick = onClick
        )
}

/**
 * Scale-on-press effect that wraps any existing clickable or Surface without overriding click.
 */
fun Modifier.pressScaleEffect(
    pressedScale: Float = 0.94f
): Modifier = composed {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) pressedScale else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "press_scale"
    )
    this.graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}

/**
 * Staggered entrance animation for list/grid items.
 * Each item fades in and slides up with a delay based on its index.
 */
@Composable
fun staggeredEntrance(
    index: Int,
    baseDelayMs: Int = 45,
    durationMs: Int = 380
): Pair<Float, Float> {
    var visible by remember { mutableStateOf(false) }
    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(
            durationMillis = durationMs,
            delayMillis = (index * baseDelayMs).coerceAtMost(350),
            easing = FastOutSlowInEasing
        ),
        label = "stagger_alpha_"
    )
    val translationY by animateFloatAsState(
        targetValue = if (visible) 0f else 35f,
        animationSpec = tween(
            durationMillis = durationMs,
            delayMillis = (index * baseDelayMs).coerceAtMost(350),
            easing = FastOutSlowInEasing
        ),
        label = "stagger_translationY_"
    )
    LaunchedEffect(Unit) { visible = true }
    return alpha to translationY
}

/**
 * Modifier for staggered slide-up + fade-in entrance.
 */
fun Modifier.animateStaggeredEntrance(
    index: Int,
    baseDelayMs: Int = 45,
    durationMs: Int = 380
): Modifier = composed {
    val (alpha, translationY) = staggeredEntrance(index, baseDelayMs, durationMs)
    this.graphicsLayer {
        this.alpha = alpha
        this.translationY = translationY
    }
}

/**
 * Gentle infinite floating animation (up/down) for decorative elements.
 */
@Composable
fun floatingOffset(
    amplitude: Float = 6f,
    durationMs: Int = 2400,
    delayMs: Int = 0
): Float {
    val infiniteTransition = rememberInfiniteTransition(label = "float")
    val offset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = amplitude,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMs, delayMillis = delayMs, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Reverse
        ),
        label = "float_offset"
    )
    return offset
}

/**
 * Pulsing scale animation for attention-grabbing icons or badges.
 */
fun Modifier.pulseEffect(
    minScale: Float = 0.94f,
    maxScale: Float = 1.06f,
    durationMs: Int = 1400
): Modifier = composed {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = minScale,
        targetValue = maxScale,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMs, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )
    this.graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}

/**
 * Slide-up entrance animation modifier. Slides up and fades in on first composition.
 */
fun Modifier.slideUpEntrance(
    durationMs: Int = 450,
    delayMs: Int = 0,
    slideDistance: Float = 50f
): Modifier = composed {
    var visible by remember { mutableStateOf(false) }
    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(durationMs, delayMillis = delayMs, easing = FastOutSlowInEasing),
        label = "slideUp_alpha"
    )
    val translationY by animateFloatAsState(
        targetValue = if (visible) 0f else slideDistance,
        animationSpec = tween(durationMs, delayMillis = delayMs, easing = FastOutSlowInEasing),
        label = "slideUp_translationY"
    )
    LaunchedEffect(Unit) { visible = true }
    this.graphicsLayer {
        this.alpha = alpha
        this.translationY = translationY
    }
}

/**
 * Slide-in from left entrance animation.
 */
fun Modifier.slideInFromLeft(
    durationMs: Int = 450,
    delayMs: Int = 0,
    slideDistance: Float = -60f
): Modifier = composed {
    var visible by remember { mutableStateOf(false) }
    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(durationMs, delayMillis = delayMs, easing = FastOutSlowInEasing),
        label = "slideLeft_alpha"
    )
    val translationX by animateFloatAsState(
        targetValue = if (visible) 0f else slideDistance,
        animationSpec = tween(durationMs, delayMillis = delayMs, easing = FastOutSlowInEasing),
        label = "slideLeft_translationX"
    )
    LaunchedEffect(Unit) { visible = true }
    this.graphicsLayer {
        this.alpha = alpha
        this.translationX = translationX
    }
}

/**
 * Scale-up entrance with spring bounce.
 */
fun Modifier.scaleEntrance(
    delayMs: Int = 0,
    initialScale: Float = 0.85f
): Modifier = composed {
    var visible by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (visible) 1f else initialScale,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "scale_entrance"
    )
    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(300, delayMillis = delayMs),
        label = "scale_entrance_alpha"
    )
    LaunchedEffect(Unit) {
        if (delayMs > 0) kotlinx.coroutines.delay(delayMs.toLong())
        visible = true
    }
    this.graphicsLayer {
        this.scaleX = scale
        this.scaleY = scale
        this.alpha = alpha
    }
}

/**
 * Modern 3-dot bouncing progress indicator for AI thinking/typing states.
 */
@Composable
fun BouncingDotsIndicator(
    color: Color,
    dotSize: Dp = 8.dp,
    dotSpacing: Dp = 5.dp
) {
    val infiniteTransition = rememberInfiniteTransition(label = "bouncing_dots")

    @Composable
    fun animateDot(delayMillis: Int): Float {
        val yOffset by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = -8f,
            animationSpec = infiniteRepeatable(
                animation = keyframes {
                    durationMillis = 1000
                    0f at 0
                    -8f at 250 with FastOutSlowInEasing
                    0f at 500 with FastOutSlowInEasing
                    0f at 1000
                },
                repeatMode = RepeatMode.Restart,
                initialStartOffset = StartOffset(delayMillis)
            ),
            label = "dot_offset_"
        )
        return yOffset
    }

    val offset1 = animateDot(0)
    val offset2 = animateDot(180)
    val offset3 = animateDot(360)

    Row(
        horizontalArrangement = Arrangement.spacedBy(dotSpacing),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .graphicsLayer { translationY = offset1 }
                .size(dotSize)
                .clip(CircleShape)
                .background(color)
        )
        Box(
            modifier = Modifier
                .graphicsLayer { translationY = offset2 }
                .size(dotSize)
                .clip(CircleShape)
                .background(color)
        )
        Box(
            modifier = Modifier
                .graphicsLayer { translationY = offset3 }
                .size(dotSize)
                .clip(CircleShape)
                .background(color)
        )
    }
}

private val EaseInOutCubic = CubicBezierEasing(0.65f, 0f, 0.35f, 1f)