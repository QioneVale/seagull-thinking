package com.qionevale.seagullthinking.data

import android.content.Context
import android.net.Uri
import java.io.File
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

object Fmt {
    private val zone: ZoneId get() = ZoneId.systemDefault()
    private val full = DateTimeFormatter.ofPattern("yyyy年M月d日 HH:mm:ss")
    private val hm = DateTimeFormatter.ofPattern("HH:mm")

    fun toLocal(ms: Long): LocalDateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(ms), zone)
    fun toDate(ms: Long): LocalDate = Instant.ofEpochMilli(ms).atZone(zone).toLocalDate()
    fun full(ms: Long?): String = if (ms == null || ms <= 0) "—" else toLocal(ms).format(full)
    fun clock(ms: Long): String = toLocal(ms).format(hm)
    fun dayKey(ms: Long): String = toDate(ms).toString()
    fun dayLabel(date: LocalDate, today: LocalDate = LocalDate.now()): String = when (date) {
        today -> "今天"
        today.minusDays(1) -> "昨天"
        else -> "${date.monthValue}月${date.dayOfMonth}日"
    }

    fun duration(ms: Long): String {
        if (ms <= 0) return "0 分钟"
        val totalSec = ms / 1000
        val h = totalSec / 3600
        val m = (totalSec % 3600) / 60
        val s = totalSec % 60
        return when {
            h > 0 -> "${h} 小时 ${m} 分"
            m > 0 -> "${m} 分 ${s} 秒"
            else -> "${s} 秒"
        }
    }

    fun shortDuration(ms: Long): String {
        val totalSec = (ms / 1000).coerceAtLeast(0)
        return "%d:%02d".format(totalSec / 60, totalSec % 60)
    }
}

object Files {
    fun musicDir(context: Context): File =
        File(context.filesDir, "music").apply { mkdirs() }

    fun coverDir(context: Context): File =
        File(context.filesDir, "covers").apply { mkdirs() }

    fun bgFile(context: Context): File =
        File(context.filesDir, "background.jpg")

    fun copyTo(context: Context, uri: Uri, target: File): File {
        context.contentResolver.openInputStream(uri).use { input ->
            requireNotNull(input) { "无法读取该文件" }
            target.outputStream().use { out -> input.copyTo(out) }
        }
        return target
    }
}
