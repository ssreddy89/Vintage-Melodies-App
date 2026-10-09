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
) {
    /**
     * Formats song details as (Contributing Artists • Album • Year),
     * strictly excluding cloud folder paths, website URLs, and internal tags.
     */
    fun getDetailsInfo(): String {
        val parts = mutableListOf<String>()

        val cleanArtist = artist.trim()
        if (cleanArtist.isNotBlank() &&
            !cleanArtist.startsWith("📁") &&
            !cleanArtist.contains("Songs/") &&
            !cleanArtist.contains("SenSongs", ignoreCase = true) &&
            !cleanArtist.equals("Vintage Melodies", ignoreCase = true)) {
            parts.add(cleanArtist)
        }

        var cleanAlbum = album.trim()
        if (cleanAlbum.isNotBlank() &&
            !cleanAlbum.startsWith("Folder:", ignoreCase = true) &&
            !cleanAlbum.startsWith("Cloudflare", ignoreCase = true)) {
            if (year.isNotBlank() && cleanAlbum.endsWith("($year)")) {
                cleanAlbum = cleanAlbum.substringBeforeLast("($year)").trim()
            }
            parts.add(cleanAlbum)
        }

        val cleanYear = year.trim()
        if (cleanYear.isNotBlank() && cleanYear.matches(Regex("\\d{4}"))) {
            parts.add(cleanYear)
        }

        return if (parts.isNotEmpty()) parts.joinToString(" • ") else "Vintage Melodies"
    }
}
