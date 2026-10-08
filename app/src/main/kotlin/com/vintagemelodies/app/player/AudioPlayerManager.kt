package com.vintagemelodies.app.player

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import com.vintagemelodies.app.NetworkUtils
import com.vintagemelodies.app.data.model.Song

/**
 * Audio Player controller with progressive streaming from Cloudflare R2 CDN.
 * Fixes Error 38: Sanitizes single quotes/special characters in URLs, supplies standard
 * User-Agent headers, acquires WakeLock, and guarantees safe MediaPlayer state transitions.
 */
class AudioPlayerManager(private val context: Context) {

    private var mediaPlayer: MediaPlayer? = null
    private var currentPlaylist: List<Song> = emptyList()
    private var currentIndex: Int = -1
    private var isPreparing: Boolean = false

    var isShuffle: Boolean = false
    var isRepeatOne: Boolean = false

    interface PlaybackListener {
        fun onSongChanged(song: Song)
        fun onPlaybackStateChanged(isPlaying: Boolean)
        fun onProgressUpdate(currentMs: Int, totalMs: Int)
        fun onError(message: String)
    }

    private var listener: PlaybackListener? = null
    private val handler = Handler(Looper.getMainLooper())
    private val progressRunnable = object : Runnable {
        override fun run() {
            mediaPlayer?.let { player ->
                if (!isPreparing && player.isPlaying) {
                    listener?.onProgressUpdate(player.currentPosition, player.duration)
                }
            }
            handler.postDelayed(this, 500)
        }
    }

    fun setListener(listener: PlaybackListener) {
        this.listener = listener
    }

    fun prepareQueue(songs: List<Song>, startIndex: Int = 0) {
        if (songs.isEmpty()) return
        this.currentPlaylist = songs
        this.currentIndex = startIndex.coerceIn(0, songs.size - 1)
        val song = currentPlaylist[currentIndex]
        listener?.onSongChanged(song)
        listener?.onPlaybackStateChanged(false)
    }

    fun playQueue(songs: List<Song>, startIndex: Int = 0) {
        if (songs.isEmpty()) return
        this.currentPlaylist = songs
        this.currentIndex = startIndex.coerceIn(0, songs.size - 1)
        playCurrentSong()
    }

    fun playSong(song: Song) {
        this.currentPlaylist = listOf(song)
        this.currentIndex = 0
        playCurrentSong()
    }

    private fun sanitizeUrl(url: String): String {
        return url
            .replace("'", "%27")
            .replace(" ", "%20")
            .replace("[", "%5B")
            .replace("]", "%5D")
    }

    private fun playCurrentSong() {
        if (currentIndex !in currentPlaylist.indices) return
        val song = currentPlaylist[currentIndex]

        if (!NetworkUtils.isNetworkAvailable(context)) {
            listener?.onError("⚠️ No internet connection. Please connect to stream music.")
            listener?.onPlaybackStateChanged(false)
            return
        }

        cleanupPlayer()
        isPreparing = true

        try {
            val safeUrl = sanitizeUrl(song.mediaUrl)
            val headers = mapOf(
                "User-Agent" to "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36",
                "Accept" to "*/*"
            )

            mediaPlayer = MediaPlayer().apply {
                setWakeMode(context, PowerManager.PARTIAL_WAKE_LOCK)
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
                )
                setDataSource(context, Uri.parse(safeUrl), headers)
                setOnPreparedListener { mp ->
                    isPreparing = false
                    mp.start()
                    listener?.onSongChanged(song)
                    listener?.onPlaybackStateChanged(true)
                    handler.post(progressRunnable)
                }
                setOnCompletionListener {
                    handleCompletion()
                }
                setOnErrorListener { _, what, extra ->
                    isPreparing = false
                    val errorDesc = if (extra == -38 || what == 38 || what == 1) {
                        "Streaming issue. Please check your internet connection."
                    } else {
                        "Playback error ($what, $extra)"
                    }
                    listener?.onError(errorDesc)
                    cleanupPlayer()
                    false
                }
                prepareAsync() // Progressive streaming
            }
        } catch (e: Exception) {
            isPreparing = false
            listener?.onError("Unable to play '${song.title}': ${e.message}")
        }
    }

    private fun handleCompletion() {
        if (isRepeatOne) {
            playCurrentSong()
        } else {
            next()
        }
    }

    fun togglePlayPause() {
        if (mediaPlayer == null) {
            if (currentPlaylist.isNotEmpty() && currentIndex in currentPlaylist.indices) {
                playCurrentSong()
            }
            return
        }

        mediaPlayer?.let { player ->
            if (!isPreparing) {
                if (player.isPlaying) {
                    player.pause()
                    listener?.onPlaybackStateChanged(false)
                } else {
                    player.start()
                    listener?.onPlaybackStateChanged(true)
                }
            }
        }
    }

    fun next() {
        if (currentPlaylist.isEmpty()) return
        if (isShuffle && currentPlaylist.size > 1) {
            var nextIdx = (0 until currentPlaylist.size).random()
            if (nextIdx == currentIndex) nextIdx = (nextIdx + 1) % currentPlaylist.size
            currentIndex = nextIdx
        } else {
            currentIndex = (currentIndex + 1) % currentPlaylist.size
        }
        playCurrentSong()
    }

    fun previous() {
        if (currentPlaylist.isEmpty()) return
        currentIndex = if (currentIndex - 1 < 0) currentPlaylist.size - 1 else currentIndex - 1
        playCurrentSong()
    }

    fun seekTo(positionMs: Int) {
        if (!isPreparing) {
            mediaPlayer?.seekTo(positionMs)
        }
    }

    fun isPlaying(): Boolean = !isPreparing && mediaPlayer?.isPlaying == true

    fun getCurrentSong(): Song? {
        return if (currentIndex in currentPlaylist.indices) currentPlaylist[currentIndex] else null
    }

    fun getCurrentPosition(): Int = if (!isPreparing) mediaPlayer?.currentPosition ?: 0 else 0

    fun getCurrentIndex(): Int = currentIndex

    fun getCurrentPlaylist(): List<Song> = currentPlaylist

    private fun cleanupPlayer() {
        handler.removeCallbacks(progressRunnable)
        isPreparing = false
        try {
            mediaPlayer?.reset()
            mediaPlayer?.release()
        } catch (e: Exception) {
            // Ignore reset exceptions during teardown
        }
        mediaPlayer = null
    }

    fun stop() {
        cleanupPlayer()
        listener?.onPlaybackStateChanged(false)
    }
}
