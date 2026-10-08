package com.vintagemelodies.app.player

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.BitmapFactory
import android.os.Build
import android.os.IBinder
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.core.app.NotificationCompat
import androidx.media.session.MediaButtonReceiver
import com.vintagemelodies.app.MainActivity
import com.vintagemelodies.app.R
import com.vintagemelodies.app.data.model.Song

/**
 * Foreground Audio Playback Service with MediaSessionCompat.
 * Powers Android 13/14+ System Quick Settings Media Player card,
 * Lock screen controls, and Bluetooth / Headset gestures (Play/Pause, Next, Prev).
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
        const val EXTRA_ART_PATH = "extra_art_path"

        var serviceListener: ServiceActionListener? = null
        var currentArtworkBitmap: android.graphics.Bitmap? = null

        fun updateNotification(
            context: Context,
            song: Song?,
            playlistName: String,
            isPlaying: Boolean,
            customBitmap: android.graphics.Bitmap? = null,
            artFilePath: String? = null
        ) {
            currentArtworkBitmap = customBitmap
            val intent = Intent(context, MusicPlaybackService::class.java).apply {
                putExtra(EXTRA_SONG_TITLE, song?.title ?: "Vintage Melodies")
                val subtitle = when {
                    song?.year?.isNotBlank() == true -> "${song.artist} • ${song.year}"
                    song?.album?.isNotBlank() == true && !song.album.startsWith("Folder:") && !song.album.startsWith("Cloudflare R2:") -> "${song.artist} • ${song.album}"
                    else -> song?.artist ?: "Classic Music"
                }
                putExtra(EXTRA_SONG_ARTIST, subtitle)
                putExtra(EXTRA_PLAYLIST_NAME, playlistName)
                putExtra(EXTRA_IS_PLAYING, isPlaying)
                putExtra(EXTRA_ART_RES, song?.artworkResId ?: R.drawable.ic_vintage_player_art_2)
                if (artFilePath != null) {
                    putExtra(EXTRA_ART_PATH, artFilePath)
                }
            }
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        fun stopService(context: Context) {
            try {
                context.stopService(Intent(context, MusicPlaybackService::class.java))
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    interface ServiceActionListener {
        fun onNotificationPlayPause()
        fun onNotificationNext()
        fun onNotificationPrev()
    }

    private var mediaSession: MediaSessionCompat? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        setupMediaSession()
    }

    private fun setupMediaSession() {
        mediaSession = MediaSessionCompat(this, "VintageMelodiesSession").apply {
            setFlags(
                MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS or
                MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS
            )

            setCallback(object : MediaSessionCompat.Callback() {
                override fun onPlay() {
                    serviceListener?.onNotificationPlayPause()
                }

                override fun onPause() {
                    serviceListener?.onNotificationPlayPause()
                }

                override fun onSkipToNext() {
                    serviceListener?.onNotificationNext()
                }

                override fun onSkipToPrevious() {
                    serviceListener?.onNotificationPrev()
                }

                override fun onStop() {
                    serviceListener?.onNotificationPlayPause()
                }

                override fun onMediaButtonEvent(mediaButtonEvent: Intent?): Boolean {
                    return super.onMediaButtonEvent(mediaButtonEvent)
                }
            })

            isActive = true
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent != null) {
            // Forward headset / Bluetooth media button clicks
            MediaButtonReceiver.handleIntent(mediaSession, intent)
        }

        when (intent?.action) {
            ACTION_PLAY_PAUSE -> serviceListener?.onNotificationPlayPause()
            ACTION_NEXT -> serviceListener?.onNotificationNext()
            ACTION_PREV -> serviceListener?.onNotificationPrev()
            ACTION_STOP -> {
                mediaSession?.isActive = false
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
                val artPath = intent?.getStringExtra(EXTRA_ART_PATH)

                val largeArt = currentArtworkBitmap
                    ?: (artPath?.let { path -> try { BitmapFactory.decodeFile(path) } catch (_: Exception) { null } })
                    ?: try {
                        BitmapFactory.decodeResource(resources, if (artRes != 0) artRes else R.drawable.ic_vintage_player_art_2)
                    } catch (e: Exception) {
                        null
                    }

                updateMediaSessionState(title, artist, playlist, isPlaying, largeArt)
                val notification = buildNotification(title, artist, playlist, isPlaying, largeArt)

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    startForeground(
                        NOTIFICATION_ID,
                        notification,
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
                    )
                } else {
                    startForeground(NOTIFICATION_ID, notification)
                }
            }
        }
        return START_NOT_STICKY
    }

    private fun updateMediaSessionState(
        title: String,
        artist: String,
        playlist: String,
        isPlaying: Boolean,
        largeArt: android.graphics.Bitmap?
    ) {
        val state = if (isPlaying) PlaybackStateCompat.STATE_PLAYING else PlaybackStateCompat.STATE_PAUSED
        val playbackState = PlaybackStateCompat.Builder()
            .setActions(
                PlaybackStateCompat.ACTION_PLAY or
                PlaybackStateCompat.ACTION_PAUSE or
                PlaybackStateCompat.ACTION_PLAY_PAUSE or
                PlaybackStateCompat.ACTION_SKIP_TO_NEXT or
                PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS or
                PlaybackStateCompat.ACTION_STOP
            )
            .setState(state, PlaybackStateCompat.PLAYBACK_POSITION_UNKNOWN, 1.0f)
            .build()

        mediaSession?.setPlaybackState(playbackState)

        val metadataBuilder = MediaMetadataCompat.Builder()
            .putString(MediaMetadataCompat.METADATA_KEY_TITLE, title)
            .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, artist)
            .putString(MediaMetadataCompat.METADATA_KEY_ALBUM, playlist)
            .putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_TITLE, title)
            .putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_SUBTITLE, artist)
            .putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_DESCRIPTION, playlist)

        if (largeArt != null) {
            metadataBuilder.putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, largeArt)
            metadataBuilder.putBitmap(MediaMetadataCompat.METADATA_KEY_ART, largeArt)
        }

        mediaSession?.setMetadata(metadataBuilder.build())
        mediaSession?.isActive = true
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
        largeArt: android.graphics.Bitmap?
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

        val playPauseIcon = if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play_arrow

        val style = androidx.media.app.NotificationCompat.MediaStyle()
            .setShowActionsInCompactView(0, 1, 2)

        mediaSession?.let {
            style.setMediaSession(it.sessionToken)
        }

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
            .setStyle(style)
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
        currentArtworkBitmap = null
        mediaSession?.apply {
            isActive = false
            release()
        }
        mediaSession = null
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
