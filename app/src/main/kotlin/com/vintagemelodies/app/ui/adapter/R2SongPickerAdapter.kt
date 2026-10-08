package com.vintagemelodies.app.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.vintagemelodies.app.R
import com.vintagemelodies.app.data.model.Song

class R2SongPickerAdapter(
    private var songs: List<Song>,
    private val selectedIds: MutableSet<String>,
    private val onSelectionChanged: (Int) -> Unit
) : RecyclerView.Adapter<R2SongPickerAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val cb: CheckBox = view.findViewById(R.id.cb_select_song)
        val title: TextView = view.findViewById(R.id.picker_song_title)
        val artist: TextView = view.findViewById(R.id.picker_song_artist)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_r2_song_picker, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val song = songs[position]
        holder.title.text = song.title
        holder.artist.text = song.artist
        holder.cb.isChecked = selectedIds.contains(song.id)

        val toggle = {
            if (selectedIds.contains(song.id)) {
                selectedIds.remove(song.id)
            } else {
                selectedIds.add(song.id)
            }
            holder.cb.isChecked = selectedIds.contains(song.id)
            onSelectionChanged(selectedIds.size)
        }

        holder.itemView.setOnClickListener { toggle() }
        holder.cb.setOnClickListener { toggle() }
    }

    override fun getItemCount(): Int = songs.size

    fun updateData(newSongs: List<Song>) {
        this.songs = newSongs
        notifyDataSetChanged()
    }

    fun selectAll() {
        songs.forEach { selectedIds.add(it.id) }
        notifyDataSetChanged()
        onSelectionChanged(selectedIds.size)
    }

    fun deselectAll() {
        selectedIds.clear()
        notifyDataSetChanged()
        onSelectionChanged(selectedIds.size)
    }
}
