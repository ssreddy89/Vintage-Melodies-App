package com.vintagemelodies.app.player

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.vintagemelodies.app.MainActivity
import com.vintagemelodies.app.R
import com.vintagemelodies.app.data.model.Song

/**
 * Foreground Audio Playback Service.
 * Displays interactive media notification with controls (Play/Pause, Next, Previous).
 */
class MusicPlaybackService : Service() {

    companion object {
        const val CHANNEL_ID = "vintage_melodies_music_channel"
        const val NOTIFICATION_ID = 1001

        const val ACTION_PLAY_PAUSE = "com.vintagemelodies.app.action.PLAY_PAUSE"
        const val ACTION_NEXT = "com.vintagemelodies.app.action.NEXT"
        const val ACTION_PREV = "com.vintagemelodies.app.action.PREV"
        const val ACTION_STOP = "com.vintagemelodies.app.action.STOP"

        const val EXTRA_SONG_TITLE = "extra_song_title"
        const val EXTRA_SONG_ARTIST = "extra_song_artist"
        const val EXTRA_PLAYLIST_NAME = "extra_playlist_name"
        const val EXTRA_IS_PLAYING = "extra_is_playing"
        const val EXTRA_ART_RES = "extra_art_res"

        var serviceListener: ServiceActionListener? = null

        fun updateNotification(
            context: Context,
            song: Song?,
            playlistName: String,
            isPlaying: Boolean
        ) {
            val intent = Intent(context, MusicPlaybackService::class.java).apply {
                putExtra(EXTRA_SONG_TITLE, song?.title ?: "Vintage Melodies")
                putExtra(EXTRA_SONG_ARTIST, song?.artist ?: "Classic Music")
                putExtra(EXTRA_PLAYLIST_NAME, playlistName)
                putExtra(EXTRA_IS_PLAYING, isPlaying)
                putExtra(EXTRA_ART_RES, song?.artworkResId ?: R.drawable.ic_vintage_player_art_2)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stopService(context: Context) {
            context.stopService(Intent(context, MusicPlaybackService::class.java))
        }
    }

    interface ServiceActionListener {
        fun onNotificationPlayPause()
        fun onNotificationNext()
        fun onNotificationPrev()
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_PLAY_PAUSE -> serviceListener?.onNotificationPlayPause()
            ACTION_NEXT -> serviceListener?.onNotificationNext()
            ACTION_PREV -> serviceListener?.onNotificationPrev()
            ACTION_STOP -> {
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                return START_NOT_STICKY
            }
            else -> {
                val title = intent?.getStringExtra(EXTRA_SONG_TITLE) ?: "Vintage Melodies"
                val artist = intent?.getStringExtra(EXTRA_SONG_ARTIST) ?: "Classic Audio"
                val playlist = intent?.getStringExtra(EXTRA_PLAYLIST_NAME) ?: "Playlist"
                val isPlaying = intent?.getBooleanExtra(EXTRA_IS_PLAYING, false) ?: false
                val artRes = intent?.getIntExtra(EXTRA_ART_RES, R.drawable.ic_vintage_player_art_2) ?: R.drawable.ic_vintage_player_art_2

                val notification = buildNotification(title, artist, playlist, isPlaying, artRes)
                startForeground(NOTIFICATION_ID, notification)
            }
        }
        return START_NOT_STICKY
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Vintage Melodies Music Player",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows now playing music controls in notification bar"
                setShowBadge(false)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(
        title: String,
        artist: String,
        playlist: String,
        isPlaying: Boolean,
        artRes: Int
    ): Notification {
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentPendingIntent = PendingIntent.getActivity(
            this,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Previous Action
        val prevIntent = Intent(this, MusicPlaybackService::class.java).apply { action = ACTION_PREV }
        val prevPending = PendingIntent.getService(this, 1, prevIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        // Play/Pause Action
        val playPauseIntent = Intent(this, MusicPlaybackService::class.java).apply { action = ACTION_PLAY_PAUSE }
        val playPausePending = PendingIntent.getService(this, 2, playPauseIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        // Next Action
        val nextIntent = Intent(this, MusicPlaybackService::class.java).apply { action = ACTION_NEXT }
        val nextPending = PendingIntent.getService(this, 3, nextIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val largeArt = try {
            BitmapFactory.decodeResource(resources, if (artRes != 0) artRes else R.drawable.ic_vintage_player_art_2)
        } catch (e: Exception) {
            null
        }

        val playPauseIcon = if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play_arrow

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_music_note)
            .setLargeIcon(largeArt)
            .setContentTitle(title)
            .setContentText("$artist • $playlist")
            .setContentIntent(contentPendingIntent)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(isPlaying)
            .addAction(R.drawable.ic_skip_previous, "Previous", prevPending)
            .addAction(playPauseIcon, if (isPlaying) "Pause" else "Play", playPausePending)
            .addAction(R.drawable.ic_skip_next, "Next", nextPending)
            .setStyle(
                androidx.media.app.NotificationCompat.MediaStyle()
                    .setShowActionsInCompactView(0, 1, 2)
            )
            .build()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
