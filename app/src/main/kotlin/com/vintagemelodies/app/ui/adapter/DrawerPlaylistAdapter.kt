package com.vintagemelodies.app.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.vintagemelodies.app.R
import com.vintagemelodies.app.data.model.Playlist

class DrawerPlaylistAdapter(
    private var playlists: List<Playlist>,
    private val onItemClick: (Playlist) -> Unit,
    private val onItemLongClick: (Playlist) -> Unit
) : RecyclerView.Adapter<DrawerPlaylistAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val icon: ImageView = view.findViewById(R.id.drawer_item_icon)
        val title: TextView = view.findViewById(R.id.drawer_item_title)
        val subtitle: TextView = view.findViewById(R.id.drawer_item_subtitle)
        val langBadge: TextView = view.findViewById(R.id.drawer_lang_badge)
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

        val loadTarget: Any = if (playlist.coverUrl.isNotBlank()) playlist.coverUrl else {
            if (playlist.coverResId != 0) playlist.coverResId else R.drawable.vm_art_village
        }

        Glide.with(holder.icon.context)
            .load(loadTarget)
            .centerCrop()
            .into(holder.icon)

        holder.itemView.setOnClickListener { onItemClick(playlist) }
        holder.itemView.setOnLongClickListener {
            onItemLongClick(playlist)
            true
        }
    }

    override fun getItemCount(): Int = playlists.size

    fun updateData(newPlaylists: List<Playlist>) {
        this.playlists = newPlaylists
        notifyDataSetChanged()
    }
}
