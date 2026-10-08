package com.vintagemelodies.app.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.vintagemelodies.app.R
import com.vintagemelodies.app.data.model.Song

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
        holder.artist.text = song.artist

        val artRes = if (song.artworkResId != 0) song.artworkResId else R.drawable.ic_vintage_player_art_2
        Glide.with(holder.art.context)
            .load(artRes)
            .centerCrop()
            .into(holder.art)

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
        this.songs = newSongs
        notifyDataSetChanged()
    }
}
