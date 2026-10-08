package com.vintagemelodies.app.data.repository

import android.content.ContentValues
import android.content.Context
import com.vintagemelodies.app.R
import com.vintagemelodies.app.data.local.VintageDatabaseHelper
import com.vintagemelodies.app.data.model.CloudFolder
import com.vintagemelodies.app.data.model.Playlist
import com.vintagemelodies.app.data.model.Song
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import kotlin.concurrent.thread

class MusicRepository(private val context: Context) {

    private val dbHelper = VintageDatabaseHelper(context)
    private val backupFile = File(context.getExternalFilesDir(null) ?: context.filesDir, "playlists_cloud_backup.json")

    fun getAllPlaylists(): List<Playlist> {
        val db = dbHelper.readableDatabase
        val list = mutableListOf<Playlist>()

        val cursor = db.rawQuery(
            """
            SELECT p.id, p.name, p.language, p.cover_res_id, p.cover_url, COUNT(ps.id) as song_count
            FROM ${VintageDatabaseHelper.TABLE_PLAYLISTS} p
            LEFT JOIN ${VintageDatabaseHelper.TABLE_PLAYLIST_SONGS} ps ON p.id = ps.playlist_id
            GROUP BY p.id, p.name, p.language, p.cover_res_id, p.cover_url
            ORDER BY p.id ASC
            """.trimIndent(),
            null
        )

        cursor.use {
            while (it.moveToNext()) {
                val id = it.getLong(0)
                val name = it.getString(1)
                val language = it.getString(2) ?: "Telugu"
                val coverRes = it.getInt(3)
                val coverUrl = it.getString(4) ?: ""
                val songCount = it.getInt(5)
                list.add(Playlist(id = id, name = name, language = language, coverResId = coverRes, coverUrl = coverUrl, songCount = songCount))
            }
        }
        return list
    }

    fun getPlaylistById(playlistId: Long): Playlist? {
        val db = dbHelper.readableDatabase
        val cursor = db.rawQuery(
            """
            SELECT p.id, p.name, p.language, p.cover_res_id, p.cover_url, COUNT(ps.id) as song_count
            FROM ${VintageDatabaseHelper.TABLE_PLAYLISTS} p
            LEFT JOIN ${VintageDatabaseHelper.TABLE_PLAYLIST_SONGS} ps ON p.id = ps.playlist_id
            WHERE p.id = ?
            GROUP BY p.id, p.name, p.language, p.cover_res_id, p.cover_url
            """.trimIndent(),
            arrayOf(playlistId.toString())
        )

        return cursor.use {
            if (it.moveToFirst()) {
                Playlist(
                    id = it.getLong(0),
                    name = it.getString(1),
                    language = it.getString(2) ?: "Telugu",
                    coverResId = it.getInt(3),
                    coverUrl = it.getString(4) ?: "",
                    songCount = it.getInt(5)
                )
            } else null
        }
    }

    fun getPlaylistsByLanguages(languages: Set<String>): List<Playlist> {
        if (languages.isEmpty()) return emptyList()

        val all = getAllPlaylists()
        return all.filter { playlist ->
            languages.any { lang -> lang.equals(playlist.language, ignoreCase = true) }
        }
    }

    fun getSongsForPlaylist(playlistId: Long): List<Song> {
        val db = dbHelper.readableDatabase
        val list = mutableListOf<Song>()

        val cursor = db.rawQuery(
            """
            SELECT s.id, s.title, s.artist, s.folder, s.album, s.duration, s.media_url, s.artwork_res_id
            FROM ${VintageDatabaseHelper.TABLE_SONGS} s
            INNER JOIN ${VintageDatabaseHelper.TABLE_PLAYLIST_SONGS} ps ON s.id = ps.song_id
            WHERE ps.playlist_id = ?
            ORDER BY ps.sort_order ASC
            """.trimIndent(),
            arrayOf(playlistId.toString())
        )

        cursor.use {
            while (it.moveToNext()) {
                list.add(
                    Song(
                        id = it.getString(0),
                        title = it.getString(1),
                        artist = it.getString(2),
                        folder = it.getString(3),
                        album = it.getString(4),
                        duration = it.getLong(5),
                        mediaUrl = it.getString(6),
                        artworkResId = it.getInt(7)
                    )
                )
            }
        }
        return list
    }

