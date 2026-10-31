package com.Nightjar.Th3GradeLibraryKMP.theme

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Surface
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment

import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.clip
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.draw.drawWithCache
import kotlinx.coroutines.launch
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.toRect
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.materialkolor.rememberDynamicColorScheme
import org.jetbrains.compose.resources.Font
import com.Nightjar.Th3GradeLibraryKMP.generated.resources.Res
import com.Nightjar.Th3GradeLibraryKMP.generated.resources.reem_kufi_bold
import com.Nightjar.Th3GradeLibraryKMP.generated.resources.reem_kufi_medium
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme

fun parseHexColor(hex: String): Color {
    return try {
        val cleanHex = hex.removePrefix("#")
        val argb = if (cleanHex.length == 6) {
            "FF$cleanHex".toLong(16)
        } else {
            cleanHex.toLong(16)
        }
        Color(argb)
    } catch (e: Exception) {
        Color(0xFF6366F1) // Fallback indigo
    }
}

val LocalIsLowEndDevice = compositionLocalOf { false }
val LocalIsNavigatingBack = compositionLocalOf { false }
val LocalSeenEntranceItems = compositionLocalOf<androidx.compose.runtime.snapshots.SnapshotStateMap<String, Boolean>?> { null }
val LocalScreenEntranceKey = compositionLocalOf { "default_screen" }
val LocalIsReturningFromPdf = compositionLocalOf { false }

// Glassmorphism modifier optimized with hardware-accelerated drawWithCache
fun Modifier.liquidGlass(
    isDark: Boolean,
    borderRadius: Dp = 24.dp,
    alpha: Float = 0.95f // Increased to hide content underneath
): Modifier = composed {
    val isLowEnd = LocalIsLowEndDevice.current
    val bg = if (isDark) {
        Color(0xFF1E293B).copy(alpha = if (isLowEnd) 1.0f else alpha) // Slate 800 (Solid for low end)
    } else {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = if (isLowEnd) 1.0f else alpha)
    }
    
    if (isLowEnd) {
        return@composed this.background(bg, RoundedCornerShape(borderRadius))
    }
    val border = if (isDark) {
        Color(0xFFFFFFFF).copy(alpha = 0.15f)
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.15f)
    }
    this.drawWithCache {
        val radiusPx = CornerRadius(borderRadius.coerceAtLeast(0.dp).toPx())
        val strokeWidth = 1.dp.toPx()
        val strokeStyle = Stroke(width = strokeWidth)
        
        onDrawBehind {
            drawRoundRect(color = bg, cornerRadius = radiusPx)
            drawRoundRect(color = border, cornerRadius = radiusPx, style = strokeStyle)
        }
    }
}

@Composable
fun Modifier.bounceClick(
    onClick: () -> Unit
): Modifier {
    val interactionSource = androidx.compose.runtime.remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = androidx.compose.animation.core.spring(
            dampingRatio = androidx.compose.animation.core.Spring.DampingRatioNoBouncy,
            stiffness = androidx.compose.animation.core.Spring.StiffnessHigh
        )
    )
    return this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .clickable(
            interactionSource = interactionSource,
            indication = androidx.compose.foundation.LocalIndication.current,
            onClick = onClick
        )
}

/**
 * Universal Material 3 Expressive Button Shape Morphing on Press (from caliq5)
 * Dynamically morphs shape from rounded pill (24.dp) to squircle/square (8.dp) with scale bounce on press.
 */
