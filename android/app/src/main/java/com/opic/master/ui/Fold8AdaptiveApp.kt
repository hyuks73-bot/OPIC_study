package com.opic.master.ui

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.opic.master.data.model.Sentence

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Fold8AdaptiveApp(
    sentences: List<Sentence>,
    isUnfolded: Boolean,
    isFlexMode: Boolean,
    onPlaySentence: (Sentence) -> Unit,
    onSyncGitHub: () -> Unit
) {
    var selectedDay by remember { mutableStateOf("day2") }
    var activeSentence by remember { mutableStateOf<Sentence?>(null) }
    var speed by remember { mutableFloatStateOf(1.0f) }
    var repeatCount by remember { mutableIntStateOf(3) }
    var isRecording by remember { mutableStateOf(false) }

    val filteredSentences = remember(sentences, selectedDay) {
        sentences.filter { it.dayKey == selectedDay }
    }

    LaunchedEffect(filteredSentences) {
        if (activeSentence == null && filteredSentences.isNotEmpty()) {
            activeSentence = filteredSentences.first()
        }
    }

    // Adapt layout according to Galaxy Z Fold 8 posture
    when {
        isFlexMode -> {
            // 1. Flex Mode (Tabletop Posture 90° ~ 115°)
            FlexModeLayout(
                activeSentence = activeSentence,
                speed = speed,
                repeatCount = repeatCount,
                isRecording = isRecording,
                onPlay = { activeSentence?.let(onPlaySentence) },
                onToggleRecord = { isRecording = !isRecording },
                onSpeedChange = { speed = it },
                onRepeatChange = { repeatCount = it }
            )
        }
        isUnfolded -> {
            // 2. Main Display (Unfolded Dual-Pane Studio 2448 x 1848)
            MainDualPaneLayout(
                selectedDay = selectedDay,
                onSelectDay = { selectedDay = it },
                sentences = filteredSentences,
                activeSentence = activeSentence,
                onSelectSentence = {
                    activeSentence = it
                    onPlaySentence(it)
                },
                speed = speed,
                repeatCount = repeatCount,
                onSyncGitHub = onSyncGitHub
            )
        }
        else -> {
            // 3. Cover Display (Folded Compact Thumb-Zone 1248 x 1972)
            CoverDisplayLayout(
                selectedDay = selectedDay,
                onSelectDay = { selectedDay = it },
                sentences = filteredSentences,
                activeSentence = activeSentence,
                onSelectSentence = {
                    activeSentence = it
                    onPlaySentence(it)
                },
                repeatCount = repeatCount,
                onCycleRepeat = {
                    repeatCount = when (repeatCount) {
                        1 -> 3
                        3 -> 5
                        5 -> 999
                        else -> 1
                    }
                },
                onPrevSentence = {
                    val idx = filteredSentences.indexOf(activeSentence)
                    if (idx > 0) {
                        activeSentence = filteredSentences[idx - 1]
                        onPlaySentence(activeSentence!!)
                    }
                },
                onNextSentence = {
                    val idx = filteredSentences.indexOf(activeSentence)
                    if (idx < filteredSentences.size - 1) {
                        activeSentence = filteredSentences[idx + 1]
                        onPlaySentence(activeSentence!!)
                    }
                },
                onSyncGitHub = onSyncGitHub
            )
        }
    }
}

