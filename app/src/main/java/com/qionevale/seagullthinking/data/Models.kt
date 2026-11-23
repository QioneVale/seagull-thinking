package com.qionevale.seagullthinking.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** 一条广告记录 */
@Entity(tableName = "ad_records")
data class AdRecord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    /** VIDEO=本地视频素材, WEB=网页链接 */
    val source: String,
    /** assets 名 / content uri / url */
    val ref: String,
    /** 看完广告的时刻 */
    val watchedAt: Long,
    /** 分析写完的时刻 */
    val analysisDoneAt: Long? = null,
    val note: String = "",
    val noteAt: Long? = null,
    /** yyyy-MM-dd，用于按天分组 */
    val dayKey: String
)

/** 广告分析里的一个分析点（人群/场景/...） */
@Entity(tableName = "analysis_items", indices = [Index("recordId")])
data class AnalysisItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val recordId: Long,
    val orderIndex: Int,
    val label: String,
    val content: String,
    val updatedAt: Long
)

/**
 * 明志墙上的一句话。
 * 位置用相对比例保存，换机型也不会跑位；字号用 scale 表示相对 18sp 的倍数。
 * fontKey / bold / italic 为后加的可空列，旧数据读出来是 null，按默认样式渲染。
 */
@Entity(tableName = "quotes")
data class Quote(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val text: String,
    val xRatio: Float,
    val yRatio: Float,
    val scale: Float = 1f,
    /** 0 表示跟随主题色 */
    val colorArgb: Int = 0,
    val createdAt: Long,
    /** system / serif / mono / cursive */
    val fontKey: String? = null,
    val bold: Boolean? = null,
    val italic: Boolean? = null
) {
    val fontFamily: String get() = fontKey ?: "serif"
    val isBold: Boolean get() = bold == true
    val isItalic: Boolean get() = italic == true
    val fontSizeSp: Float get() = 18f * scale
}

/**
 * 广告素材库：点海鸥就直接从这里取一条来播。
 * kind=VIDEO 时 ref 是 App 内部的文件路径；kind=WEB 时是网址。
 */
@Entity(tableName = "ad_assets")
data class AdAsset(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val kind: String,
    val ref: String,
    val addedAt: Long,
    val lastPlayedAt: Long = 0,
    val playCount: Int = 0
)

@Entity(tableName = "music_modes")
data class MusicMode(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val coverPath: String? = null,
    val createdAt: Long
)

@Entity(tableName = "tracks", indices = [Index("modeId")])
data class Track(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val modeId: Long,
    val title: String,
    /** file:// / content:// */
    val uri: String,
    val durationMs: Long = 0,
    /** 完整听完的次数 */
    val playCount: Int = 0,
    /** 累计收听毫秒 */
    val listenedMs: Long = 0,
    val addedAt: Long
)

/** 记录页展开的一条广告 + 它的分析项 */
data class RecordWithAnalysis(
    val record: AdRecord,
    val items: List<AnalysisItem>
)
