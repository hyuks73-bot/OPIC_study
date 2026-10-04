package com.opic.master.ui

import android.content.Context
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VerticalSplit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import coil.request.ImageRequest
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.opic.master.data.model.Sentence
import kotlinx.coroutines.flow.first
import java.util.Locale
import kotlin.math.abs
import kotlin.math.round

// =========================================================================
// DATA MODELS & PRECOMPILED REGEX UTILITIES (Optimized for Zero-GC in UI)
// =========================================================================

data class DayMeta(
    val key: String,
    val tabLabel: String,
    val title: String,
    val emoji: String
)

val DEFAULT_DAYS = listOf(
    DayMeta("day1", "Day 1", "서베이 & 2룸 아파트", "📋"),
    DayMeta("day2", "Day 2", "수변 공원 & 침실 묘사", "🌊"),
    DayMeta("day3", "Day 3", "주말 루틴 & 자전거", "🏃"),
    DayMeta("day4", "Day 4", "과거 경험 & 롤플레이", "🎸"),
    DayMeta("day5", "Day 5", "렌터카 & 가족 여행", "🚗"),
    DayMeta("day6", "Day 6", "휴일 루틴 & 홈캉스", "🏖️")
)

val NATURAL_DAY_COMPARATOR = Comparator<String> { a, b ->
    val numA = a.filter { it.isDigit() }.toIntOrNull() ?: 0
    val numB = b.filter { it.isDigit() }.toIntOrNull() ?: 0
    if (numA != numB) numA.compareTo(numB) else a.compareTo(b)
}

private val HTML_TAG_REGEX = Regex("<.*?>")
private val SLASH_TAG_REGEX = Regex("<span class=\"slash\">/</span>")
private val MULTI_SPACE_REGEX = Regex("\\s+")
private val NATURAL_ORDER_REGEX = Regex("^(.*?)(?:_|-|)(\\d+)$")

fun cleanSentenceText(text: String): String =
    text.replace(HTML_TAG_REGEX, "").trim()

fun cleanGuideText(guide: String): String =
    guide.replace(SLASH_TAG_REGEX, " / ")
        .replace(HTML_TAG_REGEX, " ")
        .replace(MULTI_SPACE_REGEX, " ")
        .trim()

val NATURAL_SENTENCE_COMPARATOR = Comparator<Sentence> { a, b ->
    if (a.orderIndex != b.orderIndex) {
        return@Comparator a.orderIndex.compareTo(b.orderIndex)
    }
    val matchA = NATURAL_ORDER_REGEX.find(a.id)
    val matchB = NATURAL_ORDER_REGEX.find(b.id)
    if (matchA != null && matchB != null) {
        val prefixComp = matchA.groupValues[1].compareTo(matchB.groupValues[1])
        if (prefixComp != 0) return@Comparator prefixComp
        val numA = matchA.groupValues[2].toIntOrNull() ?: 0
        val numB = matchB.groupValues[2].toIntOrNull() ?: 0
        val numComp = numA.compareTo(numB)
        if (numComp != 0) return@Comparator numComp
    }
    a.id.compareTo(b.id)
}

// =========================================================================
// REUSABLE UI COMPONENTS (Minimizing Redundant Layout Code)
// =========================================================================

