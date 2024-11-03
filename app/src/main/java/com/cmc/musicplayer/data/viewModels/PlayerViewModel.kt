package com.cmc.musicplayer.data.viewModels

import android.app.Application
import android.content.Context
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.cmc.musicplayer.data.models.SongModel
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

        _currentSong.value = song
        _isPlaying.value = true

        Log.v("PlayerViewModelSong", "Playing song from URI: ${song.uri}")

        requestAudioFocus() // Demander le focus audio

        try {
            mediaPlayer = MediaPlayer().apply {
                setDataSource(context, song.uri) // Utiliser le contexte passé
                prepare()
                start()
            }
        } catch (e: IOException) {
            Log.e("PlayerViewModelError", "Error setting data source", e)
            // Gérer l'erreur
        }
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
        _isPlaying.value = false
        _currentSong.value = null
        mediaPlayer?.stop()
        mediaPlayer?.release() // Release resources
        mediaPlayer = null
    }

    fun skipSong(context: Context) {
        // Logic to skip to the next song (implement this based on your playlist)
        _currentSong.value?.let { current ->
            val nextSong = getNextSong(current) // Implement this method
            nextSong?.let { playSong(context, it) } // Passer le contexte ici
        }
    }


    fun rewindSong() {
        mediaPlayer?.let {
            it.seekTo(it.currentPosition - 10000) // Rewind 10 seconds
        }
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
