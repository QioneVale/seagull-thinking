package com.qionevale.seagullthinking.ui

import android.app.Application
import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.qionevale.seagullthinking.SeagullApp
import com.qionevale.seagullthinking.data.AdAsset
import com.qionevale.seagullthinking.data.AdRecord
import com.qionevale.seagullthinking.data.AnalysisItem
import com.qionevale.seagullthinking.data.AppSettings
import com.qionevale.seagullthinking.data.Files
import com.qionevale.seagullthinking.data.Fmt
import com.qionevale.seagullthinking.data.MusicMode
import com.qionevale.seagullthinking.data.Quote
import com.qionevale.seagullthinking.data.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

data class BiliProgress(
    val loading: Boolean = false,
    val text: String = "",
    val error: Boolean = false,
    val visible: Boolean = false
)

class AppViewModel(app: Application) : AndroidViewModel(app) {

    private val container = (app as SeagullApp).container
    private val db = container.db
    private val ctx: Context = app.applicationContext

    val player = container.player
    val settingsRepo = container.settings
    private val exporter = container.exporter
    private val biliImporter = container.bili

    val settings: StateFlow<AppSettings> =
        settingsRepo.flow.stateIn(viewModelScope, SharingStarted.Eagerly, AppSettings())

    val records: StateFlow<List<AdRecord>> =
        db.adRecordDao().observeAll()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val quotes: StateFlow<List<Quote>> =
        db.quoteDao().observeAll()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val modes: StateFlow<List<MusicMode>> =
        db.musicModeDao().observeAll()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val adAssets: StateFlow<List<AdAsset>> =
        db.adAssetDao().observeAll()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val analyses: StateFlow<List<AnalysisItem>> =
        db.analysisItemDao().observeAll()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val tracks: StateFlow<List<Track>> =
        db.trackDao().observeAll()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    private val _bili = MutableStateFlow(BiliProgress())
    val bili: StateFlow<BiliProgress> = _bili.asStateFlow()

    init {
        viewModelScope.launch {
            if (db.quoteDao().count() == 0) seedQuotes() else dressLegacyQuotes()
        }
    }

    /**
     * 1.0.0 的语录只能写文字，没有样式。这里给一批"从没调整过"的老语录
     * 一次性排一下字号，让墙在升级后立刻像面墙，之后怎么改都由用户自己定。
     */
    private suspend fun dressLegacyQuotes() {
        val all = db.quoteDao().allOnce()
        if (all.isEmpty() || all.size > 10) return
        if (all.any { it.fontKey != null || it.scale != 1f || it.colorArgb != 0 }) return
        val sizes = listOf(1.55f, 1.0f, 0.95f, 1.9f, 1.55f, 1.15f, 1.0f, 0.85f)
        all.forEachIndexed { i, q ->
            db.quoteDao().update(
                q.copy(scale = sizes[i % sizes.size], fontKey = "serif")
            )
        }
    }

    private suspend fun seedQuotes() {
        val now = System.currentTimeMillis()
        val seeds = listOf(
            Quote(text = "先把一件事\n做透，再谈第二件", xRatio = 0.26f, yRatio = 0.16f, scale = 1.55f, createdAt = now),
            Quote(text = "看广告不是看热闹", xRatio = 0.66f, yRatio = 0.10f, scale = 1.0f, createdAt = now + 1),
            Quote(text = "是看别人怎么把钱挣走", xRatio = 0.50f, yRatio = 0.30f, scale = 0.95f, createdAt = now + 2),
            Quote(text = "每天三条", xRatio = 0.18f, yRatio = 0.44f, scale = 1.9f, bold = true, createdAt = now + 3),
            Quote(text = "半年就是五百四十次复盘", xRatio = 0.56f, yRatio = 0.52f, scale = 30f / 18f, fontKey = "system", createdAt = now + 4),
            Quote(text = "今天不想动", xRatio = 0.24f, yRatio = 0.66f, scale = 1.15f, italic = true, createdAt = now + 5),
            Quote(text = "就先坐着听完一首歌", xRatio = 0.62f, yRatio = 0.74f, scale = 1.0f, createdAt = now + 6),
            Quote(text = "慢一点没关系", xRatio = 0.34f, yRatio = 0.86f, scale = 0.85f, colorArgb = 0, createdAt = now + 7)
        )
        seeds.forEach { db.quoteDao().insert(it.copy(fontKey = it.fontKey ?: "serif")) }
    }

    fun say(msg: String) {
        _message.value = msg
    }

    fun consumeMessage() {
        _message.value = null
    }

    // ---------------- 广告记录 ----------------

