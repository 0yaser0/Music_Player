package com.cmc.musicplayer.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.cmc.musicplayer.R
import com.cmc.musicplayer.ui.views.MainActivity

class MusicService : Service() {

    private var mediaPlayer: MediaPlayer? = null
    private var isMusicPlaying: Boolean = false // État de la musique

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
        // Démarre le service au premier plan avec la notification
        startForeground(1, createNotification())

        when (intent?.action) {
            ACTION_PAUSE -> pauseMusic()
            ACTION_PLAY -> {
                val songUriString = intent.getStringExtra("SONG_URI")
                songUriString?.let {
                    playMusic(Uri.parse(it)) // Convertir la chaîne URI en Uri et passer à playMusic
                }
            }
            ACTION_FORWARD_10_SECONDS -> forward10Seconds()
            ACTION_REWIND_10_SECONDS -> rewind10Seconds()
        }

        return START_STICKY
    }


    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    private fun createNotification(): Notification {
        val notificationIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val contentPendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        // Créez les intents pour les actions de la notification
        val pauseIntent = Intent(this, MusicService::class.java).apply { action = ACTION_PAUSE }
        val playIntent = Intent(this, MusicService::class.java).apply { action = ACTION_PLAY }
        val forwardIntent = Intent(this, MusicService::class.java).apply { action = ACTION_FORWARD_10_SECONDS }
        val rewindIntent = Intent(this, MusicService::class.java).apply { action = ACTION_REWIND_10_SECONDS }

        val pausePendingIntent = PendingIntent.getService(this, 0, pauseIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val playPendingIntent = PendingIntent.getService(this, 1, playIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val forwardPendingIntent = PendingIntent.getService(this, 2, forwardIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val rewindPendingIntent = PendingIntent.getService(this, 3, rewindIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(if (isMusicPlaying) "Playing Music" else "Music Paused")
            .setContentText("Your song is ${if (isMusicPlaying) "playing" else "paused"}")
            .setSmallIcon(R.drawable.ic_music_note)
            .setContentIntent(contentPendingIntent) // Ajouter le PendingIntent pour l'activité
            .addAction(R.drawable.baseline_replay_10_24, "Rewind 10s", rewindPendingIntent)
            .addAction(if (isMusicPlaying) R.drawable.pause else R.drawable.play_arrow,
                if (isMusicPlaying) "Pause" else "Play",
                if (isMusicPlaying) pausePendingIntent else playPendingIntent)
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

    private fun playMusic(songUri: Uri) {
        if (mediaPlayer == null) {
            mediaPlayer = MediaPlayer().apply {
                setAudioStreamType(AudioManager.STREAM_MUSIC)
                setDataSource(applicationContext, songUri)
                prepare()
                start()
            }
        } else if (!isMusicPlaying) {
            mediaPlayer?.start()
        }
        isMusicPlaying = true
        updateBroadcast() // Notifier l'état à l'application
        startForeground(1, createNotification())
    }

    private fun pauseMusic() {
        mediaPlayer?.let {
            if (it.isPlaying) {
                it.pause()
                isMusicPlaying = false
                updateBroadcast() // Notifier l'état à l'application
                startForeground(1, createNotification())
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
        intent.putExtra("isPlaying", isMusicPlaying) // Utilisez l'état mis à jour
        sendBroadcast(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer?.release()
        mediaPlayer = null
        isMusicPlaying = false
        updateBroadcast() // Assurez-vous de notifier que la musique est arrêtée
    }
}
