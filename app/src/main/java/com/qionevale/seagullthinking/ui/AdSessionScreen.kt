@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.qionevale.seagullthinking.ui

import android.annotation.SuppressLint
import android.net.Uri
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Replay
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.qionevale.seagullthinking.data.Fmt
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

data class AdPlayRequest(
    val title: String,
    val source: String,
    val ref: String,
    val assetId: Long = 0
)

private sealed interface AdPhase {
    data object Playing : AdPhase
    data object Analyzing : AdPhase
    data class Done(val finishedAt: Long, val watchedAt: Long) : AdPhase
}

@Composable
fun AdSessionScreen(
    vm: AppViewModel,
    request: AdPlayRequest?,
    onExit: () -> Unit,
    onNextAd: () -> Unit
) {
    val settings by vm.settings.collectAsState()
    if (request == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("广告信息丢失了", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(12.dp))
                PrimaryPill("回首页", onClick = onExit)
            }
        }
        return
    }

    var phase by remember { mutableStateOf<AdPhase>(AdPhase.Playing) }
    var watchedAt by remember { mutableLongStateOf(0L) }
    var showExitConfirm by remember { mutableStateOf(false) }
    val labels = remember { settings.labels }
    val answers = remember { mutableStateListOf<String>().apply { repeat(labels.size) { add("") } } }
    var index by remember { mutableIntStateOf(0) }
    val scope = rememberCoroutineScope()
    var saving by remember { mutableStateOf(false) }

    when (val p = phase) {
        AdPhase.Playing -> {
            Box(Modifier.fillMaxSize().background(Color.Black)) {
                when (request.source) {
                    "WEB" -> WebAd(request.ref) {
                        watchedAt = System.currentTimeMillis()
                        phase = AdPhase.Analyzing
                    }

                    else -> VideoAd(request, onEnded = {
                        watchedAt = System.currentTimeMillis()
                        phase = AdPhase.Analyzing
                    })
                }

                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .align(Alignment.TopCenter),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Box(
                            Modifier
                                .size(6.dp)
                                .background(Color(0xFFF7C960), CircleShape)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "广告 · ${request.title}",
                            color = Color.White.copy(alpha = 0.85f),
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                    Box(
                        Modifier
                            .size(34.dp)
                            .background(Color.White.copy(alpha = 0.14f), CircleShape)
                            .pointerInput(Unit) { detectTapGestures { showExitConfirm = true } },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Rounded.Close, "退出", tint = Color.White, modifier = Modifier.size(18.dp))
                    }
                }

            }
            if (showExitConfirm) {
                ConfirmDialog(
                    title = "还没看完",
                    text = "现在退出不会产生记录，确定要离开吗？",
                    confirmText = "离开",
                    onDismiss = { showExitConfirm = false },
                    onConfirm = { showExitConfirm = false; onExit() }
                )
            }
        }

        AdPhase.Analyzing -> {
            AnalysisForm(
                labels = labels,
                answers = answers,
                index = index,
                onIndexChange = { index = it.coerceIn(0, labels.size - 1) },
                onAnswer = { i, v -> answers[i] = v },
                saving = saving,
                onCancel = { showExitConfirm = true },
                onSubmit = {
                    if (answers.none { it.isNotBlank() }) {
                        vm.say("至少写一条，哪怕只有一句")
                    } else if (!saving) {
                        saving = true
                        val at = if (watchedAt == 0L) System.currentTimeMillis() else watchedAt
                        scope.launch {
                            runCatching {
                                vm.saveAdSession(
                                    title = request.title,
                                    source = request.source,
                                    ref = request.ref,
                                    watchedAt = at,
                                    answers = labels.mapIndexed { i, label -> label to answers[i] }
                                )
                            }.onSuccess {
                                phase = AdPhase.Done(System.currentTimeMillis(), at)
                                saving = false
                            }.onFailure {
                                vm.say("保存失败：${it.message}")
                                saving = false
                            }
                        }
                    }
                }
            )
            if (showExitConfirm) {
                ConfirmDialog(
                    title = "放弃这次分析？",
                    text = "已经写下的内容不会保存。",
                    confirmText = "放弃",
                    onDismiss = { showExitConfirm = false },
                    onConfirm = { showExitConfirm = false; onExit() }
                )
            }
        }

        is AdPhase.Done -> {
            DoneScreen(
                title = request.title,
                watchedAt = p.watchedAt,
                finishedAt = p.finishedAt,
                count = answers.count { it.isNotBlank() },
                total = labels.size,
                onNext = onNextAd,
                onHome = onExit
            )
        }
    }
}

