package com.vintagemelodies.app.data.repository

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import com.vintagemelodies.app.data.local.VintageDatabaseHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * Syncs the catalog of songs and folders directly from Cloudflare R2 / Web API.
 * Ensures newly uploaded songs, new folders, or moved songs are immediately visible in the app,
 * while preserving clean metadata (artist, album, year).
 */
object CloudCatalogSyncManager {

    private const val LIVE_R2_API_URL = "https://vintage-melodies-kappa.vercel.app/api/cloud-r2/songs"

    fun syncFromCloud(
        context: Context,
        onComplete: ((Boolean, Int) -> Unit)? = null
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            var success = false
            var count = 0

            try {
                val url = URL(LIVE_R2_API_URL)
                val conn = (url.openConnection() as HttpURLConnection).apply {
                    connectTimeout = 8000
                    readTimeout = 12000
                    requestMethod = "GET"
                    setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 14; Mobile)")
                }

                if (conn.responseCode == 200) {
                    val jsonStr = conn.inputStream.bufferedReader().use { it.readText() }
                    val root = JSONObject(jsonStr)
                    val songsArray = root.optJSONArray("songs")

                    if (songsArray != null && songsArray.length() > 0) {
                        val dbHelper = VintageDatabaseHelper(context)
                        val db = dbHelper.writableDatabase

                        for (i in 0 until songsArray.length()) {
                            val obj = songsArray.getJSONObject(i)
                            val id = obj.optString("id", "song_$i")
                            val title = obj.optString("title", "Untitled")
                            val incomingArtist = obj.optString("artist", "")
                            val folder = obj.optString("folder", "Songs")
                            val incomingAlbum = obj.optString("album", "")
                            val incomingYear = obj.optString("year", "")
                            val mediaUrl = obj.optString("audio_url", obj.optString("media_url", ""))
                            val artworkUrl = obj.optString("artwork_url", "")

                            // Check if song already exists in db
                            val checkCursor = db.rawQuery(
                                "SELECT album, year, artwork_url, artist FROM ${VintageDatabaseHelper.TABLE_SONGS} WHERE id = ?",
                                arrayOf(id)
                            )
                            val exists = checkCursor.moveToFirst()
                            val existingAlbum = if (exists) checkCursor.getString(0) ?: "" else ""
                            val existingYear = if (exists) checkCursor.getString(1) ?: "" else ""
                            val existingArtwork = if (exists) checkCursor.getString(2) ?: "" else ""
                            val existingArtist = if (exists) checkCursor.getString(3) ?: "" else ""
                            checkCursor.close()

                            // Protect high quality metadata against folder names or 📁
                            val isBadIncomingArtist = incomingArtist.isBlank() ||
                                    incomingArtist.startsWith("📁") ||
                                    incomingArtist.contains("Songs/") ||
                                    incomingArtist.contains("SenSongs", ignoreCase = true) ||
                                    incomingArtist.equals("Vintage Melodies", ignoreCase = true)

                            val hasCleanExistingArtist = existingArtist.isNotBlank() &&
                                    !existingArtist.startsWith("📁") &&
                                    !existingArtist.contains("Songs/") &&
                                    !existingArtist.contains("SenSongs", ignoreCase = true) &&
                                    !existingArtist.equals("Vintage Melodies", ignoreCase = true)

                            val finalArtist = when {
                                hasCleanExistingArtist -> existingArtist
                                !isBadIncomingArtist -> incomingArtist
                                else -> ""
                            }

                            val isBadIncomingAlbum = incomingAlbum.isBlank() ||
                                    incomingAlbum.startsWith("Folder:", ignoreCase = true) ||
                                    incomingAlbum.startsWith("Cloudflare", ignoreCase = true)

                            val hasCleanExistingAlbum = existingAlbum.isNotBlank() &&
                                    !existingAlbum.startsWith("Folder:", ignoreCase = true) &&
                                    !existingAlbum.startsWith("Cloudflare", ignoreCase = true)

                            val finalAlbum = when {
                                hasCleanExistingAlbum -> existingAlbum
                                !isBadIncomingAlbum -> incomingAlbum
                                else -> ""
                            }

                            val finalYear = if (existingYear.isNotBlank()) existingYear else incomingYear
                            val finalArtwork = if (artworkUrl.isNotBlank()) artworkUrl else existingArtwork

                            val cv = ContentValues().apply {
                                put("id", id)
                                put("title", title)
                                put("artist", finalArtist)
                                put("folder", folder)
                                put("album", finalAlbum)
                                put("year", finalYear)
                                put("duration", 0)
                                put("media_url", mediaUrl)
                                put("artwork_url", finalArtwork)
                            }
                            val updatedRows = db.update(VintageDatabaseHelper.TABLE_SONGS, cv, "id = ?", arrayOf(id))
                            if (updatedRows == 0) {
                                db.insert(VintageDatabaseHelper.TABLE_SONGS, null, cv)
                            }
                            count++
                        }
                        success = true
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            withContext(Dispatchers.Main) {
                onComplete?.invoke(success, count)
            }
        }
    }
}
