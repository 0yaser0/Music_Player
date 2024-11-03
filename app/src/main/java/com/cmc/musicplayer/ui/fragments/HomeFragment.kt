package com.cmc.musicplayer.ui.fragments

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.cmc.musicplayer.R
import com.cmc.musicplayer.data.models.SongModel
import com.cmc.musicplayer.data.viewModels.HomeViewModel
import com.cmc.musicplayer.databinding.HomeFragmentBinding
import com.cmc.musicplayer.ui.adapters.SongAdapter

class HomeFragment : Fragment() {

    private var _binding: HomeFragmentBinding? = null
    private val binding get() = _binding!!
    private lateinit var homeViewModel: HomeViewModel
    private lateinit var songAdapter: SongAdapter
    private val SELECT_SONG_REQUEST = 1

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = HomeFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        homeViewModel = ViewModelProvider(this)[HomeViewModel::class.java]

        songAdapter = SongAdapter { song -> onSongSelected(song) }
        binding.recyclerViewSongs.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerViewSongs.adapter = songAdapter

        // Observe the songs LiveData
        homeViewModel.songs.observe(viewLifecycleOwner) { songs ->
            Log.v("HomeFragment", "Songs updated: ${songs.size}")
            songAdapter.submitList(songs) // Update the adapter with the new list
        }

        homeViewModel.toastMessage.observe(viewLifecycleOwner) { message ->
            message?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
                homeViewModel.clearToastMessage() // Clear the message after showing the toast
            }
        }

        binding.btnSelectFromLibrary.setOnClickListener {
            openMusicLibrary() // Open the music library to select songs
        }
    }

    private fun openMusicLibrary() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Audio.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(intent, SELECT_SONG_REQUEST)
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == SELECT_SONG_REQUEST && resultCode == AppCompatActivity.RESULT_OK) {
            data?.data?.let { uri ->
                // Retrieve song information from the URI
                val song = getSongFromUri(uri)
                // Add the song to the list in the ViewModel
                homeViewModel.addSong(song) // This will check for duplicates
            }
        }
    }

    private fun getSongFromUri(uri: Uri): SongModel {
        val contentResolver = requireContext().contentResolver
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.DURATION
        )

        // Get song details from the URI
        val cursor = contentResolver.query(uri, projection, null, null, null)
        var song: SongModel? = null

        cursor?.use {
            if (it.moveToFirst()) {
                val id = it.getLong(it.getColumnIndexOrThrow(MediaStore.Audio.Media._ID))
                val title = it.getString(it.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE))
                val artist = it.getString(it.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST))
                val duration = it.getLong(it.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION))

                song = SongModel(id, title, artist, duration, uri)
            }
        }

        return song ?: SongModel(0, "Unknown", "Unknown", 0, uri) // Return a default song if no details are found
    }

    private fun onSongSelected(song: SongModel) {
        // Check if the song exists in the list
        if (homeViewModel.songs.value?.any { it.id == song.id } == true) {
            // Navigate to PlayerFragment with the selected song
            val playerFragment = PlayerFragment().apply {
                // Pass the selected song to PlayerFragment
                arguments = Bundle().apply {
                    putParcelable("selectedSong", song) // Assuming SongModel implements Parcelable
                }
            }
            // Replace the current fragment with PlayerFragment
            requireActivity().supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, playerFragment) // Replace with your actual container ID
                .addToBackStack(null)
                .commit()
        } else {
            Toast.makeText(requireContext(), "${song.title} is not exists in the list", Toast.LENGTH_SHORT).show()
        }
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

