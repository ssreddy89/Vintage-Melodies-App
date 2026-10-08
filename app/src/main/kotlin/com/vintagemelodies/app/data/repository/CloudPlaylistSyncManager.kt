package com.vintagemelodies.app.data.repository

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import com.vintagemelodies.app.R
import com.vintagemelodies.app.data.local.VintageDatabaseHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * Central Cloud Playlist Sync Manager.
 * 
 * - Common Users: Automatically fetches shared playlists published by Admins from Cloudflare R2.
 * - Admins: Automatically publishes created/edited/deleted playlists & songs to Cloudflare R2
 *   so all devices and users stay 100% in sync.
 */
object CloudPlaylistSyncManager {

    private const val API_URL = "https://vintage-melodies-kappa.vercel.app/api/cloud-r2/app-playlists"
    const val ADMIN_SECRET = "Admin@2026"

    /**
     * Fetch shared playlists from cloud and update local SQLite database.
     * Safe for all users: runs asynchronously on IO dispatcher.
     */
    fun fetchAndSyncFromCloud(
        context: Context,
        repository: MusicRepository,
        onComplete: ((Boolean, Int) -> Unit)? = null
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            var success = false
            var count = 0

            try {
                val url = URL(API_URL)
                val conn = (url.openConnection() as HttpURLConnection).apply {
                    connectTimeout = 8000
                    readTimeout = 12000
                    requestMethod = "GET"
                    setRequestProperty("User-Agent", "VintageMelodies-Android")
                    setRequestProperty("Cache-Control", "no-cache")
                }

                if (conn.responseCode == 200) {
                    val jsonStr = conn.inputStream.bufferedReader().use { it.readText() }
                    val root = JSONObject(jsonStr)
                    val playlistsArray = root.optJSONArray("playlists")

                    // Only overwrite local database if the cloud actually contains published playlists!
                    if (playlistsArray != null && playlistsArray.length() > 0) {
                        val dbHelper = VintageDatabaseHelper(context)
                        val db = dbHelper.writableDatabase

                        db.beginTransaction()
                        try {
                            // Clear existing local playlists & assignments to sync exactly with master cloud copy
                            db.delete(VintageDatabaseHelper.TABLE_PLAYLIST_SONGS, null, null)
                            db.delete(VintageDatabaseHelper.TABLE_PLAYLISTS, null, null)

                            for (i in 0 until playlistsArray.length()) {
                                val pObj = playlistsArray.getJSONObject(i)
                                val name = pObj.optString("name", "Playlist ${i + 1}")
                                val lang = pObj.optString("language", "Telugu")
                                val coverRes = pObj.optInt("cover_res_id", R.drawable.vm_art_village)
                                val coverUrl = pObj.optString("cover_url", "")

                                val pCv = ContentValues().apply {
                                    put("name", name)
                                    put("language", lang)
                                    put("cover_res_id", coverRes)
                                    put("cover_url", coverUrl)
                                }
                                val newId = db.insert(VintageDatabaseHelper.TABLE_PLAYLISTS, null, pCv)

                                val songsArray = pObj.optJSONArray("song_ids")
                                if (songsArray != null && songsArray.length() > 0) {
                                    for (s in 0 until songsArray.length()) {
                                        val songId = songsArray.getString(s)
                                        val psCv = ContentValues().apply {
                                            put("playlist_id", newId)
                                            put("song_id", songId)
                                            put("sort_order", s)
                                        }
                                        db.insert(VintageDatabaseHelper.TABLE_PLAYLIST_SONGS, null, psCv)
                                    }
                                }
                                count++
                            }
                            db.setTransactionSuccessful()
                            success = true
                        } finally {
                            db.endTransaction()
                        }
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

    /**
     * Publish local playlists to Cloudflare R2 via cloud API.
     * Admin action: makes the local playlists the master copy for all devices worldwide.
     */
    fun publishPlaylistsToCloud(
        context: Context,
        repository: MusicRepository,
        onComplete: ((Boolean, String) -> Unit)? = null
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            var success = false
            var message = ""

            try {
                val playlists = repository.getAllPlaylists()
                val array = JSONArray()

                for (p in playlists) {
                    val songs = repository.getSongsForPlaylist(p.id).map { it.id }
                    val obj = JSONObject().apply {
                        put("name", p.name)
                        put("language", p.language)
                        put("cover_res_id", p.coverResId)
                        put("cover_url", p.coverUrl)
                        put("song_ids", JSONArray(songs))
                    }
                    array.put(obj)
                }

                val payload = JSONObject().apply {
                    put("adminKey", ADMIN_SECRET)
                    put("playlists", array)
                }

                val url = URL(API_URL)
                val conn = (url.openConnection() as HttpURLConnection).apply {
                    connectTimeout = 10000
                    readTimeout = 15000
                    requestMethod = "POST"
                    doOutput = true
                    setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                    setRequestProperty("User-Agent", "VintageMelodies-Android")
                }

                conn.outputStream.use { os ->
                    os.write(payload.toString().toByteArray(Charsets.UTF_8))
                }

                if (conn.responseCode == 200) {
                    val resStr = conn.inputStream.bufferedReader().use { it.readText() }
                    val resObj = JSONObject(resStr)
                    success = resObj.optBoolean("success", true)
                    message = resObj.optString("message", "Playlists published to cloud")
                } else {
                    message = "Server response: ${conn.responseCode}"
                }
            } catch (e: Exception) {
                e.printStackTrace()
                message = e.message ?: "Failed to connect to cloud"
            }

            withContext(Dispatchers.Main) {
                onComplete?.invoke(success, message)
            }
        }
    }
}
