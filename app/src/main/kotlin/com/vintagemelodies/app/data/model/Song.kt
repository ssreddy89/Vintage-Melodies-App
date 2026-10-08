package com.vintagemelodies.app.data.model

data class Song(
    val id: String,
    val title: String,
    val artist: String,
    val folder: String,
    val album: String = "",
    val year: String = "",
    val duration: Long = 0,
    val artworkResId: Int = 0,
    val artworkUrl: String = "",
    val mediaUrl: String = "",
    val audioUrl: String = mediaUrl
)
