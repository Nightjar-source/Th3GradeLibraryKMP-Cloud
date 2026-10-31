package com.Nightjar.Th3GradeLibraryKMP.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.animation.core.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.Nightjar.Th3GradeLibraryKMP.theme.liquidGlass
import com.Nightjar.Th3GradeLibraryKMP.theme.KufiReemFontFamily
import com.Nightjar.Th3GradeLibraryKMP.theme.LocalIsLowEndDevice
import com.Nightjar.Th3GradeLibraryKMP.theme.popInOnInitialLoad
import com.Nightjar.Th3GradeLibraryKMP.theme.screenEntranceCascade
import org.jetbrains.compose.resources.painterResource
import com.Nightjar.Th3GradeLibraryKMP.generated.resources.Res
import com.Nightjar.Th3GradeLibraryKMP.generated.resources.app_icon

@Composable
fun AboutScreen(
    onOpenUrl: (String) -> Unit,
    isDark: Boolean,
    bottomPadding: androidx.compose.ui.unit.Dp = 130.dp,
    onScrollableStateChanged: (Boolean) -> Unit = {},
    scrollState: androidx.compose.foundation.ScrollState = androidx.compose.foundation.rememberScrollState()
) {
    val canScroll by remember {
        derivedStateOf {
            scrollState.maxValue > 0 && scrollState.maxValue < Int.MAX_VALUE
        }
    }
    LaunchedEffect(canScroll) {
        onScrollableStateChanged(canScroll)
    }

    BoxWithConstraints(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        val screenWidthDp = with(androidx.compose.ui.platform.LocalDensity.current) { androidx.compose.ui.platform.LocalWindowInfo.current.containerSize.width.toDp() }
        val isWide = screenWidthDp >= 600.dp
        
        val statusBarHeight = WindowInsets.statusBars.add(WindowInsets.captionBar).asPaddingValues().calculateTopPadding()
        val dynamicTopPadding = if (isWide) statusBarHeight + 64.dp else statusBarHeight + 76.dp

        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 800.dp)
                .padding(horizontal = 16.dp)
                .elasticOverscroll()
                .verticalScroll(scrollState),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(dynamicTopPadding))

            if (isWide) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    AppMessageCard(modifier = Modifier.weight(1f).screenEntranceCascade(0), isDark = isDark)
                    DeveloperCard(modifier = Modifier.weight(1f).screenEntranceCascade(1), onOpenUrl = onOpenUrl)
                }
            } else {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    AppMessageCard(modifier = Modifier.fillMaxWidth().screenEntranceCascade(0), isDark = isDark)
                    DeveloperCard(modifier = Modifier.fillMaxWidth().screenEntranceCascade(1), onOpenUrl = onOpenUrl)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Telegram Official Community Card
            TelegramCommunityCard(
                modifier = Modifier.fillMaxWidth().screenEntranceCascade(2),
                onOpenUrl = onOpenUrl,
                isDark = isDark
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Golden Spiritual Card
            GoldenSpiritualCard(modifier = Modifier.fillMaxWidth().screenEntranceCascade(3), isDark = isDark)

            Spacer(modifier = Modifier.height(bottomPadding))
        }
    }
}

@Composable
fun AppMessageCard(modifier: Modifier = Modifier, isDark: Boolean) {
    Box(
        modifier = modifier
            .liquidGlass(isDark, borderRadius = 32.dp, alpha = 0.5f)
            .padding(24.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(Color.White.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(Res.drawable.app_icon),
                    contentDescription = null,
                    modifier = Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(14.dp))
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "رسالة التطبيق",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "تم تطوير هذا التطبيق لطلاب وطالبات الصف الثالث متوسط، ليكون الرفيق الدائم في رحلتهم الدراسية وتسهيل الوصول للمنهج العراقي بكل يسر وسهولة.",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Justify,
                lineHeight = 22.sp
            )
        }
    }
}

