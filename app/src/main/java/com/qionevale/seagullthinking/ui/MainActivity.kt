@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.qionevale.seagullthinking.ui

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.EventNote
import androidx.compose.material.icons.rounded.Fastfood
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.VideoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import coil.compose.AsyncImage
import com.qionevale.seagullthinking.data.Fmt
import com.qionevale.seagullthinking.data.PlayerController
import kotlinx.coroutines.launch
import java.io.File
import java.net.URLDecoder

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val vm: AppViewModel = viewModel()
            val settings by vm.settings.collectAsState()
            SeagullTheme(settings) {
                AppRoot(vm)
            }
        }
    }
}

private data class Tab(val route: String, val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector)

private val tabs = listOf(
    Tab("home", "搞薯条", Icons.Rounded.Fastfood),
    Tab("mingzhi", "明志", Icons.Rounded.GraphicEq),
    Tab("records", "记录", Icons.Rounded.EventNote),
    Tab("settings", "设置", Icons.Rounded.Tune)
)

@Composable
fun AppRoot(vm: AppViewModel) {
    val nav = rememberNavController()
    val settings by vm.settings.collectAsState()
    val current by vm.player.current.collectAsState()
    val playing by vm.player.playing.collectAsState()
    val message by vm.message.collectAsState()
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val ctx = LocalContext.current

    var pendingAd by remember { mutableStateOf<AdPlayRequest?>(null) }
    var showSource by remember { mutableStateOf(false) }
    var showWebInput by remember { mutableStateOf(false) }

    val backStack by nav.currentBackStackEntryAsState()
    val route = backStack?.destination?.route ?: "home"
    val topLevel = route in setOf("home", "mingzhi", "records", "settings")

    val localPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) {
            val name = uri.lastPathSegment?.substringAfterLast('/') ?: "本地广告视频"
            pendingAd = AdPlayRequest(name, "LOCAL", uri.toString())
            nav.navigate("ad") { launchSingleTop = true }
        }
    }

    LaunchedEffect(message) {
        message?.let {
            snackbar.showSnackbar(it)
            vm.consumeMessage()
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        settings.backgroundPath?.let { path ->
            if (File(path).exists()) {
                AsyncImage(
                    model = File(path),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background.copy(alpha = 0.87f))
                )
            }
        }

        Scaffold(
            containerColor = Color.Transparent,
            snackbarHost = { SnackbarHost(snackbar) },
            bottomBar = {
                Column {
                    if (current != null && route != "player") {
                        MiniPlayer(
                            vm = vm,
                            playing = playing,
                            onOpen = { nav.navigate("player") { launchSingleTop = true } },
                            onToggle = { vm.player.toggle() },
                            onNext = { vm.player.next() }
                        )
                    }
                    if (topLevel) {
                        SeagullBottomBar(current = route) { target ->
                            if (target != route) {
                                nav.navigate(target) {
                                    popUpTo("home") { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        }
                    }
                }
            }
        ) { padding ->
            NavHost(
                navController = nav,
                startDestination = "home",
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .padding(bottom = padding.calculateBottomPadding())
            ) {
                composable("home") {
                    HomeScreen(
                        vm = vm,
                        onStartAd = { showSource = true },
                        onOpenSettings = {
                            nav.navigate("settings") {
                                popUpTo("home") { saveState = true }
                                launchSingleTop = true
                            }
                        }
                    )
                }
                composable("ad") {
                    AdSessionScreen(
                        vm = vm,
                        request = pendingAd,
                        onExit = {
                            pendingAd = null
                            nav.popBackStack("home", false)
                        },
                        onNextAd = {
                            pendingAd = null
                            nav.popBackStack("home", false)
                            showSource = true
                        }
                    )
                }
                composable("mingzhi") {
                    MingzhiScreen(
                        vm = vm,
                        onOpenMode = { id -> nav.navigate("mode/$id") },
                        onOpenPlayer = { nav.navigate("player") { launchSingleTop = true } }
                    )
                }
                composable(
                    route = "mode/{modeId}",
                    arguments = listOf(navArgument("modeId") { type = NavType.LongType })
                ) { entry ->
                    val id = entry.arguments?.getLong("modeId") ?: 0L
                    MusicModeScreen(
                        vm = vm,
                        modeId = id,
                        onBack = { nav.popBackStack() },
                        onOpenPlayer = { nav.navigate("player") { launchSingleTop = true } }
                    )
                }
                composable("player") {
                    PlayerScreen(vm = vm, onBack = { nav.popBackStack() })
                }
                composable("records") {
                    RecordsScreen(vm = vm)
                }
                composable("settings") {
                    SettingsScreen(
                        vm = vm,
                        onPersonalize = { nav.navigate("settings/personalize") },
                        onPlan = { nav.navigate("settings/plan") },
                        onStorage = { nav.navigate("settings/storage") }
                    )
                }
                composable("settings/personalize") {
                    PersonalizeScreen(vm = vm, onBack = { nav.popBackStack() })
                }
                composable("settings/plan") {
                    PlanScreen(vm = vm, onBack = { nav.popBackStack() })
                }
                composable("settings/storage") {
                    StorageScreen(vm = vm, onBack = { nav.popBackStack() })
                }
            }
        }
    }

    if (showSource) {
        AdSourceSheet(
            settings = settings,
            onDismiss = { showSource = false },
            onSample = { index, title, ref ->
                showSource = false
                pendingAd = AdPlayRequest(title, "SAMPLE", ref)
                nav.navigate("ad") { launchSingleTop = true }
            },
            onLocal = {
                showSource = false
                localPicker.launch(arrayOf("video/*"))
            },
            onWeb = {
                showSource = false
                showWebInput = true
            }
        )
    }

    if (showWebInput) {
        InputDialog(
            title = "广告网页链接",
            placeholder = "https://…",
            onDismiss = { showWebInput = false },
            onConfirm = { url ->
                showWebInput = false
                if (!url.startsWith("http", ignoreCase = true)) {
                    vm.say("链接要以 http:// 或 https:// 开头")
                } else {
                    pendingAd = AdPlayRequest("网页广告", "WEB", url)
                    nav.navigate("ad") { launchSingleTop = true }
                }
            }
        )
    }
}

@Composable
private fun SeagullBottomBar(current: String, onSelect: (String) -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 8.dp),
        shape = RoundedCornerShape(26.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 10.dp,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
        )
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp, vertical = 7.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            tabs.forEach { tab ->
                val active = tab.route == current
                Column(
                    Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(19.dp))
                        .background(
                            if (active) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                            else Color.Transparent
                        )
                        .clickable { onSelect(tab.route) }
                        .padding(vertical = 7.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        tab.icon,
                        tab.label,
                        modifier = Modifier.size(21.dp),
                        tint = if (active) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(3.dp))
                    Text(
                        tab.label,
                        fontSize = 10.sp,
                        color = if (active) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal
                    )
                }
            }
        }
    }
}

