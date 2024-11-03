package com.cmc.musicplayer.data.viewModels

import android.app.Application
import android.content.Context
import android.content.Intent
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.cmc.musicplayer.data.models.SongModel
import com.cmc.musicplayer.services.MusicService
import java.io.IOException

class PlayerViewModel(application: Application) : AndroidViewModel(application) {

    private val _currentSong: MutableLiveData<SongModel?> = MutableLiveData()
    val currentSong: LiveData<SongModel?> get() = _currentSong

    private val _isPlaying: MutableLiveData<Boolean> = MutableLiveData(false)
    val isPlaying: LiveData<Boolean> get() = _isPlaying

    private var mediaPlayer: MediaPlayer? = null
    private val audioManager: AudioManager =
        application.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    fun playSong(context: Context, song: SongModel) {
        // Si le `MediaPlayer` est déjà en lecture pour cette chanson, ne pas le recréer
        if (mediaPlayer != null && _currentSong.value == song) {
            if (mediaPlayer?.isPlaying == false) {
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

        val serviceIntent = Intent(context, MusicService::class.java)
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
                        mediaPlayer?.let { if (!it.isPlaying) it.start() } // Resume playback if needed
                    }

                    AudioManager.AUDIOFOCUS_LOSS -> {
                        stopSong() // Stop playback
                    }

                    AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                        pauseSong() // Pause playback temporarily
                    }

                    AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                        mediaPlayer?.setVolume(0.5f, 0.5f) // Lower the volume temporarily
                    }
                }
            }
            .build()

        val result = audioManager.requestAudioFocus(focusRequest)
        if (result != AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            Log.e("AudioFocus", "Failed to gain audio focus")
        }
    }

    override fun onCleared() {
        super.onCleared()
        mediaPlayer?.release() // Release resources
        mediaPlayer = null
    }
}