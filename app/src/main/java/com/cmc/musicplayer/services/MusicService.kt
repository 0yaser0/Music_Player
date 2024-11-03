package com.cmc.musicplayer.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.cmc.musicplayer.R

class MusicService : Service() {

    private var mediaPlayer: MediaPlayer? = null

    companion object {
        const val ACTION_PAUSE = "action_pause"
        const val ACTION_PLAY = "action_play"
        const val ACTION_FORWARD_10_SECONDS = "action_forward_10_seconds"
        const val ACTION_REWIND_10_SECONDS = "action_rewind_10_seconds"
        const val CHANNEL_ID = "music_player_channel"
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(1, createNotification()) // Start foreground service with the notification

        when (intent?.action) {
            ACTION_PAUSE -> pauseMusic()
            ACTION_PLAY -> playMusic()
            ACTION_FORWARD_10_SECONDS -> forward10Seconds()
            ACTION_REWIND_10_SECONDS -> rewind10Seconds()
        }

        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    private fun createNotification(): Notification {
        val pauseIntent = Intent(this, MusicService::class.java).apply { action = ACTION_PAUSE }
        val playIntent = Intent(this, MusicService::class.java).apply { action = ACTION_PLAY }
        val forwardIntent = Intent(this, MusicService::class.java).apply { action = ACTION_FORWARD_10_SECONDS }
        val rewindIntent = Intent(this, MusicService::class.java).apply { action = ACTION_REWIND_10_SECONDS }

        val pausePendingIntent = PendingIntent.getService(this, 0, pauseIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val playPendingIntent = PendingIntent.getService(this, 1, playIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val forwardPendingIntent = PendingIntent.getService(this, 2, forwardIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val rewindPendingIntent = PendingIntent.getService(this, 3, rewindIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(if (mediaPlayer?.isPlaying == true) "Playing Music" else "Music Paused")
            .setContentText("Your song is ${if (mediaPlayer?.isPlaying == true) "playing" else "paused"}")
            .setSmallIcon(R.drawable.ic_music_note)
            .addAction(R.drawable.baseline_replay_10_24, "Rewind 10s", rewindPendingIntent)
            .addAction(if (mediaPlayer?.isPlaying == true) R.drawable.pause else R.drawable.play_arrow,
                if (mediaPlayer?.isPlaying == true) "Pause" else "Play",
                if (mediaPlayer?.isPlaying == true) pausePendingIntent else playPendingIntent)
            .addAction(R.drawable.forward_10, "Forward 10s", forwardPendingIntent)
            .setStyle(
                androidx.media.app.NotificationCompat.MediaStyle()
                    .setShowActionsInCompactView(0, 1, 2)
            )
            .setOngoing(true)
            .build()
    }

    private fun createNotificationChannel() {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(CHANNEL_ID, "Music Player", NotificationManager.IMPORTANCE_LOW)
        notificationManager.createNotificationChannel(channel)
    }

    private fun playMusic() {
        if (mediaPlayer == null) {
            mediaPlayer = MediaPlayer().apply {
                setAudioStreamType(AudioManager.STREAM_MUSIC)
                // Set data source, prepare, and start playback (add song URI or file here)
                // setDataSource(applicationContext, songUri) // Uncomment and set song URI
                prepare()
                start()
            }
        } else {
            mediaPlayer?.start()
        }
        updateBroadcast() // Notify listeners of state change
        startForeground(1, createNotification()) // Update notification
    }

    private fun pauseMusic() {
        mediaPlayer?.let {
            if (it.isPlaying) {
                it.pause()
                updateBroadcast() // Notify listeners of state change
                startForeground(1, createNotification()) // Update notification
            }
        }
    }

    private fun forward10Seconds() {
        mediaPlayer?.let {
            val newPosition = it.currentPosition + 10000
            it.seekTo(newPosition.coerceAtMost(it.duration))
        }
    }

    private fun rewind10Seconds() {
        mediaPlayer?.let {
            val newPosition = it.currentPosition - 10000
            it.seekTo(newPosition.coerceAtLeast(0))
        }
    }

    private fun updateBroadcast() {
        val intent = Intent("MUSIC_PLAYER_STATE")
        intent.putExtra("isPlaying", mediaPlayer?.isPlaying == true)
        sendBroadcast(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer?.release()
        mediaPlayer = null
    }
}