    fun getAllSongs(filter: String = ""): List<Song> {
        val db = dbHelper.readableDatabase
        val list = mutableListOf<Song>()

        val query = if (filter.isBlank()) {
            "SELECT id, title, artist, folder, album, duration, media_url, artwork_res_id FROM ${VintageDatabaseHelper.TABLE_SONGS} ORDER BY title ASC"
        } else {
            "SELECT id, title, artist, folder, album, duration, media_url, artwork_res_id FROM ${VintageDatabaseHelper.TABLE_SONGS} WHERE title LIKE ? OR artist LIKE ? OR folder LIKE ? ORDER BY title ASC"
        }

        val args = if (filter.isBlank()) null else arrayOf("%$filter%", "%$filter%", "%$filter%")
        val cursor = db.rawQuery(query, args)

        cursor.use {
            while (it.moveToNext()) {
                list.add(
                    Song(
                        id = it.getString(0),
                        title = it.getString(1),
                        artist = it.getString(2),
                        folder = it.getString(3),
                        album = it.getString(4),
                        duration = it.getLong(5),
                        mediaUrl = it.getString(6),
                        artworkResId = it.getInt(7)
                    )
                )
            }
        }
        return list
    }

    fun getAvailableCloudFolders(): List<CloudFolder> {
        val db = dbHelper.readableDatabase
        val list = mutableListOf<CloudFolder>()

        val cursor = db.rawQuery(
            """
            SELECT folder, COUNT(*) as song_count
            FROM ${VintageDatabaseHelper.TABLE_SONGS}
            WHERE id NOT IN (SELECT DISTINCT song_id FROM ${VintageDatabaseHelper.TABLE_PLAYLIST_SONGS})
            GROUP BY folder
            ORDER BY folder ASC
            """.trimIndent(),
            null
        )

        cursor.use {
            while (it.moveToNext()) {
                val folder = it.getString(0) ?: "Songs"
                val count = it.getInt(1)
                list.add(CloudFolder(name = folder, songCount = count))
            }
        }
        return list
    }

    fun getUnassignedSongs(folderFilter: String = "", query: String = ""): List<Song> {
        val db = dbHelper.readableDatabase
        val list = mutableListOf<Song>()

        val whereClauses = mutableListOf("id NOT IN (SELECT DISTINCT song_id FROM ${VintageDatabaseHelper.TABLE_PLAYLIST_SONGS})")
        val args = mutableListOf<String>()

        if (folderFilter.isNotBlank()) {
            whereClauses.add("folder = ?")
            args.add(folderFilter)
        }

        if (query.isNotBlank()) {
            whereClauses.add("(title LIKE ? OR artist LIKE ?)")
            args.add("%$query%")
            args.add("%$query%")
        }

        val sql = "SELECT id, title, artist, folder, album, duration, media_url, artwork_res_id FROM ${VintageDatabaseHelper.TABLE_SONGS} WHERE ${whereClauses.joinToString(" AND ")} ORDER BY title ASC"
        val cursor = db.rawQuery(sql, if (args.isEmpty()) null else args.toTypedArray())

        cursor.use {
            while (it.moveToNext()) {
                list.add(
                    Song(
                        id = it.getString(0),
                        title = it.getString(1),
                        artist = it.getString(2),
                        folder = it.getString(3),
                        album = it.getString(4),
                        duration = it.getLong(5),
                        mediaUrl = it.getString(6),
                        artworkResId = it.getInt(7)
                    )
                )
            }
        }
        return list
    }

    // ── Playlist Management & Persistence (Items 1, 2, 3) ───────────────────

    fun createPlaylist(
        name: String,
        language: String = "Telugu",
        coverResId: Int = R.drawable.vm_art_village,
        coverUrl: String = ""
    ): Long {
        val db = dbHelper.writableDatabase
        val cv = ContentValues().apply {
            put("name", name)
            put("language", language)
            put("cover_res_id", coverResId)
            put("cover_url", coverUrl)
        }
        val id = db.insert(VintageDatabaseHelper.TABLE_PLAYLISTS, null, cv)
        savePlaylistsBackup()
        return id
    }

    fun updatePlaylist(
        playlistId: Long,
        name: String,
        language: String,
        coverResId: Int,
        coverUrl: String = ""
    ): Boolean {
        val db = dbHelper.writableDatabase
        val cv = ContentValues().apply {
            put("name", name)
            put("language", language)
            put("cover_res_id", coverResId)
            put("cover_url", coverUrl)
        }
        val rows = db.update(VintageDatabaseHelper.TABLE_PLAYLISTS, cv, "id = ?", arrayOf(playlistId.toString()))
        if (rows > 0) savePlaylistsBackup()
        return rows > 0
    }

    fun deletePlaylist(playlistId: Long): Boolean {
        val db = dbHelper.writableDatabase
        db.delete(VintageDatabaseHelper.TABLE_PLAYLIST_SONGS, "playlist_id = ?", arrayOf(playlistId.toString()))
        val count = db.delete(VintageDatabaseHelper.TABLE_PLAYLISTS, "id = ?", arrayOf(playlistId.toString()))
        if (count > 0) savePlaylistsBackup()
        return count > 0
    }

