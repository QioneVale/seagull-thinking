@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.qionevale.seagullthinking.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.qionevale.seagullthinking.data.MusicMode
import com.qionevale.seagullthinking.data.Quote
import com.qionevale.seagullthinking.data.Track

private fun familyOf(key: String): FontFamily = when (key) {
    "system" -> FontFamily.Default
    "mono" -> FontFamily.Monospace
    "cursive" -> FontFamily.Cursive
    else -> FontFamily.Serif
}

private val WALL_COLORS = listOf(
    0,
    0xFFFFFFFF.toInt(),
    0xFF101828.toInt(),
    0xFFC0392B.toInt(),
    0xFFB4801B.toInt(),
    0xFF1F6FEB.toInt(),
    0xFF3E7C59.toInt(),
    0xFF9E3B52.toInt(),
    0xFF8E5BB5.toInt()
)

@Composable
fun MingzhiScreen(
    vm: AppViewModel,
    onOpenMode: (Long) -> Unit,
    onOpenPlayer: () -> Unit
) {
    val quotes by vm.quotes.collectAsState()
    val modes by vm.modes.collectAsState()
    val tracks by vm.tracks.collectAsState()

    var wallEditing by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    var showNewMode by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<Quote?>(null) }

    val pager = rememberPagerState(pageCount = { 2 })

    Box(Modifier.fillMaxSize()) {
        VerticalPager(
            state = pager,
            userScrollEnabled = !wallEditing && editing == null,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            if (page == 0) {
                QuoteWall(
                    quotes = quotes,
                    editing = wallEditing,
                    onMove = { q, x, y -> vm.moveQuote(q, x, y) },
                    onPick = { editing = it },
                    onAdd = { editing = newDraft() }
                )
            } else {
                MusicModesPage(
                    vm = vm,
                    modes = modes,
                    tracks = tracks,
                    onOpenMode = onOpenMode,
                    onOpenPlayer = onOpenPlayer,
                    onNewMode = { showNewMode = true }
                )
            }
        }

        if (editing == null) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        "明志",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        if (wallEditing) "拖动摆位置 · 点文字改内容与样式"
                        else if (pager.currentPage == 0) "写下来的话，会慢慢变成方向 · 下滑看音乐"
                        else "往下滑，回到明志墙",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Box(
                    Modifier
                        .size(40.dp)
                        .background(MaterialTheme.colorScheme.surface, CircleShape)
                        .clickable { if (wallEditing) wallEditing = false else showMenu = true },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        if (wallEditing) Icons.Rounded.Check else Icons.Rounded.Add,
                        if (wallEditing) "完成" else "添加",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(21.dp)
                    )
                }
            }
        }

        if (wallEditing && editing == null) {
            GhostPill(
                text = "新增一条",
                icon = Icons.Rounded.Add,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 18.dp),
                onClick = { editing = newDraft() }
            )
        }

        // 页码提示
        if (editing == null) {
            Column(
                Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                repeat(2) { i ->
                    Box(
                        Modifier
                            .size(if (pager.currentPage == i) 7.dp else 5.dp)
                            .background(
                                if (pager.currentPage == i) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f),
                                CircleShape
                            )
                    )
                }
            }
        }
    }

    if (showMenu) {
        ModalBottomSheet(onDismissRequest = { showMenu = false }) {
            Column(Modifier.padding(horizontal = 22.dp, vertical = 4.dp)) {
                Text("明志墙", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(4.dp))
                Text(
                    "平时这面墙只用来读，改不了任何效果。想改就进这里。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(14.dp))
                SheetRow(
                    icon = Icons.Rounded.Add,
                    title = "写一句新的",
                    desc = "自己定字号、颜色、字体，保存后就固定在墙上"
                ) {
                    showMenu = false
                    editing = newDraft()
                }
                SheetRow(
                    icon = Icons.Rounded.GraphicEq,
                    title = "自定义某一条",
                    desc = "改文字、字号、颜色、字体，也能拖动摆位置"
                ) {
                    showMenu = false
                    wallEditing = true
                }
                Spacer(Modifier.height(26.dp))
            }
        }
    }

    editing?.let { q ->
        QuoteEditor(
            initial = q,
            others = quotes.filter { it.id != q.id },
            onCancel = { editing = null },
            onSave = { saved ->
                if (saved.id == 0L) vm.addQuote(saved) else vm.saveQuote(saved)
                editing = null
            },
            onDelete = if (q.id == 0L) null else {
                { vm.deleteQuote(q); editing = null }
            }
        )
    }

    if (showNewMode) {
        NewModeDialog(
            onDismiss = { showNewMode = false },
            onConfirm = { name -> vm.createMode(name, null); showNewMode = false }
        )
    }
}

