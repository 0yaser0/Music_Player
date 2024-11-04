package com.cmc.musicplayer.data.viewModels

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.cmc.musicplayer.data.models.SongModel
import com.cmc.musicplayer.services.MusicService
import java.io.IOException

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
class PlayerViewModel(application: Application) : AndroidViewModel(application) {

    private val _currentSong: MutableLiveData<SongModel?> = MutableLiveData()
    val currentSong: LiveData<SongModel?> get() = _currentSong

    private val _isPlaying: MutableLiveData<Boolean> = MutableLiveData(false)
    val isPlaying: LiveData<Boolean> get() = _isPlaying

    private var mediaPlayer: MediaPlayer? = null
    private val audioManager: AudioManager =
        application.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    private val musicStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val isPlaying = intent?.getBooleanExtra("isPlaying", false) ?: false
            _isPlaying.value = isPlaying
        }
    }

    init {
        val filter = IntentFilter("MUSIC_PLAYER_STATE")
        getApplication<Application>().registerReceiver(musicStateReceiver, filter,
            Context.RECEIVER_NOT_EXPORTED)
    }

    fun playSong(context: Context, song: SongModel) {
        // Si le `MediaPlayer` est déjà en lecture pour cette chanson, ne pas le recréer
        if (mediaPlayer != null && _currentSong.value == song) {
            if (!mediaPlayer!!.isPlaying) {
                mediaPlayer?.start()
                _isPlaying.value = true
            }
            return
        }

        // Si on change de chanson ou si `mediaPlayer` est null, libérer l'ancien et recréer un nouveau
        stopSong()

        _currentSong.value = song
        _isPlaying.value = true

        Log.v("PlayerViewModelSong", "Playing song from URI: ${song.uri}")

        requestAudioFocus()

        try {
            mediaPlayer = MediaPlayer().apply {
                setDataSource(context, song.uri)
                prepare()
                start()
            }
        } catch (e: IOException) {
            Log.e("PlayerViewModelError", "Error setting data source", e)
        }

        // Démarrer le service de musique avec l'URI de la chanson
        val serviceIntent = Intent(context, MusicService::class.java).apply {
            action = MusicService.ACTION_PLAY // Passer l'action pour jouer la musique
            putExtra("SONG_URI", song.uri.toString()) // Passer l'URI de la chanson
        }
        ContextCompat.startForegroundService(context, serviceIntent)
    }

    fun pauseSong() {
        mediaPlayer?.let {
            if (it.isPlaying) {
                it.pause()
                _isPlaying.value = false
            }
        }
    }

    private fun stopSong() {
        mediaPlayer?.apply {
            if (isPlaying) stop()
            release()
        }
        mediaPlayer = null
        _isPlaying.value = false
        _currentSong.value = null

        val serviceIntent = Intent(getApplication(), MusicService::class.java)
        getApplication<Application>().stopService(serviceIntent)
    }

    fun rewind10Seconds() {
        mediaPlayer?.let {
            it.seekTo(it.currentPosition - 10000) // Rewind 10 seconds
        }
    }

    fun forward10Seconds() {
        mediaPlayer?.let {
            val newPosition = it.currentPosition + 10000
            if (newPosition < it.duration) {
                it.seekTo(newPosition) // Advance 10 seconds forward
            } else {
                it.seekTo(it.duration) // Go to the end if the new position exceeds duration
            }
        }
    }

    fun skipSong(context: Context) {
        // Logic to skip to the next song (implement this based on your playlist)
        _currentSong.value?.let { current ->
            val nextSong = getNextSong(current) // Implement this method
            nextSong?.let { playSong(context, it) } // Passer le contexte ici
        }
    }

    fun previousSong(context: Context) {
        _currentSong.value?.let { current ->
            val previousSong = getPreviousSong(current) // Implement this method to get the previous song
            previousSong?.let { playSong(context, it) } // Play the previous song
        }
    }

    // Example implementation of getPreviousSong (adjust as needed)
    private fun getPreviousSong(current: SongModel): SongModel? {
        // Logic to get the previous song in the playlist
        return null // Replace with actual logic
    }

    private fun getNextSong(current: SongModel): SongModel? {
        // Logic to get the next song in the playlist
        return null // Replace with actual logic
    }

    private fun requestAudioFocus() {
        val focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
            .setOnAudioFocusChangeListener { focusChange ->
                when (focusChange) {
                    AudioManager.AUDIOFOCUS_GAIN -> {
                        mediaPlayer?.let {
                            if (!it.isPlaying) {
                                it.start()
                                _isPlaying.value = true
                            }
                        }
                    }
                    AudioManager.AUDIOFOCUS_LOSS -> stopSong()
                    AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> pauseSong()
                    AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK ->
                        mediaPlayer?.setVolume(0.1f, 0.1f)
                }
            }
            .build()

        audioManager.requestAudioFocus(focusRequest)
    }

    override fun onCleared() {
        super.onCleared()
        stopSong() // Libérer les ressources lors de la destruction de la ViewModel
        getApplication<Application>().unregisterReceiver(musicStateReceiver) // Ne pas oublier de désenregistrer le receiver
    }
}