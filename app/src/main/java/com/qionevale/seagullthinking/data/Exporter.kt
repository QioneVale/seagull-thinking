package com.qionevale.seagullthinking.data

import android.content.Context
import android.net.Uri
import com.google.gson.GsonBuilder
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

class Exporter(private val context: Context, private val db: AppDatabase) {

    suspend fun exportJson(uri: Uri): Int = withContext(Dispatchers.IO) {
        val records = db.adRecordDao().allOnce()
        val items = db.analysisItemDao().allOnce().groupBy { it.recordId }
        val tracks = db.trackDao().allOnce()
        val modes = db.musicModeDao().observeAll().first()
        val quotes = db.quoteDao().observeAll().first()

        val root = JsonObject()
        root.addProperty("app", "海鸥思维")
        root.addProperty("version", 1)
        root.addProperty("exportedAt", Fmt.full(System.currentTimeMillis()))

        val ads = JsonArray()
        records.forEach { r ->
            val o = JsonObject()
            o.addProperty("title", r.title)
            o.addProperty("source", sourceLabel(r.source))
            o.addProperty("watchedAt", Fmt.full(r.watchedAt))
            o.addProperty("analysisDoneAt", Fmt.full(r.analysisDoneAt))
            o.addProperty("note", r.note)
            o.addProperty("noteAt", Fmt.full(r.noteAt))
            val arr = JsonArray()
            items[r.id].orEmpty().forEach { a ->
                arr.add(JsonObject().apply {
                    addProperty("label", a.label)
                    addProperty("content", a.content)
                    addProperty("updatedAt", Fmt.full(a.updatedAt))
                })
            }
            o.add("analysis", arr)
            ads.add(o)
        }
        root.add("ads", ads)

        root.add("quotes", JsonArray().apply {
            quotes.forEach { q ->
                add(JsonObject().apply {
                    addProperty("text", q.text)
                    addProperty("x", q.xRatio)
                    addProperty("y", q.yRatio)
                    addProperty("createdAt", Fmt.full(q.createdAt))
                })
            }
        })

        root.add("musicModes", JsonArray().apply {
            modes.forEach { m ->
                add(JsonObject().apply {
                    addProperty("name", m.name)
                    add("tracks", JsonArray().apply {
                        tracks.filter { it.modeId == m.id }.forEach { t ->
                            add(JsonObject().apply {
                                addProperty("title", t.title)
                                addProperty("playCount", t.playCount)
                                addProperty("listenedMs", t.listenedMs)
                                addProperty("listened", Fmt.duration(t.listenedMs))
                            })
                        }
                    })
                })
            }
        })

        root.add("stats", JsonObject().apply {
            addProperty("adRecords", records.size)
            addProperty("analysisItems", items.values.sumOf { it.size })
            addProperty("notes", records.count { it.note.isNotBlank() })
            addProperty("quotes", quotes.size)
            addProperty("listenMs", tracks.sumOf { it.listenedMs })
            addProperty("listenReadable", Fmt.duration(tracks.sumOf { it.listenedMs }))
        })

        val gson = GsonBuilder().setPrettyPrinting().create()
        context.contentResolver.openOutputStream(uri)?.use { out ->
            out.write(gson.toJson(root).toByteArray(Charsets.UTF_8))
        } ?: throw IllegalStateException("无法写入文件")
        records.size
    }

    suspend fun exportCsv(uri: Uri): Int = withContext(Dispatchers.IO) {
        val records = db.adRecordDao().allOnce()
        val items = db.analysisItemDao().allOnce().groupBy { it.recordId }
        val sb = StringBuilder()
        sb.append('\uFEFF')
        sb.append("日期,广告标题,来源,看完时间,分析点,分析内容,分析时间,随笔,随笔时间\n")
        var rows = 0
        records.forEach { r ->
            val list = items[r.id].orEmpty()
            if (list.isEmpty()) {
                sb.append(csv(Fmt.dayKey(r.watchedAt))).append(',')
                    .append(csv(r.title)).append(',')
                    .append(csv(sourceLabel(r.source))).append(',')
                    .append(csv(Fmt.full(r.watchedAt))).append(',')
                    .append(',').append(',').append(',')
                    .append(csv(r.note)).append(',')
                    .append(csv(Fmt.full(r.noteAt))).append('\n')
                rows++
            } else {
                list.forEach { a ->
                    sb.append(csv(Fmt.dayKey(r.watchedAt))).append(',')
                        .append(csv(r.title)).append(',')
                        .append(csv(sourceLabel(r.source))).append(',')
                        .append(csv(Fmt.full(r.watchedAt))).append(',')
                        .append(csv(a.label)).append(',')
                        .append(csv(a.content)).append(',')
                        .append(csv(Fmt.full(a.updatedAt))).append(',')
                        .append(csv(r.note)).append(',')
                        .append(csv(Fmt.full(r.noteAt))).append('\n')
                    rows++
                }
            }
        }
        context.contentResolver.openOutputStream(uri)?.use { out ->
            out.write(sb.toString().toByteArray(Charsets.UTF_8))
        } ?: throw IllegalStateException("无法写入文件")
        rows
    }

    fun sourceLabel(source: String): String = when (source) {
        "VIDEO", "LOCAL" -> "视频素材"
        "WEB" -> "网页链接"
        else -> source
    }

    private fun csv(s: String?): String {
        val v = s.orEmpty()
        return if (v.contains(',') || v.contains('"') || v.contains('\n')) {
            "\"" + v.replace("\"", "\"\"") + "\""
        } else v
    }
}