private fun newDraft() = Quote(
    text = "",
    xRatio = 0.5f,
    yRatio = 0.45f,
    createdAt = 0L,
    fontKey = "serif"
)

@Composable
private fun SheetRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    desc: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        onClick = onClick
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(38.dp)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(19.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column {
                Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                Text(
                    desc,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun QuoteText(quote: Quote, modifier: Modifier = Modifier, colorOverride: Color? = null) {
    Text(
        quote.text.ifBlank { "写一句话" },
        modifier = modifier,
        style = TextStyle(
            fontSize = quote.fontSizeSp.sp,
            lineHeight = (quote.fontSizeSp * 1.34f).sp,
            fontFamily = familyOf(quote.fontFamily),
            fontWeight = if (quote.isBold) FontWeight.Bold else FontWeight.Normal,
            fontStyle = if (quote.isItalic) FontStyle.Italic else FontStyle.Normal,
            textAlign = TextAlign.Center
        ),
        color = colorOverride
            ?: if (quote.colorArgb == 0) MaterialTheme.colorScheme.onBackground else Color(quote.colorArgb)
    )
}

@Composable
private fun QuoteWall(
    quotes: List<Quote>,
    editing: Boolean,
    onMove: (Quote, Float, Float) -> Unit,
    onPick: (Quote) -> Unit,
    onAdd: () -> Unit
) {
    BoxWithConstraints(
        Modifier
            .fillMaxSize()
            .clipToBounds()
            .padding(start = 20.dp, end = 20.dp, top = 76.dp, bottom = 28.dp)
    ) {
        val wallW = maxWidth
        val wallH = maxHeight

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
                Spacer(Modifier.height(10.dp))
                GhostPill(text = "写第一句", icon = Icons.Rounded.Add, onClick = onAdd)
            }
        }

        quotes.forEach { q ->
            var rx by remember(q.id, q.xRatio) { mutableFloatStateOf(q.xRatio) }
            var ry by remember(q.id, q.yRatio) { mutableFloatStateOf(q.yRatio) }
            val dragMod = if (editing) {
                Modifier
                    .pointerInput(q.id) {
                        detectDragGestures(
                            onDragEnd = { onMove(q, rx, ry) }
                        ) { change, drag ->
                            change.consume()
                            val wPx = wallW.toPx().coerceAtLeast(1f)
                            val hPx = wallH.toPx().coerceAtLeast(1f)
                            rx = (rx + drag.x / wPx).coerceIn(0.04f, 0.96f)
                            ry = (ry + drag.y / hPx).coerceIn(0.02f, 0.98f)
                        }
                    }
                    .pointerInput(q.id) { detectTapGestures { onPick(q) } }
                    .border(
                        1.dp,
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.45f),
                        RoundedCornerShape(12.dp)
                    )
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            } else Modifier

            QuoteText(
                quote = q,
                modifier = Modifier
                    .offset(x = wallW * rx, y = wallH * ry)
                    .graphicsLayer {
                        translationX = -size.width / 2f
                        translationY = -size.height / 2f
                    }
                    .widthIn(max = wallW * 0.9f)
                    .then(dragMod)
            )
        }
    }
}

