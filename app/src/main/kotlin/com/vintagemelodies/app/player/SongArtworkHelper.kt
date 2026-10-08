package com.vintagemelodies.app.player

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.Drawable
import android.media.MediaMetadataRetriever
import android.util.LruCache
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.vintagemelodies.app.R
import com.vintagemelodies.app.data.model.Song
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

/**
 * Universal Song Artwork & ID3 Metadata Manager.
 * Extracts and caches embedded APIC cover images from MP3 files/streams
 * and seamlessly loads them into Player UI, Notification, and Lock Screen.
 */
object SongArtworkHelper {

    private val maxMemory = (Runtime.getRuntime().maxMemory() / 1024).toInt()
    private val cacheSize = maxMemory / 8

    private val memoryCache = object : LruCache<String, Bitmap>(cacheSize) {
        override fun sizeOf(key: String, bitmap: Bitmap): Int {
            return bitmap.byteCount / 1024
        }
    }

    private val scope = CoroutineScope(Dispatchers.IO)

    private fun getDiskCacheDir(context: Context): File {
        val dir = File(context.cacheDir, "song_art")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    private fun getDiskFile(context: Context, songId: String): File {
        val cleanName = songId.hashCode().toString() + ".jpg"
        return File(getDiskCacheDir(context), cleanName)
    }

    /**
     * Loads artwork into an ImageView.
     * Hierarchy: Memory Cache -> Disk Cache -> Remote artworkUrl (Glide) -> Embedded MP3 Extraction -> Fallback Resource
     */
    fun loadSongArt(
        imageView: ImageView,
        song: Song,
        placeholderRes: Int = R.drawable.ic_vintage_player_art_2,
        onLoaded: ((Bitmap?, String?) -> Unit)? = null
    ) {
        val context = imageView.context
        val fallbackRes = if (song.artworkResId != 0) song.artworkResId else placeholderRes

        // 1. Check Memory Cache
        val memBitmap = memoryCache.get(song.id)
        if (memBitmap != null) {
            imageView.setImageBitmap(memBitmap)
            val diskFile = getDiskFile(context, song.id)
            onLoaded?.invoke(memBitmap, if (diskFile.exists()) diskFile.absolutePath else null)
            return
        }

        // 2. Check Disk Cache
        val diskFile = getDiskFile(context, song.id)
        if (diskFile.exists() && diskFile.length() > 0) {
            try {
                val bitmap = BitmapFactory.decodeFile(diskFile.absolutePath)
                if (bitmap != null) {
                    memoryCache.put(song.id, bitmap)
                    imageView.setImageBitmap(bitmap)
                    onLoaded?.invoke(bitmap, diskFile.absolutePath)
                    return
                }
            } catch (e: Exception) {
                diskFile.delete()
            }
        }

        // Tag the ImageView to avoid race conditions during fast scrolling
        imageView.tag = song.id

        // 3. Check Remote artworkUrl
        if (song.artworkUrl.isNotBlank() && (song.artworkUrl.startsWith("http://") || song.artworkUrl.startsWith("https://"))) {
            Glide.with(context)
                .asBitmap()
                .load(song.artworkUrl)
                .placeholder(fallbackRes)
                .error(fallbackRes)
                .centerCrop()
                .listener(object : RequestListener<Bitmap> {
                    override fun onLoadFailed(
                        e: GlideException?,
                        model: Any?,
                        target: Target<Bitmap>,
                        isFirstResource: Boolean
                    ): Boolean {
                        // Fallback to extracting embedded picture from MP3
                        extractEmbeddedArtAsync(context, song) { bitmap, path ->
                            if (imageView.tag == song.id && bitmap != null) {
                                imageView.setImageBitmap(bitmap)
                            }
                            onLoaded?.invoke(bitmap, path)
                        }
                        return false
                    }

                    override fun onResourceReady(
                        resource: Bitmap,
                        model: Any,
                        target: Target<Bitmap>?,
                        dataSource: DataSource,
                        isFirstResource: Boolean
                    ): Boolean {
                        memoryCache.put(song.id, resource)
                        scope.launch {
                            try {
                                FileOutputStream(diskFile).use { out ->
                                    resource.compress(Bitmap.CompressFormat.JPEG, 90, out)
                                }
                            } catch (_: Exception) {}
                        }
                        onLoaded?.invoke(resource, diskFile.absolutePath)
                        return false
                    }
                })
                .into(imageView)
            return
        }

        // 4. Default placeholder while extracting embedded picture from mediaUrl
        imageView.setImageResource(fallbackRes)

        if (song.mediaUrl.isNotBlank()) {
            extractEmbeddedArtAsync(context, song) { bitmap, path ->
                if (imageView.tag == song.id && bitmap != null) {
                    imageView.setImageBitmap(bitmap)
                }
                onLoaded?.invoke(bitmap, path)
            }
        } else {
            onLoaded?.invoke(null, null)
        }
    }

    /**
     * Extracts embedded APIC cover art asynchronously via MediaMetadataRetriever
     */
    fun extractEmbeddedArtAsync(
        context: Context,
        song: Song,
        callback: (Bitmap?, String?) -> Unit
    ) {
        val diskFile = getDiskFile(context, song.id)

        // Memory check
        memoryCache.get(song.id)?.let {
            callback(it, if (diskFile.exists()) diskFile.absolutePath else null)
            return
        }

        // Disk check
        if (diskFile.exists() && diskFile.length() > 0) {
            scope.launch {
                val bitmap = try {
                    BitmapFactory.decodeFile(diskFile.absolutePath)
                } catch (e: Exception) {
                    null
                }
                if (bitmap != null) {
                    memoryCache.put(song.id, bitmap)
                    withContext(Dispatchers.Main) {
                        callback(bitmap, diskFile.absolutePath)
                    }
                } else {
                    diskFile.delete()
                    fetchFromRetriever(context, song, callback)
                }
            }
            return
        }

        fetchFromRetriever(context, song, callback)
    }

    private fun fetchFromRetriever(
        context: Context,
        song: Song,
        callback: (Bitmap?, String?) -> Unit
    ) {
        if (song.mediaUrl.isBlank()) {
            callback(null, null)
            return
        }

        scope.launch {
            val retriever = MediaMetadataRetriever()
            var extractedBitmap: Bitmap? = null
            var savedPath: String? = null
            val diskFile = getDiskFile(context, song.id)

            try {
                if (song.mediaUrl.startsWith("http://") || song.mediaUrl.startsWith("https://")) {
                    val headers = HashMap<String, String>().apply {
                        put("User-Agent", "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36")
                    }
                    retriever.setDataSource(song.mediaUrl, headers)
                } else {
                    retriever.setDataSource(song.mediaUrl)
                }

                val pictureBytes = retriever.embeddedPicture
                if (pictureBytes != null && pictureBytes.isNotEmpty()) {
                    extractedBitmap = BitmapFactory.decodeByteArray(pictureBytes, 0, pictureBytes.size)
                    if (extractedBitmap != null) {
                        try {
                            FileOutputStream(diskFile).use { out ->
                                out.write(pictureBytes)
                            }
                            savedPath = diskFile.absolutePath
                        } catch (_: Exception) {}
                        memoryCache.put(song.id, extractedBitmap)
                    }
                }
            } catch (e: Exception) {
                // Ignore extraction failures for corrupted or network-interrupted streams
            } finally {
                try {
                    retriever.release()
                } catch (_: Exception) {}
            }

            withContext(Dispatchers.Main) {
                callback(extractedBitmap, savedPath)
            }
        }
    }

    /**
     * Extracts full song metadata (Title, Artist, Album, Year, Art) from a given file/stream URL.
     */
    fun extractMetadata(
        context: Context,
        mediaUrl: String,
        callback: (title: String?, artist: String?, album: String?, year: String?, art: Bitmap?) -> Unit
    ) {
        scope.launch {
            val retriever = MediaMetadataRetriever()
            var title: String? = null
            var artist: String? = null
            var album: String? = null
            var year: String? = null
            var art: Bitmap? = null

            try {
                if (mediaUrl.startsWith("http://") || mediaUrl.startsWith("https://")) {
                    val headers = HashMap<String, String>().apply {
                        put("User-Agent", "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36")
                    }
                    retriever.setDataSource(mediaUrl, headers)
                } else {
                    retriever.setDataSource(mediaUrl)
                }

                title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
                artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
                album = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM)
                year = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_YEAR)
                    ?: retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DATE)

                val artBytes = retriever.embeddedPicture
                if (artBytes != null && artBytes.isNotEmpty()) {
                    art = BitmapFactory.decodeByteArray(artBytes, 0, artBytes.size)
                }
            } catch (_: Exception) {
            } finally {
                try {
                    retriever.release()
                } catch (_: Exception) {}
            }

            withContext(Dispatchers.Main) {
                callback(title, artist, album, year, art)
            }
        }
    }
}
