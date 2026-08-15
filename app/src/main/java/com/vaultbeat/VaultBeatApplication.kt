package com.vaultbeat

import android.app.Application
import android.util.Log
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.ffmpeg.FFmpeg
import com.yausername.aria2c.Aria2c
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class VaultBeatApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
        try {
            YoutubeDL.getInstance().init(this)
            FFmpeg.getInstance().init(this)
            Aria2c.getInstance().init(this)
            Log.d("VaultBeat", "YoutubeDL, FFmpeg and Aria2c initialized successfully")
        } catch (e: Exception) {
            Log.e("VaultBeat", "Failed to initialize components", e)
        }
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Downloads"
            val descriptionText = "Notifications for song downloads"
            val importance = NotificationManager.IMPORTANCE_LOW
            val channel = NotificationChannel("download_channel", name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager =
                getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }
}