@Composable
private fun MiniPlayer(
    vm: AppViewModel,
    playing: Boolean,
    onOpen: () -> Unit,
    onToggle: () -> Unit,
    onNext: () -> Unit
) {
    val current by vm.player.current.collectAsState()
    val modes by vm.modes.collectAsState()
    val modeName by vm.player.modeName.collectAsState()
    val track = current ?: return
    val mode = modes.firstOrNull { it.name == modeName }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 6.dp,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
        ),
        onClick = onOpen
    ) {
        Row(
            Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            MusicCover(path = mode?.coverPath, size = 38.dp, seed = mode?.name ?: track.title)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    track.title,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    modeName.ifBlank { "正在播放" } + " · " + Fmt.shortDuration(track.durationMs),
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Box(
                Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                    .clickable { onToggle() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    if (playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                    "播放",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
            }
            Box(
                Modifier
                    .size(34.dp)
                    .clickable { onNext() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Rounded.SkipNext,
                    "下一首",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun AdSourceSheet(
    settings: com.qionevale.seagullthinking.data.AppSettings,
    onDismiss: () -> Unit,
    onSample: (Int, String, String) -> Unit,
    onLocal: () -> Unit,
    onWeb: () -> Unit
) {
    val samples = listOf(
        Triple("轻氧咖啡 · 0 糖 0 脂", "ads/ad1.mp4", "熬夜党的续命水"),
        Triple("北岸自习室 · 24 小时", "ads/ad2.mp4", "二战的人最怕没人陪"),
        Triple("拾光露营 · 轻露营", "ads/ad3.mp4", "说走就走的周末")
    )
    androidx.compose.material3.ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.padding(horizontal = 22.dp, vertical = 6.dp)) {
            Text("这条看什么？", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(4.dp))
            Text(
                "选一个来源，看完就要拆它",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(16.dp))

            if (settings.sourceSample) {
                samples.forEachIndexed { i, s ->
                    SourceRow(
                        icon = Icons.Rounded.Fastfood,
                        title = s.first,
                        desc = s.third
                    ) {
                        onSample(i, s.first, s.second)
                    }
                }
            }
            if (settings.sourceLocal) {
                SourceRow(
                    icon = Icons.Rounded.VideoLibrary,
                    title = "从手机选一段广告视频",
                    desc = "抖音录屏、朋友圈广告都行"
                ) { onLocal() }
            }
            if (settings.sourceWeb) {
                SourceRow(
                    icon = Icons.Rounded.Link,
                    title = "贴一个网页链接",
                    desc = "在 App 里直接打开广告页"
                ) { onWeb() }
            }
            if (!settings.sourceSample && !settings.sourceLocal && !settings.sourceWeb) {
                Text(
                    "三种来源都关掉了，去「设置 → 薯条计划」打开一个。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
            Spacer(Modifier.height(26.dp))
        }
    }
}

@Composable
private fun SourceRow(
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