@Composable
private fun QuoteEditor(
    initial: Quote,
    others: List<Quote>,
    onCancel: () -> Unit,
    onSave: (Quote) -> Unit,
    onDelete: (() -> Unit)?
) {
    var text by remember { mutableStateOf(initial.text) }
    var scale by remember { mutableFloatStateOf(initial.scale) }
    var color by remember { mutableIntStateOf(initial.colorArgb) }
    var fontKey by remember { mutableStateOf(initial.fontFamily) }
    var bold by remember { mutableStateOf(initial.isBold) }
    var italic by remember { mutableStateOf(initial.isItalic) }
    var rx by remember { mutableFloatStateOf(initial.xRatio) }
    var ry by remember { mutableFloatStateOf(initial.yRatio) }

    val draft = initial.copy(
        text = text,
        scale = scale,
        colorArgb = color,
        fontKey = fontKey,
        bold = bold,
        italic = italic,
        xRatio = rx,
        yRatio = ry
    )

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(Modifier.fillMaxSize().imePadding()) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onCancel) { Text("取消") }
                Spacer(Modifier.weight(1f))
                Text(
                    if (initial.id == 0L) "新的一句" else "编辑这一句",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.weight(1f))
                TextButton(
                    onClick = { onSave(draft) },
                    enabled = text.isNotBlank()
                ) { Text("保存", fontWeight = FontWeight.SemiBold) }
            }

            // 预览区：所见即所得，直接拖动调位置
            BoxWithConstraints(
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.5f))
            ) {
                val w = maxWidth
                val h = maxHeight
                Text(
                    "拖动可以摆位置 · 下面调样式",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 10.dp)
                )
                others.forEach { o ->
                    QuoteText(
                        quote = o,
                        colorOverride = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.25f),
                        modifier = Modifier
                            .offset(x = w * o.xRatio, y = h * o.yRatio)
                            .graphicsLayer {
                                translationX = -size.width / 2f
                                translationY = -size.height / 2f
                            }
                    )
                }
                QuoteText(
                    quote = draft,
                    modifier = Modifier
                        .offset(x = w * rx, y = h * ry)
                        .graphicsLayer {
                            translationX = -size.width / 2f
                            translationY = -size.height / 2f
                        }
                        .widthIn(max = w * 0.9f)
                        .pointerInput(Unit) {
                            detectDragGestures { change, drag ->
                                change.consume()
                                rx = (rx + drag.x / w.toPx().coerceAtLeast(1f)).coerceIn(0.05f, 0.95f)
                                ry = (ry + drag.y / h.toPx().coerceAtLeast(1f)).coerceIn(0.03f, 0.97f)
                            }
                        }
                )
            }

            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 12.dp)
            ) {
                SeagullTextField(
                    value = text,
                    onValueChange = { text = it },
                    placeholder = "写一句给自己的话",
                    minLines = 2,
                    maxLines = 3
                )
                Spacer(Modifier.height(10.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "字号",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Slider(
                        value = scale,
                        onValueChange = { scale = it },
                        valueRange = 0.7f..3.2f,
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 10.dp)
                    )
                    Text(
                        "${(18 * scale).toInt()}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.width(26.dp)
                    )
                }

                Spacer(Modifier.height(4.dp))
                Row(
                    Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    WALL_COLORS.forEach { argb ->
                        val c = if (argb == 0) MaterialTheme.colorScheme.onBackground else Color(argb)
                        Box(
                            Modifier
                                .size(if (color == argb) 30.dp else 26.dp)
                                .clip(CircleShape)
                                .background(c)
                                .border(
                                    if (color == argb) 2.dp else 1.dp,
                                    if (color == argb) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                                    CircleShape
                                )
                                .clickable { color = argb }
                        )
                    }
                    Spacer(Modifier.width(2.dp))
                    Text(
                        if (color == 0) "跟随主题" else "自定义色",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    listOf("serif" to "衬线", "system" to "黑体", "mono" to "等宽", "cursive" to "手写")
                        .forEach { (key, label) ->
                            StyleChip(label, fontKey == key) { fontKey = key }
                            Spacer(Modifier.width(8.dp))
                        }
                    Spacer(Modifier.weight(1f))
                    StyleChip("B", bold) { bold = !bold }
                    Spacer(Modifier.width(8.dp))
                    StyleChip("I", italic) { italic = !italic }
                }

                if (onDelete != null) {
                    Spacer(Modifier.height(6.dp))
                    TextButton(onClick = onDelete) {
                        Text("删除这句话", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }
}

@Composable
private fun StyleChip(text: String, active: Boolean, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(50),
        color = if (active) MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
        contentColor = if (active) MaterialTheme.colorScheme.primary
        else MaterialTheme.colorScheme.onSurfaceVariant,
        onClick = onClick
    ) {
        Text(
            text,
            Modifier.padding(horizontal = 13.dp, vertical = 7.dp),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}

@Composable
private fun MusicModesPage(
    vm: AppViewModel,
    modes: List<MusicMode>,
    tracks: List<Track>,
    onOpenMode: (Long) -> Unit,
    onOpenPlayer: () -> Unit,
    onNewMode: () -> Unit
) {
    val current by vm.player.current.collectAsState()
    val playing by vm.player.playing.collectAsState()
    val modeName by vm.player.modeName.collectAsState()

    BoxWithConstraints(Modifier.fillMaxSize()) {
        // 音乐模式只占半屏，剩下的留给正在播放
        val modesHeight = maxHeight * 0.5f
        Column(
            Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
        ) {
            Spacer(Modifier.height(76.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "音乐模式",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    "点进去听歌",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.height(14.dp))

            Column(
                Modifier
                    .fillMaxWidth()
                    .height(modesHeight.coerceAtMost(420.dp))
                    .verticalScroll(rememberScrollState())
            ) {
                val cells = modes.size + 1
                (0 until cells step 3).forEach { rowStart ->
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        for (i in rowStart until minOf(rowStart + 3, cells)) {
                            Box(Modifier.weight(1f)) {
                                if (i == modes.size) {
                                    NewModeCell(onClick = onNewMode)
                                } else {
                                    val mode = modes[i]
                                    ModeCell(
                                        mode = mode,
                                        count = tracks.count { it.modeId == mode.id },
                                        onClick = { onOpenMode(mode.id) }
                                    )
                                }
                            }
                        }
                        repeat(3 - (minOf(rowStart + 3, cells) - rowStart)) {
                            Spacer(Modifier.weight(1f))
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                }
            }

            Spacer(Modifier.height(10.dp))

            if (current != null) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
                    ),
                    onClick = onOpenPlayer
                ) {
                    Row(
                        Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val mode = modes.firstOrNull { it.name == modeName }
                        MusicCover(
                            path = mode?.coverPath,
                            size = 52.dp,
                            seed = mode?.name ?: (current?.title ?: "海")
                        )
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                "正在播放",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                current?.title ?: "",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Box(
                            Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                                .clickable { vm.player.toggle() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                if (playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                "播放",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            } else {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    onClick = onOpenPlayer
                ) {
                    Text(
                        "还没在听歌 · 到任一模式里点播放",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }

            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
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
                    onClick = onNewMode
                )
            }
            Spacer(Modifier.height(18.dp))
        }
    }
}

@Composable
private fun ModeCell(mode: MusicMode, count: Int, onClick: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable { onClick() }
            .padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        MusicCover(path = mode.coverPath, size = 88.dp, seed = mode.name)
        Spacer(Modifier.height(8.dp))
        Text(
            mode.name,
            style = MaterialTheme.typography.labelMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
        Text(
            "$count 首",
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun NewModeCell(onClick: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable { onClick() }
            .padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            Modifier
                .size(88.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Rounded.Add, "新建模式", tint = MaterialTheme.colorScheme.primary)
        }
        Spacer(Modifier.height(8.dp))
        Text(
            "新建",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(" ", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp))
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