fun Modifier.expressiveButtonMorph(
    restRadius: androidx.compose.ui.unit.Dp = 24.dp,
    pressedRadius: androidx.compose.ui.unit.Dp = 8.dp,
    pressedScale: Float = 0.92f,
    interactionSource: androidx.compose.foundation.interaction.MutableInteractionSource,
    backgroundColor: Color? = null,
    borderWidth: androidx.compose.ui.unit.Dp? = null,
    borderColor: Color? = null
): Modifier = composed {
    val isPressed by interactionSource.collectIsPressedAsState()
    val cornerRadius by androidx.compose.animation.core.animateDpAsState(
        targetValue = if (isPressed) pressedRadius else restRadius,
        animationSpec = androidx.compose.animation.core.spring(dampingRatio = 0.52f, stiffness = 420f),
        label = "expressive_corner_morph"
    )
    val scale by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (isPressed) pressedScale else 1.0f,
        animationSpec = androidx.compose.animation.core.spring(dampingRatio = 0.52f, stiffness = 420f),
        label = "expressive_scale_morph"
    )
    val shape = RoundedCornerShape(cornerRadius.coerceAtLeast(0.dp))
    this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .clip(shape)
        .then(if (backgroundColor != null) Modifier.background(backgroundColor, shape) else Modifier)
        .then(if (borderWidth != null && borderColor != null) Modifier.border(borderWidth, borderColor, shape) else Modifier)
}

/**
 * Fluid staggered sliding animation on screen load and quick navigations (matching caliq5 exactly).
 * Replays smoothly on every screen entrance or tab switch with an elegant, moist (Fluid) spring motion.
 * Protected by LocalSeenEntranceItems session cache to never re-trigger or flash during list scrolling or accordion toggles.
 */
fun Modifier.screenEntranceStagger(index: Int = 0, itemId: String? = null): Modifier = composed {
    val isLowEnd = LocalIsLowEndDevice.current
    if (isLowEnd) return@composed this

    val isReturningFromPdf = LocalIsReturningFromPdf.current
    val isNavigatingBack = LocalIsNavigatingBack.current
    val itemKey = itemId ?: index.toString()
    val isActiveSharedTarget = com.Nightjar.Th3GradeLibraryKMP.ui.TransitionTargetTracker.isTransitionActive &&
            (com.Nightjar.Th3GradeLibraryKMP.ui.TransitionTargetTracker.activeTargetId == "card_$itemKey" ||
             com.Nightjar.Th3GradeLibraryKMP.ui.TransitionTargetTracker.activeTargetId == itemKey ||
             com.Nightjar.Th3GradeLibraryKMP.ui.TransitionTargetTracker.activeTargetId?.startsWith("bookmark_$itemKey") == true)

    val screenKey = LocalScreenEntranceKey.current
    val seenMap = LocalSeenEntranceItems.current

    // When returning from PDF or navigating back or on active shared target: keep 100% stationary with zero movement!
    if (isReturningFromPdf || isNavigatingBack || isActiveSharedTarget) {
        seenMap?.put(itemKey, true)
        return@composed this
    }

    val alreadySeen = seenMap?.get(itemKey) == true
    if (alreadySeen) return@composed this

    val animProgress = androidx.compose.runtime.remember(screenKey, itemKey) { androidx.compose.animation.core.Animatable(0f) }
    val density = androidx.compose.ui.platform.LocalDensity.current
    val offsetYPx = androidx.compose.runtime.remember(density) { with(density) { 14.dp.toPx() } }

    androidx.compose.runtime.LaunchedEffect(screenKey, itemKey) {
        val staggerDelay = ((index % 8).coerceIn(0, 7) * 24L)
        if (staggerDelay > 0) kotlinx.coroutines.delay(staggerDelay)
        animProgress.animateTo(
            targetValue = 1f,
            animationSpec = androidx.compose.animation.core.spring(
                dampingRatio = 0.76f, // Ultra-moist fluid organic spring (كالجبنة)
                stiffness = 320f
            )
        )
        seenMap?.put(itemKey, true)
    }

    val p = animProgress.value
    this.graphicsLayer {
        this.alpha = 0.30f + 0.70f * p // Base alpha 0.30f eliminates harsh dark/white flashes!
        this.translationY = (1f - p) * offsetYPx
        this.scaleX = 0.985f + 0.015f * p
        this.scaleY = 0.985f + 0.015f * p
        this.clip = false
    }
}

