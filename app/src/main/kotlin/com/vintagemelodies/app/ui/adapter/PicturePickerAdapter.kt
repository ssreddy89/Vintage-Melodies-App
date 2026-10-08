package com.vintagemelodies.app.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.vintagemelodies.app.R

class PicturePickerAdapter(
    private val pictures: List<Int>,
    private var selectedResId: Int,
    private val onPictureSelected: (Int) -> Unit
) : RecyclerView.Adapter<PicturePickerAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val thumb: ImageView = view.findViewById(R.id.iv_picture_thumb)
        val border: View = view.findViewById(R.id.view_picture_selected_border)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_picture_picker, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val resId = pictures[position]
        Glide.with(holder.thumb.context)
            .load(resId)
            .centerCrop()
            .into(holder.thumb)

        holder.border.visibility = if (resId == selectedResId) View.VISIBLE else View.GONE

        holder.itemView.setOnClickListener {
            selectedResId = resId
            notifyDataSetChanged()
            onPictureSelected(resId)
        }
    }

    override fun getItemCount(): Int = pictures.size

    fun getSelectedPicture(): Int = selectedResId
}
