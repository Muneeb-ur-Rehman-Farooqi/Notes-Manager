package com.muneeb.coursemanager.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import kotlinx.coroutines.delay

@Composable
fun CountdownTimerText(
    targetMillis: Long,
    modifier: Modifier = Modifier
) {
    var remainingMillis by remember {
        mutableStateOf(targetMillis - System.currentTimeMillis())
    }

    LaunchedEffect(targetMillis) {
        while (true) {
            remainingMillis = targetMillis - System.currentTimeMillis()
            if (remainingMillis <= 0L) break
            delay(1000L)
        }
    }

    val label = if (remainingMillis <= 0L) "Due now" else formatDuration(remainingMillis)

    Text(
        text = label,
        modifier = modifier,
        color = MaterialTheme.colorScheme.error,
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Bold,
        style = MaterialTheme.typography.titleLarge
    )
}

private fun formatDuration(remainingMillis: Long): String {
    val totalSeconds = remainingMillis / 1000L
    val hours = totalSeconds / 3600L
    val minutes = (totalSeconds % 3600L) / 60L
    val seconds = totalSeconds % 60L
    return if (hours > 0L) {
        String.format("%02d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format("%02d:%02d", minutes, seconds)
    }
}