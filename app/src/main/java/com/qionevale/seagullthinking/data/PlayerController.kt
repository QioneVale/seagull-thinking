package com.qionevale.seagullthinking.data

import android.content.Context
import android.net.Uri
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

enum class RepeatUi(val label: String) { ALL("列表循环"), ONE("单曲循环"), OFF("顺序播放") }

/**
 * 全局唯一的音乐播放器：跨页面播放、统计完整听完次数与累计收听时长。
 */
@OptIn(UnstableApi::class)
class PlayerController(
    private val context: Context,
    private val trackDao: TrackDao
) {
    val player: ExoPlayer = ExoPlayer.Builder(context).build().apply {
        repeatMode = Player.REPEAT_MODE_ALL
        playWhenReady = false
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val _queue = MutableStateFlow<List<Track>>(emptyList())
    val queue: StateFlow<List<Track>> = _queue.asStateFlow()

    private val _current = MutableStateFlow<Track?>(null)
    val current: StateFlow<Track?> = _current.asStateFlow()

    private val _modeName = MutableStateFlow("")
    val modeName: StateFlow<String> = _modeName.asStateFlow()

    private val _playing = MutableStateFlow(false)
    val playing: StateFlow<Boolean> = _playing.asStateFlow()

    private val _position = MutableStateFlow(0L)
    val position: StateFlow<Long> = _position.asStateFlow()

    private val _duration = MutableStateFlow(0L)
    val duration: StateFlow<Long> = _duration.asStateFlow()

    private val _shuffle = MutableStateFlow(false)
    val shuffle: StateFlow<Boolean> = _shuffle.asStateFlow()

    private val _repeat = MutableStateFlow(RepeatUi.ALL)
    val repeat: StateFlow<RepeatUi> = _repeat.asStateFlow()

    /** 定时停止的结束时间戳；null 表示未开启 */
    private val _sleepAt = MutableStateFlow<Long?>(null)
    val sleepAt: StateFlow<Long?> = _sleepAt.asStateFlow()

    private var tickJob: Job? = null
    private var lastTick = 0L
    private var pendingListened = 0L
    private var countedTrackId: Long? = null
    private var stopAfterCurrent = false

    private val listener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _playing.value = isPlaying
            if (!isPlaying) flushListened()
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            flushListened()
            countedTrackId = null
            // 定时「播完这首就停」：先判断再重置
            if (pendingStopAfterCurrent && reason == Player.MEDIA_ITEM_TRANSITION_REASON_AUTO) {
                pendingStopAfterCurrent = false
                player.pause()
            }
            val id = mediaItem?.mediaId?.toLongOrNull()
            _current.value = _queue.value.firstOrNull { it.id == id }
            _position.value = 0
            _duration.value = _current.value?.durationMs ?: 0
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            if (playbackState == Player.STATE_READY) {
                val d = player.duration
                if (d > 0) _duration.value = d
            }
            if (playbackState == Player.STATE_ENDED) {
                _current.value?.let { t ->
                    if (countedTrackId != t.id) {
                        countedTrackId = t.id
                        scope.launch { runCatching { trackDao.bumpPlayCount(t.id) } }
                    }
                }
            }
        }

        override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
            _shuffle.value = shuffleModeEnabled
        }

        override fun onRepeatModeChanged(repeatMode: Int) {
            _repeat.value = when (repeatMode) {
                Player.REPEAT_MODE_ONE -> RepeatUi.ONE
                Player.REPEAT_MODE_ALL -> RepeatUi.ALL
                else -> RepeatUi.OFF
            }
        }
    }

    private var pendingStopAfterCurrent = false

    init {
        player.addListener(listener)
        startTicking()
    }

    private fun startTicking() {
        tickJob?.cancel()
        tickJob = scope.launch {
            while (isActive) {
                delay(500)
                val p = player
                val now = System.currentTimeMillis()
                if (p.isPlaying) {
                    val delta = if (lastTick == 0L) 500L else (now - lastTick).coerceIn(0, 2000)
                    pendingListened += delta
                    _position.value = p.currentPosition
                    val d = p.duration
                    if (d > 0) _duration.value = d
                    if (pendingListened >= 5000) flushListened()
                    // 近似听完：拖到最后 1.5 秒也算完整听完
                    val cur = _current.value
                    if (cur != null && d > 0 && p.currentPosition >= d - 1500 && countedTrackId != cur.id) {
                        countedTrackId = cur.id
                        scope.launch { runCatching { trackDao.bumpPlayCount(cur.id) } }
                    }
                }
                lastTick = now
                _sleepAt.value?.let { at ->
                    if (now >= at) {
                        p.pause()
                        _sleepAt.value = null
                    }
                }
            }
        }
    }

    private fun flushListened() {
        val id = _current.value?.id ?: return
        val delta = pendingListened
        if (delta <= 0) return
        pendingListened = 0
        scope.launch { runCatching { trackDao.addListened(id, delta) } }
    }

    fun playMode(tracks: List<Track>, startIndex: Int, modeName: String) {
        if (tracks.isEmpty()) return
        flushListened()
        _queue.value = tracks
        _modeName.value = modeName
        _shuffle.value = player.shuffleModeEnabled
        val items = tracks.map { t ->
            MediaItem.Builder()
                .setUri(Uri.parse(t.uri))
                .setMediaId(t.id.toString())
                .setMediaMetadata(
                    androidx.media3.common.MediaMetadata.Builder()
                        .setTitle(t.title)
                        .build()
                )
                .build()
        }
        player.setMediaItems(items, startIndex.coerceIn(0, tracks.size - 1), 0)
        player.prepare()
        player.play()
        _current.value = tracks.getOrNull(startIndex)
        _duration.value = tracks.getOrNull(startIndex)?.durationMs ?: 0
    }

    fun playSingle(track: Track, modeName: String) {
        val list = _queue.value
        val idx = list.indexOfFirst { it.id == track.id }
        if (idx >= 0) {
            player.seekTo(idx, 0)
            player.play()
        } else {
            playMode(listOf(track), 0, modeName)
        }
    }

    fun toggle() {
        if (player.isPlaying) player.pause() else {
            if (player.playbackState == Player.STATE_IDLE) player.prepare()
            player.play()
        }
    }

    fun next() = player.seekToNextMediaItem()
    fun previous() = player.seekToPreviousMediaItem()

    fun seekTo(ms: Long) {
        player.seekTo(ms.coerceAtLeast(0))
        _position.value = ms.coerceAtLeast(0)
    }

    fun toggleShuffle() {
        player.shuffleModeEnabled = !player.shuffleModeEnabled
        _shuffle.value = player.shuffleModeEnabled
    }

    fun cycleRepeat() {
        player.repeatMode = when (player.repeatMode) {
            Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
            Player.REPEAT_MODE_ONE -> Player.REPEAT_MODE_OFF
            else -> Player.REPEAT_MODE_ALL
        }
    }

    fun setRepeat(mode: RepeatUi) {
        player.repeatMode = when (mode) {
            RepeatUi.ALL -> Player.REPEAT_MODE_ALL
            RepeatUi.ONE -> Player.REPEAT_MODE_ONE
            RepeatUi.OFF -> Player.REPEAT_MODE_OFF
        }
        _repeat.value = mode
    }

    fun startSleepTimer(minutes: Int) {
        _sleepAt.value = System.currentTimeMillis() + minutes * 60_000L
    }

    fun stopAfterCurrent() {
        pendingStopAfterCurrent = true
    }

    fun cancelSleepTimer() {
        _sleepAt.value = null
        pendingStopAfterCurrent = false
    }

    fun release() {
        tickJob?.cancel()
        player.removeListener(listener)
        player.release()
    }
}