@Composable
fun DaySelectorTabs(
    days: List<DayMeta>,
    selectedDay: String,
    onSelectDay: (String) -> Unit,
    modifier: Modifier = Modifier,
    horizontalPadding: Int = 12
) {
    LazyRow(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = horizontalPadding.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(days, key = { it.key }) { meta ->
            val isSelected = selectedDay == meta.key
            Surface(
                shape = CircleShape,
                color = if (isSelected) Color(0xFF6366F1) else Color(0xFF1E293B),
                modifier = Modifier.clickable { onSelectDay(meta.key) }
            ) {
                Text(
                    text = "${meta.emoji} ${meta.tabLabel}",
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                    color = if (isSelected) Color.White else Color(0xFF94A3B8),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
fun RepeatSpeedSettingButton(
    repeatCount: Int,
    repeatSpeeds: List<Float>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isVertical: Boolean = false
) {
    val speedSummary = remember(repeatSpeeds) {
        if (repeatSpeeds.isEmpty()) "1.0x"
        else {
            val min = repeatSpeeds.minOrNull() ?: 1.0f
            val max = repeatSpeeds.maxOrNull() ?: 1.0f
            if (min == max) "${min}x" else "${min}~${max}x"
        }
    }

    Surface(
        color = Color(0xFF1E293B),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, Color(0xFF475569)),
        modifier = modifier.clickable { onClick() }
    ) {
        if (isVertical) {
            Column(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "설정",
                        tint = Color(0xFF818CF8),
                        modifier = Modifier.size(13.dp)
                    )
                    Text(
                        text = "반복 ${repeatCount}회",
                        fontSize = 11.sp,
                        color = Color(0xFFFBBF24),
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    text = "⚡ $speedSummary",
                    fontSize = 10.sp,
                    color = Color(0xFFA5B4FC),
                    fontWeight = FontWeight.Bold
                )
            }
        } else {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "설정",
                    tint = Color(0xFF818CF8),
                    modifier = Modifier.size(15.dp)
                )
                Text(
                    text = "반복 ${repeatCount}회",
                    fontSize = 11.sp,
                    color = Color(0xFFFBBF24),
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "·",
                    fontSize = 11.sp,
                    color = Color(0xFF64748B)
                )
                Text(
                    text = "⚡ $speedSummary",
                    fontSize = 11.sp,
                    color = Color(0xFFA5B4FC),
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun PlaybackSettingsDialog(
    initialRepeatCount: Int,
    initialRepeatSpeeds: List<Float>,
    onDismiss: () -> Unit,
    onSave: (Int, List<Float>) -> Unit
) {
    var repeatCount by remember { mutableIntStateOf(initialRepeatCount.coerceIn(1, 10)) }
    var speeds by remember {
        mutableStateOf(
            if (initialRepeatSpeeds.size == initialRepeatCount) {
                initialRepeatSpeeds
            } else {
                List(initialRepeatCount) { idx ->
                    initialRepeatSpeeds.getOrElse(idx) { 1.0f }
                }
            }
        )
    }

    fun updateCount(newCount: Int) {
        val clamped = newCount.coerceIn(1, 10)
        repeatCount = clamped
        val lastSpeed = speeds.lastOrNull() ?: 1.0f
        speeds = if (clamped <= speeds.size) {
            speeds.take(clamped)
        } else {
            speeds + List(clamped - speeds.size) { lastSpeed }
        }
    }

    val availableSpeeds = listOf(0.7f, 0.8f, 0.9f, 1.0f, 1.1f, 1.2f, 1.3f, 1.5f)
    var isRepeatDropdownExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF0F172A),
        shape = RoundedCornerShape(20.dp),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    color = Color(0xFF312E81),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.size(32.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = null,
                            tint = Color(0xFFA5B4FC),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "반복 및 배속 상세 설정",
                        color = Color.White,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "회차별 맞춤 섀도잉 속도 조절",
                        color = Color(0xFF94A3B8),
                        fontSize = 11.sp
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                // 1. 반복 횟수 선택 (1~10 드롭다운)
                Text(
                    text = "🔁 반복 횟수 선택 (1 ~ 10회)",
                    color = Color(0xFFFBBF24),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))

                Box(modifier = Modifier.fillMaxWidth()) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { isRepeatDropdownExpanded = true },
                        color = Color(0xFF1E293B),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, Color(0xFF475569))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "문장당 ${repeatCount}회 반복",
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = null,
                                tint = Color(0xFFA5B4FC)
                            )
                        }
                    }

                    DropdownMenu(
                        expanded = isRepeatDropdownExpanded,
                        onDismissRequest = { isRepeatDropdownExpanded = false },
                        modifier = Modifier.background(Color(0xFF1E293B))
                    ) {
                        for (count in 1..10) {
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = "${count}회 반복${if (count == repeatCount) "  ✓" else ""}",
                                        color = if (count == repeatCount) Color(0xFFFBBF24) else Color.White,
                                        fontWeight = if (count == repeatCount) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                onClick = {
                                    updateCount(count)
                                    isRepeatDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // 2. 회차별 배속 설정 헤더 & 프리셋
                Text(
                    text = "⚡ 회차별 배속 지정",
                    color = Color(0xFF7DD3FC),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Quick Presets
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // 점진적 가속 프리셋
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                val stepSpeeds = listOf(0.8f, 0.9f, 1.0f, 1.1f, 1.2f, 1.3f, 1.5f, 1.5f, 1.5f, 1.5f)
                                speeds = List(repeatCount) { idx ->
                                    stepSpeeds.getOrElse(idx) { 1.2f }
                                }
                            },
                        color = Color(0xFF1E1B4B),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, Color(0xFF4338CA))
                    ) {
                        Text(
                            text = "📈 점진 가속",
                            color = Color(0xFFA5B4FC),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 6.dp),
                            textAlign = TextAlign.Center
                        )
                    }

                    // 1.0x 표준 통일
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                speeds = List(repeatCount) { 1.0f }
                            },
                        color = Color(0xFF064E3B),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, Color(0xFF059669))
                    ) {
                        Text(
                            text = "▶ 1.0x 통일",
                            color = Color(0xFF34D399),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 6.dp),
                            textAlign = TextAlign.Center
                        )
                    }

                    // 0.8x 정밀 청취 통일
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                speeds = List(repeatCount) { 0.8f }
                            },
                        color = Color(0xFF451A03),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, Color(0xFFD97706))
                    ) {
                        Text(
                            text = "🐢 0.8x 통일",
                            color = Color(0xFFFBBF24),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 6.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // List of iterations
                Surface(
                    color = Color(0xFF0B1120),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color(0xFF1E293B)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        for (i in 0 until repeatCount) {
                            val currentSpeed = speeds.getOrElse(i) { 1.0f }
                            var isSpeedMenuOpen by remember { mutableStateOf(false) }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFF1E293B), RoundedCornerShape(8.dp))
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Surface(
                                        color = when (i) {
                                            0 -> Color(0xFF0369A1)
                                            1 -> Color(0xFF4338CA)
                                            2 -> Color(0xFF7C3AED)
                                            else -> Color(0xFF334155)
                                        },
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text(
                                            text = "${i + 1}회차",
                                            color = Color.White,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                        )
                                    }
                                    val desc = when (i) {
                                        0 -> "첫 청취"
                                        1 -> "섀도잉"
                                        2 -> "발화 완성"
                                        else -> "심화 반복"
                                    }
                                    Text(
                                        text = desc,
                                        color = Color(0xFF94A3B8),
                                        fontSize = 11.sp
                                    )
                                }

                                Box {
                                    Surface(
                                        color = Color(0xFF0F172A),
                                        shape = RoundedCornerShape(6.dp),
                                        border = BorderStroke(1.dp, Color(0xFF4F46E5)),
                                        modifier = Modifier.clickable { isSpeedMenuOpen = true }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Text(
                                                text = "${currentSpeed}x",
                                                color = Color(0xFFA5B4FC),
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Icon(
                                                imageVector = Icons.Default.ArrowDropDown,
                                                contentDescription = null,
                                                tint = Color(0xFFA5B4FC),
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }

                                    DropdownMenu(
                                        expanded = isSpeedMenuOpen,
                                        onDismissRequest = { isSpeedMenuOpen = false },
                                        modifier = Modifier.background(Color(0xFF1E293B))
                                    ) {
                                        availableSpeeds.forEach { sp ->
                                            DropdownMenuItem(
                                                text = {
                                                    Text(
                                                        text = "${sp}x${if (sp == 1.0f) " (표준)" else ""}${if (sp == currentSpeed) "  ✓" else ""}",
                                                        color = if (sp == currentSpeed) Color(0xFF7DD3FC) else Color.White,
                                                        fontWeight = if (sp == currentSpeed) FontWeight.Bold else FontWeight.Normal
                                                    )
                                                },
                                                onClick = {
                                                    val newSpeeds = speeds.toMutableList()
                                                    if (i in newSpeeds.indices) {
                                                        newSpeeds[i] = sp
                                                    }
                                                    speeds = newSpeeds
                                                    isSpeedMenuOpen = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(repeatCount, speeds) },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1)),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("설정 적용", fontWeight = FontWeight.Bold, color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("취소", color = Color(0xFF94A3B8))
            }
        }
    )
}

@Composable
fun CoachingGuideCard(
    guide: String,
    tip: String,
    modifier: Modifier = Modifier,
    containerColor: Color = Color(0xFF0B1120),
    borderColor: Color = Color(0xFF1E3A5F),
    fontSize: TextUnit = 13.5.sp
) {
    val cleanGuide = remember(guide) { cleanGuideText(guide) }
    if (cleanGuide.isEmpty() && tip.isEmpty()) return

    Surface(
        color = containerColor,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, borderColor),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
            if (cleanGuide.isNotEmpty()) {
                Text(
                    text = buildAnnotatedString {
                        withStyle(SpanStyle(color = Color(0xFF38BDF8), fontWeight = FontWeight.Bold)) {
                            append("🗣️ 낭독·강세: ")
                        }
                        withStyle(SpanStyle(color = Color(0xFFE0F2FE), fontWeight = FontWeight.Normal)) {
                            append(cleanGuide)
                        }
                    },
                    fontSize = fontSize,
                    lineHeight = (fontSize.value * 1.45f).sp
                )
            }
            if (tip.isNotEmpty()) {
                if (cleanGuide.isNotEmpty()) Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = buildAnnotatedString {
                        withStyle(SpanStyle(color = Color(0xFFFBBF24), fontWeight = FontWeight.Bold)) {
                            append("💡 팁: ")
                        }
                        withStyle(SpanStyle(color = Color(0xFFCBD5E1), fontWeight = FontWeight.Normal)) {
                            append(tip)
                        }
                    },
                    fontSize = (fontSize.value - 0.5f).sp,
                    lineHeight = ((fontSize.value - 0.5f) * 1.45f).sp
                )
            }
        }
    }
}

/**
 * Visual mode toggle for coaching card area:
 * Allows user to switch between "🗣️ 낭독·강세" coaching guide and "🖼️ 이미지" scene illustration.
 */
enum class CoachingDisplayMode {
    PRONUNCIATION,
    IMAGE
}

