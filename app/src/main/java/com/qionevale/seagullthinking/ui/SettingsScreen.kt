@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.qionevale.seagullthinking.ui

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.ColorLens
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.qionevale.seagullthinking.data.Fmt

@Composable
fun SettingsScreen(
    vm: AppViewModel,
    onPersonalize: () -> Unit,
    onPlan: () -> Unit,
    onStorage: () -> Unit
) {
    val settings by vm.settings.collectAsState()
    val records by vm.records.collectAsState()
    val quotes by vm.quotes.collectAsState()
    val tracks by vm.tracks.collectAsState()

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        PageHeader(title = "设置", subtitle = "把这个 App 调成你喜欢的样子")

        Column(Modifier.padding(horizontal = 20.dp)) {
            SettingsEntry(
                icon = Icons.Rounded.ColorLens,
                title = "个性化设置",
                desc = "${settings.accent.label} · ${settings.dark.label} · ${settings.font.label}"
            ) { onPersonalize() }
            Spacer(Modifier.height(10.dp))
            SettingsEntry(
                icon = Icons.Rounded.Restaurant,
                title = "薯条计划设置",
                desc = "每天 ${settings.dailyTarget} 条 · ${settings.labels.size} 个分析点"
            ) { onPlan() }
            Spacer(Modifier.height(10.dp))
            SettingsEntry(
                icon = Icons.Rounded.Storage,
                title = "存储与导出设置",
                desc = "${records.size} 条记录 · 听歌 ${Fmt.duration(tracks.sumOf { it.listenedMs })}"
            ) { onStorage() }

            Spacer(Modifier.height(22.dp))
            SectionLabel("这一刻的数据")
            SeagullCard {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    MiniStat("广告复盘", "${records.size}")
                    MiniStat("分析组", "${records.count { it.analysisDoneAt != null }}")
                    MiniStat("随想", "${records.count { it.note.isNotBlank() }}")
                    MiniStat("明志", "${quotes.size}")
                }
                Spacer(Modifier.height(14.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    MiniStat("听过", Fmt.duration(tracks.sumOf { it.listenedMs }))
                    MiniStat("曲目", "${tracks.size}")
                    MiniStat("听完", "${tracks.sumOf { it.playCount }} 次")
                    Box(Modifier.width(1.dp))
                }
            }

            Spacer(Modifier.height(22.dp))
            SectionLabel("关于")
            SeagullCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier
                            .size(40.dp)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Rounded.Info,
                            null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text("海鸥思维 1.0.0", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                        Text(
                            "看广告，拆商业模式；听音乐，把日子过成节奏。",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    "所有数据都只存在这台手机里，不上传、不联网同步。",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Spacer(Modifier.height(110.dp))
    }
}

@Composable
private fun SettingsEntry(
    icon: ImageVector,
    title: String,
    desc: String,
    onClick: () -> Unit
) {
    SeagullCard(onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(42.dp)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(21.dp))
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text(
                    desc,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                Icons.Rounded.ChevronRight,
                null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun MiniStat(label: String, value: String) {
    Column {
        Text(
            value,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
