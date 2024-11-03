package com.cmc.musicplayer.ui.fragments

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.cmc.musicplayer.data.models.SongModel
import com.cmc.musicplayer.data.viewModels.PlayerViewModel
import com.cmc.musicplayer.databinding.FragmentPlayerBinding

class PlayerFragment : Fragment() {

    private var _binding: FragmentPlayerBinding? = null
    private val binding get() = _binding!!
    private lateinit var playerViewModel: PlayerViewModel

    private val REQUEST_CODE_READ_EXTERNAL_STORAGE = 100

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPlayerBinding.inflate(inflater, container, false)
        binding.lifecycleOwner = viewLifecycleOwner // Set lifecycle owner
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        requestStoragePermission() // Request storage permission if not granted

        // Initialize ViewModel
        playerViewModel = ViewModelProvider(this)[PlayerViewModel::class.java]
        binding.viewModel = playerViewModel

        arguments?.getParcelable<SongModel>("selectedSong")?.let { song ->
            if (isUriAccessible(song.uri)) {
                playerViewModel.playSong(requireContext(), song) // Start playing the song
            } else {
                Log.e("PlayerFragmentError", "Invalid URI: ${song.uri}")
                // Handle invalid URI error
            }
        }

        // Observe changes to the current song
        playerViewModel.currentSong.observe(viewLifecycleOwner) { song ->
            song?.let {
                binding.songTitle.text = it.title
                binding.songArtist.text = it.artist
                binding.songDuration.text = formatDuration(it.duration)
            }
        }

        playerViewModel.isPlaying.observe(viewLifecycleOwner) { isPlaying ->
            binding.playPauseButton.text = if (isPlaying) "Pause" else "Play"

            binding.playPauseButton.setOnClickListener {
                if (isPlaying) {
                    playerViewModel.pauseSong()
                } else {
                    playerViewModel.currentSong.value?.let { song ->
                        playerViewModel.playSong(requireContext(), song) // Assurez-vous que la chanson est bien définie
                    }
                }
            }
        }

        binding.nextButton.setOnClickListener {
            playerViewModel.skipSong(requireContext()) // Passer le contexte ici
        }

        // Handle previous button click
        binding.previousButton.setOnClickListener {
            playerViewModel.rewindSong() // Rewind to the previous song or handle accordingly
        }
    }

    private fun requestStoragePermission() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(requireActivity(),
                arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                REQUEST_CODE_READ_EXTERNAL_STORAGE)
        } else {
            Log.v("Permission", "already done")
        }
    }

    private fun isUriAccessible(uri: Uri): Boolean {
        return try {
            val inputStream = requireContext().contentResolver.openInputStream(uri)
            inputStream?.close()
            true
        } catch (e: Exception) {
            Log.e("PlayerFragment", "Error accessing URI: $uri", e)
            false
        }
    }

    private fun formatDuration(duration: Long): String {
        val minutes = (duration / 1000) / 60
        val seconds = (duration / 1000) % 60
        return String.format("%02d:%02d", minutes, seconds)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null // Clean up binding resources
    }
}
