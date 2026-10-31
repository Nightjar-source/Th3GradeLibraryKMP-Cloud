package com.Nightjar.Th3GradeLibraryKMP.ui

import com.Nightjar.Th3GradeLibraryKMP.theme.expressiveButtonMorph
import com.Nightjar.Th3GradeLibraryKMP.theme.contrastingTextColor
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.border
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.Nightjar.Th3GradeLibraryKMP.data.AppSettings
import com.Nightjar.Th3GradeLibraryKMP.theme.parseHexColor
import com.Nightjar.Th3GradeLibraryKMP.theme.liquidGlass

import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.composed
import androidx.compose.ui.layout.layout

fun Modifier.horizontalFadingEdges(
    scrollState: androidx.compose.foundation.ScrollState,
    length: Dp = 16.dp
): Modifier = composed {
    val density = androidx.compose.ui.platform.LocalDensity.current
    val lengthPx = with(density) { length.toPx() }
    
    this
        .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
        .drawWithContent {
            drawContent()
            val fraction = (lengthPx / size.width).coerceIn(0f, 0.5f)
            drawRect(
                brush = Brush.horizontalGradient(
                    0.0f to Color.Transparent,
                    fraction to Color.Black,
                    (1f - fraction) to Color.Black,
                    1.0f to Color.Transparent,
                    startX = 0f,
                    endX = size.width
                ),
                blendMode = BlendMode.DstIn
            )
        }
}

fun getGradientForColor(colorHex: String): Brush {
    return when (colorHex) {
        "#ef4444" -> Brush.linearGradient(listOf(Color(0xFFEF4444), Color(0xFFB91C1C))) // Red
        "#f472b6" -> Brush.linearGradient(listOf(Color(0xFFF472B6), Color(0xFFDB2777))) // Pink
        "#22c55e" -> Brush.linearGradient(listOf(Color(0xFF22C55E), Color(0xFF15803D))) // Green
        "#eab308" -> Brush.linearGradient(listOf(Color(0xFFEAB308), Color(0xFFA16207))) // Gold
        "#800000" -> Brush.linearGradient(listOf(Color(0xFF800000), Color(0xFF4A0000))) // Maroon
        "#FFD700" -> Brush.linearGradient(listOf(Color(0xFFFFD700), Color(0xFFB8860B))) // Gold New
        "#C0C0C0" -> Brush.linearGradient(listOf(Color(0xFFC0C0C0), Color(0xFF808080))) // Silver
        "#00BCD4" -> Brush.linearGradient(listOf(Color(0xFF00BCD4), Color(0xFF0097A7))) // Cyan
        "#FF5722" -> Brush.linearGradient(listOf(Color(0xFFFF5722), Color(0xFFE64A19))) // Deep Orange
        "#673AB7" -> Brush.linearGradient(listOf(Color(0xFF673AB7), Color(0xFF512DA8))) // Deep Indigo
        "#795548" -> Brush.linearGradient(listOf(Color(0xFF795548), Color(0xFF5D4037))) // Brown
        "#009688" -> Brush.linearGradient(listOf(Color(0xFF009688), Color(0xFF00796B))) // Teal
        "#CDDC39" -> Brush.linearGradient(listOf(Color(0xFFCDDC39), Color(0xFFAFB42B))) // Lime
        "#9C27B0" -> Brush.linearGradient(listOf(Color(0xFF9C27B0), Color(0xFF7B1FA2))) // Purple
        "#2563eb" -> Brush.linearGradient(listOf(Color(0xFF2563EB), Color(0xFF3B82F6))) // Royal Blue gradient
        "#ec4899" -> Brush.linearGradient(listOf(Color(0xFFEC4899), Color(0xFF3B82F6))) // Cloudy Pink-Blue
        "#06b6d4" -> Brush.linearGradient(listOf(Color(0xFF06B6D4), Color(0xFFEC4899))) // Sky Pink-Blue
        "#1d4ed8" -> Brush.linearGradient(listOf(Color(0xFF2563EB), Color(0xFF10B981))) // Royal-Emerald
        "#10b981" -> Brush.linearGradient(listOf(Color(0xFF10B981), Color(0xFF14B8A6))) // Emerald Green
        "#f59e0b" -> Brush.linearGradient(listOf(Color(0xFFF59E0B), Color(0xFFEF4444))) // Gold/Sunset
        "#8b5cf6" -> Brush.linearGradient(listOf(Color(0xFF8B5CF6), Color(0xFFEC4899))) // Deep Purple
        else -> Brush.linearGradient(listOf(parseHexColor(colorHex), parseHexColor(colorHex))) // Solid
    }
}

