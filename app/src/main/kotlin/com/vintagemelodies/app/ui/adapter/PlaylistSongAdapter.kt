package com.vintagemelodies.app.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.vintagemelodies.app.R
import com.vintagemelodies.app.data.model.Song
import com.vintagemelodies.app.player.SongArtworkHelper

class PlaylistSongAdapter(
    private var songs: List<Song>,
    private val onItemClick: (Song, Int) -> Unit,
    private val onItemLongClick: (Song) -> Unit
) : RecyclerView.Adapter<PlaylistSongAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val art: ImageView = view.findViewById(R.id.song_item_art)
        val title: TextView = view.findViewById(R.id.song_item_title)
        val artist: TextView = view.findViewById(R.id.song_item_artist)
        val more: View = view.findViewById(R.id.song_item_more)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_song, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val song = songs[position]
        holder.title.text = song.title
        
        // Show artist with release year or album if available
        val subtitle = when {
            song.year.isNotBlank() && song.artist.isNotBlank() -> "${song.artist} • ${song.year}"
            song.album.isNotBlank() && !song.album.startsWith("Folder:") && !song.album.startsWith("Cloudflare R2:") -> "${song.artist} • ${song.album}"
            else -> song.artist
        }
        holder.artist.text = subtitle

        // Load true artwork
        SongArtworkHelper.loadSongArt(holder.art, song)

        holder.itemView.setOnClickListener { onItemClick(song, position) }
        holder.itemView.setOnLongClickListener {
            onItemLongClick(song)
            true
        }
        holder.more.setOnClickListener {
            onItemLongClick(song)
        }
    }

    override fun getItemCount(): Int = songs.size

    fun updateData(newSongs: List<Song>) {
        this.songs = newSongs.sortedBy { it.title.lowercase() }
        notifyDataSetChanged()
    }
}
