package com.cmc.musicplayer.ui.adapters

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.cmc.musicplayer.data.models.SongModel
import com.cmc.musicplayer.databinding.ItemSongBinding

class SongAdapter(private val onSongClick: (SongModel) -> Unit) : ListAdapter<SongModel, SongAdapter.SongViewHolder>(SongDiffCallback()) {

    inner class SongViewHolder(private val binding: ItemSongBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(song: SongModel) {
            binding.song = song // Set the song for data binding
            binding.executePendingBindings() // Ensure immediate binding
            binding.root.setOnClickListener { onSongClick(song) } // Set click listener if needed
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SongViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ItemSongBinding.inflate(inflater, parent, false)
        return SongViewHolder(binding)
    }

    override fun onBindViewHolder(holder: SongViewHolder, position: Int) {
        val song = getItem(position)
        holder.bind(song) // Use bind method to set up data binding
    }
}

class SongDiffCallback : DiffUtil.ItemCallback<SongModel>() {
    override fun areItemsTheSame(oldItem: SongModel, newItem: SongModel): Boolean {
        return oldItem.id == newItem.id // Assuming you have an id field
    }

    override fun areContentsTheSame(oldItem: SongModel, newItem: SongModel): Boolean {
        return oldItem == newItem
    }
}

