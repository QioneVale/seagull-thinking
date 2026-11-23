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
import androidx.compose.ui.graphics.Brush
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

    val backStack by nav.currentBackStackEntryAsState()
    val route = backStack?.destination?.route ?: "home"
    val topLevel = route in setOf("home", "mingzhi", "records", "settings")

    // 点海鸥 → 直接从素材库取一条开播，不再问"用哪个来源"
    fun playAsset(asset: com.qionevale.seagullthinking.data.AdAsset) {
        pendingAd = AdPlayRequest(asset.title, asset.kind, asset.ref, asset.id)
        nav.navigate("ad") { launchSingleTop = true }
    }

    val localPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) {
            vm.addAdVideos(listOf(uri)) { added ->
                if (added > 0) {
                    scope.launch { playAsset(vm.takeNextAd() ?: return@launch) }
                }
            }
        }
    }

    var adBusy by remember { mutableStateOf(false) }

    fun playNextAd() {
        if (adBusy) return
        adBusy = true
        scope.launch {
            val asset = vm.takeNextAd()
            adBusy = false
            if (asset == null) {
                vm.say("广告库还空着，先挑一段真实广告")
                localPicker.launch(arrayOf("video/*"))
            } else {
                playAsset(asset)
            }
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
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    MaterialTheme.colorScheme.background.copy(alpha = 0.34f),
                                    MaterialTheme.colorScheme.background.copy(alpha = 0.52f),
                                    MaterialTheme.colorScheme.background.copy(alpha = 0.78f)
                                )
                            )
                        )
                )
            }
        }

        Scaffold(
            containerColor = Color.Transparent,
            snackbarHost = { SnackbarHost(snackbar) },
            bottomBar = {
                Column {
                    if (current != null && route != "player" && route != "ad") {
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
                        onStartAd = { playNextAd() },
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
                            playNextAd()
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