@OptIn(UnstableApi::class)
@Composable
private fun VideoAd(request: AdPlayRequest, onEnded: () -> Unit) {
    val ctx = LocalContext.current
    val ended by rememberUpdatedState(onEnded)
    val exo = remember {
        ExoPlayer.Builder(ctx).build().apply { repeatMode = Player.REPEAT_MODE_OFF }
    }
    var endedOnce by remember { mutableStateOf(false) }

    DisposableEffect(exo) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_ENDED && !endedOnce) {
                    endedOnce = true
                    ended()
                }
            }
        }
        exo.addListener(listener)
        onDispose {
            exo.removeListener(listener)
            exo.release()
        }
    }

    LaunchedEffect(request.ref) {
        exo.setMediaItem(MediaItem.fromUri(Uri.parse(request.ref)))
        exo.prepare()
        exo.play()
    }

    AndroidView(
        factory = { c ->
            PlayerView(c).apply {
                player = exo
                useController = false
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            }
        },
        modifier = Modifier.fillMaxSize()
    )
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun WebAd(url: String, onDone: () -> Unit) {
    Box(Modifier.fillMaxSize()) {
        AndroidView(
            factory = { c ->
                WebView(c).apply {
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.mediaPlaybackRequiresUserGesture = false
                    webChromeClient = WebChromeClient()
                    webViewClient = WebViewClient()
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                }
            },
            update = { it.loadUrl(url) },
            modifier = Modifier.fillMaxSize()
        )
        PrimaryPill(
            text = "看完了，去分析",
            icon = Icons.Rounded.Check,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 40.dp),
            onClick = onDone
        )
    }
}

