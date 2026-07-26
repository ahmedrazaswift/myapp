package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer

/**
 * High-performance hardware-accelerated press scale effect for interactive UI elements
 * (buttons, cards, chips, grid items).
 */
fun Modifier.bounceOnClick(
    enabled: Boolean = true,
    scaleDownRatio: Float = 0.96f,
    onClick: (() -> Unit)? = null
): Modifier = composed {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    val scale by animateFloatAsState(
        targetValue = if (isPressed && enabled) scaleDownRatio else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "BounceScale"
    )

    this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .then(
            if (onClick != null) {
                Modifier.clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    enabled = enabled,
                    onClick = onClick
                )
            } else Modifier
        )
}

/**
 * Animated container wrapper for Dialogs and Modals to provide a smooth, springy
 * scale-and-fade entrance effect using hardware acceleration (graphicsLayer).
 */
@Composable
fun AnimatedDialogContainer(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    var isVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        isVisible = true
    }

    val alpha by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing),
        label = "DialogAlpha"
    )

    val scale by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0.92f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "DialogScale"
    )

    androidx.compose.foundation.layout.Box(
        modifier = modifier.graphicsLayer {
            this.alpha = alpha
            this.scaleX = scale
            this.scaleY = scale
        }
    ) {
        content()
    }
}

/**
 * Standard optimized slide & cross-fade transition spec for screen and tab navigation.
 */
fun getStandardScreenTransitionSpec(): ContentTransform {
    return (fadeIn(animationSpec = tween(280, easing = LinearOutSlowInEasing)) +
            slideInHorizontally(animationSpec = tween(280, easing = FastOutSlowInEasing)) { width -> (width * 0.05f).toInt() }) togetherWith
            (fadeOut(animationSpec = tween(200, easing = FastOutLinearInEasing)) +
                    slideOutHorizontally(animationSpec = tween(200, easing = FastOutLinearInEasing)) { width -> -(width * 0.05f).toInt() })
}
