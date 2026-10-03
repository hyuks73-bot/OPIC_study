package com.opic.master.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.opic.master.data.model.Sentence

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

fun naturalDayComparator(): Comparator<String> = Comparator { a, b ->
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

fun naturalSentenceComparator(): Comparator<Sentence> = Comparator { a, b ->
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
        items(days) { meta ->
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
fun RepeatSpeedBadges(
    repeatCount: Int,
    speed: Float,
    onCycleRepeat: () -> Unit,
    onCycleSpeed: () -> Unit,
    modifier: Modifier = Modifier,
    isVertical: Boolean = false
) {
    val items = @Composable {
        Surface(
            color = Color(0xFF1E293B),
            shape = RoundedCornerShape(6.dp),
            modifier = Modifier.clickable { onCycleRepeat() }
        ) {
            Text(
                text = "🔁 ${if (repeatCount >= 999) "무한" else "${repeatCount}회"}",
                fontSize = 11.sp,
                color = Color(0xFFFBBF24),
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
            )
        }
        Surface(
            color = Color(0xFF1E293B),
            shape = RoundedCornerShape(6.dp),
            modifier = Modifier.clickable { onCycleSpeed() }
        ) {
            Text(
                text = "⚡ ${speed}x",
                fontSize = 11.sp,
                color = Color(0xFFA5B4FC),
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
            )
        }
    }

    if (isVertical) {
        Column(
            modifier = modifier,
            verticalArrangement = Arrangement.spacedBy(4.dp),
            horizontalAlignment = Alignment.End
        ) {
            items()
        }
    } else {
        Row(
            modifier = modifier,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            items()
        }
    }
}

@Composable
fun CoachingGuideCard(
    guide: String,
    tip: String,
    modifier: Modifier = Modifier,
    containerColor: Color = Color(0xFF0B1120),
    borderColor: Color = Color(0xFF1E3A5F)
) {
    val cleanGuide = remember(guide) { cleanGuideText(guide) }
    if (cleanGuide.isEmpty() && tip.isEmpty()) return

    Surface(
        color = containerColor,
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, borderColor),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            if (cleanGuide.isNotEmpty()) {
                Text(
                    text = "🗣️ 낭독·강세: $cleanGuide",
                    color = Color(0xFF7DD3FC),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    lineHeight = 16.sp
                )
            }
            if (tip.isNotEmpty()) {
                if (cleanGuide.isNotEmpty()) Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "💡 팁: $tip",
                    color = Color(0xFF94A3B8),
                    fontSize = 11.sp,
                    lineHeight = 15.sp
                )
            }
        }
    }
}