    suspend fun saveAdSession(
        title: String,
        source: String,
        ref: String,
        watchedAt: Long,
        answers: List<Pair<String, String>>
    ): Long {
        val now = System.currentTimeMillis()
        val id = db.adRecordDao().insert(
            AdRecord(
                title = title,
                source = source,
                ref = ref,
                watchedAt = watchedAt,
                analysisDoneAt = now,
                dayKey = Fmt.dayKey(watchedAt)
            )
        )
        val items = answers.mapIndexedNotNull { index, pair ->
            if (pair.second.isBlank()) null
            else AnalysisItem(
                recordId = id,
                orderIndex = index,
                label = pair.first,
                content = pair.second.trim(),
                updatedAt = now
            )
        }
        if (items.isNotEmpty()) db.analysisItemDao().insertAll(items)
        return id
    }

    suspend fun analysisOf(recordId: Long): List<AnalysisItem> =
        withContext(Dispatchers.IO) { db.analysisItemDao().forRecord(recordId) }

    suspend fun saveNote(record: AdRecord, note: String) {
        db.adRecordDao().update(
            record.copy(note = note.trim(), noteAt = System.currentTimeMillis())
        )
    }

    suspend fun deleteRecord(record: AdRecord) {
        db.analysisItemDao().deleteFor(record.id)
        db.adRecordDao().delete(record)
    }

    // ---------------- 语录 ----------------

    fun addQuote(quote: Quote) {
        if (quote.text.isBlank()) return
        viewModelScope.launch {
            db.quoteDao().insert(
                quote.copy(id = 0, text = quote.text.trim(), createdAt = System.currentTimeMillis())
            )
        }
    }

    /** 保存一句话的内容与全部排版 */
    fun saveQuote(quote: Quote) {
        if (quote.text.isBlank()) return
        viewModelScope.launch { db.quoteDao().update(quote.copy(text = quote.text.trim())) }
    }

    /** 只在编辑模式下移动位置 */
    fun moveQuote(quote: Quote, x: Float, y: Float) {
        viewModelScope.launch {
            db.quoteDao().update(quote.copy(xRatio = x.coerceIn(0.03f, 0.97f), yRatio = y.coerceIn(0.03f, 0.97f)))
        }
    }

    fun editQuote(quote: Quote, text: String) {
        if (text.isBlank()) return
        viewModelScope.launch { db.quoteDao().update(quote.copy(text = text.trim())) }
    }

    fun deleteQuote(quote: Quote) {
        viewModelScope.launch { db.quoteDao().delete(quote) }
    }

    // ---------------- 广告素材库 ----------------

    /**
     * 取下一条要看的广告：优先挑最久没看过的，全都看过就从头轮。
     * 返回 null 表示库里还没有素材。
     */
    suspend fun takeNextAd(): AdAsset? = withContext(Dispatchers.IO) {
        val all = db.adAssetDao().allOnce()
        val next = all.firstOrNull() ?: return@withContext null
        db.adAssetDao().update(
            next.copy(lastPlayedAt = System.currentTimeMillis(), playCount = next.playCount + 1)
        )
        next
    }

    /** 把选中的视频复制进 App 内部，之后永远能播，不依赖相册权限 */
    fun addAdVideos(uris: List<Uri>, onDone: (Int) -> Unit = {}) {
        if (uris.isEmpty()) return
        viewModelScope.launch {
            var added = 0
            withContext(Dispatchers.IO) {
                uris.forEachIndexed { i, uri ->
                    runCatching {
                        val name = queryName(uri) ?: "广告 ${System.currentTimeMillis()}"
                        val target = File(
                            Files.adDir(ctx),
                            "ad_${System.currentTimeMillis()}_${i}." + name.substringAfterLast('.', "mp4")
                        )
                        Files.copyTo(ctx, uri, target)
                        db.adAssetDao().insert(
                            AdAsset(
                                title = name.substringBeforeLast(".").take(60).ifBlank { "未命名广告" },
                                kind = "VIDEO",
                                ref = Uri.fromFile(target).toString(),
                                addedAt = System.currentTimeMillis()
                            )
                        )
                        added++
                    }
                }
            }
            onDone(added)
        }
    }

    fun addAdLink(title: String, url: String) {
        if (!url.startsWith("http", ignoreCase = true)) {
            say("链接要以 http:// 或 https:// 开头")
            return
        }
        viewModelScope.launch {
            db.adAssetDao().insert(
                AdAsset(
                    title = title.ifBlank { "网页广告" }.take(60),
                    kind = "WEB",
                    ref = url.trim(),
                    addedAt = System.currentTimeMillis()
                )
            )
        }
    }

    fun deleteAdAsset(asset: AdAsset) {
        viewModelScope.launch {
            db.adAssetDao().delete(asset)
            if (asset.kind == "VIDEO") {
                withContext(Dispatchers.IO) {
                    runCatching { File(Uri.parse(asset.ref).path ?: "").delete() }
                }
            }
        }
    }

    // ---------------- 音乐 ----------------

