package com.cmc.musicplayer.data.viewModels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.cmc.musicplayer.data.models.SongModel

class HomeViewModel : ViewModel() {

    private val _songs = MutableLiveData<List<SongModel>>()
    val songs: LiveData<List<SongModel>> get() = _songs

    private val _toastMessage = MutableLiveData<String?>()
    val toastMessage: LiveData<String?> get() = _toastMessage

    init {
        // Initial empty list
        _songs.value = emptyList()
    }

    fun addSong(newSong: SongModel) {
        // Check if the song is already in the list
        if (!songs.value!!.any { it.id == newSong.id }) {
            // If it's not, add it to the list
            val updatedList = songs.value?.toMutableList() ?: mutableListOf()
            updatedList.add(newSong)
            _songs.value = updatedList // Update the LiveData
        } else {
            // Set the toast message if the song already exists
            _toastMessage.value = "Song already exists: ${newSong.title}"
        }
    }

    fun clearToastMessage() {
        _toastMessage.value = null
    }
}

