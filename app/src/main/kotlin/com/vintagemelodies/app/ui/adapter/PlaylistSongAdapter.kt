package com.vintagemelodies.app.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.vintagemelodies.app.R
import com.vintagemelodies.app.data.model.Song
import com.vintagemelodies.app.player.SongArtworkHelper

class PlaylistSongAdapter(
    private var songs: List<Song>,
    private val onItemClick: (Song, Int) -> Unit,
    private val onPlayPauseClick: (Song, Int) -> Unit,
    private val onItemLongClick: (Song) -> Unit
) : RecyclerView.Adapter<PlaylistSongAdapter.ViewHolder>() {

    private var playingSongId: String? = null
    private var isPlaying: Boolean = false

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val container: View = view.findViewById(R.id.song_item_container)
        val art: ImageView = view.findViewById(R.id.song_item_art)
        val title: TextView = view.findViewById(R.id.song_item_title)
        val artist: TextView = view.findViewById(R.id.song_item_artist)
        val playPauseBtn: ImageButton = view.findViewById(R.id.btn_song_item_play_pause)
        val more: View = view.findViewById(R.id.song_item_more)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_song, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val song = songs[position]
        holder.title.text = song.title

        // Show details: Contributing Artists • Album • Year
        val details = song.getDetailsInfo()
        holder.artist.text = if (details.isNotBlank()) details else song.artist

        // Highlight currently active / playing song
        val isCurrent = (song.id == playingSongId)
        if (isCurrent) {
            holder.playPauseBtn.visibility = View.VISIBLE
            if (isPlaying) {
                holder.playPauseBtn.setImageResource(R.drawable.ic_pause)
                holder.playPauseBtn.contentDescription = "Pause"
            } else {
                holder.playPauseBtn.setImageResource(R.drawable.ic_play_arrow)
                holder.playPauseBtn.contentDescription = "Play"
            }
            holder.title.setTextColor(ContextCompat.getColor(holder.itemView.context, R.color.amber_accent))
            holder.container.setBackgroundResource(R.drawable.bg_song_item_playing)
        } else {
            holder.playPauseBtn.visibility = View.GONE
            holder.title.setTextColor(ContextCompat.getColor(holder.itemView.context, R.color.text_primary))
            holder.container.setBackgroundResource(R.drawable.bg_song_item_normal)
        }

        // Load true artwork
        SongArtworkHelper.loadSongArt(holder.art, song)

        // Direct Play/Pause button on row
        holder.playPauseBtn.setOnClickListener {
            onPlayPauseClick(song, position)
        }

        // Row click
        holder.itemView.setOnClickListener {
            onItemClick(song, position)
        }

        holder.itemView.setOnLongClickListener {
            onItemLongClick(song)
            true
        }

        holder.more.setOnClickListener {
            onItemLongClick(song)
        }
    }

    override fun getItemCount(): Int = songs.size

    fun getSongs(): List<Song> = songs

    fun setPlayingSong(songId: String?, playing: Boolean) {
        if (playingSongId != songId || isPlaying != playing) {
            playingSongId = songId
            isPlaying = playing
            notifyDataSetChanged()
        }
    }

    fun updateData(newSongs: List<Song>) {
        this.songs = newSongs.sortedBy { it.title.lowercase() }
        notifyDataSetChanged()
    }
}
