package com.qionevale.seagullthinking

import android.app.Application
import android.content.Context
import com.qionevale.seagullthinking.data.AppDatabase
import com.qionevale.seagullthinking.data.BiliImporter
import com.qionevale.seagullthinking.data.Exporter
import com.qionevale.seagullthinking.data.PlayerController
import com.qionevale.seagullthinking.data.SettingsRepository

class AppContainer(context: Context) {
    val db: AppDatabase = AppDatabase.get(context)
    val settings = SettingsRepository(context)
    val player = PlayerController(context, db.trackDao())
    val bili = BiliImporter(context)
    val exporter = Exporter(context, db)
}

class SeagullApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
