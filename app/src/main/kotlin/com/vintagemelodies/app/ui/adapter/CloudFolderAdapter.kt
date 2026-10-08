package com.vintagemelodies.app.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.vintagemelodies.app.R
import com.vintagemelodies.app.data.model.CloudFolder

class CloudFolderAdapter(
    private var folders: List<CloudFolder>,
    private val onItemClick: (CloudFolder) -> Unit
) : RecyclerView.Adapter<CloudFolderAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val name: TextView = view.findViewById(R.id.tv_folder_name)
        val count: TextView = view.findViewById(R.id.tv_folder_song_count)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_cloud_folder, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val folder = folders[position]
        holder.name.text = "📁 ${folder.name}"
        holder.count.text = "${folder.songCount} unadded song${if (folder.songCount == 1) "" else "s"}"

        holder.itemView.setOnClickListener { onItemClick(folder) }
    }

    override fun getItemCount(): Int = folders.size

    fun updateData(newFolders: List<CloudFolder>) {
        this.folders = newFolders
        notifyDataSetChanged()
    }
}