    fun copyPlaylist(playlistId: Long, newName: String? = null): Long {
        val db = dbHelper.writableDatabase
        var originalName = ""
        var originalLang = "Telugu"
        var originalCover = R.drawable.vm_art_village
        var originalCoverUrl = ""

        val c = db.rawQuery("SELECT name, language, cover_res_id, cover_url FROM ${VintageDatabaseHelper.TABLE_PLAYLISTS} WHERE id = ?", arrayOf(playlistId.toString()))
        c.use {
            if (it.moveToFirst()) {
                originalName = it.getString(0)
                originalLang = it.getString(1) ?: "Telugu"
                originalCover = it.getInt(2)
                originalCoverUrl = it.getString(3) ?: ""
            }
        }

        val copyName = newName ?: "$originalName (Copy)"
        val newPlaylistId = createPlaylist(copyName, originalLang, originalCover, originalCoverUrl)

        val songs = getSongsForPlaylist(playlistId)
        addSongsToPlaylist(newPlaylistId, songs.map { it.id })

        savePlaylistsBackup()
        return newPlaylistId
    }

    fun addSongsToPlaylist(playlistId: Long, songIds: List<String>) {
        val db = dbHelper.writableDatabase
        db.beginTransaction()
        try {
            var currentMaxOrder = 0
            val c = db.rawQuery("SELECT MAX(sort_order) FROM ${VintageDatabaseHelper.TABLE_PLAYLIST_SONGS} WHERE playlist_id = ?", arrayOf(playlistId.toString()))
            c.use {
                if (it.moveToFirst() && !it.isNull(0)) {
                    currentMaxOrder = it.getInt(0) + 1
                }
            }

            songIds.forEachIndexed { index, songId ->
                val checkCursor = db.rawQuery(
                    "SELECT 1 FROM ${VintageDatabaseHelper.TABLE_PLAYLIST_SONGS} WHERE playlist_id = ? AND song_id = ?",
                    arrayOf(playlistId.toString(), songId)
                )
                val exists = checkCursor.use { it.moveToFirst() }
                if (!exists) {
                    val cv = ContentValues().apply {
                        put("playlist_id", playlistId)
                        put("song_id", songId)
                        put("sort_order", currentMaxOrder + index)
                    }
                    db.insert(VintageDatabaseHelper.TABLE_PLAYLIST_SONGS, null, cv)
                }
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
        savePlaylistsBackup()
    }

    fun copySongToPlaylist(targetPlaylistId: Long, songId: String): Boolean {
        addSongsToPlaylist(targetPlaylistId, listOf(songId))
        return true
    }

    fun moveSongToPlaylist(fromPlaylistId: Long, toPlaylistId: Long, songId: String): Boolean {
        copySongToPlaylist(toPlaylistId, songId)
        return removeSongFromPlaylist(fromPlaylistId, songId)
    }

    fun removeSongFromPlaylist(playlistId: Long, songId: String): Boolean {
        val db = dbHelper.writableDatabase
        val count = db.delete(
            VintageDatabaseHelper.TABLE_PLAYLIST_SONGS,
            "playlist_id = ? AND song_id = ?",
            arrayOf(playlistId.toString(), songId)
        )
        if (count > 0) savePlaylistsBackup()
        return count > 0
    }

    // ── Persistent Backup & Central DB Sync (Items 2 & 3) ────────────────────

    private fun savePlaylistsBackup() {
        thread {
            try {
                val playlists = getAllPlaylists()
                val array = JSONArray()
                for (p in playlists) {
                    val songs = getSongsForPlaylist(p.id).map { it.id }
                    val obj = JSONObject().apply {
                        put("name", p.name)
                        put("language", p.language)
                        put("cover_res_id", p.coverResId)
                        put("cover_url", p.coverUrl)
                        put("song_ids", JSONArray(songs))
                    }
                    array.put(obj)
                }
                backupFile.writeText(array.toString())
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun restoreFromBackupIfEmpty() {
        try {
            if (!backupFile.exists()) return
            val jsonStr = backupFile.readText()
            if (jsonStr.isBlank()) return
            val array = JSONArray(jsonStr)

            val existingPlaylists = getAllPlaylists()
            val existingNames = existingPlaylists.map { it.name }.toSet()

            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val name = obj.optString("name")
                if (name !in existingNames) {
                    val lang = obj.optString("language", "Telugu")
                    val coverRes = obj.optInt("cover_res_id", R.drawable.vm_art_village)
                    val coverUrl = obj.optString("cover_url", "")
                    val newId = createPlaylist(name, lang, coverRes, coverUrl)

                    val songsArray = obj.optJSONArray("song_ids")
                    if (songsArray != null && songsArray.length() > 0) {
                        val ids = mutableListOf<String>()
                        for (s in 0 until songsArray.length()) {
                            ids.add(songsArray.getString(s))
                        }
                        addSongsToPlaylist(newId, ids)
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