@Composable
fun AppSettingsModal(
    isOpen: Boolean,
    onClose: () -> Unit,
    settings: AppSettings,
    onUpdateSettings: (AppSettings) -> Unit
) {
    val colors = listOf(
        "#2563eb", // Royal Blue
        "#ec4899", // Cloudy Pink-Blue
        "#06b6d4", // Sky Pink-Blue
        "#1d4ed8", // Royal-Emerald
        "#10b981", // Emerald Green
        "#f59e0b", // Gold/Sunset
        "#8b5cf6", // Deep Purple
        "#ef4444", // Red
        "#f472b6", // Pink
        "#22c55e", // Green
        "#eab308", // Gold
        "#800000", // Maroon
        "#FFD700", // Gold New
        "#C0C0C0", // Silver
        "#00BCD4", // Cyan
        "#FF5722", // Deep Orange
        "#673AB7", // Deep Indigo
        "#795548", // Brown
        "#009688", // Teal
        "#CDDC39", // Lime
        "#9C27B0"  // Purple
    )

    AnimatedVisibility(
        visible = isOpen,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = Modifier.zIndex(300f)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.5f))
                .clickable { onClose() },
            contentAlignment = Alignment.BottomCenter
        ) {
            Box(
                modifier = Modifier
                    .widthIn(max = 480.dp) // Responsive capsule for tablets/DeX
                    .fillMaxWidth()
                    .clickable(enabled = false) {}
                    .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .animateEnterExit(
                        enter = slideInVertically(
                            initialOffsetY = { it },
                            animationSpec = androidx.compose.animation.core.spring(dampingRatio = 0.8f, stiffness = androidx.compose.animation.core.Spring.StiffnessLow)
                        ),
                        exit = slideOutVertically(
                            targetOffsetY = { it },
                            animationSpec = androidx.compose.animation.core.spring(dampingRatio = 0.9f, stiffness = androidx.compose.animation.core.Spring.StiffnessMedium)
                        )
                    )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(top = 16.dp, start = 20.dp, end = 20.dp) // Compact padding
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .windowInsetsPadding(WindowInsets.statusBars.add(WindowInsets.captionBar)), // Prevent overlap with status bar and caption bar
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "المظهر واللون",
                            fontSize = 18.sp, // Compact title
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        IconButton(
                            onClick = onClose,
                            modifier = Modifier
                                .size(36.dp) // Compact close button
                                .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Close", modifier = Modifier.size(20.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp)) // Compact height

                    // Theme Settings
                    Text("وضع الإضاءة", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(horizontal = 20.dp))
                    Spacer(modifier = Modifier.height(8.dp))
                    BoxWithConstraints(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp)
                            .height(48.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
                    ) {
                        val options = listOf("light", "dark", "system")
                        val selectedIndex = options.indexOf(settings.theme).takeIf { it >= 0 } ?: 2

                        val tabWidth = maxWidth / 3
                        val indicatorOffset by androidx.compose.animation.core.animateDpAsState(
                            targetValue = tabWidth * selectedIndex,
                            animationSpec = androidx.compose.animation.core.spring(
                                dampingRatio = androidx.compose.animation.core.Spring.DampingRatioNoBouncy,
                                stiffness = androidx.compose.animation.core.Spring.StiffnessMedium
                            ),
                            label = "theme_indicator_offset"
                        )

                        // Sliding Indicator
                        Box(
                            modifier = Modifier
                                .offset(x = indicatorOffset)
                                .width(tabWidth)
                                .fillMaxHeight()
                                .padding(4.dp)
                                .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(10.dp))
                        )

                        Row(modifier = Modifier.fillMaxSize()) {
                            ThemeOptionButton(
                                label = "فاتح",
                                isSelected = settings.theme == "light",
                                onClick = { onUpdateSettings(settings.copy(theme = "light")) },
                                modifier = Modifier.weight(1f).fillMaxHeight()
                            )
                            ThemeOptionButton(
                                label = "مظلم",
                                isSelected = settings.theme == "dark",
                                onClick = { onUpdateSettings(settings.copy(theme = "dark")) },
                                modifier = Modifier.weight(1f).fillMaxHeight()
                            )
                            ThemeOptionButton(
                                label = "تلقائي",
                                isSelected = settings.theme == "system",
                                onClick = { onUpdateSettings(settings.copy(theme = "system")) },
                                modifier = Modifier.weight(1f).fillMaxHeight()
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                    Spacer(modifier = Modifier.height(16.dp))

                    val isSystemDark = androidx.compose.foundation.isSystemInDarkTheme()
                    val isDarkThemeActive = settings.theme == "dark" || (settings.theme == "system" && isSystemDark)
                    val isLightThemeActive = settings.theme == "light" || (settings.theme == "system" && !isSystemDark)

                    // Pure White Mode (Light only)
                    AnimatedVisibility(
                        visible = isLightThemeActive,
                        enter = expandVertically(spring(dampingRatio = 0.8f, stiffness = 400f)) + fadeIn(),
                        exit = shrinkVertically(spring(dampingRatio = 0.8f, stiffness = 400f)) + fadeOut()
                    ) {
                        Column {
                            Text("الوضع الأبيض الصافي", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(horizontal = 20.dp))
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                "استخدام خلفية بيضاء ناصعة تماماً للوضع الفاتح لتسهيل القراءة وتوفير مظهر فخم.",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 20.dp)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 20.dp)
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                                    .padding(horizontal = 16.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("تفعيل الأبيض الصافي", fontWeight = FontWeight.Medium, fontSize = 13.sp)
                                Switch(
                                    checked = settings.pureWhiteMode,
                                    onCheckedChange = { onUpdateSettings(settings.copy(pureWhiteMode = it)) }
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))
                            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    }

                    // Pure Black Mode (Dark only)
                    AnimatedVisibility(
                        visible = isDarkThemeActive,
                        enter = expandVertically(spring(dampingRatio = 0.8f, stiffness = 400f)) + fadeIn(),
                        exit = shrinkVertically(spring(dampingRatio = 0.8f, stiffness = 400f)) + fadeOut()
                    ) {
                        Column {
                            Text("الوضع الأسود النقي", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(horizontal = 20.dp))
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                "استخدام لون أسود صرف للخلفيات لتوفير طاقة البطارية في شاشات OLED وإعطاء مظهر فخم.",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 20.dp)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 20.dp)
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                                    .padding(horizontal = 16.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("تفعيل الأسود النقي", fontWeight = FontWeight.Medium, fontSize = 13.sp)
                                Switch(
                                    checked = settings.pureBlackMode,
                                    onCheckedChange = { onUpdateSettings(settings.copy(pureBlackMode = it)) }
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))
                            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    }

                    // Material You
                    Text("ألوان متريال ديزاين 3", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(horizontal = 20.dp))
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        "استخراج الألوان من نظام أندرويد 12+ (Dynamic Colors) لتعطي تباين وتناسق مريح للعين.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 20.dp)
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("تفعيل الألوان الديناميكية", fontWeight = FontWeight.Medium, fontSize = 13.sp)
                        Switch(
                            checked = settings.useMaterialYou,
                            onCheckedChange = { onUpdateSettings(settings.copy(useMaterialYou = it)) }
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Manual color options
                    AnimatedVisibility(
                        visible = !settings.useMaterialYou,
                        enter = expandVertically(
                            animationSpec = spring(
                                dampingRatio = 0.82f,
                                stiffness = androidx.compose.animation.core.Spring.StiffnessLow
                            ),
                            clip = false
                        ) + fadeIn(androidx.compose.animation.core.tween(300)),
                        exit = shrinkVertically(
                            animationSpec = spring(
                                dampingRatio = 0.85f,
                                stiffness = androidx.compose.animation.core.Spring.StiffnessLow
                            ),
                            clip = false
                        ) + fadeOut(androidx.compose.animation.core.tween(220))
                    ) {
                        Column {
                            Text("اختر لونك المفضل يدوياً:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(horizontal = 20.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            val colorScrollState = rememberScrollState()
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalFadingEdges(colorScrollState, 24.dp)
                                    .horizontalScroll(colorScrollState)
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Spacer(modifier = Modifier.width(6.dp))
                                colors.forEach { colorHex ->
                                    val isSelected = settings.primaryColor == colorHex
                                    val gradient = getGradientForColor(colorHex)
                                    val colorInteractionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
                                    Box(
                                        modifier = Modifier
                                            .size(38.dp) // Compact size
                                            .expressiveButtonMorph(
                                                restRadius = 19.dp,
                                                pressedRadius = 8.dp,
                                                pressedScale = 0.88f,
                                                interactionSource = colorInteractionSource
                                            )
                                            .background(gradient)
                                            .border(
                                                width = if (isSelected) 3.dp else 1.dp,
                                                color = if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                                                shape = RoundedCornerShape(19.dp)
                                            )
                                            .clickable(
                                                interactionSource = colorInteractionSource,
                                                indication = null
                                            ) { onUpdateSettings(settings.copy(primaryColor = colorHex)) }
                                    )
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(20.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                    Spacer(modifier = Modifier.height(16.dp))

                    // --- Downloads & Offline Management Section ---
                    Text(
                        "إدارة التنزيلات والمحتوى الأوفلاين",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 20.dp)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        "التحكم في التنزيل التلقائي لجميع المقررات أو تنزيلها دفعة واحدة لتعمل بدون إنترنت.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 20.dp)
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    // Switch: Auto Download On Update
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                            Text("تنزيل المقررات تلقائياً عند التحديث", fontWeight = FontWeight.Medium, fontSize = 13.sp)
                            Text("تحميل الكتب والملازم الجديدة في الخلفية تلقائياً", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(
                            checked = settings.autoDownloadOnUpdate,
                            onCheckedChange = { onUpdateSettings(settings.copy(autoDownloadOnUpdate = it)) }
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Button: Download All Books and Notes Now
                    val platformContext = coil3.compose.LocalPlatformContext.current
                    val isBatchDownloading by com.Nightjar.Th3GradeLibraryKMP.network.BookDownloadManager.isBatchDownloading.collectAsState()
                    val batchProgress by com.Nightjar.Th3GradeLibraryKMP.network.BookDownloadManager.batchProgress.collectAsState()

                    Button(
                        onClick = {
                            if (!isBatchDownloading) {
                                val all = com.Nightjar.Th3GradeLibraryKMP.data.AllItems.books + com.Nightjar.Th3GradeLibraryKMP.data.AllItems.notes
                                com.Nightjar.Th3GradeLibraryKMP.network.BookDownloadManager.downloadAll(platformContext, all)
                                com.Nightjar.Th3GradeLibraryKMP.ui.ToastManager.showToast("بدأ تنزيل المناهج بالكامل للعمل أوفلاين...")
                            } else {
                                com.Nightjar.Th3GradeLibraryKMP.network.BookDownloadManager.cancelAll()
                                com.Nightjar.Th3GradeLibraryKMP.ui.ToastManager.showToast("تم إيقاف التنزيل الجماعي")
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp)
                            .height(48.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isBatchDownloading) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                            Icon(
                                imageVector = if (isBatchDownloading) Icons.Default.Close else Icons.Default.Download,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isBatchDownloading) "إلغاء التنزيل الجماعي (${(batchProgress * 100).toInt()}%)" else "تحميل كافة الكتب والملازم الآن (أوفلاين)",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.navigationBarsPadding().height(16.dp))
                } // Close Column


            }
        }
    }
}

@Composable
fun NewsSettingsModal(
    isOpen: Boolean,
    onClose: () -> Unit,
    settings: AppSettings,
    onUpdateSettings: (AppSettings) -> Unit,
    onRequestClearCache: () -> Unit
) {

    AnimatedVisibility(
        visible = isOpen,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = Modifier.zIndex(300f)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.5f))
                .clickable { onClose() },
            contentAlignment = Alignment.BottomCenter
        ) {
            Box(
                modifier = Modifier
                    .widthIn(max = 480.dp) // Responsive capsule for tablets/DeX
                    .fillMaxWidth()
                    .clickable(enabled = false) {}
                    .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .animateEnterExit(
                        enter = slideInVertically(
                            initialOffsetY = { it },
                            animationSpec = androidx.compose.animation.core.spring(dampingRatio = 0.8f, stiffness = androidx.compose.animation.core.Spring.StiffnessLow)
                        ),
                        exit = slideOutVertically(
                            targetOffsetY = { it },
                            animationSpec = androidx.compose.animation.core.spring(dampingRatio = 0.9f, stiffness = androidx.compose.animation.core.Spring.StiffnessMedium)
                        )
                    )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .animateContentSize()
                        .verticalScroll(rememberScrollState())
                        .padding(top = 16.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp)
                            .windowInsetsPadding(WindowInsets.statusBars.add(WindowInsets.captionBar)), // Prevent overlap with status bar and caption bar
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "إعدادات الأخبار والمزامنة",
                            fontSize = 18.sp, // Compact title
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        IconButton(
                            onClick = onClose,
                            modifier = Modifier
                                .size(36.dp) // Compact close button
                                .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Close", modifier = Modifier.size(20.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    val isBatteryIgnored = remember { com.Nightjar.Th3GradeLibraryKMP.network.SyncEngine.platformActionHandler?.isBatteryOptimizationIgnored() ?: false }
                    var batteryStateIgnored by remember { mutableStateOf(isBatteryIgnored) }

                    val isAutoRevokeExempt = remember { com.Nightjar.Th3GradeLibraryKMP.network.SyncEngine.platformActionHandler?.isAutoRevokeWhitelisted() ?: false }
                    var autoRevokeIgnored by remember { mutableStateOf(isAutoRevokeExempt) }

                    LaunchedEffect(Unit) {
                        while (true) {
                            batteryStateIgnored = com.Nightjar.Th3GradeLibraryKMP.network.SyncEngine.platformActionHandler?.isBatteryOptimizationIgnored() ?: false
                            autoRevokeIgnored = com.Nightjar.Th3GradeLibraryKMP.network.SyncEngine.platformActionHandler?.isAutoRevokeWhitelisted() ?: false
                            kotlinx.coroutines.delay(2000)
                        }
                    }

                    AnimatedVisibility(
                        visible = !batteryStateIgnored || !autoRevokeIgnored,
                        enter = expandVertically(spring(dampingRatio = 0.8f, stiffness = 400f)) + fadeIn(),
                        exit = shrinkVertically(spring(dampingRatio = 0.8f, stiffness = 400f)) + fadeOut()
                    ) {
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f)
                            ),
                            shape = RoundedCornerShape(16.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.4f))
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                horizontalAlignment = Alignment.Start
                            ) {
                                Text(
                                    text = "حماية التطبيق من إيقاف النظام ⚠️",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.error
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "لضمان عمل إشعارات وجلب الأخبار في وقتها المحدد، يرجى تفعيل الميزات أدناه:",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    lineHeight = 16.sp
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                
                                AnimatedVisibility(
                                    visible = !batteryStateIgnored,
                                    enter = expandVertically() + fadeIn(),
                                    exit = shrinkVertically() + fadeOut()
                                ) {
                                    // تخطي الخمول
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text("1. تخطي الخمول للبطارية", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                            Text("يمنع تقييد التطبيق عند إطفاء الشاشة.", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Button(
                                            onClick = {
                                                com.Nightjar.Th3GradeLibraryKMP.network.SyncEngine.platformActionHandler?.showToast("يرجى اختيار 'بدون قيود' أو 'عدم التحسين' للتطبيق")
                                                com.Nightjar.Th3GradeLibraryKMP.network.SyncEngine.platformActionHandler?.requestBatteryOptimizationExemption()
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                            shape = RoundedCornerShape(10.dp),
                                            contentPadding = PaddingValues(horizontal = 8.dp)
                                        ) {
                                            Text("تفعيل", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                        }
                                    }
                                }
                                
                                AnimatedVisibility(
                                    visible = !batteryStateIgnored && !autoRevokeIgnored,
                                    enter = expandVertically() + fadeIn(),
                                    exit = shrinkVertically() + fadeOut()
                                ) {
                                    Column {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        HorizontalDivider(color = MaterialTheme.colorScheme.error.copy(alpha = 0.2f))
                                        Spacer(modifier = Modifier.height(8.dp))
                                    }
                                }
                                
                                AnimatedVisibility(
                                    visible = !autoRevokeIgnored,
                                    enter = expandVertically() + fadeIn(),
                                    exit = shrinkVertically() + fadeOut()
                                ) {
                                    // منع السبات
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text("2. منع إيقاف نشاط التطبيق", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                            Text("يمنع النظام من إيقاف الإشعارات والمزامنة.", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Button(
                                            onClick = {
                                                com.Nightjar.Th3GradeLibraryKMP.network.SyncEngine.platformActionHandler?.requestAutoRevokeExemption()
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                            shape = RoundedCornerShape(10.dp),
                                            contentPadding = PaddingValues(horizontal = 8.dp)
                                        ) {
                                            Text("إيقاف", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                        }
                                    }
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    // Sync intervals
                    Text("مزامنة الأخبار بالخلفية", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(horizontal = 20.dp))
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        "سيتم تحديث الأخبار ومزامنتها آلياً حسب التوقيت المناسب لك.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 20.dp)
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(horizontal = 20.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            OptionGridButton("15 دقيقة", settings.syncInterval == "15m", { onUpdateSettings(settings.copy(syncInterval = "15m")) }, Modifier.weight(1f))
                            OptionGridButton("نصف ساعة", settings.syncInterval == "30m", { onUpdateSettings(settings.copy(syncInterval = "30m")) }, Modifier.weight(1f))
                            OptionGridButton("كل ساعة", settings.syncInterval == "1h", { onUpdateSettings(settings.copy(syncInterval = "1h")) }, Modifier.weight(1f))
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            OptionGridButton("كل 12 ساعة", settings.syncInterval == "12h", { onUpdateSettings(settings.copy(syncInterval = "12h")) }, Modifier.weight(1f))
                            OptionGridButton("كل يوم", settings.syncInterval == "24h", { onUpdateSettings(settings.copy(syncInterval = "24h")) }, Modifier.weight(1f))
                        }
                        OptionGridButton("عند فتح التطبيق فقط", settings.syncInterval == "on_open", { onUpdateSettings(settings.copy(syncInterval = "on_open")) }, Modifier.fillMaxWidth())
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.padding(horizontal = 20.dp))
                    Spacer(modifier = Modifier.height(16.dp))

                    // Notification Sound
                    Text("التنبيهات والأصوات", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(horizontal = 20.dp))
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("تفعيل صوت الإشعار للأخبار الجديدة", fontWeight = FontWeight.Medium, fontSize = 13.sp)
                        Switch(
                            checked = settings.notificationSound,
                            onCheckedChange = { onUpdateSettings(settings.copy(notificationSound = it)) }
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.padding(horizontal = 20.dp))
                    Spacer(modifier = Modifier.height(16.dp))

                    // Cache intervals
                    Text("التخزين المؤقت وحذف الأخبار (الكاش)", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(horizontal = 20.dp))
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        "تجنب امتلاء ذاكرة الهاتف بحذف الأخبار القديمة تلقائياً.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 20.dp)
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(horizontal = 20.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            OptionGridButton("كل أسبوع", settings.cacheClearInterval == "7d", { onUpdateSettings(settings.copy(cacheClearInterval = "7d")) }, Modifier.weight(1f))
                            OptionGridButton("كل شهر", settings.cacheClearInterval == "1m", { onUpdateSettings(settings.copy(cacheClearInterval = "1m")) }, Modifier.weight(1f))
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            OptionGridButton("كل 3 أشهر", settings.cacheClearInterval == "3m", { onUpdateSettings(settings.copy(cacheClearInterval = "3m")) }, Modifier.weight(1f))
                            OptionGridButton("كل 6 أشهر", settings.cacheClearInterval == "6m", { onUpdateSettings(settings.copy(cacheClearInterval = "6m")) }, Modifier.weight(1f))
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            OptionGridButton("أبداً (لا تحذف)", settings.cacheClearInterval == "never", { onUpdateSettings(settings.copy(cacheClearInterval = "never")) }, Modifier.weight(1f))
                            com.Nightjar.Th3GradeLibraryKMP.theme.MorphingButton(
                                onClick = { onRequestClearCache() },
                                modifier = Modifier.weight(1f).height(46.dp),
                                restRadius = 14.dp,
                                pressedRadius = 6.dp,
                                pressedScale = 0.94f,
                                containerColor = MaterialTheme.colorScheme.errorContainer,
                                contentColor = MaterialTheme.colorScheme.errorContainer.contrastingTextColor(),
                                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.3f))
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("حذف الآن", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.navigationBarsPadding().height(16.dp))
                } // Close Column


            }
        }
    }
}



@Composable
fun ThemeOptionButton(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val textColor by androidx.compose.animation.animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.primary.contrastingTextColor() else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = androidx.compose.animation.core.tween(durationMillis = 220),
        label = "theme_text_color"
    )

    com.Nightjar.Th3GradeLibraryKMP.theme.MorphingButton(
        onClick = onClick,
        modifier = modifier.padding(2.dp),
        restRadius = 10.dp,
        pressedRadius = 4.dp,
        pressedScale = 0.95f,
        containerColor = Color.Transparent,
        contentColor = textColor,
        border = null
    ) {
        Text(
            text = label,
            fontSize = 14.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            textAlign = TextAlign.Justify
        )
    }
}

@Composable
fun OptionGridButton(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bg = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
    val border = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
    val contentColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.contrastingTextColor() else MaterialTheme.colorScheme.onSurface

    com.Nightjar.Th3GradeLibraryKMP.theme.MorphingButton(
        onClick = onClick,
        modifier = modifier,
        restRadius = 16.dp,
        pressedRadius = 6.dp,
        pressedScale = 0.95f,
        containerColor = bg,
        contentColor = contentColor,
        border = androidx.compose.foundation.BorderStroke(1.dp, border)
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Justify
        )
    }
}
