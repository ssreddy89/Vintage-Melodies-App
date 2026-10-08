package com.vintagemelodies.app.data.local

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.vintagemelodies.app.R
import org.json.JSONObject

/**
 * SQLite Database Open Helper for local standalone data management.
 * Pre-seeds the database with default playlists (with languages & covers) and Cloudflare R2 songs.
 */
class VintageDatabaseHelper(private val context: Context) :
    SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        const val DATABASE_NAME = "vintage_melodies_standalone.db"
        const val DATABASE_VERSION = 8

        const val TABLE_SONGS = "songs"
        const val TABLE_PLAYLISTS = "playlists"
        const val TABLE_PLAYLIST_SONGS = "playlist_songs"
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE $TABLE_SONGS (
                id TEXT PRIMARY KEY,
                title TEXT NOT NULL,
                artist TEXT,
                folder TEXT,
                album TEXT,
                year TEXT DEFAULT '',
                duration INTEGER DEFAULT 0,
                media_url TEXT NOT NULL,
                artwork_res_id INTEGER DEFAULT 0,
                artwork_url TEXT DEFAULT ''
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE $TABLE_PLAYLISTS (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                name TEXT NOT NULL,
                language TEXT NOT NULL DEFAULT 'Telugu',
                cover_res_id INTEGER DEFAULT 0,
                cover_url TEXT DEFAULT '',
                created_at INTEGER DEFAULT (strftime('%s', 'now'))
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE $TABLE_PLAYLIST_SONGS (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                playlist_id INTEGER NOT NULL,
                song_id TEXT NOT NULL,
                sort_order INTEGER DEFAULT 0,
                FOREIGN KEY (playlist_id) REFERENCES $TABLE_PLAYLISTS(id) ON DELETE CASCADE,
                FOREIGN KEY (song_id) REFERENCES $TABLE_SONGS(id) ON DELETE CASCADE
            )
            """.trimIndent()
        )

        preSeedData(db)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 8) {
            try {
                db.execSQL("ALTER TABLE $TABLE_SONGS ADD COLUMN year TEXT DEFAULT ''")
            } catch (_: Exception) {}
            try {
                db.execSQL("ALTER TABLE $TABLE_SONGS ADD COLUMN artwork_url TEXT DEFAULT ''")
            } catch (_: Exception) {}
            preSeedSongsOnly(db)
        }
    }

    private fun preSeedSongsOnly(db: SQLiteDatabase) {
        try {
            val jsonString = context.assets.open("r2_songs.json").bufferedReader().use { it.readText() }
            val root = JSONObject(jsonString)
            val songsArray = root.optJSONArray("songs") ?: return

            val artworks = listOf(
                R.drawable.ic_vintage_player_art_2,
                R.drawable.ic_vintage_player_art_3,
                R.drawable.vm_gif_kitchen,
                R.drawable.vm_gif_tailoring,
                R.drawable.vm_gif_roadtrip,
                R.drawable.vm_art_monsoon,
                R.drawable.vm_art_sunset,
                R.drawable.vm_art_classic
            )

            for (i in 0 until songsArray.length()) {
                val songObj = songsArray.getJSONObject(i)
                val id = songObj.optString("id", "song_$i")
                val title = songObj.optString("title", "Untitled")
                val artist = songObj.optString("artist", "Vintage Melodies")
                val folder = songObj.optString("folder", "Songs")
                val album = songObj.optString("album", "Cloudflare R2: $folder")
                val year = songObj.optString("year", "")
                val mediaUrl = songObj.optString("audio_url", songObj.optString("media_url", ""))
                val artworkUrl = songObj.optString("artwork_url", "")
                val artResId = artworks[i % artworks.size]

                val cv = ContentValues().apply {
                    put("id", id)
                    put("title", title)
                    put("artist", artist)
                    put("folder", folder)
                    put("album", album)
                    put("year", year)
                    put("duration", 0)
                    put("media_url", mediaUrl)
                    put("artwork_res_id", artResId)
                    put("artwork_url", artworkUrl)
                }
                val updatedRows = db.update(TABLE_SONGS, cv, "id = ?", arrayOf(id))
                if (updatedRows == 0) {
                    db.insert(TABLE_SONGS, null, cv)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun preSeedData(db: SQLiteDatabase) {
        try {
            // Load bundled R2 catalog from assets
            val jsonString = context.assets.open("r2_songs.json").bufferedReader().use { it.readText() }
            val root = JSONObject(jsonString)
            val songsArray = root.optJSONArray("songs") ?: return

            val artworks = listOf(
                R.drawable.ic_vintage_player_art_2,
                R.drawable.ic_vintage_player_art_3,
                R.drawable.vm_gif_kitchen,
                R.drawable.vm_gif_tailoring,
                R.drawable.vm_gif_roadtrip,
                R.drawable.vm_art_monsoon,
                R.drawable.vm_art_sunset,
                R.drawable.vm_art_classic
            )

            val songIds = mutableListOf<String>()

            for (i in 0 until songsArray.length()) {
                val songObj = songsArray.getJSONObject(i)
                val id = songObj.optString("id", "song_$i")
                val title = songObj.optString("title", "Untitled")
                val artist = songObj.optString("artist", "Vintage Melodies")
                val folder = songObj.optString("folder", "Songs")
                val album = songObj.optString("album", "Cloudflare R2: $folder")
                val year = songObj.optString("year", "")
                val mediaUrl = songObj.optString("audio_url", songObj.optString("media_url", ""))
                val artworkUrl = songObj.optString("artwork_url", "")
                val artResId = artworks[i % artworks.size]

                val cv = ContentValues().apply {
                    put("id", id)
                    put("title", title)
                    put("artist", artist)
                    put("folder", folder)
                    put("album", album)
                    put("year", year)
                    put("duration", 0)
                    put("media_url", mediaUrl)
                    put("artwork_res_id", artResId)
                    put("artwork_url", artworkUrl)
                }
                db.insertWithOnConflict(TABLE_SONGS, null, cv, SQLiteDatabase.CONFLICT_REPLACE)
                songIds.add(id)
            }

            // Create initial curated playlists with multilingual support and pictures
            val defaultPlaylists = listOf(
                Triple("Vintage Hits", "Telugu", R.drawable.vm_gif_kitchen),
                Triple("Tailoring Melodies", "Telugu", R.drawable.vm_gif_tailoring),
                Triple("Road Trip Beats", "English", R.drawable.vm_gif_roadtrip),
                Triple("Monsoon Classics", "Hindi", R.drawable.vm_art_monsoon),
                Triple("Village Melodies", "Telugu", R.drawable.vm_art_village),
                Triple("Golden Sunset", "Tamil", R.drawable.vm_art_sunset),
                Triple("Timeless Classics", "Kannada", R.drawable.vm_art_classic),
                Triple("Nostalgia Beats", "Hindi", R.drawable.vm_art_nostalgia)
            )

            // Seed 2 songs per playlist initially (16 total assigned songs),
            // leaving remaining 43 songs unassigned in Cloud storage ready to be added!
            defaultPlaylists.forEachIndexed { pIdx, (name, lang, coverRes) ->
                val pCv = ContentValues().apply {
                    put("name", name)
                    put("language", lang)
                    put("cover_res_id", coverRes)
                    put("cover_url", "")
                }
                val playlistId = db.insert(TABLE_PLAYLISTS, null, pCv)

                // Assign 2 songs per playlist
                val subset = songIds.drop(pIdx * 2).take(2)
                subset.forEachIndexed { sIdx, sId ->
                    val psCv = ContentValues().apply {
                        put("playlist_id", playlistId)
                        put("song_id", sId)
                        put("sort_order", sIdx)
                    }
                    db.insert(TABLE_PLAYLIST_SONGS, null, psCv)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
