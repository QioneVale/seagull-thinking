@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.qionevale.seagullthinking.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.qionevale.seagullthinking.data.Fmt
import com.qionevale.seagullthinking.data.RepeatUi
import kotlinx.coroutines.delay

@Composable
fun PlayerScreen(
    vm: AppViewModel,
    onBack: () -> Unit
) {
    val current by vm.player.current.collectAsState()
    val playing by vm.player.playing.collectAsState()
    val position by vm.player.position.collectAsState()
    val duration by vm.player.duration.collectAsState()
    val shuffle by vm.player.shuffle.collectAsState()
    val repeat by vm.player.repeat.collectAsState()
    val modeName by vm.player.modeName.collectAsState()
    val sleepAt by vm.player.sleepAt.collectAsState()
    val allTracks by vm.tracks.collectAsState()
    val modes by vm.modes.collectAsState()

    val track = current
    val mode = modes.firstOrNull { m ->
        allTracks.any { it.id == track?.id && it.modeId == m.id }
    }
    val stat = allTracks.firstOrNull { it.id == track?.id }

    var showTimer by remember { mutableStateOf(false) }
    var dragValue by remember { mutableFloatStateOf(-1f) }
    var now by remember { mutableStateOf(System.currentTimeMillis()) }

    LaunchedEffect(Unit) {
        while (true) {
            now = System.currentTimeMillis()
            delay(1000)
        }
    }

    val rotation = remember { Animatable(0f) }
    LaunchedEffect(playing, track?.id) {
        if (playing) {
            while (true) {
                rotation.animateTo(rotation.value + 360f, tween(16000, easing = LinearEasing))
            }
        }
    }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    Modifier
                        .size(38.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f), CircleShape)
                        .clickable { onBack() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.KeyboardArrowDown, "收起", modifier = Modifier.size(22.dp))
                }
                Spacer(Modifier.weight(1f))
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "正在播放",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        modeName.ifBlank { mode?.name ?: "—" },
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Spacer(Modifier.weight(1f))
                Box(
                    Modifier
                        .size(38.dp)
                        .background(
                            if (sleepAt != null) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                            CircleShape
                        )
                        .clickable { showTimer = true },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Rounded.Timer,
                        "定时",
                        modifier = Modifier.size(19.dp),
                        tint = if (sleepAt != null) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (sleepAt != null) {
                val left = ((sleepAt!! - now) / 1000).coerceAtLeast(0)
                Text(
                    "定时停止：还剩 ${left / 60}:${"%02d".format(left % 60)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showTimer = true },
                    textAlign = TextAlign.Center
                )
            }

            Spacer(Modifier.weight(1f))

            if (track == null) {
                Column(
                    Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    MusicCover(path = null, size = 200.dp, seed = "海")
                    Spacer(Modifier.height(20.dp))
                    Text("还没有在听的歌", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "去「明志」里选一个模式，点首歌",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                Box(contentAlignment = Alignment.Center) {
                    Box(
                        Modifier
                            .size(268.dp)
                            .graphicsLayer { rotationZ = rotation.value }
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    listOf(
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.20f),
                                        Color.Transparent
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        MusicCover(
                            path = mode?.coverPath,
                            size = 228.dp,
                            seed = mode?.name ?: track.title
                        )
                        // 唱片中心孔
                        Box(
                            Modifier
                                .size(28.dp)
                                .background(MaterialTheme.colorScheme.background, CircleShape)
                        )
                    }
                }
                Spacer(Modifier.height(28.dp))
                Text(
                    track.title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(horizontal = 32.dp),
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    "听完 ${stat?.playCount ?: 0} 次 · 累计 ${Fmt.duration(stat?.listenedMs ?: 0)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(Modifier.weight(1f))

            val total = if (duration > 0) duration else (track?.durationMs ?: 0L)
            val shown = if (dragValue >= 0f) dragValue else {
                if (total > 0) (position.toFloat() / total).coerceIn(0f, 1f) else 0f
            }
            Column(Modifier.padding(horizontal = 28.dp)) {
                Slider(
                    value = shown,
                    onValueChange = { dragValue = it },
                    onValueChangeFinished = {
                        if (total > 0) vm.player.seekTo((dragValue * total).toLong())
                        dragValue = -1f
                    },
                    enabled = track != null,
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.primary,
                        activeTrackColor = MaterialTheme.colorScheme.primary
                    )
                )
                Row(Modifier.fillMaxWidth()) {
                    Text(
                        Fmt.shortDuration(if (dragValue >= 0f) (dragValue * total).toLong() else position),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.weight(1f))
                    Text(
                        Fmt.shortDuration(total),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.height(6.dp))

            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 30.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconAction(
                    icon = Icons.Rounded.Shuffle,
                    active = shuffle,
                    label = "随机",
                    onClick = { vm.player.toggleShuffle() }
                )
                IconAction(
                    icon = Icons.Rounded.SkipPrevious,
                    label = "上一首",
                    size = 34.dp,
                    onClick = { vm.player.previous() }
                )
                Box(
                    Modifier
                        .size(74.dp)
                        .background(MaterialTheme.colorScheme.primary, CircleShape)
                        .clickable { vm.player.toggle() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        if (playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        if (playing) "暂停" else "播放",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(34.dp)
                    )
                }
                IconAction(
                    icon = Icons.Rounded.SkipNext,
                    label = "下一首",
                    size = 34.dp,
                    onClick = { vm.player.next() }
                )
                IconAction(
                    icon = if (repeat == RepeatUi.ONE) Icons.Rounded.RepeatOne else Icons.Rounded.Repeat,
                    active = repeat != RepeatUi.OFF,
                    label = repeat.label,
                    onClick = { vm.player.cycleRepeat() }
                )
            }

            Spacer(Modifier.height(10.dp))
            Text(
                "${if (shuffle) "随机播放" else "顺序播放"} · ${repeat.label}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(90.dp))
        }
    }

    if (showTimer) {
        AlertDialog(
            onDismissRequest = { showTimer = false },
            title = { Text("定时停止", style = MaterialTheme.typography.titleMedium) },
            text = {
                Column {
                    listOf(15, 30, 45, 60).forEach { m ->
                        TimerRow("$m 分钟后停止") {
                            vm.player.startSleepTimer(m)
                            showTimer = false
                        }
                    }
                    TimerRow("播完这一首就停") {
                        vm.player.stopAfterCurrent()
                        showTimer = false
                    }
                    if (sleepAt != null) {
                        TimerRow("取消定时", error = true) {
                            vm.player.cancelSleepTimer()
                            showTimer = false
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showTimer = false }) { Text("关闭") }
            }
        )
    }
}

@Composable
private fun TimerRow(text: String, error: Boolean = false, onClick: () -> Unit) {
    Surface(
        shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
        color = Color.Transparent,
        onClick = onClick
    ) {
        Text(
            text,
            Modifier
                .fillMaxWidth()
                .padding(vertical = 13.dp, horizontal = 4.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = if (error) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun IconAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    active: Boolean = false,
    size: androidx.compose.ui.unit.Dp = 24.dp,
    onClick: () -> Unit
) {
    Box(
        Modifier
            .size(46.dp)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            icon,
            label,
            modifier = Modifier.size(size),
            tint = if (active) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
