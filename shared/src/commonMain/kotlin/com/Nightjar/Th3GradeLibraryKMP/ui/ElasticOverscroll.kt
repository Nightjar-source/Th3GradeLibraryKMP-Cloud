package com.Nightjar.Th3GradeLibraryKMP.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.offset
import kotlin.math.roundToInt
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.Velocity
import kotlinx.coroutines.launch

/**
 * Samsung One UI signature liquid elastic overscroll effect on vertical scrolling.
 * Provides organic rubber-band pull-down reachability physics with smooth spring rebound.
 */
fun Modifier.elasticOverscroll(
    maxStretchRatio: Float = 0.16f,
    topEnabled: Boolean = true,
    bottomEnabled: Boolean = true
): Modifier = composed {
    val isLowEnd = com.Nightjar.Th3GradeLibraryKMP.theme.LocalIsLowEndDevice.current
    if (isLowEnd) return@composed this

    var componentHeight by remember { mutableStateOf(0f) }
    val overscrollOffset = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()

    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                val current = overscrollOffset.value
                if (current != 0f && source == NestedScrollSource.UserInput) {
                    val delta = available.y
                    if ((current > 0 && delta < 0) || (current < 0 && delta > 0)) {
                        val newOffset = current + delta
                        return if ((current > 0 && newOffset <= 0) || (current < 0 && newOffset >= 0)) {
                            scope.launch { overscrollOffset.snapTo(0f) }
                            Offset(0f, current)
                        } else {
                            scope.launch { overscrollOffset.snapTo(newOffset) }
                            Offset(0f, delta)
                        }
                    }
                }
                return Offset.Zero
            }

            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource
            ): Offset {
                if (source == NestedScrollSource.UserInput && available.y != 0f) {
                    val isPullingDown = available.y > 0 // Top edge overscroll
                    val isPullingUp = available.y < 0   // Bottom edge overscroll
                    
                    if ((isPullingDown && !topEnabled && overscrollOffset.value <= 0f) || 
                        (isPullingUp && !bottomEnabled && overscrollOffset.value >= 0f)) {
                        return Offset.Zero
                    }
                    
                    val maxOffset = if (componentHeight > 0) componentHeight * maxStretchRatio else 180f
                    val current = overscrollOffset.value
                    val progress = (kotlin.math.abs(current) / maxOffset).coerceIn(0f, 1f)
                    val resistance = 0.45f * (1f - progress * 0.70f)
                    val targetOffset = (current + available.y * resistance).coerceIn(-maxOffset, maxOffset)
                    scope.launch { overscrollOffset.snapTo(targetOffset) }
                    return Offset(0f, available.y)
                }
                return Offset.Zero
            }

            override suspend fun onPreFling(available: Velocity): Velocity {
                if (overscrollOffset.value != 0f) {
                    overscrollOffset.animateTo(
                        0f,
                        spring(
                            dampingRatio = 0.76f,
                            stiffness = 280f
                        )
                    )
                    return available
                }
                return Velocity.Zero
            }
        }
    }

    LaunchedEffect(overscrollOffset.value) {
        TransitionTargetTracker.currentOverscrollY = overscrollOffset.value
    }

    this
        .onSizeChanged { componentHeight = it.height.toFloat() }
        .nestedScroll(nestedScrollConnection)
        .offset {
            androidx.compose.ui.unit.IntOffset(0, overscrollOffset.value.roundToInt())
        }
}