// -------------------------------------------------------------
// 1. COVER DISPLAY LAYOUT (1248 x 1972 Thumb-Zone Optimized)
// -------------------------------------------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CoverDisplayLayout(
    selectedDay: String,
    onSelectDay: (String) -> Unit,
    sentences: List<Sentence>,
    activeSentence: Sentence?,
    onSelectSentence: (Sentence) -> Unit,
    repeatCount: Int,
    onCycleRepeat: () -> Unit,
    onPrevSentence: () -> Unit,
    onNextSentence: () -> Unit,
    onSyncGitHub: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("OPIc Master IH", fontSize = 16.sp, fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = onSyncGitHub) {
                        Icon(Icons.Default.CloudSync, contentDescription = "GitHub Sync", tint = Color(0xFF34D399))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF0F172A))
            )
        },
        bottomBar = {
            // Pinned Floating Bottom Player Bar (Thumb-Zone)
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color(0xFF0B1120),
                tonalElevation = 8.dp,
                shadowElevation = 16.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                        .navigationBarsPadding(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.width(90.dp)) {
                        Text(
                            text = activeSentence?.id?.uppercase() ?: "Sentence",
                            color = Color(0xFFA5B4FC),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "화면 꺼짐 연속 재생",
                            color = Color(0xFF10B981),
                            fontSize = 10.sp
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        IconButton(onClick = onPrevSentence, modifier = Modifier.size(42.dp)) {
                            Icon(Icons.Default.SkipPrevious, contentDescription = "Prev", tint = Color.White)
                        }
                        FilledIconButton(
                            onClick = { activeSentence?.let(onSelectSentence) },
                            modifier = Modifier.size(48.dp),
                            colors = IconButtonDefaults.filledIconButtonColors(containerColor = Color(0xFF6366F1))
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = "Play", tint = Color.White)
                        }
                        IconButton(onClick = onNextSentence, modifier = Modifier.size(42.dp)) {
                            Icon(Icons.Default.SkipNext, contentDescription = "Next", tint = Color.White)
                        }
                    }

                    OutlinedButton(
                        onClick = onCycleRepeat,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("🔁 ${if (repeatCount >= 999) "무한" else "${repeatCount}회"}", fontSize = 11.sp, color = Color(0xFFFBBF24))
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
            // Day Filter Tabs
            LazyRow(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val days = listOf("day1" to "Day 1", "day2" to "Day 2", "day3" to "Day 3", "day4" to "Day 4", "day5" to "Day 5", "day6" to "Day 6")
                items(days) { (key, label) ->
                    val isSelected = selectedDay == key
                    Surface(
                        shape = CircleShape,
                        color = if (isSelected) Color(0xFF6366F1) else Color(0xFF1E293B),
                        modifier = Modifier.clickable { onSelectDay(key) }
                    ) {
                        Text(
                            text = label,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                            color = if (isSelected) Color.White else Color(0xFF94A3B8),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            // Sentences List
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 20.dp)
            ) {
                items(sentences) { sentence ->
                    val isPlaying = activeSentence?.id == sentence.id
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelectSentence(sentence) }
                            .border(
                                width = if (isPlaying) 2.dp else 1.dp,
                                color = if (isPlaying) Color(0xFF6366F1) else Color(0xFF334155),
                                shape = RoundedCornerShape(14.dp)
                            ),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isPlaying) Color(0xFF1E293B) else Color(0xFF0F172A)
                        ),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = sentence.id.uppercase(),
                                    color = Color(0xFFA5B4FC),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                if (sentence.isDownloaded) {
                                    Text("⚡ 오프라인 저장됨", color = Color(0xFF10B981), fontSize = 10.sp)
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = sentence.en.replace(Regex("<.*?>"), ""),
                                color = Color.White,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                lineHeight = 22.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
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

// -------------------------------------------------------------
// 2. MAIN DUAL-PANE STUDIO LAYOUT (2448 x 1848 Expanded)
// -------------------------------------------------------------
@Composable
fun MainDualPaneLayout(
    selectedDay: String,
    onSelectDay: (String) -> Unit,
    sentences: List<Sentence>,
    activeSentence: Sentence?,
    onSelectSentence: (Sentence) -> Unit,
    speed: Float,
    repeatCount: Int,
    onSyncGitHub: () -> Unit
) {
    Row(modifier = Modifier.fillMaxSize().background(Color(0xFF0B1120))) {
        // Left Pane: Storyboard & Full Paragraph
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .padding(16.dp)
        ) {
            Text(
                "OPIc Master IH · 대화면 듀얼 스튜디오",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(10.dp))
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(sentences) { s ->
                    val isSelected = s.id == activeSentence?.id
                    Surface(
                        color = if (isSelected) Color(0xFF1E293B) else Color(0xFF0F172A),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth().clickable { onSelectSentence(s) }
                    ) {
                        Text(
                            text = s.en.replace(Regex("<.*?>"), ""),
                            modifier = Modifier.padding(12.dp),
                            color = if (isSelected) Color(0xFFA5B4FC) else Color(0xFFCBD5E1),
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }

        // Right Pane: Deep Shadowing & Waveform Studio
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .background(Color(0xFF0F172A))
                .padding(20.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = activeSentence?.id?.uppercase() ?: "Sentence",
                    color = Color(0xFF6366F1),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = activeSentence?.en?.replace(Regex("<.*?>"), "") ?: "",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    lineHeight = 30.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = activeSentence?.ko ?: "",
                    color = Color(0xFF94A3B8),
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(16.dp))

                // Pronunciation Tip Box
                Surface(
                    color = Color(0xFF1E293B),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text("🗣️ 발음 & 낭독 코칭", color = Color(0xFF38BDF8), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(activeSentence?.tip ?: "", color = Color(0xFFE2E8F0), fontSize = 12.sp)
                    }
                }
            }

            // Big Player & Record Controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = { activeSentence?.let(onSelectSentence) },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1))
                ) {
                    Text("🔊 원어민 연속 재생")
                }
                Button(
                    onClick = { },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))
                ) {
                    Text("🎙️ 섀도잉 녹음")
                }
            }
        }
    }
}

// -------------------------------------------------------------
// 3. FLEX MODE LAYOUT (Tabletop 90° Hinge View)
// -------------------------------------------------------------
@Composable
fun FlexModeLayout(
    activeSentence: Sentence?,
    speed: Float,
    repeatCount: Int,
    isRecording: Boolean,
    onPlay: () -> Unit,
    onToggleRecord: () -> Unit,
    onSpeedChange: (Float) -> Unit,
    onRepeatChange: (Int) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize().background(Color(0xFF0B1120))) {
        // Top Screen: Reading Stand
        Box(
            modifier = Modifier.weight(1f).fillMaxWidth().padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = activeSentence?.en?.replace(Regex("<.*?>"), "") ?: "",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    lineHeight = 32.sp
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(text = activeSentence?.ko ?: "", color = Color(0xFF94A3B8), fontSize = 14.sp)
            }
        }

        // Hinge Divider
        Box(modifier = Modifier.fillMaxWidth().height(4.dp).background(Color(0xFF334155)))

        // Bottom Screen: Tabletop Touchpad
        Column(
            modifier = Modifier.weight(1f).fillMaxWidth().background(Color(0xFF0F172A)).padding(16.dp),
            verticalArrangement = Arrangement.SpaceEvenly,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                FilledIconButton(onClick = onPlay, modifier = Modifier.size(64.dp)) {
                    Icon(Icons.Default.PlayArrow, contentDescription = "Play", modifier = Modifier.size(32.dp))
                }
                FilledIconButton(
                    onClick = onToggleRecord,
                    modifier = Modifier.size(64.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(containerColor = Color(0xFFEF4444))
                ) {
                    Icon(Icons.Default.Mic, contentDescription = "Record", modifier = Modifier.size(32.dp))
                }
            }
            Text("반복: ${repeatCount}회 · 화면 꺼짐 무중단 연속 재생", color = Color(0xFF10B981), fontSize = 12.sp)
        }
    }
}
