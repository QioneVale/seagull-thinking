package com.qionevale.seagullthinking.data

import android.content.Context
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.util.concurrent.TimeUnit

data class BiliAudio(val title: String, val durationMs: Long, val file: File)

/**
 * 从 BV 号解析 B 站音频并落盘。
 * 说明：B 站接口存在风控，若返回 -403/-404 通常是需要登录 Cookie 或视频受限。
 */
class BiliImporter(private val context: Context) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    private var cookieProvider: () -> String = { "" }

    private val ua =
        "Mozilla/5.0 (Linux; Android 13; Pixel 7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0 Mobile Safari/537.36"

    suspend fun import(bvInput: String, cookie: String = ""): BiliAudio = withContext(Dispatchers.IO) {
        cookieProvider = { cookie }
        val bvid = extractBvid(bvInput)
            ?: throw IllegalArgumentException("没找到 BV 号，请粘贴形如 BV1xx411c7mD 的编号")

        val view = getJson("https://api.bilibili.com/x/web-interface/view?bvid=$bvid")
        ensureOk(view, "获取视频信息失败")
        val data = view.getAsJsonObject("data")
        val title = data.get("title")?.asString ?: "B站音频 $bvid"
        val page = data.getAsJsonArray("pages")?.firstOrNull()?.asJsonObject
        val cid = page?.get("cid")?.asLong ?: data.get("cid")?.asLong
            ?: throw IllegalStateException("这个视频没有可用的分 P 信息")
        val durationSec = data.get("duration")?.asLong ?: 0L

        val audioUrl = try {
            resolveAudioUrl(bvid, cid)
        } catch (e: Exception) {
            throw IllegalStateException(
                "解析音频失败：${e.message}\n\n" +
                    "常见原因：该视频需要登录 / 是付费或番剧内容 / B 站风控。\n" +
                    "可以到「设置 → 薯条计划 → B站 Cookie」填入浏览器里的 SESSDATA 后重试。"
            )
        }

        val target = File(Files.musicDir(context), safeName(bvid) + ".m4a")
        val request = Request.Builder()
            .url(audioUrl)
            .header("User-Agent", ua)
            .header("Referer", "https://www.bilibili.com/video/$bvid")
            .apply { cookieHeader()?.let { header("Cookie", it) } }
            .build()

        client.newCall(request).execute().use { resp ->
            if (!resp.isSuccessful) throw IllegalStateException("音频下载失败：HTTP ${resp.code}")
            val body = resp.body ?: throw IllegalStateException("音频流为空")
            target.outputStream().use { out -> body.byteStream().copyTo(out) }
        }
        if (target.length() < 20_000) {
            target.delete()
            throw IllegalStateException("下载到的文件太小，可能被风控拦截了，换个视频或填 Cookie 再试")
        }
        BiliAudio(title, durationSec * 1000, target)
    }

    private fun resolveAudioUrl(bvid: String, cid: Long): String {
        val json = getJson(
            "https://api.bilibili.com/x/player/playurl?bvid=$bvid&cid=$cid&fnval=16&fourk=1&qn=64"
        )
        ensureOk(json, "获取播放地址失败")
        val data = json.getAsJsonObject("data")
        val dash = data.getAsJsonObject("dash")
        if (dash != null) {
            val audios = dash.getAsJsonArray("audio")
            if (audios != null && audios.size() > 0) {
                val best = audios.map { it.asJsonObject }
                    .maxByOrNull { it.get("bandwidth")?.asLong ?: 0L }
                best?.get("baseUrl")?.asString?.let { return it }
                best?.get("base_url")?.asString?.let { return it }
            }
            // 部分视频只有 flac
            dash.getAsJsonObject("flac")?.getAsJsonObject("audio")
                ?.get("baseUrl")?.asString?.let { return it }
        }
        val durl = data.getAsJsonArray("durl")
        if (durl != null && durl.size() > 0) {
            durl[0].asJsonObject.get("url")?.asString?.let { return it }
        }
        throw IllegalStateException("没有找到音频流")
    }

    private fun getJson(url: String): JsonObject {
        val builder = Request.Builder()
            .url(url)
            .header("User-Agent", ua)
            .header("Referer", "https://www.bilibili.com/")
        cookieHeader()?.let { builder.header("Cookie", it) }
        client.newCall(builder.build()).execute().use { resp ->
            val text = resp.body?.string().orEmpty()
            if (text.isBlank()) throw IllegalStateException("接口返回为空（HTTP ${resp.code}）")
            return JsonParser.parseString(text).asJsonObject
        }
    }

    private fun cookieHeader(): String? {
        val raw = cookieProvider().trim()
        if (raw.isBlank()) return null
        return if (raw.contains("=")) raw else "SESSDATA=$raw"
    }

    private fun ensureOk(json: JsonObject, fallback: String) {
        val code = json.get("code")?.asInt ?: -1
        if (code != 0) {
            val msg = json.get("message")?.asString ?: fallback
            throw IllegalStateException("$msg（错误码 $code）")
        }
    }

    private fun extractBvid(input: String): String? {
        val regex = Regex("(BV[0-9A-Za-z]{10})")
        return regex.find(input.trim())?.value
    }

    private fun safeName(s: String): String = s.replace(Regex("[^0-9A-Za-z_-]"), "_")
}