/**
 * Screen entrance cascade alias with item ID support.
 */
fun Modifier.screenEntranceCascade(index: Int = 0, itemId: String? = null): Modifier = this.screenEntranceStagger(index, itemId)

/**
 * Pop-in animation on initial load (caliq5 standard).
 */
fun Modifier.popInOnInitialLoad(index: Int = 0, itemId: String? = null): Modifier = this.screenEntranceStagger(index, itemId)

/**
 * Unified adaptive entrance: Screen Entrance Cascade for initial visible items (0..8).
 * Scrolled items (> 8) are rendered instantly without overhead to preserve 120 FPS scrolling.
 */
fun Modifier.adaptiveItemEntrance(index: Int = 0, itemId: String? = null): Modifier = this.screenEntranceCascade(index % 8, itemId)

val KufiReemFontFamily: FontFamily
    @Composable
    get() = FontFamily(
        Font(Res.font.reem_kufi_medium, FontWeight.Medium),
        Font(Res.font.reem_kufi_bold, FontWeight.Bold)
    )

@Composable
fun animateColorScheme(targetColorScheme: ColorScheme): ColorScheme {
    val animationSpec = tween<Color>(durationMillis = 350, easing = LinearOutSlowInEasing)
    
    val primary = animateColorAsState(targetColorScheme.primary, animationSpec).value
    val onPrimary = animateColorAsState(targetColorScheme.onPrimary, animationSpec).value
    val primaryContainer = animateColorAsState(targetColorScheme.primaryContainer, animationSpec).value
    val onPrimaryContainer = animateColorAsState(targetColorScheme.onPrimaryContainer, animationSpec).value
    val inversePrimary = animateColorAsState(targetColorScheme.inversePrimary, animationSpec).value
    val secondary = animateColorAsState(targetColorScheme.secondary, animationSpec).value
    val onSecondary = animateColorAsState(targetColorScheme.onSecondary, animationSpec).value
    val secondaryContainer = animateColorAsState(targetColorScheme.secondaryContainer, animationSpec).value
    val onSecondaryContainer = animateColorAsState(targetColorScheme.onSecondaryContainer, animationSpec).value
    val tertiary = animateColorAsState(targetColorScheme.tertiary, animationSpec).value
    val onTertiary = animateColorAsState(targetColorScheme.onTertiary, animationSpec).value
    val tertiaryContainer = animateColorAsState(targetColorScheme.tertiaryContainer, animationSpec).value
    val onTertiaryContainer = animateColorAsState(targetColorScheme.onTertiaryContainer, animationSpec).value
    val background = animateColorAsState(targetColorScheme.background, animationSpec).value
    val onBackground = animateColorAsState(targetColorScheme.onBackground, animationSpec).value
    val surface = animateColorAsState(targetColorScheme.surface, animationSpec).value
    val onSurface = animateColorAsState(targetColorScheme.onSurface, animationSpec).value
    val surfaceVariant = animateColorAsState(targetColorScheme.surfaceVariant, animationSpec).value
    val onSurfaceVariant = animateColorAsState(targetColorScheme.onSurfaceVariant, animationSpec).value
    val surfaceTint = animateColorAsState(targetColorScheme.surfaceTint, animationSpec).value
    val inverseSurface = animateColorAsState(targetColorScheme.inverseSurface, animationSpec).value
    val inverseOnSurface = animateColorAsState(targetColorScheme.inverseOnSurface, animationSpec).value
    val error = animateColorAsState(targetColorScheme.error, animationSpec).value
    val onError = animateColorAsState(targetColorScheme.onError, animationSpec).value
    val errorContainer = animateColorAsState(targetColorScheme.errorContainer, animationSpec).value
    val onErrorContainer = animateColorAsState(targetColorScheme.onErrorContainer, animationSpec).value
    val outline = animateColorAsState(targetColorScheme.outline, animationSpec).value
    val outlineVariant = animateColorAsState(targetColorScheme.outlineVariant, animationSpec).value
    val scrim = animateColorAsState(targetColorScheme.scrim, animationSpec).value

    return ColorScheme(
        primary = primary,
        onPrimary = onPrimary,
        primaryContainer = primaryContainer,
        onPrimaryContainer = onPrimaryContainer,
        inversePrimary = inversePrimary,
        secondary = secondary,
        onSecondary = onSecondary,
        secondaryContainer = secondaryContainer,
        onSecondaryContainer = onSecondaryContainer,
        tertiary = tertiary,
        onTertiary = onTertiary,
        tertiaryContainer = tertiaryContainer,
        onTertiaryContainer = onTertiaryContainer,
        background = background,
        onBackground = onBackground,
        surface = surface,
        onSurface = onSurface,
        surfaceVariant = surfaceVariant,
        onSurfaceVariant = onSurfaceVariant,
        surfaceTint = surfaceTint,
        inverseSurface = inverseSurface,
        inverseOnSurface = inverseOnSurface,
        error = error,
        onError = onError,
        errorContainer = errorContainer,
        onErrorContainer = onErrorContainer,
        outline = outline,
        outlineVariant = outlineVariant,
        scrim = scrim
    )
}

