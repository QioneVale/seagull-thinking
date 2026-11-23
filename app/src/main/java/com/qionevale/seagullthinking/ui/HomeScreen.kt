package com.qionevale.seagullthinking.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.qionevale.seagullthinking.data.Fmt
import java.time.LocalDate
import kotlin.math.max

@Composable
fun HomeScreen(
    vm: AppViewModel,
    onStartAd: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val settings by vm.settings.collectAsState()
    val records by vm.records.collectAsState()
    val today = LocalDate.now()
    val todayKey = today.toString()
    val todayRecords = records.filter { it.dayKey == todayKey }
    val target = settings.dailyTarget
    val done = todayRecords.size
    val streak = remember(records) { streakDays(records.map { it.dayKey }.toSet(), today) }

    Column(
        Modifier
            .fillMaxSize()
            .padding(top = 12.dp)
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 22.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    greeting(),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    "${today.monthValue}月${today.dayOfMonth}日 · 星期${"日一二三四五六"[today.dayOfWeek.value % 7]}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Box(
                Modifier
                    .size(42.dp)
                    .background(MaterialTheme.colorScheme.surface, CircleShape)
                    .clickableCircle(onOpenSettings),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Rounded.Tune,
                    "设置",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(19.dp)
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        // 今日进度
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 22.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(Modifier.weight(1f)) {
                SeagullCard {
                    Text("今日薯条", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            "$done",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            " / $target 条",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 3.dp)
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    ProgressDots(done = done, total = target)
                }
            }
            Box(Modifier.width(112.dp)) {
                SeagullCard {
                    Text("连续", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "$streak",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text("天", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        GullScene(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 8.dp),
            onClick = onStartAd,
            done = done,
            target = target
        )

        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                if (done >= target) "今天的薯条吃饱了，还想拆就继续点它" else "点一下海鸥，它会叼着薯条飞过来",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(6.dp))
            val last = todayRecords.maxByOrNull { it.watchedAt }
            if (last != null) {
                Text(
                    "上一条：${last.title} · ${Fmt.clock(last.watchedAt)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
                )
            }
        }
    }
}

private fun Modifier.clickableCircle(onClick: () -> Unit) = this.then(
    Modifier.pointerInput(Unit) { detectTapGestures { onClick() } }
)

private fun greeting(): String {
    val h = java.time.LocalTime.now().hour
    return when {
        h < 5 -> "还没睡？"
        h < 11 -> "早上好"
        h < 14 -> "中午好"
        h < 18 -> "下午好"
        h < 23 -> "晚上好"
        else -> "夜深了"
    }
}

private fun streakDays(days: Set<String>, today: LocalDate): Int {
    var d = today
    if (!days.contains(d.toString())) d = d.minusDays(1)
    var n = 0
    while (days.contains(d.toString())) {
        n++
        d = d.minusDays(1)
        if (n > 999) break
    }
    return n
}

@Composable
private fun ProgressDots(done: Int, total: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        val shown = total.coerceAtMost(12)
        repeat(shown) { i ->
            val filled = i < done
            Box(
                Modifier
                    .weight(1f, fill = false)
                    .height(5.dp)
                    .background(
                        if (filled) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                        RoundedCornerShape(3.dp)
                    )
                    .padding(end = 0.dp)
            )
        }
        if (total > shown) {
            Text(
                "+${total - shown}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun GullScene(
    modifier: Modifier,
    onClick: () -> Unit,
    done: Int,
    target: Int
) {
    val arrive = remember { Animatable(1f) }
    LaunchedEffect(Unit) { arrive.animateTo(0f, tween(1100, easing = FastOutSlowInEasing)) }

    val infinite = rememberInfiniteTransition(label = "gull")
    val bob by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1900, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "bob"
    )
    val flap by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(620, easing = LinearEasing), RepeatMode.Reverse),
        label = "flap"
    )

    val primary = MaterialTheme.colorScheme.primary
    val skyTop = primary.copy(alpha = 0.10f)
    val skyBottom = primary.copy(alpha = 0.02f)

    Canvas(
        modifier = modifier
            .pointerInput(Unit) { detectTapGestures { onClick() } }
    ) {
        val w = size.width
        val h = size.height
        val s = (minOf(w, h) / 330f).coerceAtLeast(0.75f)

        drawRect(Brush.verticalGradient(listOf(skyTop, skyBottom)))

        // 远处的云
        val cloud = Color.White.copy(alpha = 0.55f)
        drawOval(cloud, Offset(w * 0.10f, h * 0.14f), Size(w * 0.30f, h * 0.055f))
        drawOval(cloud.copy(alpha = 0.4f), Offset(w * 0.58f, h * 0.22f), Size(w * 0.24f, h * 0.045f))
        drawOval(cloud.copy(alpha = 0.3f), Offset(w * 0.34f, h * 0.07f), Size(w * 0.18f, h * 0.035f))

        val branchY = h * 0.80f
        val gullX = w * 0.5f
        val gullY = branchY - 96f * s + bob * 4f * s
        val slide = arrive.value * w * 1.15f

        withTransform({
            translate(left = slide, top = -40f * s * arrive.value)
        }) {
            drawGull(
                cx = gullX,
                cy = gullY,
                s = s,
                flap = flap,
                body = Color(0xFFFDFEFF),
                wingShade = Color(0xFFE8EEF6),
                beak = Color(0xFFF0A24B),
                fry = Color(0xFFF7C960),
                eye = Color(0xFF16202E)
            )
        }

        // 树枝
        val branch = Path().apply {
            moveTo(0f, branchY + 16f * s)
            cubicTo(w * 0.22f, branchY - 6f * s, w * 0.42f, branchY + 2f * s, w * 0.78f, branchY - 2f * s)
            cubicTo(w * 0.88f, branchY - 4f * s, w * 0.95f, branchY - 10f * s, w, branchY - 16f * s)
        }
        drawPath(branch, Color(0xFF6B5138), style = Stroke(width = 9f * s, cap = StrokeCap.Round))
        val twig = Path().apply {
            moveTo(w * 0.72f, branchY - 2f * s)
            quadraticTo(w * 0.76f, branchY - 34f * s, w * 0.83f, branchY - 52f * s)
        }
        drawPath(twig, Color(0xFF6B5138), style = Stroke(width = 5f * s, cap = StrokeCap.Round))
        // 叶子
        drawOval(Color(0xFF7FA67A), Offset(w * 0.80f, branchY - 62f * s), Size(26f * s, 12f * s))
        drawOval(Color(0xFF6C9668), Offset(w * 0.86f, branchY - 46f * s), Size(22f * s, 10f * s))

        // 脚下的抓握高光
        drawOval(
            Color.Black.copy(alpha = 0.06f),
            Offset(gullX - 34f * s, branchY - 8f * s),
            Size(68f * s, 12f * s)
        )
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawGull(
    cx: Float,
    cy: Float,
    s: Float,
    flap: Float,
    body: Color,
    wingShade: Color,
    beak: Color,
    fry: Color,
    eye: Color
) {
    // 尾羽
    val tail = Path().apply {
        moveTo(cx - 46f * s, cy + 6f * s)
        lineTo(cx - 92f * s, cy - 10f * s)
        lineTo(cx - 88f * s, cy + 6f * s)
        lineTo(cx - 44f * s, cy + 30f * s)
        close()
    }
    drawPath(tail, body)

    // 身体
    drawOval(body, Offset(cx - 58f * s, cy - 20f * s), Size(112f * s, 84f * s))

    // 腿
    val legTop = cy + 54f * s
    drawLine(beak.copy(alpha = 0.9f), Offset(cx + 6f * s, legTop), Offset(cx + 6f * s, legTop + 30f * s), strokeWidth = 4f * s, cap = StrokeCap.Round)
    drawLine(beak.copy(alpha = 0.9f), Offset(cx - 12f * s, legTop), Offset(cx - 12f * s, legTop + 30f * s), strokeWidth = 4f * s, cap = StrokeCap.Round)
    drawLine(beak, Offset(cx + 6f * s, legTop + 30f * s), Offset(cx + 18f * s, legTop + 34f * s), strokeWidth = 3.5f * s, cap = StrokeCap.Round)
    drawLine(beak, Offset(cx - 12f * s, legTop + 30f * s), Offset(cx, legTop + 34f * s), strokeWidth = 3.5f * s, cap = StrokeCap.Round)

    // 翅膀（扇动）
    rotate(degrees = -10f + flap * 16f, pivot = Offset(cx + 4f * s, cy - 16f * s)) {
        val wing = Path().apply {
            moveTo(cx + 10f * s, cy - 16f * s)
            cubicTo(
                cx - 6f * s, cy - 62f * s,
                cx - 62f * s, cy - 74f * s,
                cx - 86f * s, cy - 34f * s
            )
            cubicTo(
                cx - 58f * s, cy - 40f * s,
                cx - 26f * s, cy - 20f * s,
                cx + 10f * s, cy - 4f * s
            )
            close()
        }
        drawPath(wing, wingShade)
        drawPath(wing, body.copy(alpha = 0.85f))
        drawPath(wing, Color(0xFFDCE5F0), style = Stroke(width = 1.6f * s))
    }

    // 头
    drawCircle(body, radius = 30f * s, center = Offset(cx + 34f * s, cy - 32f * s))

    // 喙
    val beakPath = Path().apply {
        moveTo(cx + 60f * s, cy - 40f * s)
        lineTo(cx + 92f * s, cy - 28f * s)
        lineTo(cx + 60f * s, cy - 18f * s)
        close()
    }
    drawPath(beakPath, beak)

    // 薯条
    rotate(degrees = -7f, pivot = Offset(cx + 92f * s, cy - 28f * s)) {
        drawRoundRect(
            fry,
            topLeft = Offset(cx + 84f * s, cy - 40f * s),
            size = Size(66f * s, 15f * s),
            cornerRadius = CornerRadius(7f * s)
        )
        drawRoundRect(
            Color(0xFFE8B44A),
            topLeft = Offset(cx + 84f * s, cy - 22f * s),
            size = Size(52f * s, 9f * s),
            cornerRadius = CornerRadius(4f * s)
        )
    }

    // 眼睛
    drawCircle(eye, radius = 4.6f * s, center = Offset(cx + 44f * s, cy - 38f * s))
    drawCircle(Color.White.copy(alpha = 0.9f), radius = 1.6f * s, center = Offset(cx + 45.4f * s, cy - 39.4f * s))
}
