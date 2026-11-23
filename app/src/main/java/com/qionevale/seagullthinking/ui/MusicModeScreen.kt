@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.qionevale.seagullthinking.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.LibraryAdd
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.qionevale.seagullthinking.data.Fmt
import com.qionevale.seagullthinking.data.MusicMode
import com.qionevale.seagullthinking.data.Track

@Composable
fun MusicModeScreen(
    vm: AppViewModel,
    modeId: Long,
    onBack: () -> Unit,
    onOpenPlayer: () -> Unit
) {
    val modes by vm.modes.collectAsState()
    val allTracks by vm.tracks.collectAsState()
    val bili by vm.bili.collectAsState()
    val current by vm.player.current.collectAsState()
    val mode = modes.firstOrNull { it.id == modeId }
    val tracks = allTracks.filter { it.modeId == modeId }

    var bv by remember { mutableStateOf("") }
    var renaming by remember { mutableStateOf(false) }
    var confirmingDelete by remember { mutableStateOf<Track?>(null) }
    var confirmDeleteMode by remember { mutableStateOf(false) }

    val audioPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenMultipleDocuments()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) vm.addLocalTracks(modeId, uris) { n -> vm.say("已导入 $n 首") }
    }
    val coverPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null && mode != null) vm.renameMode(mode, mode.name, uri)
    }

    if (mode == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("这个模式不见了", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(12.dp))
                PrimaryPill("返回", onClick = onBack)
            }
        }
        return
    }

    Column(Modifier.fillMaxSize()) {
        PageHeader(
            title = mode.name,
            subtitle = "${tracks.size} 首 · 共 ${Fmt.duration(tracks.sumOf { it.listenedMs })}",
            onBack = onBack,
            actions = {
                GhostPill(text = "改名", icon = Icons.Rounded.Edit, onClick = { renaming = true })
            }
        )

        LazyColumn(
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 130.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    MusicCover(
                        path = mode.coverPath,
                        size = 96.dp,
                        seed = mode.name,
                        modifier = Modifier.clickable { coverPicker.launch("image/*") }
                    )
                    Spacer(Modifier.width(16.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            "点封面换图",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            PrimaryPill(
                                text = "播放全部",
                                icon = Icons.Rounded.PlayArrow,
                                enabled = tracks.isNotEmpty(),
                                onClick = {
                                    vm.player.playMode(tracks, 0, mode.name)
                                    onOpenPlayer()
                                }
                            )
                        }
                    }
                }
            }

            item { Spacer(Modifier.height(6.dp)) }

            item {
                SeagullCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "导入音乐",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.weight(1f)
                        )
                        GhostPill(
                            text = "本地文件",
                            icon = Icons.Rounded.LibraryAdd,
                            onClick = { audioPicker.launch(arrayOf("audio/*")) }
                        )
                    }
                    Spacer(Modifier.height(14.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.weight(1f)) {
                            SeagullTextField(
                                value = bv,
                                onValueChange = { bv = it },
                                placeholder = "粘贴 B 站 BV 号",
                                singleLine = true
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                            onClick = { vm.importBili(modeId, bv) }
                        ) {
                            Box(Modifier.size(46.dp), contentAlignment = Alignment.Center) {
                                if (bili.loading) {
                                    CircularProgressIndicator(
                                        Modifier.size(20.dp),
                                        color = MaterialTheme.colorScheme.onPrimary,
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Icon(Icons.Rounded.Download, "导入", Modifier.size(20.dp))
                                }
                            }
                        }
                    }
                    if (bili.visible) {
                        Spacer(Modifier.height(10.dp))
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (bili.error) MaterialTheme.colorScheme.error.copy(alpha = 0.10f)
                            else MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
                        ) {
                            Row(
                                Modifier.padding(12.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Text(
                                    bili.text,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (bili.error) MaterialTheme.colorScheme.error
                                    else MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(Modifier.width(8.dp))
                                Icon(
                                    Icons.Rounded.Close,
                                    "关闭",
                                    Modifier
                                        .size(16.dp)
                                        .clickable { vm.dismissBili() },
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    Text(
                        "B 站音频会下载为本机 m4a 文件，只供个人学习使用，请尊重原作者版权。",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (tracks.isEmpty()) {
                item { EmptyHint("还没有歌。从本地导入，或者贴一个 BV 号。") }
            }

            itemsIndexed(tracks, key = { _, t -> t.id }) { i, track ->
                TrackRow(
                    index = i + 1,
                    track = track,
                    playing = current?.id == track.id,
                    onPlay = {
                        vm.player.playMode(tracks, i, mode.name)
                        onOpenPlayer()
                    },
                    onDelete = { confirmingDelete = track }
                )
            }

            item {
                Spacer(Modifier.height(10.dp))
                Text(
                    "删除这个模式",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.error.copy(alpha = 0.85f),
                    modifier = Modifier
                        .clickable { confirmDeleteMode = true }
                        .padding(vertical = 8.dp, horizontal = 4.dp)
                )
            }
        }
    }

    if (renaming) {
        InputDialog(
            title = "改个名字",
            initial = mode.name,
            onDismiss = { renaming = false },
            onConfirm = { name -> vm.renameMode(mode, name); renaming = false }
        )
    }

    confirmingDelete?.let { t ->
        ConfirmDialog(
            title = "删掉这首歌？",
            text = "${t.title}\n（只是从 App 里移除）",
            confirmText = "删除",
            onDismiss = { confirmingDelete = null },
            onConfirm = { vm.deleteTrack(t); confirmingDelete = null }
        )
    }

    if (confirmDeleteMode) {
        ConfirmDialog(
            title = "删除「${mode.name}」？",
            text = "这个模式里的 ${tracks.size} 首歌会一起移除，且不可恢复。",
            confirmText = "删除模式",
            onDismiss = { confirmDeleteMode = false },
            onConfirm = {
                confirmDeleteMode = false
                vm.deleteMode(mode)
                onBack()
            }
        )
    }
}

@Composable
private fun TrackRow(
    index: Int,
    track: Track,
    playing: Boolean,
    onPlay: () -> Unit,
    onDelete: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = if (playing) MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
        else MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (playing) MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
            else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
        ),
        onClick = onPlay
    ) {
        Row(
            Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier
                    .size(30.dp)
                    .background(
                        if (playing) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (playing) {
                    Icon(
                        Icons.Rounded.PlayArrow,
                        null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                } else {
                    Text(
                        "$index",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    track.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (playing) FontWeight.SemiBold else FontWeight.Normal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    buildString {
                        append(if (track.durationMs > 0) Fmt.shortDuration(track.durationMs) else "--:--")
                        append(" · 听完 ${track.playCount} 次")
                        if (track.listenedMs > 0) append(" · 累计 ${Fmt.duration(track.listenedMs)}")
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Box(
                Modifier
                    .size(30.dp)
                    .clickable { onDelete() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Rounded.Close,
                    "删除",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}
