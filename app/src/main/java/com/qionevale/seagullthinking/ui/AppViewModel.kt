package com.qionevale.seagullthinking.ui

import android.app.Application
import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.qionevale.seagullthinking.SeagullApp
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
            if (db.quoteDao().count() == 0) seedQuotes()
        }
    }

    private suspend fun seedQuotes() {
        val now = System.currentTimeMillis()
        val seeds = listOf(
            Triple("先把一件事做透，再谈第二件。", 0.22f, 0.18f),
            Triple("看广告不是看热闹，是看别人怎么把钱挣走。", 0.5f, 0.38f),
            Triple("每天三条，半年就是五百四十次商业复盘。", 0.3f, 0.62f),
            Triple("今天不想动，就先坐着听完一首歌。", 0.62f, 0.78f)
        )
        seeds.forEachIndexed { i, (text, x, y) ->
            db.quoteDao().insert(
                Quote(text = text, xRatio = x, yRatio = y, createdAt = now + i)
            )
        }
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

    fun addQuote(text: String, x: Float, y: Float) {
        if (text.isBlank()) return
        viewModelScope.launch {
            db.quoteDao().insert(
                Quote(text = text.trim(), xRatio = x, yRatio = y, createdAt = System.currentTimeMillis())
            )
        }
    }

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
