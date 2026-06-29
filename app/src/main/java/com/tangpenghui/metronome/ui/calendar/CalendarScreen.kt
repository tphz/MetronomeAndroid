package com.tangpenghui.metronome.ui.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tangpenghui.metronome.ui.theme.AccentGreen
import com.tangpenghui.metronome.ui.theme.BgCard
import com.tangpenghui.metronome.ui.theme.TextMuted
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(viewModel: CalendarViewModel, onBack: () -> Unit, modifier: Modifier = Modifier) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var currentMonth by remember { mutableStateOf(YearMonth.now()) }
    var selectedDay by remember { mutableStateOf<LocalDate?>(null) }

    LaunchedEffect(currentMonth) {
        viewModel.refreshMonth(currentMonth.year, currentMonth.monthValue)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("运动记录") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BgCard)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(modifier = modifier.padding(padding).fillMaxSize().padding(16.dp)) {
            MonthHeader(currentMonth,
                onPrev = { currentMonth = currentMonth.minusMonths(1) },
                onNext = { currentMonth = currentMonth.plusMonths(1) })
            Spacer(Modifier.height(16.dp))
            MonthStatsCard(state.totalDaysThisMonth, state.totalSecondsThisMonth, state.streakDays)
            Spacer(Modifier.height(16.dp))
            MonthGrid(
                month = currentMonth,
                activeDays = state.monthDays.keys.mapNotNull { runCatching { LocalDate.parse(it) }.getOrNull() }.toSet(),
                selectedDay = selectedDay,
                onDayClick = { selectedDay = it }
            )
            Spacer(Modifier.height(16.dp))
            DayDetailCard(
                day = selectedDay,
                totalSec = selectedDay?.let { state.monthDays[it.toString()] } ?: 0
            )
        }
    }
}

@Composable
private fun MonthHeader(month: YearMonth, onPrev: () -> Unit, onNext: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
        TextButton(onClick = onPrev) { Text("◀", color = AccentGreen) }
        Text(month.format(DateTimeFormatter.ofPattern("yyyy 年 MM 月")), fontWeight = FontWeight.Bold)
        TextButton(onClick = onNext) { Text("▶", color = AccentGreen) }
    }
}

@Composable
private fun MonthStatsCard(totalDays: Int, totalSeconds: Int, streakDays: Int) {
    Card(colors = CardDefaults.cardColors(containerColor = BgCard), shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text("本月统计", color = TextMuted, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.SpaceAround, modifier = Modifier.fillMaxWidth()) {
                StatCell("跑步天数", "$totalDays")
                StatCell("总时长", "${totalSeconds / 60} 分")
                StatCell("连续天数", "$streakDays")
            }
        }
    }
}

@Composable
private fun StatCell(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontSize = 24.sp, color = AccentGreen, fontWeight = FontWeight.Bold)
        Text(label, color = TextMuted, fontSize = 11.sp)
    }
}

@Composable
private fun MonthGrid(
    month: YearMonth, activeDays: Set<LocalDate>,
    selectedDay: LocalDate?, onDayClick: (LocalDate) -> Unit
) {
    val firstDay = month.atDay(1)
    val firstDayOfWeek = firstDay.dayOfWeek.value % 7
    val daysInMonth = month.lengthOfMonth()
    val today = LocalDate.now()
    val cells = List<LocalDate?>(firstDayOfWeek) { null } +
                (1..daysInMonth).map { month.atDay(it) }

    Card(colors = CardDefaults.cardColors(containerColor = BgCard), shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(8.dp)) {
            Row(horizontalArrangement = Arrangement.SpaceAround, modifier = Modifier.fillMaxWidth()) {
                listOf("日", "一", "二", "三", "四", "五", "六").forEach {
                    Text(it, color = TextMuted, fontSize = 12.sp, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                }
            }
            Spacer(Modifier.height(4.dp))
            cells.chunked(7).forEach { week ->
                Row(modifier = Modifier.fillMaxWidth()) {
                    week.forEach { day ->
                        val bg = when {
                            day == null -> Color.Transparent
                            day == today -> AccentGreen.copy(alpha = 0.3f)
                            day == selectedDay -> AccentGreen.copy(alpha = 0.5f)
                            day in activeDays -> AccentGreen.copy(alpha = 0.2f)
                            else -> Color.Transparent
                        }
                        Box(
                            modifier = Modifier.weight(1f).aspectRatio(1f).padding(2.dp)
                                .background(bg, RoundedCornerShape(4.dp))
                                .let { if (day != null) it.clickable { onDayClick(day) } else it },
                            contentAlignment = Alignment.Center
                        ) {
                            if (day != null) {
                                Text("${day.dayOfMonth}", fontSize = 12.sp,
                                    color = if (day == today) AccentGreen else Color.White)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DayDetailCard(day: LocalDate?, totalSec: Int) {
    if (day == null) return
    Card(colors = CardDefaults.cardColors(containerColor = BgCard), shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text(day.format(DateTimeFormatter.ofPattern("yyyy 年 MM 月 dd 日")), fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text(
                if (totalSec > 0) "总时长: ${totalSec / 60} 分 ${totalSec % 60} 秒" else "该日无运动记录",
                color = if (totalSec > 0) AccentGreen else TextMuted
            )
        }
    }
}