@Composable
fun Th3GradeTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    pureBlackMode: Boolean = false,
    pureWhiteMode: Boolean = false,
    seedColorHex: String = "#2563eb",
    dynamicColorScheme: androidx.compose.material3.ColorScheme? = null,
    content: @Composable () -> Unit
) {
    val seedColor = parseHexColor(seedColorHex)
    val isMonochrome = seedColorHex.equals("#171717", ignoreCase = true) || seedColorHex.equals("#ffffff", ignoreCase = true)
    
    val baseColorScheme = dynamicColorScheme ?: rememberDynamicColorScheme(
        seedColor = seedColor,
        isDark = darkTheme
    )

    val colorScheme = if (isMonochrome) {
        if (darkTheme) {
            darkColorScheme(
                primary = Color.White,
                onPrimary = Color.Black,
                primaryContainer = Color(0xFF262626),
                onPrimaryContainer = Color.White,
                inversePrimary = Color.Black,
                secondary = Color(0xFFE5E5E5),
                onSecondary = Color.Black,
                secondaryContainer = Color(0xFF404040),
                onSecondaryContainer = Color.White,
                tertiary = Color(0xFFD4D4D4),
                onTertiary = Color.Black,
                tertiaryContainer = Color(0xFF262626),
                onTertiaryContainer = Color(0xFFD4D4D4),
                background = Color.Black,
                onBackground = Color.White,
                surface = Color(0xFF0A0A0A),
                onSurface = Color.White,
                surfaceVariant = Color(0xFF171717),
                onSurfaceVariant = Color(0xFFD4D4D4),
                surfaceTint = Color.White,
                inverseSurface = Color.White,
                inverseOnSurface = Color.Black,
                error = Color(0xFFF87171),
                onError = Color.Black,
                errorContainer = Color(0xFF7F1D1D),
                onErrorContainer = Color(0xFFF87171),
                outline = Color(0xFF404040),
                outlineVariant = Color(0xFF262626),
                scrim = Color.Black
            )
        } else {
            lightColorScheme(
                primary = Color.Black,
                onPrimary = Color.White,
                primaryContainer = Color(0xFFF5F5F5),
                onPrimaryContainer = Color.Black,
                inversePrimary = Color.White,
                secondary = Color(0xFF404040),
                onSecondary = Color.White,
                secondaryContainer = Color(0xFFE5E5E5),
                onSecondaryContainer = Color.Black,
                tertiary = Color(0xFF737373),
                onTertiary = Color.White,
                tertiaryContainer = Color(0xFFF5F5F5),
                onTertiaryContainer = Color(0xFF262626),
                background = Color.White,
                onBackground = Color.Black,
                surface = Color(0xFFFAFAFA),
                onSurface = Color.Black,
                surfaceVariant = Color(0xFFF5F5F5),
                onSurfaceVariant = Color(0xFF525252),
                surfaceTint = Color.Black,
                inverseSurface = Color.Black,
                inverseOnSurface = Color.White,
                error = Color(0xFFB91C1C),
                onError = Color.White,
                errorContainer = Color(0xFFFEE2E2),
                onErrorContainer = Color(0xFFB91C1C),
                outline = Color(0xFFD4D4D4),
                outlineVariant = Color(0xFFE5E5E5),
                scrim = Color.Black
            )
        }
    } else {
        if (darkTheme) {
            if (pureBlackMode) {
                baseColorScheme.copy(
                    background = Color.Black,
                    surface = Color.Black,
                    surfaceVariant = Color(0xFF121212)
                )
            } else {
                baseColorScheme
            }
        } else {
            if (pureWhiteMode) {
                baseColorScheme.copy(
                    background = Color.White,
                    surface = Color.White,
                    surfaceVariant = Color(0xFFF1F5F9), // Light slate/gray background variant for items
                    onBackground = Color(0xFF0F172A), // Slate 900 for premium high contrast
                    onSurface = Color(0xFF0F172A),
                    onSurfaceVariant = Color(0xFF334155), // Slate 700 for subtext contrast
                    outline = Color(0xFFE2E8F0),
                    outlineVariant = Color(0xFFF1F5F9)
                )
            } else {
                baseColorScheme
            }
        }
    }

    val animatedColorScheme = animateColorScheme(colorScheme)

    MaterialTheme(
        colorScheme = animatedColorScheme,
        typography = Typography(),
        content = content
    )
}

