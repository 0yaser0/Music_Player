package com.cmc.musicplayer

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager


class Application: Application() {

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            "MUSIC_CHANNEL",
            "Music Playback",
            NotificationManager.IMPORTANCE_LOW
        )
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }
}