@Composable
fun <T> SelectionPillsRow(
    items: List<Pair<T, String>>,
    selectedItem: T,
    onSelect: (T) -> Unit,
    selectedColor: Color,
    selectedBorderColor: Color,
    modifier: Modifier = Modifier,
    heightDp: Int = 36
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items.forEach { (value, label) ->
            val isSelected = selectedItem == value
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .height(heightDp.dp)
                    .clickable { onSelect(value) },
                color = if (isSelected) selectedColor else Color(0xFF1E293B),
                shape = RoundedCornerShape(10.dp),
                border = if (isSelected) BorderStroke(1.dp, selectedBorderColor) else null
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = label,
                        color = if (isSelected) Color.White else Color(0xFF94A3B8),
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                    )
                }
            }
        }
    }
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
    onPlaySentence: (Sentence, Int) -> Unit,
    onPlayAll: (List<Sentence>, Int) -> Unit,
    onTogglePlay: () -> Unit,
    onStop: () -> Unit,
    onUpdateRepeatCount: (Int) -> Unit,
    onUpdateSpeed: (Float) -> Unit = {},
    isSyncing: Boolean = false,
    onSyncGitHub: () -> Unit
) {
    val availableDays = remember(days, sentences) {
        if (days.isNotEmpty()) {
            days
        } else if (sentences.isNotEmpty()) {
            val distinctKeys = sentences.map { it.dayKey }.distinct().sortedWith(naturalDayComparator())
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

    var selectedDay by remember { mutableStateOf(availableDays.firstOrNull()?.key ?: "day1") }
    var activeSentence by remember { mutableStateOf<Sentence?>(null) }
    var speed by remember { mutableFloatStateOf(1.0f) }
    var repeatCount by remember { mutableIntStateOf(3) }
    var showCoaching by remember { mutableStateOf(true) }

    LaunchedEffect(availableDays) {
        if (availableDays.isNotEmpty() && availableDays.none { it.key == selectedDay }) {
            selectedDay = availableDays.first().key
        }
    }

    val filteredSentences = remember(sentences, selectedDay) {
        sentences.filter { it.dayKey == selectedDay }
            .sortedWith(naturalSentenceComparator())
    }

    val currentActiveSentence = activeSentence?.takeIf { s -> filteredSentences.any { it.id == s.id } }
        ?: filteredSentences.firstOrNull()

    fun handlePrevSentence() {
        if (filteredSentences.isEmpty()) return
        val currentIndex = filteredSentences.indexOfFirst { it.id == currentActiveSentence?.id }
        val prevIndex = if (currentIndex > 0) currentIndex - 1 else filteredSentences.size - 1
        val prev = filteredSentences[prevIndex]
        activeSentence = prev
        onPlaySentence(prev, repeatCount)
    }

    fun handleNextSentence() {
        if (filteredSentences.isEmpty()) return
        val currentIndex = filteredSentences.indexOfFirst { it.id == currentActiveSentence?.id }
        val nextIndex = if (currentIndex in filteredSentences.indices && currentIndex + 1 < filteredSentences.size) currentIndex + 1 else 0
        val next = filteredSentences[nextIndex]
        activeSentence = next
        onPlaySentence(next, repeatCount)
    }

    fun handleRepeatCycle() {
        val next = when (repeatCount) {
            1 -> 3
            3 -> 5
            5 -> 999
            else -> 1
        }
        repeatCount = next
        onUpdateRepeatCount(next)
    }

    fun handleSpeedCycle() {
        val next = when (speed) {
            0.8f -> 1.0f
            1.0f -> 1.2f
            else -> 0.8f
        }
        speed = next
        onUpdateSpeed(next)
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
            currentActiveSentence?.let { onPlaySentence(it, repeatCount) }
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
                speed = speed,
                showCoaching = showCoaching,
                onToggleCoaching = { showCoaching = !showCoaching },
                onSelectSentence = {
                    activeSentence = it
                    onPlaySentence(it, repeatCount)
                },
                onPlayAll = { onPlayAll(filteredSentences, repeatCount) },
                onTogglePlay = handleToggleOrPlayActive,
                onStop = onStop,
                onCycleRepeat = { handleRepeatCycle() },
                onCycleSpeed = { handleSpeedCycle() },
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
                speed = speed,
                onTogglePlay = handleToggleOrPlayActive,
                onStop = onStop,
                onPlaySentence = { s ->
                    activeSentence = s
                    onPlaySentence(s, repeatCount)
                },
                onPrev = { handlePrevSentence() },
                onNext = { handleNextSentence() },
                onSelectSpeed = { newSpeed ->
                    speed = newSpeed
                    onUpdateSpeed(newSpeed)
                },
                onSelectRepeat = { count ->
                    repeatCount = count
                    onUpdateRepeatCount(count)
                },
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
                speed = speed,
                onSelectSentence = {
                    activeSentence = it
                    onPlaySentence(it, repeatCount)
                },
                onPlayAll = { onPlayAll(filteredSentences, repeatCount) },
                onTogglePlay = handleToggleOrPlayActive,
                onStop = onStop,
                onPrev = { handlePrevSentence() },
                onNext = { handleNextSentence() },
                onCycleRepeat = { handleRepeatCycle() },
                onCycleSpeed = { handleSpeedCycle() },
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
    speed: Float,
    showCoaching: Boolean,
    onToggleCoaching: () -> Unit,
    onSelectSentence: (Sentence) -> Unit,
    onPlayAll: () -> Unit,
    onTogglePlay: () -> Unit,
    onStop: () -> Unit,
    onCycleRepeat: () -> Unit,
    onCycleSpeed: () -> Unit,
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

                        RepeatSpeedBadges(
                            repeatCount = repeatCount,
                            speed = speed,
                            onCycleRepeat = onCycleRepeat,
                            onCycleSpeed = onCycleSpeed,
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
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (showCoaching) Color(0xFF0369A1) else Color(0xFF1E293B),
                            modifier = Modifier.clickable { onToggleCoaching() }
                        ) {
                            Text(
                                text = "🗣️ 코칭 ${if (showCoaching) "ON" else "OFF"}",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                fontSize = 11.sp,
                                color = if (showCoaching) Color.White else Color(0xFF94A3B8),
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text("${sentences.size}개 문장", fontSize = 11.sp, color = Color(0xFF94A3B8))
                    }
                }
            }

            // Sentences Card List
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                items(sentences) { sentence ->
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
                                CoachingGuideCard(
                                    guide = sentence.guide,
                                    tip = sentence.tip,
                                    modifier = Modifier.padding(top = 8.dp)
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
    speed: Float,
    onSelectSentence: (Sentence) -> Unit,
    onPlayAll: () -> Unit,
    onTogglePlay: () -> Unit,
    onStop: () -> Unit,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onCycleRepeat: () -> Unit,
    onCycleSpeed: () -> Unit,
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

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(sentences) { s ->
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
            }

            // HINGE SEPARATOR
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(1.dp)
                    .background(Color(0xFF1E293B))
            )

            // RIGHT PANE: Focused Shadowing Studio (Weight 1f)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .background(Color(0xFF0F172A))
                    .padding(20.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
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

                        RepeatSpeedBadges(
                            repeatCount = repeatCount,
                            speed = speed,
                            onCycleRepeat = onCycleRepeat,
                            onCycleSpeed = onCycleSpeed
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Surface(
                        color = Color(0xFF131D33),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, Color(0xFF1E293B)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Text(
                                text = activeSentence?.let { cleanSentenceText(it.en) } ?: "문장을 선택해 주세요.",
                                color = Color.White,
                                fontSize = 19.sp,
                                fontWeight = FontWeight.Bold,
                                lineHeight = 28.sp
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = activeSentence?.ko ?: "",
                                color = Color(0xFF94A3B8),
                                fontSize = 13.sp,
                                lineHeight = 20.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    CoachingGuideCard(
                        guide = activeSentence?.guide ?: "",
                        tip = activeSentence?.tip ?: ""
                    )
                }

                // Bottom Player Control Deck
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilledIconButton(
                        onClick = onPrev,
                        modifier = Modifier.size(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = IconButtonDefaults.filledIconButtonColors(containerColor = Color(0xFF1E293B))
                    ) {
                        Icon(Icons.Default.SkipPrevious, contentDescription = "Prev", tint = Color.White)
                    }

                    Button(
                        onClick = onTogglePlay,
                        modifier = Modifier.weight(1f).height(48.dp),
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
                        modifier = Modifier.size(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = IconButtonDefaults.filledIconButtonColors(containerColor = Color(0xFF1E293B))
                    ) {
                        Icon(Icons.Default.Stop, contentDescription = "Stop", tint = Color(0xFFEF4444), modifier = Modifier.size(22.dp))
                    }

                    FilledIconButton(
                        onClick = onNext,
                        modifier = Modifier.size(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = IconButtonDefaults.filledIconButtonColors(containerColor = Color(0xFF1E293B))
                    ) {
                        Icon(Icons.Default.SkipNext, contentDescription = "Next", tint = Color.White)
                    }
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
    speed: Float,
    onTogglePlay: () -> Unit,
    onStop: () -> Unit,
    onPlaySentence: (Sentence) -> Unit,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onSelectSpeed: (Float) -> Unit,
    onSelectRepeat: (Int) -> Unit,
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
        // Mode Header & Day Selector
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Color(0xFF0F172A),
            tonalElevation = 4.dp
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "📐 갤럭시 Z 폴드 8 플렉스 거치 모드 (L자 스탠드)",
                        color = Color(0xFFA5B4FC),
                        fontSize = 13.sp,
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
                            modifier = Modifier.size(32.dp)
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
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.VerticalSplit, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("📖 메인 대화면 전환", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
                DaySelectorTabs(days = availableDays, selectedDay = selectedDay, onSelectDay = onSelectDay, horizontalPadding = 16)
                Spacer(modifier = Modifier.height(4.dp))
            }
        }

        // TOP HALF: Reading Stand (독서대)
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
                        .padding(16.dp)
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

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = if (totalCount > 0) "Sentence ${currentIndex + 1} / $totalCount" else "Sentence",
                            color = Color(0xFF818CF8),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.ExtraBold
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        val cleanEn = activeSentence?.let { cleanSentenceText(it.en) } ?: "선택된 문장이 없습니다."
                        Text(
                            text = "\"$cleanEn\"",
                            color = Color.White,
                            fontSize = 19.sp,
                            fontWeight = FontWeight.Bold,
                            lineHeight = 28.sp
                        )

                        CoachingGuideCard(
                            guide = activeSentence?.guide ?: "",
                            tip = activeSentence?.tip ?: "",
                            modifier = Modifier.padding(top = 10.dp),
                            containerColor = Color(0xFF082F49).copy(alpha = 0.55f),
                            borderColor = Color(0xFF0284C7).copy(alpha = 0.4f)
                        )
                    }

                    if (!activeSentence?.ko.isNullOrEmpty()) {
                        Spacer(modifier = Modifier.height(10.dp))
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
                        Surface(
                            color = Color(0xFF451A03),
                            shape = RoundedCornerShape(6.dp),
                            border = BorderStroke(1.dp, Color(0xFFF59E0B).copy(alpha = 0.5f))
                        ) {
                            Text(
                                text = "반복 ${if (repeatCount >= 999) "무한" else "${repeatCount}회"} 선택됨",
                                color = Color(0xFFFBBF24),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }

                    // Speed Selector Pills
                    SelectionPillsRow(
                        items = listOf(0.8f to "0.8x", 1.0f to "1.0x (보통)", 1.2f to "1.2x"),
                        selectedItem = speed,
                        onSelect = onSelectSpeed,
                        selectedColor = Color(0xFF4F46E5),
                        selectedBorderColor = Color(0xFF818CF8),
                        heightDp = 36
                    )

                    // Repeat Count Selector Pills
                    SelectionPillsRow(
                        items = listOf(1 to "1회", 3 to "3회", 5 to "5회", 999 to "무한 🔁"),
                        selectedItem = repeatCount,
                        onSelect = onSelectRepeat,
                        selectedColor = Color(0xFF047857),
                        selectedBorderColor = Color(0xFF34D399),
                        heightDp = 34
                    )

                    // Central Big Player Controls
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FilledIconButton(
                            onClick = onPrev,
                            modifier = Modifier.size(48.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = IconButtonDefaults.filledIconButtonColors(containerColor = Color(0xFF1E293B))
                        ) {
                            Icon(Icons.Default.SkipPrevious, contentDescription = "Prev", modifier = Modifier.size(24.dp), tint = Color.White)
                        }

                        Spacer(modifier = Modifier.width(18.dp))

                        FilledIconButton(
                            onClick = onTogglePlay,
                            modifier = Modifier.size(64.dp),
                            shape = RoundedCornerShape(22.dp),
                            colors = IconButtonDefaults.filledIconButtonColors(containerColor = Color(0xFF6366F1))
                        ) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (isPlaying) "Pause" else "Play",
                                modifier = Modifier.size(34.dp),
                                tint = Color.White
                            )
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        FilledIconButton(
                            onClick = onStop,
                            modifier = Modifier.size(48.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = IconButtonDefaults.filledIconButtonColors(containerColor = Color(0xFF1E293B))
                        ) {
                            Icon(Icons.Default.Stop, contentDescription = "Stop", modifier = Modifier.size(24.dp), tint = Color(0xFFEF4444))
                        }

                        Spacer(modifier = Modifier.width(18.dp))

                        FilledIconButton(
                            onClick = onNext,
                            modifier = Modifier.size(48.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = IconButtonDefaults.filledIconButtonColors(containerColor = Color(0xFF1E293B))
                        ) {
                            Icon(Icons.Default.SkipNext, contentDescription = "Next", modifier = Modifier.size(24.dp), tint = Color.White)
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