@Composable
fun SentenceMediaCoachingSection(
    sentence: Sentence?,
    displayMode: CoachingDisplayMode,
    onDisplayModeChange: (CoachingDisplayMode) -> Unit,
    modifier: Modifier = Modifier,
    containerColor: Color = Color(0xFF0F2338),
    borderColor: Color = Color(0xFF0284C7).copy(alpha = 0.6f),
    fontSize: TextUnit = 17.sp,
    imageHeight: androidx.compose.ui.unit.Dp = 220.dp
) {
    val context = LocalContext.current
    val imageModel = remember(sentence?.id, sentence?.localImagePath, sentence?.imageUrl) {
        val local = sentence?.localImagePath
        if (!local.isNullOrEmpty() && java.io.File(local).exists()) {
            java.io.File(local)
        } else {
            // Check bundled APK asset first (e.g. file:///android_asset/images/{id}.jpg)
            val imgFileName = if (!sentence?.imageUrl.isNullOrEmpty()) {
                sentence.imageUrl.substringAfterLast("/")
            } else if (!sentence?.id.isNullOrEmpty()) {
                "${sentence.id}.jpg"
            } else ""

            if (imgFileName.isNotEmpty()) {
                "file:///android_asset/images/$imgFileName"
            } else {
                null
            }
        }
    }

    Surface(
        color = containerColor,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, borderColor),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            // Top Toggle Buttons: [🗣️ 낭독·강세] vs [🖼️ 연상 이미지]
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Pronunciation Tab Button
                    Surface(
                        color = if (displayMode == CoachingDisplayMode.PRONUNCIATION) Color(0xFF0284C7) else Color(0xFF1E293B),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(
                            1.dp,
                            if (displayMode == CoachingDisplayMode.PRONUNCIATION) Color(0xFF38BDF8) else Color(0xFF334155)
                        ),
                        modifier = Modifier.clickable { onDisplayModeChange(CoachingDisplayMode.PRONUNCIATION) }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.RecordVoiceOver,
                                contentDescription = null,
                                tint = if (displayMode == CoachingDisplayMode.PRONUNCIATION) Color.White else Color(0xFF94A3B8),
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = "낭독·강세",
                                color = if (displayMode == CoachingDisplayMode.PRONUNCIATION) Color.White else Color(0xFF94A3B8),
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Image Tab Button
                    Surface(
                        color = if (displayMode == CoachingDisplayMode.IMAGE) Color(0xFF6366F1) else Color(0xFF1E293B),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(
                            1.dp,
                            if (displayMode == CoachingDisplayMode.IMAGE) Color(0xFFA5B4FC) else Color(0xFF334155)
                        ),
                        modifier = Modifier.clickable { onDisplayModeChange(CoachingDisplayMode.IMAGE) }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Image,
                                contentDescription = null,
                                tint = if (displayMode == CoachingDisplayMode.IMAGE) Color.White else Color(0xFF94A3B8),
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = "연상 이미지",
                                color = if (displayMode == CoachingDisplayMode.IMAGE) Color.White else Color(0xFF94A3B8),
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Text(
                    text = if (displayMode == CoachingDisplayMode.PRONUNCIATION) "발화 가이드" else "상황 시각화",
                    color = Color(0xFF64748B),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Body Content based on active mode
            when (displayMode) {
                CoachingDisplayMode.PRONUNCIATION -> {
                    val cleanGuide = remember(sentence?.guide) { cleanGuideText(sentence?.guide ?: "") }
                    val tip = sentence?.tip ?: ""

                    if (cleanGuide.isNotEmpty()) {
                        Text(
                            text = buildAnnotatedString {
                                withStyle(SpanStyle(color = Color(0xFF38BDF8), fontWeight = FontWeight.Bold)) {
                                    append("🗣️ 낭독·강세: ")
                                }
                                withStyle(SpanStyle(color = Color(0xFFE0F2FE), fontWeight = FontWeight.Normal)) {
                                    append(cleanGuide)
                                }
                            },
                            fontSize = fontSize,
                            lineHeight = (fontSize.value * 1.45f).sp
                        )
                    }

                    if (tip.isNotEmpty()) {
                        if (cleanGuide.isNotEmpty()) Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = buildAnnotatedString {
                                withStyle(SpanStyle(color = Color(0xFFFBBF24), fontWeight = FontWeight.Bold)) {
                                    append("💡 팁: ")
                                }
                                withStyle(SpanStyle(color = Color(0xFFCBD5E1), fontWeight = FontWeight.Normal)) {
                                    append(tip)
                                }
                            },
                            fontSize = (fontSize.value - 0.5f).sp,
                            lineHeight = ((fontSize.value - 0.5f) * 1.45f).sp
                        )
                    }

                    if (cleanGuide.isEmpty() && tip.isEmpty()) {
                        Text(
                            text = "제공된 낭독 및 강세 가이드가 없습니다.",
                            color = Color(0xFF64748B),
                            fontSize = 12.sp
                        )
                    }
                }

                CoachingDisplayMode.IMAGE -> {
                    if (imageModel != null) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(imageHeight),
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFF020617),
                            border = BorderStroke(1.dp, Color(0xFF1E293B))
                        ) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                AsyncImage(
                                    model = ImageRequest.Builder(context)
                                        .data(imageModel)
                                        .crossfade(true)
                                        .build(),
                                    contentDescription = sentence?.id ?: "Sentence Illustration",
                                    contentScale = ContentScale.Fit,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(RoundedCornerShape(10.dp))
                                )
                            }
                        }
                    } else {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp),
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFF020617),
                            border = BorderStroke(1.dp, Color(0xFF1E293B))
                        ) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "등록된 연상 이미지가 없습니다.",
                                    color = Color(0xFF64748B),
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PlaybackRepeatProgressIndicator(
    currentRepeatIndex: Int,
    repeatTargetCount: Int,
    isPlaying: Boolean,
    modifier: Modifier = Modifier
) {
    val displayIndex = currentRepeatIndex.coerceIn(1, repeatTargetCount)
    Surface(
        color = if (isPlaying) Color(0xFF1E1B4B) else Color(0xFF0F172A),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(
            1.dp,
            if (isPlaying) Color(0xFF4F46E5) else Color(0xFF1E293B)
        ),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(if (isPlaying) Color(0xFF10B981) else Color(0xFF64748B))
                    )
                    Text(
                        text = if (isPlaying) "🔄 섀도잉 반복 진행 중" else "⏸️ 반복 대기 중",
                        color = if (isPlaying) Color(0xFF34D399) else Color(0xFF94A3B8),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Surface(
                    color = if (isPlaying) Color(0xFF4338CA) else Color(0xFF1E293B),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "현재 $displayIndex / $repeatTargetCount 회차",
                        color = if (isPlaying) Color.White else Color(0xFF94A3B8),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            // Step Indicator Chips (1..repeatTargetCount)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                for (step in 1..repeatTargetCount) {
                    val isCompleted = isPlaying && step < displayIndex
                    val isCurrent = isPlaying && step == displayIndex

                    Surface(
                        modifier = Modifier.weight(1f),
                        color = when {
                            isCurrent -> Color(0xFF6366F1)
                            isCompleted -> Color(0xFF064E3B)
                            else -> Color(0xFF1E293B)
                        },
                        shape = RoundedCornerShape(6.dp),
                        border = BorderStroke(
                            1.dp,
                            when {
                                isCurrent -> Color(0xFFA5B4FC)
                                isCompleted -> Color(0xFF059669)
                                else -> Color(0xFF334155)
                            }
                        )
                    ) {
                        Text(
                            text = when {
                                isCompleted -> "${step}회 ✓"
                                isCurrent -> "▶ ${step}회"
                                else -> "${step}회"
                            },
                            color = when {
                                isCurrent -> Color.White
                                isCompleted -> Color(0xFF34D399)
                                else -> Color(0xFF64748B)
                            },
                            fontSize = 10.sp,
                            fontWeight = if (isCurrent) FontWeight.ExtraBold else FontWeight.Medium,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DragSpeedSlider(
    currentSpeed: Float,
    onSpeedChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    var sliderValue by remember(currentSpeed) { mutableFloatStateOf(currentSpeed.coerceIn(0.5f, 2.0f)) }

    Surface(
        color = Color(0xFF0F172A),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, Color(0xFF1E293B)),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "⚡ 배속 드래그 조절",
                        color = Color(0xFF7DD3FC),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Surface(
                        color = Color(0xFF1E1B4B),
                        shape = RoundedCornerShape(6.dp),
                        border = BorderStroke(1.dp, Color(0xFF4338CA))
                    ) {
                        Text(
                            text = String.format(Locale.US, "%.1fx", sliderValue),
                            color = Color(0xFFA5B4FC),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.ExtraBold,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                        )
                    }
                }

                // Quick Presets
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf(0.8f, 1.0f, 1.2f, 1.5f).forEach { preset ->
                        Surface(
                            color = if (abs(sliderValue - preset) < 0.05f) Color(0xFF4338CA) else Color(0xFF1E293B),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.clickable {
                                sliderValue = preset
                                onSpeedChange(preset)
                            }
                        ) {
                            Text(
                                text = "${preset}x",
                                color = if (abs(sliderValue - preset) < 0.05f) Color.White else Color(0xFF94A3B8),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(2.dp))

            Slider(
                value = sliderValue,
                onValueChange = { newValue ->
                    val rounded = (round(newValue * 10f) / 10f).coerceIn(0.5f, 2.0f)
                    sliderValue = rounded
                    onSpeedChange(rounded)
                },
                valueRange = 0.5f..2.0f,
                steps = 14,
                colors = SliderDefaults.colors(
                    thumbColor = Color(0xFF818CF8),
                    activeTrackColor = Color(0xFF6366F1),
                    inactiveTrackColor = Color(0xFF1E293B)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(32.dp)
            )
        }
    }
}

// =========================================================================
// AUTO-SCROLL HELPER
// =========================================================================

/**
 * Smoothly scrolls so the item at [index] sits in the vertical center of the viewport.
 * If the item is off-screen it is first brought into view, then its actual measured
 * size is used for the centering step (card heights vary with the coaching box).
 */
suspend fun LazyListState.centerOnItem(index: Int) {
    if (layoutInfo.visibleItemsInfo.none { it.index == index }) {
        animateScrollToItem(index)
    }
    val info = layoutInfo
    val item = info.visibleItemsInfo.firstOrNull { it.index == index } ?: return
    val viewportCenter = (info.viewportStartOffset + info.viewportEndOffset) / 2
    val itemCenter = item.offset + item.size / 2
    val delta = (itemCenter - viewportCenter).toFloat()
    if (delta != 0f) animateScrollBy(delta)
}

// =========================================================================
// MAIN ADAPTIVE APP CONTAINER (Galaxy Fold 8 Screen Posture Handler)
// =========================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Fold8AdaptiveApp(
    sentences: List<Sentence>,
    days: List<DayMeta> = emptyList(),
    isUnfolded: Boolean,
    isFlexMode: Boolean,
    isPlaying: Boolean,
    currentPlayingId: String?,
    currentRepeatIndex: Int = 1,
    onPlaySentence: (Sentence, Int, List<Float>) -> Unit,
    onPlayAll: (List<Sentence>, Int, List<Float>) -> Unit,
    onTogglePlay: () -> Unit,
    onStop: () -> Unit,
    onUpdateSettings: (Int, List<Float>) -> Unit = { _, _ -> },
    onSetTemporarySpeed: (Float) -> Unit = {},
    isSyncing: Boolean = false,
    onSyncGitHub: () -> Unit
) {
    val availableDays = remember(days, sentences) {
        if (days.isNotEmpty()) {
            days
        } else if (sentences.isNotEmpty()) {
            val distinctKeys = sentences.map { it.dayKey }.distinct().sortedWith(NATURAL_DAY_COMPARATOR)
            distinctKeys.map { key ->
                DEFAULT_DAYS.find { it.key == key } ?: run {
                    val num = key.filter { it.isDigit() }
                    val label = if (num.isNotEmpty()) "Day $num" else key.uppercase()
                    DayMeta(key, label, "$label 학습 세트", "📖")
                }
            }
        } else {
            DEFAULT_DAYS
        }
    }

    val context = LocalContext.current
    val prefs = remember(context) { context.getSharedPreferences("opic_playback_prefs", Context.MODE_PRIVATE) }

    val savedRepeatCount = remember(prefs) { prefs.getInt("repeat_count", 3).coerceIn(1, 10) }
    val savedSpeeds = remember(prefs, savedRepeatCount) {
        val str = prefs.getString("repeat_speeds", null)
        if (!str.isNullOrEmpty()) {
            val list = str.split(",").mapNotNull { it.toFloatOrNull() }
            if (list.size == savedRepeatCount) list else List(savedRepeatCount) { 1.0f }
        } else {
            List(savedRepeatCount) { 1.0f }
        }
    }

    var selectedDay by remember { mutableStateOf(availableDays.firstOrNull()?.key ?: "day1") }
    var activeSentence by remember { mutableStateOf<Sentence?>(null) }
    var repeatCount by remember { mutableIntStateOf(savedRepeatCount) }
    var repeatSpeeds by remember { mutableStateOf(savedSpeeds) }
    var showCoaching by remember { mutableStateOf(true) }
    var showSettingsDialog by remember { mutableStateOf(false) }

    val savedDisplayModeStr = remember(prefs) { prefs.getString("coaching_display_mode", CoachingDisplayMode.PRONUNCIATION.name) }
    var coachingDisplayMode by remember(savedDisplayModeStr) {
        mutableStateOf(
            try {
                CoachingDisplayMode.valueOf(savedDisplayModeStr ?: CoachingDisplayMode.PRONUNCIATION.name)
            } catch (_: Exception) {
                CoachingDisplayMode.PRONUNCIATION
            }
        )
    }

    fun handleDisplayModeChange(newMode: CoachingDisplayMode) {
        coachingDisplayMode = newMode
        prefs.edit().putString("coaching_display_mode", newMode.name).apply()
    }

    fun handleSaveSettings(newCount: Int, newSpeeds: List<Float>) {
        repeatCount = newCount
        repeatSpeeds = newSpeeds
        prefs.edit()
            .putInt("repeat_count", newCount)
            .putString("repeat_speeds", newSpeeds.joinToString(","))
            .apply()
        // Idle: the next play request carries the new settings, so don't start the service just for this.
        if (isPlaying || currentPlayingId != null) {
            onUpdateSettings(newCount, newSpeeds)
        }
    }

    if (showSettingsDialog) {
        PlaybackSettingsDialog(
            initialRepeatCount = repeatCount,
            initialRepeatSpeeds = repeatSpeeds,
            onDismiss = { showSettingsDialog = false },
            onSave = { count, speeds ->
                handleSaveSettings(count, speeds)
                showSettingsDialog = false
            }
        )
    }

    LaunchedEffect(availableDays) {
        if (availableDays.isNotEmpty() && availableDays.none { it.key == selectedDay }) {
            selectedDay = availableDays.first().key
        }
    }

    val filteredSentences = remember(sentences, selectedDay) {
        sentences.filter { it.dayKey == selectedDay }
            .sortedWith(NATURAL_SENTENCE_COMPARATOR)
    }

    val currentActiveSentence = activeSentence?.takeIf { s -> filteredSentences.any { it.id == s.id } }
        ?: filteredSentences.firstOrNull()

    fun handlePrevSentence() {
        if (filteredSentences.isEmpty()) return
        val currentIndex = filteredSentences.indexOfFirst { it.id == currentActiveSentence?.id }
        val prevIndex = if (currentIndex > 0) currentIndex - 1 else filteredSentences.size - 1
        val prev = filteredSentences[prevIndex]
        activeSentence = prev
        onPlaySentence(prev, repeatCount, repeatSpeeds)
    }

    fun handleNextSentence() {
        if (filteredSentences.isEmpty()) return
        val currentIndex = filteredSentences.indexOfFirst { it.id == currentActiveSentence?.id }
        val nextIndex = if (currentIndex in filteredSentences.indices && currentIndex + 1 < filteredSentences.size) currentIndex + 1 else 0
        val next = filteredSentences[nextIndex]
        activeSentence = next
        onPlaySentence(next, repeatCount, repeatSpeeds)
    }

    fun handleSelectDay(newDay: String) {
        if (newDay != selectedDay) {
            if (isPlaying || currentPlayingId != null) {
                onStop()
            }
            selectedDay = newDay
        }
    }

    val handleToggleOrPlayActive: () -> Unit = {
        if (isPlaying) {
            onTogglePlay()
        } else if (currentPlayingId != null && currentPlayingId == currentActiveSentence?.id) {
            onTogglePlay()
        } else {
            currentActiveSentence?.let { onPlaySentence(it, repeatCount, repeatSpeeds) }
        }
    }

    // Auto-sync active sentence with player state or day change
    LaunchedEffect(currentPlayingId, filteredSentences) {
        if (currentPlayingId != null) {
            val matched = filteredSentences.find { it.id == currentPlayingId }
            if (matched != null) activeSentence = matched
        } else if (activeSentence == null && filteredSentences.isNotEmpty()) {
            activeSentence = filteredSentences.first()
        } else if (filteredSentences.isNotEmpty() && filteredSentences.none { it.id == activeSentence?.id }) {
            activeSentence = filteredSentences.first()
        }
    }

    var isManualFlexActive by remember { mutableStateOf(false) }

    LaunchedEffect(isFlexMode) {
        if (isFlexMode) isManualFlexActive = true
    }

    LaunchedEffect(isUnfolded) {
        if (!isUnfolded) isManualFlexActive = false
    }

    val isDisplayingFlex = isUnfolded && (isFlexMode || isManualFlexActive)

    // Temporary speed override applied ONLY to the current repeat iteration
    var tempSpeedOverride by remember { mutableStateOf<Float?>(null) }

    // When the repeat count advances or sentence changes or playback stops, reset the temporary override
    LaunchedEffect(currentRepeatIndex, currentPlayingId, isPlaying) {
        tempSpeedOverride = null
    }

    // Effective playback speed for the currently playing iteration
    val effectiveCurrentSpeed = tempSpeedOverride
        ?: repeatSpeeds.getOrElse((currentRepeatIndex - 1).coerceAtLeast(0)) { 1.0f }

    fun handleSpeedChange(newSpeed: Float) {
        tempSpeedOverride = newSpeed
        onSetTemporarySpeed(newSpeed)
    }

    when {
        !isUnfolded -> {
            CoverDisplayLayout(
                availableDays = availableDays,
                selectedDay = selectedDay,
                onSelectDay = { handleSelectDay(it) },
                sentences = filteredSentences,
                activeSentence = currentActiveSentence,
                isPlaying = isPlaying,
                repeatCount = repeatCount,
                repeatSpeeds = repeatSpeeds,
                onOpenSettings = { showSettingsDialog = true },
                showCoaching = showCoaching,
                onToggleCoaching = { showCoaching = !showCoaching },
                coachingDisplayMode = coachingDisplayMode,
                onCoachingDisplayModeChange = { handleDisplayModeChange(it) },
                onSelectSentence = {
                    activeSentence = it
                    onPlaySentence(it, repeatCount, repeatSpeeds)
                },
                onPlayAll = { onPlayAll(filteredSentences, repeatCount, repeatSpeeds) },
                onTogglePlay = handleToggleOrPlayActive,
                onStop = onStop,
                onPrevSentence = { handlePrevSentence() },
                onNextSentence = { handleNextSentence() },
                onSyncGitHub = onSyncGitHub,
                isSyncing = isSyncing
            )
        }
        isDisplayingFlex -> {
            FlexModeLayout(
                availableDays = availableDays,
                selectedDay = selectedDay,
                onSelectDay = { handleSelectDay(it) },
                activeSentence = currentActiveSentence,
                sentences = filteredSentences,
                isPlaying = isPlaying,
                repeatCount = repeatCount,
                repeatSpeeds = repeatSpeeds,
                currentRepeatIndex = currentRepeatIndex,
                currentSpeed = effectiveCurrentSpeed,
                coachingDisplayMode = coachingDisplayMode,
                onCoachingDisplayModeChange = { handleDisplayModeChange(it) },
                onSpeedChange = { handleSpeedChange(it) },
                onOpenSettings = { showSettingsDialog = true },
                onPlayAll = { onPlayAll(filteredSentences, repeatCount, repeatSpeeds) },
                onTogglePlay = handleToggleOrPlayActive,
                onStop = onStop,
                onPlaySentence = { s ->
                    activeSentence = s
                    onPlaySentence(s, repeatCount, repeatSpeeds)
                },
                onPrev = { handlePrevSentence() },
                onNext = { handleNextSentence() },
                onToggleFlexMode = { isManualFlexActive = false },
                onSyncGitHub = onSyncGitHub,
                isSyncing = isSyncing
            )
        }
        else -> {
            MainDualPaneLayout(
                availableDays = availableDays,
                selectedDay = selectedDay,
                onSelectDay = { handleSelectDay(it) },
                sentences = filteredSentences,
                activeSentence = currentActiveSentence,
                isPlaying = isPlaying,
                repeatCount = repeatCount,
                repeatSpeeds = repeatSpeeds,
                currentRepeatIndex = currentRepeatIndex,
                currentSpeed = effectiveCurrentSpeed,
                coachingDisplayMode = coachingDisplayMode,
                onCoachingDisplayModeChange = { handleDisplayModeChange(it) },
                onSpeedChange = { handleSpeedChange(it) },
                onOpenSettings = { showSettingsDialog = true },
                onSelectSentence = {
                    activeSentence = it
                    onPlaySentence(it, repeatCount, repeatSpeeds)
                },
                onPlayAll = { onPlayAll(filteredSentences, repeatCount, repeatSpeeds) },
                onTogglePlay = handleToggleOrPlayActive,
                onStop = onStop,
                onPrev = { handlePrevSentence() },
                onNext = { handleNextSentence() },
                onToggleFlexMode = { isManualFlexActive = true },
                onSyncGitHub = onSyncGitHub,
                isSyncing = isSyncing
            )
        }
    }
}

// =========================================================================
// 1. COVER DISPLAY LAYOUT (1248 x 1972 Folded Thumb-Zone Optimized)
// =========================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CoverDisplayLayout(
    availableDays: List<DayMeta>,
    selectedDay: String,
    onSelectDay: (String) -> Unit,
    sentences: List<Sentence>,
    activeSentence: Sentence?,
    isPlaying: Boolean,
    repeatCount: Int,
    repeatSpeeds: List<Float>,
    onOpenSettings: () -> Unit,
    showCoaching: Boolean,
    onToggleCoaching: () -> Unit,
    coachingDisplayMode: CoachingDisplayMode = CoachingDisplayMode.PRONUNCIATION,
    onCoachingDisplayModeChange: (CoachingDisplayMode) -> Unit = {},
    onSelectSentence: (Sentence) -> Unit,
    onPlayAll: () -> Unit,
    onTogglePlay: () -> Unit,
    onStop: () -> Unit,
    onPrevSentence: () -> Unit,
    onNextSentence: () -> Unit,
    isSyncing: Boolean = false,
    onSyncGitHub: () -> Unit
) {
    val currentDayMeta = availableDays.find { it.key == selectedDay } ?: availableDays.firstOrNull() ?: DEFAULT_DAYS.first()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "OPIc Master IH",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                        Text(
                            text = "${currentDayMeta.emoji} ${currentDayMeta.title}",
                            fontSize = 11.sp,
                            color = Color(0xFFA5B4FC)
                        )
                    }
                },
                actions = {
                    val infiniteTransition = rememberInfiniteTransition(label = "coverSyncTransition")
                    val syncRotation by infiniteTransition.animateFloat(
                        initialValue = 0f,
                        targetValue = 360f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(1000, easing = LinearEasing),
                            repeatMode = RepeatMode.Restart
                        ),
                        label = "coverSyncRotation"
                    )
                    IconButton(
                        onClick = onSyncGitHub,
                        enabled = !isSyncing
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudSync,
                            contentDescription = "GitHub Sync",
                            tint = if (isSyncing) Color(0xFFFBBF24) else Color(0xFF34D399),
                            modifier = if (isSyncing) Modifier.graphicsLayer { rotationZ = syncRotation } else Modifier
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF0F172A))
            )
        },
        bottomBar = {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color(0xFF0F172A),
                tonalElevation = 8.dp,
                shadowElevation = 16.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                ) {
                    HorizontalDivider(color = Color(0xFF1E293B), thickness = 1.dp)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.width(90.dp)) {
                            Text(
                                text = activeSentence?.id?.uppercase() ?: "SENTENCE",
                                color = Color(0xFFA5B4FC),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(if (isPlaying) Color(0xFF10B981) else Color(0xFF64748B))
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (isPlaying) "연속 재생 중" else "화면 꺼짐 지원",
                                    color = if (isPlaying) Color(0xFF10B981) else Color(0xFF94A3B8),
                                    fontSize = 10.sp
                                )
                            }
                        }

                        // Central Player Controls
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = onPrevSentence, modifier = Modifier.size(36.dp)) {
                                Icon(Icons.Default.SkipPrevious, contentDescription = "Prev", tint = Color.White)
                            }
                            FilledIconButton(
                                onClick = onTogglePlay,
                                modifier = Modifier.size(44.dp),
                                colors = IconButtonDefaults.filledIconButtonColors(containerColor = Color(0xFF6366F1))
                            ) {
                                Icon(
                                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = if (isPlaying) "Pause" else "Play",
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            IconButton(onClick = onStop, modifier = Modifier.size(36.dp)) {
                                Icon(Icons.Default.Stop, contentDescription = "Stop", tint = Color(0xFFEF4444), modifier = Modifier.size(24.dp))
                            }
                            IconButton(onClick = onNextSentence, modifier = Modifier.size(36.dp)) {
                                Icon(Icons.Default.SkipNext, contentDescription = "Next", tint = Color.White)
                            }
                        }

                        RepeatSpeedSettingButton(
                            repeatCount = repeatCount,
                            repeatSpeeds = repeatSpeeds,
                            onClick = onOpenSettings,
                            isVertical = true
                        )
                    }
                }
            }
        },
        containerColor = Color(0xFF0B1120)
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            DaySelectorTabs(days = availableDays, selectedDay = selectedDay, onSelectDay = onSelectDay)

            // Play-All Bar & Coaching Toggle
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                color = Color(0xFF131D33),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, Color(0xFF1E293B))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = onPlayAll,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1)),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("▶ 탭 전체 연속 재생", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 1. Coaching ON (낭독·강세) Button
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (showCoaching && coachingDisplayMode == CoachingDisplayMode.PRONUNCIATION) Color(0xFF0369A1) else Color(0xFF1E293B),
                            border = BorderStroke(
                                1.dp,
                                if (showCoaching && coachingDisplayMode == CoachingDisplayMode.PRONUNCIATION) Color(0xFF38BDF8) else Color(0xFF334155)
                            ),
                            modifier = Modifier.clickable {
                                if (!showCoaching) onToggleCoaching()
                                onCoachingDisplayModeChange(CoachingDisplayMode.PRONUNCIATION)
                            }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Text(
                                    text = "🗣️ 낭독",
                                    fontSize = 11.sp,
                                    color = if (showCoaching && coachingDisplayMode == CoachingDisplayMode.PRONUNCIATION) Color.White else Color(0xFF94A3B8),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // 2. Image (연상 이미지) Button
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (showCoaching && coachingDisplayMode == CoachingDisplayMode.IMAGE) Color(0xFF6366F1) else Color(0xFF1E293B),
                            border = BorderStroke(
                                1.dp,
                                if (showCoaching && coachingDisplayMode == CoachingDisplayMode.IMAGE) Color(0xFFA5B4FC) else Color(0xFF334155)
                            ),
                            modifier = Modifier.clickable {
                                if (!showCoaching) onToggleCoaching()
                                onCoachingDisplayModeChange(CoachingDisplayMode.IMAGE)
                            }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Text(
                                    text = "🖼️ 이미지",
                                    fontSize = 11.sp,
                                    color = if (showCoaching && coachingDisplayMode == CoachingDisplayMode.IMAGE) Color.White else Color(0xFF94A3B8),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // 3. OFF (접기) Button
                        if (showCoaching) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFF1E293B),
                                border = BorderStroke(1.dp, Color(0xFF475569)),
                                modifier = Modifier.clickable { onToggleCoaching() }
                            ) {
                                Text(
                                    text = "접기",
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 5.dp),
                                    fontSize = 10.sp,
                                    color = Color(0xFF94A3B8),
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }

            // Sentences Card List with Auto-Centering on Playback
            val listState = rememberLazyListState()
            val latestIsPlaying by rememberUpdatedState(isPlaying)

            // Keyed only on the sentence id: the 1.2s shadowing pause toggles isPlaying
            // between repeats, which must not re-trigger the scroll.
            LaunchedEffect(activeSentence?.id) {
                val targetId = activeSentence?.id ?: return@LaunchedEffect
                snapshotFlow { latestIsPlaying }.first { it }
                val index = sentences.indexOfFirst { it.id == targetId }
                if (index >= 0) listState.centerOnItem(index)
            }

            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                items(sentences, key = { it.id }) { sentence ->
                    val isThisActive = activeSentence?.id == sentence.id
                    val isThisPlaying = isThisActive && isPlaying

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelectSentence(sentence) }
                            .border(
                                width = if (isThisPlaying) 2.dp else 1.dp,
                                color = if (isThisPlaying) Color(0xFF6366F1) else Color(0xFF1E293B),
                                shape = RoundedCornerShape(14.dp)
                            ),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isThisPlaying) Color(0xFF1E1B4B) else Color(0xFF0F172A)
                        ),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        color = if (isThisPlaying) Color(0xFF6366F1) else Color(0xFF1E293B),
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text(
                                            text = sentence.id.uppercase(),
                                            color = Color.White,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                    if (isThisPlaying) {
                                        Text("● 재생 중", color = Color(0xFF10B981), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }
                                }

                                if (sentence.isDownloaded) {
                                    Text("⚡ 오프라인 저장됨", color = Color(0xFF10B981), fontSize = 10.sp)
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = cleanSentenceText(sentence.en),
                                color = Color.White,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                lineHeight = 22.sp
                            )

                            if (showCoaching) {
                                SentenceMediaCoachingSection(
                                    sentence = sentence,
                                    displayMode = coachingDisplayMode,
                                    onDisplayModeChange = onCoachingDisplayModeChange,
                                    modifier = Modifier.padding(top = 8.dp),
                                    containerColor = Color(0xFF0B1120),
                                    borderColor = Color(0xFF1E3A5F),
                                    fontSize = 13.5.sp,
                                    imageHeight = 170.dp
                                )
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            Text(
                                text = sentence.ko,
                                color = Color(0xFF94A3B8),
                                fontSize = 12.sp,
                                lineHeight = 18.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

// =========================================================================
// 2. MAIN DUAL-PANE STUDIO LAYOUT (2448 x 1848 Unfolded Studio)
// =========================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainDualPaneLayout(
    availableDays: List<DayMeta>,
    selectedDay: String,
    onSelectDay: (String) -> Unit,
    sentences: List<Sentence>,
    activeSentence: Sentence?,
    isPlaying: Boolean,
    repeatCount: Int,
    repeatSpeeds: List<Float>,
    currentRepeatIndex: Int = 1,
    currentSpeed: Float = 1.0f,
    coachingDisplayMode: CoachingDisplayMode = CoachingDisplayMode.PRONUNCIATION,
    onCoachingDisplayModeChange: (CoachingDisplayMode) -> Unit = {},
    onSpeedChange: (Float) -> Unit = {},
    onOpenSettings: () -> Unit,
    onSelectSentence: (Sentence) -> Unit,
    onPlayAll: () -> Unit,
    onTogglePlay: () -> Unit,
    onStop: () -> Unit,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onToggleFlexMode: () -> Unit,
    isSyncing: Boolean = false,
    onSyncGitHub: () -> Unit
) {
    val currentDayMeta = availableDays.find { it.key == selectedDay } ?: availableDays.firstOrNull() ?: DEFAULT_DAYS.first()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "OPIC MASTER IH",
                            color = Color(0xFFA5B4FC),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("|", color = Color(0xFF475569))
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "갤럭시 Z 폴드 8 듀얼 스튜디오 모드",
                            color = Color(0xFFE2E8F0),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                },
                actions = {
                    Button(
                        onClick = onToggleFlexMode,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4F46E5)),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.VerticalSplit, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("📐 플렉스 모드", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                    Spacer(modifier = Modifier.width(8.dp))

                    Surface(
                        color = if (isSyncing) Color(0xFF78350F) else Color(0xFF064E3B),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(if (isSyncing) Color(0xFFFBBF24) else Color(0xFF34D399))
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isSyncing) "GitHub 동기화 중..." else "GitHub 오프라인 완료 (${sentences.size}개)",
                                color = if (isSyncing) Color(0xFFFBBF24) else Color(0xFF34D399),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    val infiniteTransition = rememberInfiniteTransition(label = "mainSyncTransition")
                    val syncRotation by infiniteTransition.animateFloat(
                        initialValue = 0f,
                        targetValue = 360f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(1000, easing = LinearEasing),
                            repeatMode = RepeatMode.Restart
                        ),
                        label = "mainSyncRotation"
                    )
                    IconButton(
                        onClick = onSyncGitHub,
                        enabled = !isSyncing
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudSync,
                            contentDescription = "Sync",
                            tint = if (isSyncing) Color(0xFFFBBF24) else Color(0xFF34D399),
                            modifier = if (isSyncing) Modifier.graphicsLayer { rotationZ = syncRotation } else Modifier
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF0F172A))
            )
        },
        containerColor = Color(0xFF0B1120)
    ) { innerPadding ->
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .navigationBarsPadding()
        ) {
            // LEFT PANE: Sentence List & Day Selector (Weight 1f)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(16.dp)
            ) {
                DaySelectorTabs(days = availableDays, selectedDay = selectedDay, onSelectDay = onSelectDay, horizontalPadding = 0)

                Spacer(modifier = Modifier.height(10.dp))

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = Color(0xFF1E1B4B),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, Color(0xFF3730A3))
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "${currentDayMeta.emoji} ${currentDayMeta.title}",
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "총 ${sentences.size}개 문장 세트 · Andrew HD Voice",
                                color = Color(0xFFA5B4FC),
                                fontSize = 11.sp
                            )
                        }
                        Button(
                            onClick = onPlayAll,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1)),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("전체 연속 청취", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "📜 전체 문단 목록 (클릭하여 이동)",
                    color = Color(0xFF94A3B8),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))

                val listState = rememberLazyListState()
                val latestIsPlaying by rememberUpdatedState(isPlaying)

                // Keyed only on the sentence id: the 1.2s shadowing pause toggles isPlaying
                // between repeats, which must not re-trigger the scroll.
                LaunchedEffect(activeSentence?.id) {
                    val targetId = activeSentence?.id ?: return@LaunchedEffect
                    snapshotFlow { latestIsPlaying }.first { it }
                    val index = sentences.indexOfFirst { it.id == targetId }
                    if (index >= 0) listState.centerOnItem(index)
                }

                LazyColumn(
                    state = listState,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(sentences, key = { it.id }) { s ->
                        val isSelected = s.id == activeSentence?.id
                        val isThisPlaying = isSelected && isPlaying

                        Surface(
                            color = if (isThisPlaying) Color(0xFF1E1B4B) else if (isSelected) Color(0xFF1E293B) else Color(0xFF0F172A),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(
                                width = if (isThisPlaying) 2.dp else if (isSelected) 1.dp else 0.dp,
                                color = if (isThisPlaying) Color(0xFF6366F1) else if (isSelected) Color(0xFF475569) else Color.Transparent
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelectSentence(s) }
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = s.id.uppercase(),
                                        color = if (isThisPlaying) Color(0xFFA5B4FC) else Color(0xFF64748B),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    if (isThisPlaying) {
                                        Text("▶ 재생 중", color = Color(0xFF10B981), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = cleanSentenceText(s.en),
                                    color = if (isSelected) Color.White else Color(0xFFCBD5E1),
                                    fontSize = 13.sp,
                                    lineHeight = 18.sp
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Left Pane Bottom: Repeat Progress
                PlaybackRepeatProgressIndicator(
                    currentRepeatIndex = currentRepeatIndex,
                    repeatTargetCount = repeatCount,
                    isPlaying = isPlaying
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Left Pane Bottom: Player Control Deck
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilledIconButton(
                        onClick = onPrev,
                        modifier = Modifier.size(46.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = IconButtonDefaults.filledIconButtonColors(containerColor = Color(0xFF1E293B))
                    ) {
                        Icon(Icons.Default.SkipPrevious, contentDescription = "Prev", tint = Color.White)
                    }

                    Button(
                        onClick = onTogglePlay,
                        modifier = Modifier.weight(1f).height(46.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isPlaying) Color(0xFF4338CA) else Color(0xFF6366F1)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isPlaying) "일시 정지" else "🔊 원어민 재생",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    FilledIconButton(
                        onClick = onStop,
                        modifier = Modifier.size(46.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = IconButtonDefaults.filledIconButtonColors(containerColor = Color(0xFF1E293B))
                    ) {
                        Icon(Icons.Default.Stop, contentDescription = "Stop", tint = Color(0xFFEF4444), modifier = Modifier.size(22.dp))
                    }

                    FilledIconButton(
                        onClick = onNext,
                        modifier = Modifier.size(46.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = IconButtonDefaults.filledIconButtonColors(containerColor = Color(0xFF1E293B))
                    ) {
                        Icon(Icons.Default.SkipNext, contentDescription = "Next", tint = Color.White)
                    }
                }
            }

            // HINGE SEPARATOR
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(1.dp)
                    .background(Color(0xFF1E293B))
            )

            // RIGHT PANE: Focused Shadowing Studio (낭독·강세 집중 스튜디오)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .background(Color(0xFF0F172A))
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Scrollable Top Content
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                color = Color(0xFF6366F1),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = activeSentence?.id?.uppercase() ?: "SENTENCE",
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                            Text(
                                text = "👨‍💼 Andrew HD Neural Voice",
                                color = Color(0xFFA5B4FC),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        RepeatSpeedSettingButton(
                            repeatCount = repeatCount,
                            repeatSpeeds = repeatSpeeds,
                            onClick = onOpenSettings
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // English Sentence & Korean Translation Card with Compact Size
                    Surface(
                        color = Color(0xFF131D33),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, Color(0xFF2E3856)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                text = activeSentence?.let { cleanSentenceText(it.en) } ?: "문장을 선택해 주세요.",
                                color = Color.White,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                lineHeight = 25.sp
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = activeSentence?.ko ?: "",
                                color = Color(0xFF94A3B8),
                                fontSize = 13.sp,
                                lineHeight = 19.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Sentence Media Coaching Section (낭독·강세 & 연상 이미지 토글 버튼)
                    SentenceMediaCoachingSection(
                        sentence = activeSentence,
                        displayMode = coachingDisplayMode,
                        onDisplayModeChange = onCoachingDisplayModeChange,
                        containerColor = Color(0xFF0F2338),
                        borderColor = Color(0xFF0284C7).copy(alpha = 0.6f),
                        fontSize = 17.sp,
                        imageHeight = 260.dp
                    )

                    Spacer(modifier = Modifier.height(10.dp))
                }

                // Right Pane Bottom: Speed Slider Only
                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Spacer(modifier = Modifier.height(6.dp))
                    DragSpeedSlider(
                        currentSpeed = currentSpeed,
                        onSpeedChange = onSpeedChange
                    )
                }
            }
        }
    }
}

// =========================================================================
// 3. FLEX MODE LAYOUT (Tabletop 90° Hinge View L-Stand)
// =========================================================================

@Composable
fun FlexModeLayout(
    availableDays: List<DayMeta>,
    selectedDay: String,
    onSelectDay: (String) -> Unit,
    activeSentence: Sentence?,
    sentences: List<Sentence>,
    isPlaying: Boolean,
    repeatCount: Int,
    repeatSpeeds: List<Float> = emptyList(),
    currentRepeatIndex: Int = 1,
    currentSpeed: Float = 1.0f,
    coachingDisplayMode: CoachingDisplayMode = CoachingDisplayMode.PRONUNCIATION,
    onCoachingDisplayModeChange: (CoachingDisplayMode) -> Unit = {},
    onSpeedChange: (Float) -> Unit = {},
    onOpenSettings: () -> Unit = {},
    onPlayAll: () -> Unit = {},
    onTogglePlay: () -> Unit,
    onStop: () -> Unit,
    onPlaySentence: (Sentence) -> Unit,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onToggleFlexMode: () -> Unit,
    isSyncing: Boolean = false,
    onSyncGitHub: () -> Unit = {}
) {
    val currentIndex = sentences.indexOfFirst { it.id == activeSentence?.id }.takeIf { it >= 0 } ?: 0
    val totalCount = sentences.size

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0B1120))
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // TOP HALF: Header + Reading Stand (독서대) - Exactly 50% of screen height
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            // Mode Header & Day Selector inside Top Half
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color(0xFF0F172A),
                tonalElevation = 4.dp
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "📐 갤럭시 Z 폴드 8 플렉스 거치 모드 (L자 스탠드)",
                            color = Color(0xFFA5B4FC),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            val infiniteTransition = rememberInfiniteTransition(label = "flexSyncTransition")
                            val syncRotation by infiniteTransition.animateFloat(
                                initialValue = 0f,
                                targetValue = 360f,
                                animationSpec = infiniteRepeatable(
                                    animation = tween(1000, easing = LinearEasing),
                                    repeatMode = RepeatMode.Restart
                                ),
                                label = "flexSyncRotation"
                            )
                            IconButton(
                                onClick = onSyncGitHub,
                                enabled = !isSyncing,
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CloudSync,
                                    contentDescription = "Sync",
                                    tint = if (isSyncing) Color(0xFFFBBF24) else Color(0xFF34D399),
                                    modifier = if (isSyncing) Modifier.graphicsLayer { rotationZ = syncRotation } else Modifier
                                )
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Button(
                                onClick = onToggleFlexMode,
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4F46E5)),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Icon(Icons.Default.VerticalSplit, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color.White)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("📖 대화면 전환", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }
                    }
                    DaySelectorTabs(days = availableDays, selectedDay = selectedDay, onSelectDay = onSelectDay, horizontalPadding = 16)
                    Spacer(modifier = Modifier.height(2.dp))
                }
            }

            // Reading Stand Card filling the remainder of top half
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color(0xFF020617),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, Color(0xFF1E293B)),
                    shadowElevation = 6.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(14.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "[상단 디스플레이: 낭독 독서대]",
                                    fontSize = 11.sp,
                                    color = Color(0xFF94A3B8),
                                    fontWeight = FontWeight.Medium
                                )
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(7.dp)
                                            .clip(CircleShape)
                                            .background(if (isPlaying) Color(0xFF10B981) else Color(0xFF64748B))
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (isPlaying) "● 섀도잉 모드 재생 중" else "● 거치 대기 중",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isPlaying) Color(0xFF34D399) else Color(0xFF94A3B8)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = if (totalCount > 0) "Sentence ${currentIndex + 1} / $totalCount" else "Sentence",
                                color = Color(0xFF818CF8),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.ExtraBold
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            val cleanEn = activeSentence?.let { cleanSentenceText(it.en) } ?: "선택된 문장이 없습니다."
                            Text(
                                text = "\"$cleanEn\"",
                                color = Color.White,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                lineHeight = 26.sp
                            )

                            SentenceMediaCoachingSection(
                                sentence = activeSentence,
                                displayMode = coachingDisplayMode,
                                onDisplayModeChange = onCoachingDisplayModeChange,
                                modifier = Modifier.padding(top = 8.dp),
                                containerColor = Color(0xFF082F49).copy(alpha = 0.55f),
                                borderColor = Color(0xFF0284C7).copy(alpha = 0.4f),
                                fontSize = 14.sp,
                                imageHeight = 180.dp
                            )
                        }

                        if (!activeSentence?.ko.isNullOrEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = activeSentence.ko,
                                color = Color(0xFF94A3B8),
                                fontSize = 13.sp,
                                lineHeight = 19.sp
                            )
                        }
                    }
                }
            }
        }

        // PHYSICAL HINGE DIVIDER
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(22.dp),
            color = Color(0xFF1E293B)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = "═══ 힌지 접힘선 (HINGE FOLD 90° ~ 115°) ═══",
                    color = Color(0xFF64748B),
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // BOTTOM HALF: Touch Controller Deck
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = Color(0xFF020617),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, Color(0xFF1E293B)),
                shadowElevation = 8.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.SpaceBetween,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "[하단 디스플레이: 평면 터치 컨트롤 패드]",
                            fontSize = 11.sp,
                            color = Color(0xFF94A3B8),
                            fontWeight = FontWeight.Medium
                        )
                    }

                    PlaybackRepeatProgressIndicator(
                        currentRepeatIndex = currentRepeatIndex,
                        repeatTargetCount = repeatCount,
                        isPlaying = isPlaying
                    )

                    DragSpeedSlider(
                        currentSpeed = currentSpeed,
                        onSpeedChange = onSpeedChange
                    )

                    // Full-width prominent Repeat & Speed settings button in the center
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onOpenSettings() },
                        color = Color(0xFF1E1B4B).copy(alpha = 0.6f),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Color(0xFF4F46E5).copy(alpha = 0.5f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Tune,
                                    contentDescription = null,
                                    tint = Color(0xFFA5B4FC),
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = "반복 및 배속 상세 설정",
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    val summary = if (repeatSpeeds.isNotEmpty()) {
                                        val min = repeatSpeeds.minOrNull() ?: 1.0f
                                        val max = repeatSpeeds.maxOrNull() ?: 1.0f
                                        if (min == max) "${min}x 동일 속도" else "${min}x ~ ${max}x 점진 가속"
                                    } else "1.0x 표준"
                                    Text(
                                        text = "반복 ${repeatCount}회 · $summary",
                                        color = Color(0xFF94A3B8),
                                        fontSize = 10.sp
                                    )
                                }
                            }
                            Surface(
                                color = Color(0xFF312E81),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = "설정 변경 ⚙️",
                                    color = Color(0xFFC7D2FE),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }

                    // Central Big Player Controls
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FilledIconButton(
                            onClick = onPrev,
                            modifier = Modifier.size(46.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = IconButtonDefaults.filledIconButtonColors(containerColor = Color(0xFF1E293B))
                        ) {
                            Icon(Icons.Default.SkipPrevious, contentDescription = "Prev", modifier = Modifier.size(24.dp), tint = Color.White)
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        FilledIconButton(
                            onClick = onTogglePlay,
                            modifier = Modifier.size(60.dp),
                            shape = RoundedCornerShape(20.dp),
                            colors = IconButtonDefaults.filledIconButtonColors(containerColor = Color(0xFF6366F1))
                        ) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (isPlaying) "Pause" else "Play",
                                modifier = Modifier.size(32.dp),
                                tint = Color.White
                            )
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        FilledIconButton(
                            onClick = onStop,
                            modifier = Modifier.size(46.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = IconButtonDefaults.filledIconButtonColors(containerColor = Color(0xFF1E293B))
                        ) {
                            Icon(Icons.Default.Stop, contentDescription = "Stop", modifier = Modifier.size(24.dp), tint = Color(0xFFEF4444))
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        FilledIconButton(
                            onClick = onNext,
                            modifier = Modifier.size(46.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = IconButtonDefaults.filledIconButtonColors(containerColor = Color(0xFF1E293B))
                        ) {
                            Icon(Icons.Default.SkipNext, contentDescription = "Next", modifier = Modifier.size(24.dp), tint = Color.White)
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        // 전체 연속 청취 버튼
                        Button(
                            onClick = onPlayAll,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF312E81)),
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, Color(0xFF6366F1)),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                            modifier = Modifier.height(46.dp)
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color(0xFFA5B4FC))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("전체 연속", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }

                    // Screen-Off Continuous Playback Status
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF10B981))
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "화면 꺼짐(AOD) 무중단 백그라운드 연속 재생 활성화",
                            color = Color(0xFF10B981),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}
