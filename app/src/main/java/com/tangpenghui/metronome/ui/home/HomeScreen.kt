package com.tangpenghui.metronome.ui.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tangpenghui.metronome.audio.BeatType
import com.tangpenghui.metronome.controller.MetronomeState
import com.tangpenghui.metronome.controller.RunState
import com.tangpenghui.metronome.controller.TimerMode
import com.tangpenghui.metronome.ui.theme.AccentBlue
import com.tangpenghui.metronome.ui.theme.AccentGreen
import com.tangpenghui.metronome.ui.theme.BgCard
import com.tangpenghui.metronome.ui.theme.SurfaceDim
import com.tangpenghui.metronome.ui.theme.TextMuted
import kotlinx.coroutines.delay

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onOpenCalendar: () -> Unit,
    onExit: () -> Unit = {},
    triggerProvider: () -> BeatType = { BeatType.NONE },
    modifier: Modifier = Modifier
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    var beatType by remember { mutableStateOf(BeatType.NONE) }
    LaunchedEffect(state.runState) {
        while (state.runState == RunState.RUNNING) {
            beatType = triggerProvider()
            delay(40)
        }
        beatType = BeatType.NONE
    }

    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Spacer(Modifier.height(20.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("SUPER JOGGING", style = MaterialTheme.typography.titleLarge, color = AccentGreen)
            Text("${state.bpm} BPM • CADENCE", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextMuted)
        }

        TimeCard(state, beatType, viewModel::start, viewModel::pause, viewModel::stop, onExit)
        ModeSelector(state.mode, state.customMinutes, viewModel::setMode)
        BpmSelector(state.bpm, viewModel::setBpm)
        VolumeCard(state.volume, viewModel::setVolume)
        OutlinedButton(onClick = onOpenCalendar, modifier = Modifier.fillMaxWidth()) {
            Text("📊  运动记录", color = AccentGreen)
        }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun TimeCard(
    state: MetronomeState, beatTrigger: BeatType,
    onStart: () -> Unit, onPause: () -> Unit, onStop: () -> Unit,
    onExit: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = BgCard),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth().heightIn(min = 180.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                LedIndicator(beatTrigger)
                Spacer(Modifier.width(12.dp))
                val statusText = when (state.runState) {
                    RunState.RUNNING -> "• RUNNING •"
                    RunState.PAUSED -> "• PAUSED •"
                    RunState.STOPPED -> "• READY •"
                }
                Text(statusText, fontWeight = FontWeight.Bold, color = TextMuted)
            }
            Spacer(Modifier.height(12.dp))
            TimeDisplay(
                seconds = if (state.mode == TimerMode.MODE_FREE) state.timeElapsedSec else state.timeLeftSec,
                style = MaterialTheme.typography.displayLarge
            )
            Spacer(Modifier.height(8.dp))
            Text(
                if (state.mode == TimerMode.MODE_FREE)
                    "已用跑时: ${formatTime(state.timeElapsedSec)} / 自由畅跑无限制"
                else
                    "已用跑时: ${formatTime(state.timeElapsedSec)} / 目标: ${formatTime(state.totalDurationSec)}",
                color = TextMuted, fontSize = 12.sp
            )
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onStart,
                    enabled = state.runState != RunState.RUNNING,
                    colors = ButtonDefaults.buttonColors(containerColor = if (state.runState == RunState.RUNNING) SurfaceDim else AccentGreen)
                ) { Text(if (state.runState == RunState.PAUSED) "继续" else "开始") }
                Button(
                    onClick = onPause,
                    enabled = state.runState == RunState.RUNNING,
                    colors = ButtonDefaults.buttonColors(containerColor = if (state.runState == RunState.RUNNING) AccentBlue else SurfaceDim)
                ) { Text("暂停") }
                Button(
                    onClick = { onStop(); onExit() },
                    enabled = state.runState != RunState.STOPPED,
                    colors = ButtonDefaults.buttonColors(containerColor = SurfaceDim)
                ) { Text("结束") }
            }
        }
    }
}

@Composable
private fun ModeSelector(current: TimerMode, customMinutes: Int, onModeChange: (TimerMode, Int?) -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = BgCard), shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text("定时跑步模式", fontWeight = FontWeight.Bold, color = TextMuted)
            Spacer(Modifier.height(8.dp))
            val labels = listOf("30分钟" to TimerMode.MODE_30MIN, "45分钟" to TimerMode.MODE_45MIN,
                "自定义" to TimerMode.MODE_CUSTOM, "自由模式" to TimerMode.MODE_FREE)
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                labels.forEachIndexed { i, (label, mode) ->
                    SegmentedButton(
                        selected = current == mode,
                        onClick = { onModeChange(mode, customMinutes) },
                        shape = SegmentedButtonDefaults.itemShape(i, labels.size),
                        colors = SegmentedButtonDefaults.colors(activeContainerColor = AccentGreen)
                    ) { Text(label, fontSize = 12.sp) }
                }
            }
            if (current == TimerMode.MODE_CUSTOM) {
                Spacer(Modifier.height(12.dp))
                var sliderValue by remember { mutableFloatStateOf(customMinutes.toFloat()) }
                Text("自定义时长: ${sliderValue.toInt()} 分钟", color = AccentGreen)
                Slider(value = sliderValue, onValueChange = { sliderValue = it },
                    onValueChangeFinished = { onModeChange(TimerMode.MODE_CUSTOM, sliderValue.toInt()) },
                    valueRange = 5f..120f, steps = 22)
            }
        }
    }
}

@Composable
private fun BpmSelector(bpm: Int, onPreset: (Int) -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = BgCard), shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text("步频节拍", fontWeight = FontWeight.Bold, color = TextMuted)
            Spacer(Modifier.height(8.dp))
            val labels = listOf("轻松120" to 120, "标准150" to 150, "高效180" to 180, "自定义" to -1)
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                labels.forEachIndexed { i, (label, value) ->
                    SegmentedButton(
                        selected = if (value == -1) bpm !in listOf(120, 150, 180) else bpm == value,
                        onClick = { if (value != -1) onPreset(value) },
                        shape = SegmentedButtonDefaults.itemShape(i, labels.size),
                        colors = SegmentedButtonDefaults.colors(activeContainerColor = AccentGreen)
                    ) { Text(label, fontSize = 12.sp) }
                }
            }
            if (bpm !in listOf(120, 150, 180)) {
                Spacer(Modifier.height(12.dp))
                var sliderValue by remember { mutableFloatStateOf(bpm.toFloat()) }
                Text("自定义步频: ${sliderValue.toInt()} BPM", color = AccentGreen)
                Slider(value = sliderValue, onValueChange = { sliderValue = it },
                    onValueChangeFinished = { onPreset(sliderValue.toInt()) },
                    valueRange = 120f..200f, steps = 79)
            }
        }
    }
}

@Composable
private fun VolumeCard(volume: Float, onChange: (Float) -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = BgCard), shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Text("背景木鱼音量: ${(volume * 100).toInt()}%", color = TextMuted)
            Slider(value = volume, onValueChange = onChange, modifier = Modifier.width(200.dp), valueRange = 0f..1f)
        }
    }
}

internal fun formatTime(sec: Int): String {
    val m = sec / 60; val s = sec % 60
    return "%02d:%02d".format(m, s)
}