fun Color.contrastingTextColor(): Color {
    // Relative luminance calculation according to WCAG 2.1
    val lum = 0.2126f * red + 0.7152f * green + 0.0722f * blue
    return if (lum > 0.45f) Color(0xFF1E293B) else Color.White
}

@Composable
fun MorphingButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    restRadius: androidx.compose.ui.unit.Dp = 24.dp,
    pressedRadius: androidx.compose.ui.unit.Dp = 8.dp,
    pressedScale: Float = 0.92f,
    containerColor: Color = MaterialTheme.colorScheme.primary,
    contentColor: Color = MaterialTheme.colorScheme.onPrimary,
    border: androidx.compose.foundation.BorderStroke? = null,
    content: @Composable RowScope.() -> Unit
) {
    val coroutineScope = androidx.compose.runtime.rememberCoroutineScope()
    var isImpulseActive by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
    val interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val isMorphActive = isPressed || isImpulseActive

    val cornerRadius by androidx.compose.animation.core.animateDpAsState(
        targetValue = if (isMorphActive) pressedRadius else restRadius,
        animationSpec = androidx.compose.animation.core.spring(dampingRatio = 0.52f, stiffness = 420f),
        label = "expressive_corner_morph"
    )
    val scale by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (isMorphActive) pressedScale else 1.0f,
        animationSpec = androidx.compose.animation.core.spring(dampingRatio = 0.52f, stiffness = 420f),
        label = "expressive_scale_morph"
    )

    val shape = androidx.compose.foundation.shape.RoundedCornerShape(cornerRadius.coerceAtLeast(0.dp))
    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(shape)
            .background(containerColor)
            .then(if (border != null) Modifier.border(border, shape) else Modifier)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = {
                    coroutineScope.launch {
                        isImpulseActive = true
                        kotlinx.coroutines.delay(130)
                        isImpulseActive = false
                    }
                    onClick()
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        androidx.compose.runtime.CompositionLocalProvider(
            androidx.compose.material3.LocalContentColor provides contentColor
        ) {
            Row(
                Modifier
                    .defaultMinSize(minWidth = 64.dp, minHeight = 40.dp)
                    .padding(horizontal = 24.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
                content = content
            )
        }
    }
}

