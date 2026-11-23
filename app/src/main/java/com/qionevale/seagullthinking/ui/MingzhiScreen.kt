@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.qionevale.seagullthinking.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.qionevale.seagullthinking.data.Quote

@Composable
fun MingzhiScreen(
    vm: AppViewModel,
    onOpenMode: (Long) -> Unit,
    onOpenPlayer: () -> Unit
) {
    val quotes by vm.quotes.collectAsState()
    val modes by vm.modes.collectAsState()
    val tracks by vm.tracks.collectAsState()

    var showAdd by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<Quote?>(null) }
    var confirmDelete by remember { mutableStateOf<Quote?>(null) }
    var showNewMode by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize()) {
        PageHeader(
            title = "明志",
            subtitle = "写下来的话，会慢慢变成方向",
            actions = {
                Box(
                    Modifier
                        .size(38.dp)
                        .background(MaterialTheme.colorScheme.surface, CircleShape)
                        .clickable { showAdd = true },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Rounded.Add,
                        "添加语录",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        )

        Spacer(Modifier.height(6.dp))

        BoxWithConstraints(
            Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 16.dp)
                .clip(RoundedCornerShape(26.dp))
                .background(
                    Brush.verticalGradient(
                        listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.10f),
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.02f)
                        )
                    )
                )
        ) {
            val wallW = maxWidth
            val wallH = maxHeight
            Canvas(Modifier.fillMaxSize()) {
                val step = 22.dp.toPx()
                val dot = Color.Gray.copy(alpha = 0.14f)
                var x = step
                while (x < size.width) {
                    var y = step
                    while (y < size.height) {
                        drawCircle(dot, radius = 1.2f, center = Offset(x, y))
                        y += step
                    }
                    x += step
                }
            }

            if (quotes.isEmpty()) {
                Column(
                    Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "墙上还空着",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(8.dp))
                    GhostPill(text = "写第一句", icon = Icons.Rounded.Add, onClick = { showAdd = true })
                }
            }

            quotes.forEach { q ->
                var rx by remember(q.id) { mutableFloatStateOf(q.xRatio) }
                var ry by remember(q.id) { mutableFloatStateOf(q.yRatio) }
                val cardW = 188.dp
                Box(
                    Modifier
                        .offset(x = (wallW * rx) - cardW * 0.5f, y = (wallH * ry) - 28.dp)
                        .width(cardW)
                        .pointerInput(q.id) {
                            detectDragGestures(
                                onDragEnd = { vm.moveQuote(q, rx, ry) },
                                onDrag = { change, drag ->
                                    change.consume()
                                    val wPx = size.width.toFloat().coerceAtLeast(1f)
                                    val hPx = size.height.toFloat().coerceAtLeast(1f)
                                    rx = (rx + drag.x / wPx).coerceIn(0.16f, 0.84f)
                                    ry = (ry + drag.y / hPx).coerceIn(0.08f, 0.92f)
                                }
                            )
                        }
                        .pointerInput(q.id) {
                            detectTapGestures(
                                onTap = { editing = q },
                                onLongPress = { confirmDelete = q }
                            )
                        }
                ) {
                    QuoteCard(q)
                }
            }

            Text(
                "拖一下挪位置 · 点一下改写 · 长按删除",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 12.dp)
            )
        }

        Spacer(Modifier.height(14.dp))

        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "音乐模式",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f)
            )
            Text(
                "点进去听歌",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.height(10.dp))

        LazyRow(
            contentPadding = PaddingValues(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.height(116.dp)
        ) {
            item {
                Column(
                    Modifier
                        .width(84.dp)
                        .clickable { showNewMode = true },
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Rounded.Add, "新建模式", tint = MaterialTheme.colorScheme.primary)
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "新建",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            items(modes, key = { it.id }) { mode ->
                Column(
                    Modifier
                        .width(84.dp)
                        .clickable { onOpenMode(mode.id) },
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    MusicCover(path = mode.coverPath, size = 64.dp, seed = mode.name)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        mode.name,
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        "${tracks.count { it.modeId == mode.id }} 首",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(Modifier.height(10.dp))
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            GhostPill(
                text = "打开播放器",
                icon = Icons.Rounded.GraphicEq,
                modifier = Modifier.weight(1f),
                onClick = onOpenPlayer
            )
            GhostPill(
                text = "新建模式",
                icon = Icons.Rounded.Add,
                modifier = Modifier.weight(1f),
                onClick = { showNewMode = true }
            )
        }
        Spacer(Modifier.height(84.dp))
    }

    if (showAdd) {
        InputDialog(
            title = "写一句给自己的话",
            placeholder = "比如：先把一件事做透，再谈第二件。",
            onDismiss = { showAdd = false },
            onConfirm = { text ->
                vm.addQuote(
                    text,
                    (0.20f + (0..40).random() / 100f),
                    (0.18f + (0..50).random() / 100f)
                )
                showAdd = false
            }
        )
    }

    editing?.let { q ->
        InputDialog(
            title = "改写这句话",
            initial = q.text,
            onDismiss = { editing = null },
            onConfirm = { text -> vm.editQuote(q, text); editing = null }
        )
    }

    confirmDelete?.let { q ->
        ConfirmDialog(
            title = "删掉这句话？",
            text = q.text,
            confirmText = "删除",
            onDismiss = { confirmDelete = null },
            onConfirm = { vm.deleteQuote(q); confirmDelete = null }
        )
    }

    if (showNewMode) {
        NewModeDialog(
            onDismiss = { showNewMode = false },
            onConfirm = { name -> vm.createMode(name, null); showNewMode = false }
        )
    }
}

@Composable
private fun QuoteCard(q: Quote) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
        shadowElevation = 2.dp
    ) {
        Column(Modifier.padding(horizontal = 14.dp, vertical = 11.dp)) {
            Text(
                q.text,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontFamily = FontFamily.Serif,
                    fontSize = 14.sp,
                    lineHeight = 21.sp
                ),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 4,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun NewModeDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("新建音乐模式", style = MaterialTheme.typography.titleMedium) },
        text = {
            Column {
                SeagullTextField(
                    value = name,
                    onValueChange = { name = it },
                    placeholder = "模式名，比如「写代码」「睡前」",
                    singleLine = true
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    "封面可以在模式里再换",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { if (name.isNotBlank()) onConfirm(name) }) { Text("创建") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}
