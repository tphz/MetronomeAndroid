package com.tangpenghui.metronome.ui.home

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.tangpenghui.metronome.audio.BeatType
import com.tangpenghui.metronome.ui.theme.AccentBlue
import com.tangpenghui.metronome.ui.theme.AccentGreen
import com.tangpenghui.metronome.ui.theme.SurfaceDim
import kotlinx.coroutines.delay

@Composable
fun LedIndicator(trigger: BeatType, modifier: Modifier = Modifier) {
    var visible by remember { mutableStateOf(false) }
    var color by remember { mutableStateOf(Color.Transparent) }

    LaunchedEffect(trigger) {
        if (trigger != BeatType.NONE) {
            color = if (trigger == BeatType.HEAVY) AccentGreen else AccentBlue
            visible = true
            delay(90)
            visible = false
        }
    }

    val alpha by animateFloatAsState(
        targetValue = if (visible) 1.0f else 0.15f,
        animationSpec = tween(80), label = "led-alpha"
    )

    Box(
        modifier = modifier.size(16.dp).alpha(alpha)
            .background(if (visible) color else SurfaceDim, CircleShape)
    )
}

@Composable
fun TimeDisplay(seconds: Int, style: androidx.compose.ui.text.TextStyle) {
    val m = seconds / 60; val s = seconds % 60
    Text("%02d:%02d".format(m, s), style = style)
}
