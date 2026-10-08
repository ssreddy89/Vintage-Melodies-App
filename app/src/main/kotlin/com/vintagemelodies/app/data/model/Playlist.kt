package com.vintagemelodies.app.data.model

data class Playlist(
    val id: Long,
    val name: String,
    val language: String = "Telugu",
    val coverResId: Int = 0,
    val coverUrl: String = "",
    val songIds: MutableList<String> = mutableListOf(),
    val songCount: Int = songIds.size
)
