package com.vintagemelodies.app.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.vintagemelodies.app.R
import com.vintagemelodies.app.data.model.Playlist

class MoodPlaylistAdapter(
    private var playlists: List<Playlist>,
    private val onItemClick: (Playlist) -> Unit,
    private val onPlayClick: ((Playlist) -> Unit)? = null
) : RecyclerView.Adapter<MoodPlaylistAdapter.ViewHolder>() {

    private var playingPlaylistId: Long? = null
    private var isPlaying: Boolean = false

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val root: View = view.findViewById(R.id.card_root_container)
        val image: ImageView = view.findViewById(R.id.card_image)
        val title: TextView = view.findViewById(R.id.card_title)
        val subtitle: TextView = view.findViewById(R.id.card_subtitle)
        val langBadge: TextView = view.findViewById(R.id.card_lang_badge)
        val playingIndicator: View = view.findViewById(R.id.card_playing_indicator)
        val playingIcon: ImageView = view.findViewById(R.id.card_playing_icon)
        val playingText: TextView = view.findViewById(R.id.card_playing_text)
        val playBtn: ImageButton = view.findViewById(R.id.btn_mood_card_play)
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

        // Highlight if this playlist is currently active / playing
        val isCurrent = (playlist.id == playingPlaylistId)
        if (isCurrent) {
            holder.playingIndicator.visibility = View.VISIBLE
            if (isPlaying) {
                holder.playingText.text = "PLAYING"
                holder.playingIcon.setImageResource(R.drawable.ic_equalizer)
                holder.playBtn.setImageResource(R.drawable.ic_pause)
            } else {
                holder.playingText.text = "PAUSED"
                holder.playingIcon.setImageResource(R.drawable.ic_play_arrow)
                holder.playBtn.setImageResource(R.drawable.ic_play_arrow)
            }
            holder.title.setTextColor(ContextCompat.getColor(holder.itemView.context, R.color.amber_accent))
            holder.root.setBackgroundResource(R.drawable.bg_mood_card_playing)
            holder.playBtn.setColorFilter(ContextCompat.getColor(holder.itemView.context, R.color.amber_accent))
        } else {
            holder.playingIndicator.visibility = View.GONE
            holder.title.setTextColor(ContextCompat.getColor(holder.itemView.context, R.color.text_primary))
            holder.root.setBackgroundResource(R.drawable.bg_mood_card)
            holder.playBtn.setImageResource(R.drawable.ic_play_arrow)
            holder.playBtn.setColorFilter(ContextCompat.getColor(holder.itemView.context, R.color.text_primary))
        }

        // Safe coverUrl handling (protect against missing local paths from other devices)
        val hasValidCoverUrl = playlist.coverUrl.isNotBlank() &&
            (playlist.coverUrl.startsWith("http") || playlist.coverUrl.startsWith("data:") || java.io.File(playlist.coverUrl).exists())

        val fallbackRes = if (playlist.coverResId != 0) playlist.coverResId else R.drawable.vm_art_village
        val loadTarget: Any = if (hasValidCoverUrl) playlist.coverUrl else fallbackRes

        Glide.with(holder.image.context)
            .load(loadTarget)
            .centerCrop()
            .error(fallbackRes)
            .into(holder.image)

        holder.itemView.setOnClickListener { onItemClick(playlist) }
        holder.playBtn.setOnClickListener {
            onPlayClick?.invoke(playlist) ?: onItemClick(playlist)
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
