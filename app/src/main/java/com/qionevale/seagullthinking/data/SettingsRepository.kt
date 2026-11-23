package com.qionevale.seagullthinking.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsStore: DataStore<Preferences> by preferencesDataStore(name = "seagull_settings")

enum class Accent(val id: String, val label: String, val argb: Long) {
    OCEAN("ocean", "海蓝", 0xFF1F6FEB),
    AMBER("amber", "琥珀", 0xFFB4801B),
    MOSS("moss", "苔绿", 0xFF3E7C59),
    PLUM("plum", "酒红", 0xFF9E3B52),
    INK("ink", "藏青", 0xFF2B3A67),
    CLAY("clay", "暮橙", 0xFFC85F35);

    companion object {
        fun of(id: String?): Accent = entries.firstOrNull { it.id == id } ?: OCEAN
    }
}

enum class DarkPref(val id: String, val label: String) {
    LIGHT("light", "浅色"), DARK("dark", "深色"), SYSTEM("system", "跟随系统");

    companion object {
        fun of(id: String?): DarkPref = entries.firstOrNull { it.id == id } ?: SYSTEM
    }
}

enum class FontPref(val id: String, val label: String) {
    SYSTEM("system", "系统"), SERIF("serif", "衬线"), MONO("mono", "等宽");

    companion object {
        fun of(id: String?): FontPref = entries.firstOrNull { it.id == id } ?: SYSTEM
    }
}

data class AppSettings(
    val accent: Accent = Accent.OCEAN,
    val dark: DarkPref = DarkPref.SYSTEM,
    val font: FontPref = FontPref.SYSTEM,
    val fontScale: Float = 1.0f,
    val backgroundPath: String? = null,
    val cardRound: Boolean = true,
    val dailyTarget: Int = 3,
    val labels: List<String> = DEFAULT_LABELS,
    val biliCookie: String = "",
    val exportTreeUri: String? = null,
    val autoNext: Boolean = true
) {
    companion object {
        val DEFAULT_LABELS = listOf(
            "人群", "场景", "需求", "卖点", "产品痛点", "商业模式", "个人心得"
        )
    }
}

class SettingsRepository(private val context: Context) {
    private val gson = Gson()

    private object Keys {
        val ACCENT = stringPreferencesKey("accent")
        val DARK = stringPreferencesKey("dark")
        val FONT = stringPreferencesKey("font")
        val FONT_SCALE = floatPreferencesKey("font_scale")
        val BACKGROUND = stringPreferencesKey("background")
        val CARD_ROUND = booleanPreferencesKey("card_round")
        val DAILY_TARGET = intPreferencesKey("daily_target")
        val LABELS = stringPreferencesKey("labels")
        val BILI_COOKIE = stringPreferencesKey("bili_cookie")
        val EXPORT_TREE = stringPreferencesKey("export_tree")
        val AUTO_NEXT = booleanPreferencesKey("auto_next")
    }

    val flow: Flow<AppSettings> = context.settingsStore.data.map { p ->
        AppSettings(
            accent = Accent.of(p[Keys.ACCENT]),
            dark = DarkPref.of(p[Keys.DARK]),
            font = FontPref.of(p[Keys.FONT]),
            fontScale = p[Keys.FONT_SCALE] ?: 1.0f,
            backgroundPath = p[Keys.BACKGROUND]?.takeIf { it.isNotBlank() },
            cardRound = p[Keys.CARD_ROUND] ?: true,
            dailyTarget = (p[Keys.DAILY_TARGET] ?: 3).coerceIn(1, 50),
            labels = decodeLabels(p[Keys.LABELS]),
            biliCookie = p[Keys.BILI_COOKIE] ?: "",
            exportTreeUri = p[Keys.EXPORT_TREE]?.takeIf { it.isNotBlank() },
            autoNext = p[Keys.AUTO_NEXT] ?: true
        )
    }

    private fun decodeLabels(raw: String?): List<String> {
        if (raw.isNullOrBlank()) return AppSettings.DEFAULT_LABELS
        return runCatching {
            val type = object : TypeToken<List<String>>() {}.type
            val list: List<String> = gson.fromJson(raw, type)
            list.filter { it.isNotBlank() }.ifEmpty { AppSettings.DEFAULT_LABELS }
        }.getOrDefault(AppSettings.DEFAULT_LABELS)
    }

    suspend fun setAccent(a: Accent) = edit { it[Keys.ACCENT] = a.id }
    suspend fun setDark(d: DarkPref) = edit { it[Keys.DARK] = d.id }
    suspend fun setFont(f: FontPref) = edit { it[Keys.FONT] = f.id }
    suspend fun setFontScale(v: Float) = edit { it[Keys.FONT_SCALE] = v }
    suspend fun setBackground(path: String?) = edit {
        if (path == null) it.remove(Keys.BACKGROUND) else it[Keys.BACKGROUND] = path
    }
    suspend fun setCardRound(v: Boolean) = edit { it[Keys.CARD_ROUND] = v }
    suspend fun setDailyTarget(v: Int) = edit { it[Keys.DAILY_TARGET] = v.coerceIn(1, 50) }
    suspend fun setLabels(list: List<String>) = edit {
        it[Keys.LABELS] = gson.toJson(list.ifEmpty { AppSettings.DEFAULT_LABELS })
    }
    suspend fun setBiliCookie(v: String) = edit { it[Keys.BILI_COOKIE] = v }
    suspend fun setExportTree(uri: String?) = edit {
        if (uri == null) it.remove(Keys.EXPORT_TREE) else it[Keys.EXPORT_TREE] = uri
    }
    suspend fun setAutoNext(v: Boolean) = edit { it[Keys.AUTO_NEXT] = v }

    private suspend fun edit(block: (androidx.datastore.preferences.core.MutablePreferences) -> Unit) {
        context.settingsStore.edit { block(it) }
    }
}
