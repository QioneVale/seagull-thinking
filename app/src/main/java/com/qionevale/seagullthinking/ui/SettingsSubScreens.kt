@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.qionevale.seagullthinking.ui

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.documentfile.provider.DocumentFile
import com.qionevale.seagullthinking.data.Accent
import com.qionevale.seagullthinking.data.AppSettings
import com.qionevale.seagullthinking.data.DarkPref
import com.qionevale.seagullthinking.data.Files
import com.qionevale.seagullthinking.data.Fmt
import com.qionevale.seagullthinking.data.FontPref
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun PersonalizeScreen(vm: AppViewModel, onBack: () -> Unit) {
    val settings by vm.settings.collectAsState()
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()

    val bgPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            scope.launch {
                runCatching {
                    val f = Files.bgFile(ctx)
                    Files.copyTo(ctx, uri, f)
                    vm.settingsRepo.setBackground(f.absolutePath)
                }.onFailure { vm.say("这张图读不了：${it.message}") }
            }
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        PageHeader(title = "个性化", subtitle = "改完立刻生效", onBack = onBack)
        Column(Modifier.padding(horizontal = 20.dp)) {

            SectionLabel("深浅模式")
            SegmentedOptions(
                options = DarkPref.entries.map { it to it.label },
                selected = settings.dark,
                onSelect = { scope.launch { vm.settingsRepo.setDark(it) } }
            )

            Spacer(Modifier.height(24.dp))
            SectionLabel("主题色")
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Accent.entries.forEach { accent ->
                    val selected = settings.accent == accent
                    Box(
                        Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(Color(accent.argb))
                            .then(
                                if (selected) Modifier.border(
                                    2.dp,
                                    MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f),
                                    CircleShape
                                ) else Modifier
                            )
                            .clickable { scope.launch { vm.settingsRepo.setAccent(accent) } },
                        contentAlignment = Alignment.Center
                    ) {
                        if (selected) {
                            Icon(Icons.Rounded.Check, null, tint = Color.White, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
            Spacer(Modifier.height(6.dp))
            Text(
                "当前：${settings.accent.label}，会用在按钮、进度、选中态上",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(24.dp))
            SectionLabel("背景图")
            SeagullCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            if (settings.backgroundPath != null) "已设置自定义背景" else "默认纯色背景",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            "从相册选一张，会铺满整个 App",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    GhostPill(text = "选择", onClick = { bgPicker.launch("image/*") })
                }
                if (settings.backgroundPath != null) {
                    Spacer(Modifier.height(10.dp))
                    Text(
                        "清除背景图",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.clickable {
                            scope.launch {
                                vm.settingsRepo.setBackground(null)
                                File(settings.backgroundPath!!).delete()
                            }
                        }
                    )
                }
            }

            Spacer(Modifier.height(24.dp))
            SectionLabel("字体")
            SegmentedOptions(
                options = FontPref.entries.map { it to it.label },
                selected = settings.font,
                onSelect = { scope.launch { vm.settingsRepo.setFont(it) } }
            )
            Spacer(Modifier.height(12.dp))
            SeagullCard {
                Text(
                    "看广告不是看热闹，是看别人怎么把钱挣走。",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontFamily = when (settings.font) {
                            FontPref.SYSTEM -> FontFamily.Default
                            FontPref.SERIF -> FontFamily.Serif
                            FontPref.MONO -> FontFamily.Monospace
                        }
                    )
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    "上面这段就是预览，换字体立刻能看到差别。",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(Modifier.height(24.dp))
            SectionLabel("字号")
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("A", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Slider(
                    value = settings.fontScale,
                    onValueChange = { scope.launch { vm.settingsRepo.setFontScale(it) } },
                    valueRange = 0.85f..1.25f,
                    modifier = Modifier.weight(1f).padding(horizontal = 10.dp)
                )
                Text("A", fontSize = 20.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(
                "当前 ${(settings.fontScale * 100).toInt()}%",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(24.dp))
            SectionLabel("卡片圆角")
            SegmentedOptions(
                options = listOf(true to "圆角", false to "直角"),
                selected = settings.cardRound,
                onSelect = { scope.launch { vm.settingsRepo.setCardRound(it) } }
            )

            Spacer(Modifier.height(40.dp))
        }
    }
}

@Composable
fun PlanScreen(vm: AppViewModel, onBack: () -> Unit) {
    val settings by vm.settings.collectAsState()
    val scope = rememberCoroutineScope()
    var adding by remember { mutableStateOf(false) }
    var editingIndex by remember { mutableStateOf(-1) }
    var editingText by remember { mutableStateOf("") }
    var cookie by remember { mutableStateOf("") }

    LaunchedEffect(settings.biliCookie) { cookie = settings.biliCookie }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        PageHeader(title = "薯条计划", subtitle = "定个量，剩下的交给习惯", onBack = onBack)
        Column(Modifier.padding(horizontal = 20.dp)) {

            SectionLabel("每天看几条")
            SeagullCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "${settings.dailyTarget}",
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "条 / 天",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.weight(1f))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(3, 5, 10).forEach { n ->
                            SurfaceChip(
                                text = "$n",
                                active = settings.dailyTarget == n,
                                onClick = { scope.launch { vm.settingsRepo.setDailyTarget(n) } }
                            )
                        }
                    }
                }
                Slider(
                    value = settings.dailyTarget.toFloat(),
                    onValueChange = { scope.launch { vm.settingsRepo.setDailyTarget(it.toInt()) } },
                    valueRange = 1f..50f
                )
                Text(
                    "范围 1–50 条。半年 × 每天 10 条 = 1800 次商业观察。",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(Modifier.height(24.dp))
            SectionLabel("广告来源")
            SeagullCard {
                ToggleRow(
                    title = "内置示例广告",
                    desc = "3 条 9 秒的示例素材，装上就能用",
                    checked = settings.sourceSample,
                    onChange = { scope.launch { vm.settingsRepo.setSourceSample(it) } }
                )
                Spacer(Modifier.height(12.dp))
                ToggleRow(
                    title = "本地视频",
                    desc = "从手机里选广告视频或录屏",
                    checked = settings.sourceLocal,
                    onChange = { scope.launch { vm.settingsRepo.setSourceLocal(it) } }
                )
                Spacer(Modifier.height(12.dp))
                ToggleRow(
                    title = "网页链接",
                    desc = "贴一个广告页链接，App 内打开",
                    checked = settings.sourceWeb,
                    onChange = { scope.launch { vm.settingsRepo.setSourceWeb(it) } }
                )
            }

            Spacer(Modifier.height(24.dp))
            SectionLabel("分析点（决定表单问什么）")
            SeagullCard {
                settings.labels.forEachIndexed { index, label ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "${index + 1}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.width(22.dp)
                        )
                        Text(
                            label,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f)
                        )
                        if (index > 0) {
                            SmallIcon(Icons.Rounded.KeyboardArrowUp) {
                                val list = settings.labels.toMutableList()
                                val tmp = list[index]
                                list[index] = list[index - 1]
                                list[index - 1] = tmp
                                scope.launch { vm.settingsRepo.setLabels(list) }
                            }
                        }
                        if (index < settings.labels.lastIndex) {
                            SmallIcon(Icons.Rounded.KeyboardArrowDown) {
                                val list = settings.labels.toMutableList()
                                val tmp = list[index]
                                list[index] = list[index + 1]
                                list[index + 1] = tmp
                                scope.launch { vm.settingsRepo.setLabels(list) }
                            }
                        }
                        SmallIcon(Icons.Rounded.Edit) {
                            editingIndex = index
                            editingText = label
                        }
                        SmallIcon(Icons.Rounded.Delete) {
                            if (settings.labels.size > 1) {
                                val list = settings.labels.toMutableList().also { it.removeAt(index) }
                                scope.launch { vm.settingsRepo.setLabels(list) }
                            } else {
                                vm.say("至少留一个分析点")
                            }
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                GhostPill(text = "添加分析点", icon = Icons.Rounded.Add, onClick = { adding = true })
                Spacer(Modifier.height(8.dp))
                Text(
                    "默认七点：人群、场景、需求、卖点、产品痛点、商业模式、个人心得。改完对新记录生效，旧记录不受影响。",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(Modifier.height(24.dp))
            SectionLabel("B 站 Cookie（可选）")
            SeagullCard {
                Text(
                    "遇到「需要登录」的提示时，把浏览器里的 SESSDATA 填进来，成功率会高很多。只保存在本机。",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(10.dp))
                SeagullTextField(
                    value = cookie,
                    onValueChange = { cookie = it },
                    placeholder = "SESSDATA=xxxx 或直接粘贴值",
                    singleLine = true
                )
                Spacer(Modifier.height(10.dp))
                Row {
                    PrimaryPill(text = "保存 Cookie", onClick = {
                        scope.launch {
                            vm.settingsRepo.setBiliCookie(cookie.trim())
                            vm.say("已保存")
                        }
                    })
                    Spacer(Modifier.width(10.dp))
                    GhostPill(text = "清空", onClick = {
                        cookie = ""
                        scope.launch { vm.settingsRepo.setBiliCookie("") }
                    })
                }
            }

            Spacer(Modifier.height(40.dp))
        }
    }

    if (adding) {
        InputDialog(
            title = "添加分析点",
            placeholder = "比如「竞品」「投放渠道」",
            onDismiss = { adding = false },
            onConfirm = { text ->
                if (settings.labels.contains(text)) {
                    vm.say("已经有这一项了")
                } else {
                    scope.launch { vm.settingsRepo.setLabels(settings.labels + text) }
                }
                adding = false
            }
        )
    }

    if (editingIndex >= 0) {
        InputDialog(
            title = "改这个分析点",
            initial = editingText,
            onDismiss = { editingIndex = -1 },
            onConfirm = { text ->
                val list = settings.labels.toMutableList()
                list[editingIndex] = text
                scope.launch { vm.settingsRepo.setLabels(list) }
                editingIndex = -1
            }
        )
    }
}

@Composable
private fun ToggleRow(
    title: String,
    desc: String,
    checked: Boolean,
    onChange: (Boolean) -> Unit
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyMedium)
            Text(
                desc,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
private fun SmallIcon(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    Box(
        Modifier
            .size(32.dp)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            icon,
            null,
            Modifier.size(17.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
        )
    }
}

@Composable
private fun SurfaceChip(text: String, active: Boolean, onClick: () -> Unit) {
    androidx.compose.material3.Surface(
        shape = RoundedCornerShape(50),
        color = if (active) MaterialTheme.colorScheme.primary
        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        contentColor = if (active) MaterialTheme.colorScheme.onPrimary
        else MaterialTheme.colorScheme.onSurfaceVariant,
        onClick = onClick
    ) {
        Text(
            text,
            Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
            style = MaterialTheme.typography.labelMedium
        )
    }
}

@Composable
fun StorageScreen(vm: AppViewModel, onBack: () -> Unit) {
    val settings by vm.settings.collectAsState()
    val records by vm.records.collectAsState()
    val quotes by vm.quotes.collectAsState()
    val tracks by vm.tracks.collectAsState()
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var confirmClear by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }

    val exportName = remember {
        "seagull-" + SimpleDateFormat("yyyyMMdd-HHmm", Locale.getDefault()).format(Date())
    }

    fun doExport(mime: String, name: String, writer: suspend (Uri) -> Int) {
        val tree = settings.exportTreeUri
        if (tree != null) {
            scope.launch {
                busy = true
                runCatching {
                    val dir = DocumentFile.fromTreeUri(ctx, Uri.parse(tree))
                    val file = dir?.createFile(mime, name)
                        ?: throw IllegalStateException("这个目录写不进去，换一个目录试试")
                    writer(file.uri)
                }.onSuccess { vm.say("已导出 $it 条到所选目录") }
                    .onFailure { vm.say("导出失败：${it.message}") }
                busy = false
            }
        } else {
            vm.say("先选一个导出目录")
        }
    }

    val treePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) {
            runCatching {
                ctx.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )
            }
            scope.launch {
                vm.settingsRepo.setExportTree(uri.toString())
                vm.say("目录已设定")
            }
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        PageHeader(title = "存储与导出", subtitle = "数据是你的，随时能带走", onBack = onBack)
        Column(Modifier.padding(horizontal = 20.dp)) {

            SectionLabel("数据概览")
            SeagullCard {
                StatRow("广告记录", "${records.size} 条")
                StatRow("分析内容", "${records.count { it.analysisDoneAt != null }} 组")
                StatRow("随笔", "${records.count { it.note.isNotBlank() }} 条")
                StatRow("明志语录", "${quotes.size} 句")
                StatRow("曲目", "${tracks.size} 首")
                StatRow("累计听歌", Fmt.duration(tracks.sumOf { it.listenedMs }))
                StatRow("完整听完", "${tracks.sumOf { it.playCount }} 次")
            }

            Spacer(Modifier.height(24.dp))
            SectionLabel("存储位置")
            SeagullCard {
                Text(
                    if (settings.exportTreeUri == null) "还没有选导出目录" else "已选目录，导出会直接写进去",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    settings.exportTreeUri ?: "App 内部数据始终存在应用私有目录，不会因为选目录而搬家。",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(12.dp))
                Row {
                    PrimaryPill(text = "选择目录", onClick = { treePicker.launch(null) })
                    if (settings.exportTreeUri != null) {
                        Spacer(Modifier.width(10.dp))
                        GhostPill(text = "清除", onClick = {
                            scope.launch { vm.settingsRepo.setExportTree(null) }
                        })
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
            SectionLabel("导出")
            SeagullCard {
                Text(
                    "JSON 是完整备份（含分析、随笔、语录、听歌统计）；CSV 方便丢进 Excel 复盘。",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    PrimaryPill(
                        text = if (busy) "导出中…" else "导出 JSON",
                        enabled = !busy,
                        onClick = { doExport("application/json", "$exportName.json") { vm.exportJson(it) } }
                    )
                    GhostPill(
                        text = "导出 CSV",
                        onClick = { doExport("text/csv", "$exportName.csv") { vm.exportCsv(it) } }
                    )
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    "已选目录后会直接写入；没选目录会提示先选一个。",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(Modifier.height(24.dp))
            SectionLabel("危险操作")
            SeagullCard {
                Text(
                    "清空所有记录、分析、随笔、语录、曲目和统计。不可恢复，建议先导出。",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    "清空全部数据",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.clickable { confirmClear = true }
                )
            }

            Spacer(Modifier.height(40.dp))
        }
    }

    if (confirmClear) {
        ConfirmDialog(
            title = "确定清空？",
            text = "所有数据会被删除，且无法恢复。先导出备份更稳妥。",
            confirmText = "清空",
            onDismiss = { confirmClear = false },
            onConfirm = {
                confirmClear = false
                vm.clearAll { vm.say("已清空") }
            }
        )
    }
}

@Composable
private fun StatRow(label: String, value: String) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
    }
}