@Composable
fun DeveloperCard(modifier: Modifier = Modifier, onOpenUrl: (String) -> Unit) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(32.dp))
            .background(
                Brush.linearGradient(
                    listOf(Color(0xFF0F172A), Color(0xFF1E293B))
                )
            )
            .padding(24.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = Color(0xFFA5B4FC),
                        modifier = Modifier.size(32.dp)
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column {
                    Text(
                        text = "المطور",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFA5B4FC)
                    )
                    Text(
                        text = "عبودي",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.Black.copy(alpha = 0.2f))
                    .padding(16.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "للاتصال بنا، أو للإبلاغ عن خطأ راسلنا على تليغرام:",
                        fontSize = 13.sp,
                        color = Color(0xFFCBD5E1),
                        textAlign = TextAlign.Justify
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = { onOpenUrl("https://t.me/iraqitrb") },
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF6366F1)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Message,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("مراسلة عبر تليغرام", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

/**
 * Compact golden spiritual card with clean Glassmorphism background and sweeping golden glittering calligraphy.
 */
@Composable
fun GoldenSpiritualCard(modifier: Modifier = Modifier, isDark: Boolean = false) {
    val isLowEnd = LocalIsLowEndDevice.current
    val transition = rememberInfiniteTransition(label = "spiritual_gold_shimmer")
    
    val shimmerOffset by if (!isLowEnd) {
        transition.animateFloat(
            initialValue = -350f,
            targetValue = 900f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 2800, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "gold_shimmer_offset"
        )
    } else remember { mutableStateOf(200f) }

    val goldenShimmerBrush = if (!isLowEnd) {
        Brush.linearGradient(
            colors = listOf(
                if (isDark) Color(0xFFFFD54F) else Color(0xFFC58E00),
                Color(0xFFFFF9C4),
                Color.White.copy(alpha = 0.98f),
                if (isDark) Color(0xFFFFB300) else Color(0xFFD48800),
                if (isDark) Color(0xFFFFD54F) else Color(0xFFC58E00)
            ),
            start = Offset(shimmerOffset, 0f),
            end = Offset(shimmerOffset + 380f, 60f)
        )
    } else {
        Brush.linearGradient(
            colors = listOf(
                if (isDark) Color(0xFFFFD54F) else Color(0xFFC58E00),
                if (isDark) Color(0xFFFFB300) else Color(0xFFD48800)
            )
        )
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .liquidGlass(isDark, borderRadius = 24.dp, alpha = 0.55f)
            .padding(horizontal = 20.dp, vertical = 18.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "بِاسْمِ الْعَلِيِّ الْعَظِيمِ، وَقُلْ هُوَ اللَّهُ أَحَدٌ، وَلَا حَوْلَ وَلَا قُوَّةَ إِلَّا بِاللَّهِ الْعَلِيِّ الْعَظِيمِ",
            style = MaterialTheme.typography.titleSmall.copy(
                fontWeight = FontWeight.Bold,
                fontFamily = KufiReemFontFamily,
                fontSize = 14.5.sp,
                lineHeight = 23.sp,
                brush = goldenShimmerBrush
            ),
            textAlign = TextAlign.Justify
        )
    }
}

@Composable
fun TelegramCommunityCard(modifier: Modifier = Modifier, onOpenUrl: (String) -> Unit, isDark: Boolean) {
    Box(
        modifier = modifier
            .liquidGlass(isDark, borderRadius = 28.dp, alpha = 0.65f)
            .padding(20.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF2AABEE).copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Message,
                        contentDescription = null,
                        tint = Color(0xFF2AABEE),
                        modifier = Modifier.size(26.dp)
                    )
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column {
                    Text(
                        text = "قنوات ومجتمع تليغرام الرسمي",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "المصدر المعتمد للملازم والكتب والأخبار الوزارية",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = { onOpenUrl("https://t.me/bookiraqi") },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF2AABEE),
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.weight(1f).height(46.dp)
                ) {
                    Text("قناة الكتب @bookiraqi", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = { onOpenUrl("https://t.me/Bookiraqbot") },
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = if (isDark) Color(0xFF81D4FA) else Color(0xFF0288D1)
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.weight(1f).height(46.dp)
                ) {
                    Text("البوت @Bookiraqbot", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