    fun createMode(name: String, cover: Uri?) {
        if (name.isBlank()) return
        viewModelScope.launch {
            val coverPath = withContext(Dispatchers.IO) {
                cover?.let { uri ->
                    runCatching {
                        val f = File(Files.coverDir(ctx), "cover_${System.currentTimeMillis()}.jpg")
                        Files.copyTo(ctx, uri, f)
                        f.absolutePath
                    }.getOrNull()
                }
            }
            db.musicModeDao().insert(
                MusicMode(name = name.trim(), coverPath = coverPath, createdAt = System.currentTimeMillis())
            )
        }
    }

    fun renameMode(mode: MusicMode, name: String, cover: Uri? = null) {
        if (name.isBlank()) return
        viewModelScope.launch {
            val coverPath = withContext(Dispatchers.IO) {
                cover?.let { uri ->
                    runCatching {
                        val f = File(Files.coverDir(ctx), "cover_${System.currentTimeMillis()}.jpg")
                        Files.copyTo(ctx, uri, f)
                        f.absolutePath
                    }.getOrNull()
                }
            } ?: mode.coverPath
            db.musicModeDao().update(mode.copy(name = name.trim(), coverPath = coverPath))
        }
    }

    fun deleteMode(mode: MusicMode) {
        viewModelScope.launch {
            db.trackDao().deleteForMode(mode.id)
            db.musicModeDao().delete(mode)
            withContext(Dispatchers.IO) { mode.coverPath?.let { File(it).delete() } }
        }
    }

    /** 从系统文件选择器导入音频，复制到应用内部，避免权限失效 */
    fun addLocalTracks(modeId: Long, uris: List<Uri>, onDone: (Int) -> Unit = {}) {
        viewModelScope.launch {
            var added = 0
            withContext(Dispatchers.IO) {
                uris.forEach { uri ->
                    runCatching {
                        val name = queryName(uri) ?: "未命名音频"
                        val ext = name.substringAfterLast('.', "m4a")
                        val target = File(
                            Files.musicDir(ctx),
                            "${System.currentTimeMillis()}_${name.replace(Regex("[\\\\/:*?\"<>|]"), "_")}"
                        )
                        Files.copyTo(ctx, uri, target)
                        val duration = readDuration(uri)
                        db.trackDao().insert(
                            Track(
                                modeId = modeId,
                                title = name.substringBeforeLast(".").take(80),
                                uri = Uri.fromFile(target).toString(),
                                durationMs = duration,
                                addedAt = System.currentTimeMillis()
                            )
                        )
                        added++
                    }
                }
            }
            onDone(added)
        }
    }

    fun importBili(modeId: Long, bv: String) {
        if (_bili.value.loading) return
        viewModelScope.launch {
            _bili.value = BiliProgress(loading = true, text = "正在解析 B 站音频…", visible = true)
            runCatching {
                withContext(Dispatchers.IO) {
                    biliImporter.import(bv, settings.value.biliCookie)
                }
            }.onSuccess { audio ->
                db.trackDao().insert(
                    Track(
                        modeId = modeId,
                        title = audio.title.take(80),
                        uri = Uri.fromFile(audio.file).toString(),
                        durationMs = audio.durationMs,
                        addedAt = System.currentTimeMillis()
                    )
                )
                _bili.value = BiliProgress(text = "已导入：${audio.title}", visible = true)
            }.onFailure { e ->
                _bili.value = BiliProgress(
                    text = e.message ?: "导入失败，请稍后重试",
                    error = true,
                    visible = true
                )
            }
        }
    }

    fun dismissBili() {
        _bili.value = BiliProgress()
    }

    fun deleteTrack(track: Track) {
        viewModelScope.launch {
            db.trackDao().delete(track)
            withContext(Dispatchers.IO) {
                runCatching { File(Uri.parse(track.uri).path ?: "").delete() }
            }
        }
    }

    private fun queryName(uri: Uri): String? {
        return runCatching {
            ctx.contentResolver.query(uri, null, null, null, null)?.use { c ->
                val idx = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (idx >= 0 && c.moveToFirst()) c.getString(idx) else null
            }
        }.getOrNull()
    }

    private fun readDuration(uri: Uri): Long {
        return runCatching {
            val mmr = android.media.MediaMetadataRetriever()
            mmr.setDataSource(ctx, uri)
            val d = mmr.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
            mmr.release()
            d
        }.getOrDefault(0L)
    }

    // ---------------- 导出 ----------------

    suspend fun exportJson(uri: Uri): Int = exporter.exportJson(uri)
    suspend fun exportCsv(uri: Uri): Int = exporter.exportCsv(uri)

    fun clearAll(onDone: () -> Unit = {}) {
        viewModelScope.launch {
            db.analysisItemDao().clear()
            db.adRecordDao().clear()
            db.trackDao().clear()
            db.musicModeDao().clear()
            db.quoteDao().clear()
            db.adAssetDao().clear()
            player.player.clearMediaItems()
            seedQuotes()
            onDone()
        }
    }

    fun totalListenMs(): Long = tracks.value.sumOf { it.listenedMs }

    override fun onCleared() {
        super.onCleared()
        // player 由 Application 持有，这里不释放，保证切页不断播
    }
}
