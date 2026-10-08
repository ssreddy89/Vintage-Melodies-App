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

class MoodPlaylistAdapter(
    private var playlists: List<Playlist>,
    private val onItemClick: (Playlist) -> Unit
) : RecyclerView.Adapter<MoodPlaylistAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val image: ImageView = view.findViewById(R.id.card_image)
        val title: TextView = view.findViewById(R.id.card_title)
        val subtitle: TextView = view.findViewById(R.id.card_subtitle)
        val langBadge: TextView = view.findViewById(R.id.card_lang_badge)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_mood_card, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val playlist = playlists[position]
        holder.title.text = playlist.name
        holder.subtitle.text = "${playlist.language} • ${playlist.songCount} Songs"
        holder.langBadge.text = playlist.language.uppercase()

        // Load coverUrl (custom file or GIF) if set, otherwise fallback to coverResId
        val loadTarget: Any = if (playlist.coverUrl.isNotBlank()) playlist.coverUrl else {
            if (playlist.coverResId != 0) playlist.coverResId else R.drawable.vm_art_village
        }

        Glide.with(holder.image.context)
            .load(loadTarget)
            .centerCrop()
            .into(holder.image)

        holder.itemView.setOnClickListener { onItemClick(playlist) }
    }

    override fun getItemCount(): Int = playlists.size

    fun updateData(newPlaylists: List<Playlist>) {
        this.playlists = newPlaylists
        notifyDataSetChanged()
    }
}
