@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.qionevale.seagullthinking.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.qionevale.seagullthinking.data.AdRecord
import com.qionevale.seagullthinking.data.AnalysisItem
import com.qionevale.seagullthinking.data.Fmt
import java.time.Instant
import java.time.LocalDate
import kotlinx.coroutines.launch
import java.time.ZoneOffset

@Composable
fun RecordsScreen(vm: AppViewModel) {
    val records by vm.records.collectAsState()
    val analyses by vm.analyses.collectAsState()
    val scope = rememberCoroutineScope()

    var date by remember { mutableStateOf(LocalDate.now()) }
    var showPicker by remember { mutableStateOf(false) }
    var expandedId by remember { mutableStateOf<Long?>(null) }
    var noteEditing by remember { mutableStateOf<AdRecord?>(null) }

    val dayRecords = records.filter { it.dayKey == date.toString() }
    val byRecord = remember(analyses) { analyses.groupBy { it.recordId } }

    Column(Modifier.fillMaxSize()) {
        PageHeader(
            title = "记录",
            subtitle = "一共 ${records.size} 条广告复盘"
        )

        // 日期切换
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CircleIcon(Icons.Rounded.ChevronLeft, "前一天") {
                date = date.minusDays(1)
                expandedId = null
            }
            Spacer(Modifier.width(10.dp))
            Box(
                Modifier
                    .weight(1f)
                    .height(52.dp)
                    .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(18.dp))
                    .clickable { showPicker = true },
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Rounded.CalendarMonth,
                        null,
                        Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.width(8.dp))
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        val dayLabel = Fmt.dayLabel(date)
                        val dayFull = "${date.monthValue}月${date.dayOfMonth}日"
                        Text(
                            if (dayLabel == dayFull) dayFull else "$dayLabel · $dayFull",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            "${dayRecords.size} 条记录",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            Spacer(Modifier.width(10.dp))
            CircleIcon(Icons.Rounded.ChevronRight, "后一天") {
                if (date.isBefore(LocalDate.now())) {
                    date = date.plusDays(1)
                    expandedId = null
                }
            }
        }

        if (dayRecords.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "这一天没有记录",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "去「搞薯条」看一条，回来就有",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }
        } else {
            // 内容从中线向上下两端展开，约占剩余空间的三分之二
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 18.dp),
                contentPadding = PaddingValues(vertical = 20.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                items(dayRecords, key = { it.id }) { record ->
                    RecordCard(
                        record = record,
                        items = byRecord[record.id].orEmpty(),
                        expanded = expandedId == record.id,
                        onToggle = {
                            expandedId = if (expandedId == record.id) null else record.id
                        },
                        onEditNote = { noteEditing = record },
                        onDelete = { scope.launch { vm.deleteRecord(record) } }
                    )
                    Spacer(Modifier.height(10.dp))
                }
            }
            Text(
                "点开一条可看：广告内容 · 分析 · 随笔",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                modifier = Modifier.padding(bottom = 92.dp)
            )
        }
    }

    if (showPicker) {
        val state = rememberDatePickerState(
            initialSelectedDateMillis = date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { showPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let {
                        date = Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate()
                        expandedId = null
                    }
                    showPicker = false
                }) { Text("看这天") }
            },
            dismissButton = { TextButton(onClick = { showPicker = false }) { Text("取消") } }
        ) {
            DatePicker(state = state)
        }
    }

    noteEditing?.let { record ->
        InputDialog(
            title = "写这条随笔",
            initial = record.note,
            placeholder = "看完这条广告，你联想到什么？",
            onDismiss = { noteEditing = null },
            onConfirm = { text ->
                scope.launch { vm.saveNote(record, text) }
                noteEditing = null
            }
        )
    }
}

@Composable
private fun CircleIcon(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Box(
        Modifier
            .size(40.dp)
            .background(MaterialTheme.colorScheme.surface, CircleShape)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, label, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun RecordCard(
    record: AdRecord,
    items: List<AnalysisItem>,
    expanded: Boolean,
    onToggle: () -> Unit,
    onEditNote: () -> Unit,
    onDelete: () -> Unit
) {
    var section by remember(record.id) { mutableStateOf("ad") }
    var confirmDelete by remember { mutableStateOf(false) }

    SeagullCard(
        modifier = Modifier.pointerInput(record.id) {
            detectTapGestures(
                onTap = { onToggle() },
                onLongPress = { confirmDelete = true }
            )
        }
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(38.dp)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "${Fmt.toLocal(record.watchedAt).hour}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    record.title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    "${Fmt.clock(record.watchedAt)} · ${sourceLabel(record.source)} · ${items.size} 个分析点",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                if (expanded) "收起" else "展开",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        AnimatedVisibility(visible = expanded) {
            Column {
                Spacer(Modifier.height(14.dp))
                // 广告内容
                SectionRow(
                    title = "广告内容",
                    active = section == "ad",
                    onClick = { section = if (section == "ad") "" else "ad" }
                )
                if (section == "ad") {
                    Column(Modifier.padding(start = 4.dp, top = 6.dp, bottom = 4.dp)) {
                        Text(record.title, style = MaterialTheme.typography.bodyMedium)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "来源：${sourceLabel(record.source)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            "看完时间：${Fmt.full(record.watchedAt)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // 分析
                SectionRow(
                    title = "分析（${items.size}）",
                    active = section == "analysis",
                    onClick = { section = if (section == "analysis") "" else "analysis" }
                )
                if (section == "analysis") {
                    Column(Modifier.padding(start = 4.dp, top = 6.dp, bottom = 4.dp)) {
                        if (items.isEmpty()) {
                            Text(
                                "这条没有填分析",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        items.forEach { a ->
                            Text(
                                a.label,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                a.content,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                        }
                        if (items.isNotEmpty()) {
                            Text(
                                "写完分析：${Fmt.full(record.analysisDoneAt)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // 随笔
                SectionRow(
                    title = if (record.note.isBlank()) "随笔（空）" else "随笔",
                    active = section == "note",
                    onClick = { section = if (section == "note") "" else "note" }
                )
                if (section == "note") {
                    Column(Modifier.padding(start = 4.dp, top = 6.dp, bottom = 4.dp)) {
                        if (record.note.isBlank()) {
                            Text(
                                "还没写",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            Text(record.note, style = MaterialTheme.typography.bodyMedium)
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "写完随笔：${Fmt.full(record.noteAt)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        GhostPill(
                            text = if (record.note.isBlank()) "写随笔" else "修改随笔",
                            onClick = onEditNote
                        )
                    }
                }
            }
        }
    }

    if (confirmDelete) {
        ConfirmDialog(
            title = "删除这条记录？",
            text = "${record.title}\n分析内容和随笔会一起删掉。",
            confirmText = "删除",
            onDismiss = { confirmDelete = false },
            onConfirm = { confirmDelete = false; onDelete() }
        )
    }
}

@Composable
private fun SectionRow(title: String, active: Boolean, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 11.dp, horizontal = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(6.dp)
                .background(
                    if (active) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.outline,
                    CircleShape
                )
        )
        Spacer(Modifier.width(10.dp))
        Text(
            title,
            style = MaterialTheme.typography.titleSmall,
            color = if (active) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurface
        )
        Spacer(Modifier.weight(1f))
        Icon(
            if (active) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
            null,
            Modifier.size(18.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun sourceLabel(source: String): String = when (source) {
    "SAMPLE" -> "内置示例"
    "LOCAL" -> "本地视频"
    "WEB" -> "网页链接"
    else -> source
}
