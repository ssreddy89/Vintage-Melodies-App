package com.vintagemelodies.app.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.vintagemelodies.app.R
import com.vintagemelodies.app.data.model.Playlist

class DrawerPlaylistAdapter(
    private var playlists: List<Playlist>,
    private val onItemClick: (Playlist) -> Unit,
    private val onItemLongClick: (Playlist) -> Unit
) : RecyclerView.Adapter<DrawerPlaylistAdapter.ViewHolder>() {

    private var playingPlaylistId: Long? = null
    private var isPlaying: Boolean = false

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val root: View = view.findViewById(R.id.drawer_item_container)
        val icon: ImageView = view.findViewById(R.id.drawer_item_icon)
        val title: TextView = view.findViewById(R.id.drawer_item_title)
        val subtitle: TextView = view.findViewById(R.id.drawer_item_subtitle)
        val langBadge: TextView = view.findViewById(R.id.drawer_lang_badge)
        val playingIcon: ImageView = view.findViewById(R.id.drawer_item_playing_icon)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_drawer_playlist, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val playlist = playlists[position]
        holder.title.text = playlist.name
        holder.subtitle.text = "${playlist.language} • ${playlist.songCount} songs"
        holder.langBadge.text = playlist.language.uppercase()

        val isCurrent = (playlist.id == playingPlaylistId)
        if (isCurrent) {
            holder.playingIcon.visibility = View.VISIBLE
            holder.playingIcon.setImageResource(if (isPlaying) R.drawable.ic_equalizer else R.drawable.ic_play_arrow)
            holder.title.setTextColor(ContextCompat.getColor(holder.itemView.context, R.color.amber_accent))
            holder.root.setBackgroundResource(R.drawable.bg_drawer_item_playing)
        } else {
            holder.playingIcon.visibility = View.GONE
            holder.title.setTextColor(ContextCompat.getColor(holder.itemView.context, R.color.text_primary))
            holder.root.setBackgroundResource(R.drawable.bg_song_item_normal)
        }

        val hasValidCoverUrl = playlist.coverUrl.isNotBlank() &&
            (playlist.coverUrl.startsWith("http") || playlist.coverUrl.startsWith("data:") || java.io.File(playlist.coverUrl).exists())

        val fallbackRes = if (playlist.coverResId != 0) playlist.coverResId else R.drawable.vm_art_village
        val loadTarget: Any = if (hasValidCoverUrl) playlist.coverUrl else fallbackRes

        Glide.with(holder.icon.context)
            .load(loadTarget)
            .centerCrop()
            .error(fallbackRes)
            .into(holder.icon)

        holder.itemView.setOnClickListener { onItemClick(playlist) }
        holder.itemView.setOnLongClickListener {
            onItemLongClick(playlist)
            true
        }
    }

    override fun getItemCount(): Int = playlists.size

    fun setPlayingPlaylistId(id: Long?, isPlaying: Boolean = false) {
        if (playingPlaylistId != id || this.isPlaying != isPlaying) {
            playingPlaylistId = id
            this.isPlaying = isPlaying
            notifyDataSetChanged()
        }
    }

    fun updateData(newPlaylists: List<Playlist>) {
        this.playlists = newPlaylists
        notifyDataSetChanged()
    }
}
