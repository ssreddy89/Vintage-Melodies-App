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
 * Ensures newly uploaded songs, new folders, or moved songs are immediately visible in the app.
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
                            val artist = obj.optString("artist", "Vintage Melodies")
                            val folder = obj.optString("folder", "Songs")
                            val album = obj.optString("album", "")
                            val year = obj.optString("year", "")
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

                            val finalAlbum = if (album.isNotBlank() && !album.startsWith("Folder:")) album
                                else if (existingAlbum.isNotBlank()) existingAlbum
                                else "Folder: $folder"
                            val finalYear = if (year.isNotBlank()) year else existingYear
                            val finalArtwork = if (artworkUrl.isNotBlank()) artworkUrl else existingArtwork
                            val finalArtist = if (artist.isNotBlank() && artist != "Vintage Melodies") artist
                                else if (existingArtist.isNotBlank()) existingArtist
                                else artist

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
                            db.insertWithOnConflict(VintageDatabaseHelper.TABLE_SONGS, null, cv, SQLiteDatabase.CONFLICT_REPLACE)
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