@Composable
private fun AnalysisForm(
    labels: List<String>,
    answers: MutableList<String>,
    index: Int,
    onIndexChange: (Int) -> Unit,
    onAnswer: (Int, String) -> Unit,
    saving: Boolean,
    onCancel: () -> Unit,
    onSubmit: () -> Unit
) {
    val label = labels.getOrNull(index) ?: ""
    val isLast = index == labels.lastIndex
    val filled = answers.count { it.isNotBlank() }
    val scroll = rememberScrollState()
    val scope = rememberCoroutineScope()

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(scroll)
            .imePadding()
            .padding(horizontal = 22.dp)
    ) {
        Spacer(Modifier.height(20.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onCancel) {
                Text("退出", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.weight(1f))
            Text(
                "${index + 1} / ${labels.size}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(Modifier.height(10.dp))
        LinearProgressIndicator(
            progress = { (index + 1f) / labels.size },
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )
        Spacer(Modifier.height(26.dp))

        Text(
            "拆解这一条广告",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(10.dp))

        AnimatedContent(
            targetState = index,
            transitionSpec = {
                if (targetState > initialState) {
                    (slideInHorizontally(tween(260)) { it / 3 } + fadeIn(tween(260))) togetherWith
                        (slideOutHorizontally(tween(200)) { -it / 4 } + fadeOut(tween(160)))
                } else {
                    (slideInHorizontally(tween(260)) { -it / 3 } + fadeIn(tween(260))) togetherWith
                        (slideOutHorizontally(tween(200)) { it / 4 } + fadeOut(tween(160)))
                }
            },
            label = "step"
        ) { i ->
            Column {
                Text(
                    labels.getOrNull(i) ?: "",
                    style = MaterialTheme.typography.displaySmall.copy(fontSize = 34.sp),
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    hintFor(labels.getOrNull(i) ?: ""),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(18.dp))
                SeagullTextField(
                    value = answers.getOrElse(i) { "" },
                    onValueChange = { onAnswer(i, it) },
                    placeholder = "写具体一点，越具体越有用…",
                    minLines = 6,
                    maxLines = 10
                )
            }
        }

        Spacer(Modifier.height(20.dp))
        if (filled > 0) {
            Text(
                "已写 $filled / ${labels.size} 项 · 点标签可回改",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))
            Row(
                Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                labels.forEachIndexed { i, l ->
                    val has = answers.getOrElse(i) { "" }.isNotBlank()
                    val current = i == index
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = when {
                            current -> MaterialTheme.colorScheme.primary
                            has -> MaterialTheme.colorScheme.primaryContainer
                            else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        },
                        contentColor = when {
                            current -> MaterialTheme.colorScheme.onPrimary
                            has -> MaterialTheme.colorScheme.onPrimaryContainer
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        onClick = { onIndexChange(i) }
                    ) {
                        Text(
                            l,
                            Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(26.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (index > 0) {
                GhostPill(text = "上一步", onClick = { onIndexChange(index - 1) })
                Spacer(Modifier.width(10.dp))
            }
            Spacer(Modifier.weight(1f))
            if (!isLast) {
                PrimaryPill(
                    text = "下一步",
                    onClick = {
                        onIndexChange(index + 1)
                        scope.launch { scroll.animateScrollTo(0) }
                    }
                )
            } else {
                PrimaryPill(
                    text = if (saving) "保存中…" else "完成并记录",
                    icon = Icons.Rounded.Check,
                    enabled = !saving,
                    onClick = onSubmit
                )
            }
        }
        Spacer(Modifier.height(40.dp))

        if (index == labels.lastIndex) {
            Text(
                "完成后会记下两个时间：看完广告的时间、写完分析的时间，都精确到秒。",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 30.dp)
            )
        }
    }
}

private fun hintFor(label: String): String = when (label.trim()) {
    "人群" -> "这条广告在跟谁说话？年龄、职业、身份、正在经历什么。越具体越好。"
    "场景" -> "它出现在什么时刻、什么地点？用户当时在干什么、心里在想什么？"
    "需求" -> "它满足的是哪一层需求？功能上的，还是情绪上的、身份上的？"
    "卖点" -> "它反复强调的那一句话是什么？为什么选这句，而不是别的？"
    "产品痛点" -> "它暗示你现在用的东西哪里不好？不买会损失什么？"
    "商业模式" -> "它靠什么赚钱？一次性买断、订阅、复购、加盟、抽佣，还是卖流量？"
    "个人心得", "个人心痛" -> "如果让你照抄一步，你会抄哪一步？哪一点你现在就能用上？"
    else -> "写下你看到的、想到的，一句话也算。"
}

@Composable
private fun DoneScreen(
    title: String,
    watchedAt: Long,
    finishedAt: Long,
    count: Int,
    total: Int,
    onNext: () -> Unit,
    onHome: () -> Unit
) {
    Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        SeagullCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(44.dp)
                        .background(MaterialTheme.colorScheme.primary, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.Check, null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(22.dp))
                }
                Spacer(Modifier.width(14.dp))
                Column {
                    Text("这一条，收进本子了", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text("$count / $total 个点有内容", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Spacer(Modifier.height(18.dp))
            Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(12.dp))
            InfoLine("看完广告", Fmt.full(watchedAt))
            InfoLine("写完分析", Fmt.full(finishedAt))
            Spacer(Modifier.height(22.dp))
            Row {
                PrimaryPill(text = "再看一条", icon = Icons.Rounded.Replay, onClick = onNext)
                Spacer(Modifier.width(10.dp))
                GhostPill(text = "回首页", onClick = onHome)
            }
        }
    }
}

@Composable
private fun InfoLine(label: String, value: String) {
    Row(Modifier.padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(
            label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(74.dp)
        )
        Text(value, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
    }
}
