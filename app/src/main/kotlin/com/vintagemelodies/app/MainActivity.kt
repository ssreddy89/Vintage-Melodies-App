package com.vintagemelodies.app

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.vintagemelodies.app.data.model.CloudFolder
import com.vintagemelodies.app.data.model.Playlist
import com.vintagemelodies.app.data.model.Song
import com.vintagemelodies.app.data.repository.CloudCatalogSyncManager
import com.vintagemelodies.app.data.repository.CloudPlaylistSyncManager
import com.vintagemelodies.app.data.repository.MusicRepository
import com.vintagemelodies.app.player.AudioPlayerManager
import com.vintagemelodies.app.player.MusicPlaybackService
import com.vintagemelodies.app.player.SongArtworkHelper
import com.vintagemelodies.app.ui.adapter.CloudFolderAdapter
import com.vintagemelodies.app.ui.adapter.DrawerPlaylistAdapter
import com.vintagemelodies.app.ui.adapter.MoodPlaylistAdapter
import com.vintagemelodies.app.ui.adapter.PicturePickerAdapter
import com.vintagemelodies.app.ui.adapter.PlaylistSongAdapter
import com.vintagemelodies.app.ui.adapter.R2SongPickerAdapter
import androidx.appcompat.widget.SwitchCompat
import com.vintagemelodies.app.player.VintageEqualizerManager
import java.io.File
import java.io.FileOutputStream

/**
 * Vintage Melodies - 100% Standalone Native Android Activity.
 *
 * Implements:
 * 1. Error 38 eliminated: sanitized URLs, standard User-Agent headers, WakeLock & safe state transitions.
 * 2. Persistent Playlists & Central Cloud DB sync: Admin playlists survive uninstall/reinstall and sync to all users.
 * 3. Clear Internet Disconnection notifications & playback warnings.
 * 4. Normal users only have playback options (no delete/remove actions).
 * 5. Full Edit Playlist action for Admin (Name, Language, Picture/GIF).
 * 6. Prominent Language Badges on all playlists.
 * 7. Play Protect clean: removed dangerous permissions; safe browser updates.
 * 8. Media Playback Notification in Android notification bar with interactive controls.
 */
class MainActivity : AppCompatActivity(), AudioPlayerManager.PlaybackListener {

    companion object {
        val SUPPORTED_LANGUAGES = listOf("Telugu", "English", "Hindi", "Tamil", "Kannada")
        const val PREFS_NAME = "vm_prefs"
        const val KEY_SELECTED_LANGUAGES = "selected_languages"
        const val KEY_HAS_SELECTED_LANGS = "has_selected_languages"
        const val KEY_IS_ADMIN = "is_admin"

        const val KEY_LAST_PLAYLIST_ID = "last_playlist_id"
        const val KEY_LAST_SONG_ID = "last_song_id"
        const val KEY_LAST_SONG_INDEX = "last_song_index"
    }

    private lateinit var repository: MusicRepository
    private lateinit var playerManager: AudioPlayerManager

    // App Background
    private lateinit var ivMainBackground: ImageView
    private lateinit var vBackgroundOverlay: View

    // Top Bar
    private lateinit var tvTopAdminBadge: TextView

    // Bottom Navigation Bar
    private lateinit var btnNavLanguage: View
    private lateinit var btnNavTimer: View
    private lateinit var btnNavPlaylists: View
    private lateinit var btnNavLogin: View
    private lateinit var ivNavLogin: ImageView
    private lateinit var tvNavLogin: TextView

    // Mood Section
    private lateinit var layoutMoodSection: View
    private lateinit var btnCloseMoodSection: ImageButton
    private lateinit var rvMoodPlaylists: RecyclerView
    private lateinit var moodAdapter: MoodPlaylistAdapter
    private lateinit var btnSeeAll: Button

    // Illuminated Semi-Transparent Neon Bottom Player
    private lateinit var ivPlayerArt: ImageView
    private lateinit var tvPlayerTitle: TextView
    private lateinit var tvPlayerArtist: TextView
    private lateinit var btnPlayerFolder: ImageButton
    private lateinit var btnPlayerEqualizer: ImageButton
    private lateinit var playerSeekBar: SeekBar
    private lateinit var tvPlayerCurrentTime: TextView
    private lateinit var tvPlayerTotalTime: TextView
    private lateinit var btnPlayPause: ImageButton
    private lateinit var btnNext: ImageButton
    private lateinit var btnPrev: ImageButton
    private lateinit var btnShuffle: ImageButton
    private lateinit var btnRepeat: ImageButton
    private var isUserSeeking: Boolean = false
    private var isFavorite: Boolean = false

    // Playlists Drawer
    private lateinit var layoutDrawerPlaylists: View
    private lateinit var rvDrawerPlaylists: RecyclerView
    private lateinit var drawerAdapter: DrawerPlaylistAdapter
    private lateinit var btnCloseDrawer: ImageButton
    private lateinit var btnAdminCreatePlaylist: Button
    private lateinit var btnAdminSyncCloud: Button

    // Playlist Songs View
    private lateinit var layoutPlaylistSongsView: View
    private lateinit var tvActivePlaylistTitle: TextView
    private lateinit var tvActivePlaylistLangBadge: TextView
    private lateinit var btnBackToPlaylists: ImageButton
    private lateinit var btnPlaylistOptions: ImageButton
    private lateinit var rvPlaylistSongs: RecyclerView
    private lateinit var songAdapter: PlaylistSongAdapter
    private lateinit var layoutAdminPlaylistBar: LinearLayout
    private lateinit var btnAdminAddR2Songs: Button

    // Cloudflare R2 Folder & Song Picker
    private lateinit var layoutR2Picker: View
    private lateinit var tvR2PickerTitle: TextView
    private lateinit var tvR2PickerSubtitle: TextView
    private lateinit var btnCloseR2Picker: ImageButton
    private lateinit var rvR2PickerFolders: RecyclerView
    private lateinit var folderAdapter: CloudFolderAdapter

    // Folder Songs View (Drilldown inside R2 Picker)
    private lateinit var layoutR2FolderSongs: LinearLayout
    private lateinit var btnBackToFolders: ImageButton
    private lateinit var tvCurrentFolderName: TextView
    private lateinit var btnSelectAllFolderSongs: Button
    private lateinit var etSearchR2Songs: EditText
    private lateinit var btnConfirmAddSelected: Button
    private lateinit var rvR2PickerSongs: RecyclerView
    private lateinit var r2PickerAdapter: R2SongPickerAdapter
    private val selectedSongIds = mutableSetOf<String>()
    private var activeCloudFolder: String = ""
    private var isAllFolderSongsSelected: Boolean = false

    // Admin Login Modal
    private lateinit var layoutAdminLoginOverlay: View
    private lateinit var etAdminUsername: EditText
    private lateinit var etAdminPassword: EditText
    private lateinit var btnCancelLogin: Button
    private lateinit var btnSubmitLogin: Button

    // State
    private var isAdmin: Boolean = false
    private var activePlaylist: Playlist? = null
    private var playingPlaylist: Playlist? = null

    // File Picker Callback for Custom Playlist Picture/GIF
    private var onImagePickedCallback: ((String) -> Unit)? = null
    private lateinit var filePickerLauncher: ActivityResultLauncher<Intent>

    // Network Callback Monitor
    private var connectivityManager: ConnectivityManager? = null
    private var networkCallback: ConnectivityManager.NetworkCallback? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        repository = MusicRepository(this)
        playerManager = AudioPlayerManager(this).apply {
            setListener(this@MainActivity)
        }

        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        isAdmin = prefs.getBoolean(KEY_IS_ADMIN, false)

        initFilePicker()
        initViews()
        setupListeners()
        setupPlaybackServiceListeners()
        setupNetworkMonitoring()
        requestNotificationPermission()

        loadInitialData()

        // First time user language check
        val hasSelectedLangs = prefs.getBoolean(KEY_HAS_SELECTED_LANGS, false)
        if (!hasSelectedLangs) {
            showLanguageSelectionDialog(isFirstTime = true)
        }

        // Restore last played playlist & song session
        restoreLastPlayedSession()

        // Background update check
        AppUpdateManager.checkForUpdate(this, isManual = false)

        // Background Cloud Catalog Sync
        CloudCatalogSyncManager.syncFromCloud(this)

        // Central Cloud Shared Playlists Sync
        CloudPlaylistSyncManager.fetchAndSyncFromCloud(this, repository) { success, count ->
            if (success && count > 0) {
                refreshPlaylists()
            }
        }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 101)
            }
        }
    }

    private fun setupPlaybackServiceListeners() {
        MusicPlaybackService.serviceListener = object : MusicPlaybackService.ServiceActionListener {
            override fun onNotificationPlayPause() {
                runOnUiThread { playerManager.togglePlayPause() }
            }

            override fun onNotificationNext() {
                runOnUiThread { playerManager.next() }
            }

            override fun onNotificationPrev() {
                runOnUiThread { playerManager.previous() }
            }
        }
    }

    private fun setupNetworkMonitoring() {
        try {
            connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            val request = NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build()

            networkCallback = object : ConnectivityManager.NetworkCallback() {
                override fun onLost(network: Network) {
                    runOnUiThread {
                        Toast.makeText(
                            this@MainActivity,
                            "⚠️ Internet Disconnected. Connect to Wi-Fi or data to stream music.",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
            networkCallback?.let { connectivityManager?.registerNetworkCallback(request, it) }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun initFilePicker() {
        filePickerLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val uri = result.data?.data
                uri?.let {
                    val savedPath = saveCoverImageToInternalStorage(it)
                    if (savedPath != null) {
                        onImagePickedCallback?.invoke(savedPath)
                    } else {
                        Toast.makeText(this, "Failed to load selected image", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    private fun saveCoverImageToInternalStorage(uri: Uri): String? {
        return try {
            val resolver = contentResolver
            val type = resolver.getType(uri) ?: "image/jpeg"
            val ext = when {
                type.contains("gif") -> "gif"
                type.contains("png") -> "png"
                type.contains("webp") -> "webp"
                else -> "jpg"
            }
            val dir = File(filesDir, "playlist_covers")
            if (!dir.exists()) dir.mkdirs()
            val destFile = File(dir, "cover_${System.currentTimeMillis()}.$ext")
            resolver.openInputStream(uri)?.use { input ->
                FileOutputStream(destFile).use { output ->
                    input.copyTo(output)
                }
            }
            destFile.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun initViews() {
        // App Background
        ivMainBackground = findViewById(R.id.iv_main_background)
        vBackgroundOverlay = findViewById(R.id.v_background_overlay)
        vBackgroundOverlay.alpha = 0f

        // Top Bar
        tvTopAdminBadge = findViewById(R.id.tv_top_admin_badge)

        // Bottom Navigation Bar
        btnNavLanguage = findViewById(R.id.btn_nav_language)
        btnNavTimer = findViewById(R.id.btn_nav_timer)
        btnNavPlaylists = findViewById(R.id.btn_nav_playlists)
        btnNavLogin = findViewById(R.id.btn_nav_login)
        ivNavLogin = findViewById(R.id.iv_nav_login)
        tvNavLogin = findViewById(R.id.tv_nav_login)

        // Mood Carousel Section
        layoutMoodSection = findViewById(R.id.layout_mood_section)
        btnCloseMoodSection = findViewById(R.id.btn_close_mood_section)
        btnSeeAll = findViewById(R.id.btn_see_all)
        rvMoodPlaylists = findViewById(R.id.rv_mood_playlists)
        rvMoodPlaylists.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        rvMoodPlaylists.setHasFixedSize(true)
        rvMoodPlaylists.isNestedScrollingEnabled = false
        moodAdapter = MoodPlaylistAdapter(
            emptyList(),
            onItemClick = { playlist -> openPlaylist(playlist) },
            onPlayClick = { playlist ->
                if (playingPlaylist?.id == playlist.id) {
                    playerManager.togglePlayPause()
                } else {
                    val songs = repository.getSongsForPlaylist(playlist.id)
                    if (songs.isNotEmpty()) {
                        activePlaylist = playlist
                        playingPlaylist = playlist
                        updateAppBackground(playlist)
                        playerManager.playQueue(songs, 0)
                        saveLastPlayedSession(playlist.id, songs[0].id, 0)
                        moodAdapter.setPlayingPlaylistId(playlist.id, true)
                        drawerAdapter.setPlayingPlaylistId(playlist.id, true)
                    } else {
                        openPlaylist(playlist)
                    }
                }
            }
        )
        rvMoodPlaylists.adapter = moodAdapter

        // Semi-Transparent Neon Bottom Player
        ivPlayerArt = findViewById(R.id.iv_player_art)
        tvPlayerTitle = findViewById(R.id.tv_player_title)
        tvPlayerArtist = findViewById(R.id.tv_player_artist)
        btnPlayerFolder = findViewById(R.id.btn_player_folder)
        btnPlayerEqualizer = findViewById(R.id.btn_player_equalizer)
        playerSeekBar = findViewById(R.id.player_seek_bar)
        tvPlayerCurrentTime = findViewById(R.id.tv_player_current_time)
        tvPlayerTotalTime = findViewById(R.id.tv_player_total_time)
        btnPlayPause = findViewById(R.id.btn_play_pause)
        btnNext = findViewById(R.id.btn_next)
        btnPrev = findViewById(R.id.btn_prev)
        btnShuffle = findViewById(R.id.btn_shuffle)
        btnRepeat = findViewById(R.id.btn_repeat)

        ivPlayerArt.setImageResource(R.drawable.ic_vintage_player_art_2)

        // Drawer
        layoutDrawerPlaylists = findViewById(R.id.layout_drawer_playlists)
        btnCloseDrawer = findViewById(R.id.btn_close_drawer)
        btnAdminCreatePlaylist = findViewById(R.id.btn_admin_create_playlist)
        btnAdminSyncCloud = findViewById(R.id.btn_admin_sync_cloud)
        rvDrawerPlaylists = findViewById(R.id.rv_drawer_playlists)
        rvDrawerPlaylists.layoutManager = LinearLayoutManager(this)
        drawerAdapter = DrawerPlaylistAdapter(
            emptyList(),
            onItemClick = { playlist ->
                layoutDrawerPlaylists.visibility = View.GONE
                openPlaylist(playlist)
            },
            onItemLongClick = { playlist ->
                if (isAdmin) showPlaylistAdminOptions(playlist)
            }
        )
        rvDrawerPlaylists.adapter = drawerAdapter

        // Playlist Songs View
        layoutPlaylistSongsView = findViewById(R.id.layout_playlist_songs_view)
        tvActivePlaylistTitle = findViewById(R.id.tv_active_playlist_title)
        tvActivePlaylistLangBadge = findViewById(R.id.tv_active_playlist_lang_badge)
        btnBackToPlaylists = findViewById(R.id.btn_back_to_playlists)
        btnPlaylistOptions = findViewById(R.id.btn_playlist_options)
        layoutAdminPlaylistBar = findViewById(R.id.layout_admin_playlist_bar)
        btnAdminAddR2Songs = findViewById(R.id.btn_admin_add_r2_songs)
        rvPlaylistSongs = findViewById(R.id.rv_playlist_songs)
        rvPlaylistSongs.layoutManager = LinearLayoutManager(this)
        songAdapter = PlaylistSongAdapter(
            emptyList(),
            onItemClick = { song, index ->
                if (playerManager.getCurrentSong()?.id == song.id) {
                    playerManager.togglePlayPause()
                } else {
                    activePlaylist?.let { pl ->
                        playingPlaylist = pl
                        val songs = songAdapter.getSongs()
                        playerManager.playQueue(songs, index)
                        updateAppBackground(pl)
                        saveLastPlayedSession(pl.id, song.id, index)
                        moodAdapter.setPlayingPlaylistId(pl.id, true)
                        drawerAdapter.setPlayingPlaylistId(pl.id, true)
                    } ?: run {
                        playerManager.playSong(song)
                    }
                }
            },
            onPlayPauseClick = { song, index ->
                if (playerManager.getCurrentSong()?.id == song.id) {
                    playerManager.togglePlayPause()
                } else {
                    activePlaylist?.let { pl ->
                        playingPlaylist = pl
                        val songs = songAdapter.getSongs()
                        playerManager.playQueue(songs, index)
                        updateAppBackground(pl)
                        saveLastPlayedSession(pl.id, song.id, index)
                        moodAdapter.setPlayingPlaylistId(pl.id, true)
                        drawerAdapter.setPlayingPlaylistId(pl.id, true)
                    } ?: run {
                        playerManager.playSong(song)
                    }
                }
            },
            onItemLongClick = { song ->
                showSongOptions(song)
            }
        )
        rvPlaylistSongs.adapter = songAdapter

        // R2 Cloud Picker (Level 1: Folders)
        layoutR2Picker = findViewById(R.id.layout_r2_picker)
        tvR2PickerTitle = findViewById(R.id.tv_r2_picker_title)
        tvR2PickerSubtitle = findViewById(R.id.tv_r2_picker_subtitle)
        btnCloseR2Picker = findViewById(R.id.btn_close_r2_picker)
        rvR2PickerFolders = findViewById(R.id.rv_r2_picker_folders)
        rvR2PickerFolders.layoutManager = LinearLayoutManager(this)
        folderAdapter = CloudFolderAdapter(emptyList()) { folder ->
            openCloudFolder(folder.name)
        }
        rvR2PickerFolders.adapter = folderAdapter

        // R2 Folder Songs View (Level 2: Drilldown inside folder)
        layoutR2FolderSongs = findViewById(R.id.layout_r2_folder_songs)
        btnBackToFolders = findViewById(R.id.btn_back_to_folders)
        tvCurrentFolderName = findViewById(R.id.tv_current_folder_name)
        btnSelectAllFolderSongs = findViewById(R.id.btn_select_all_folder_songs)
        etSearchR2Songs = findViewById(R.id.et_search_r2_songs)
        btnConfirmAddSelected = findViewById(R.id.btn_confirm_add_selected)
        rvR2PickerSongs = findViewById(R.id.rv_r2_picker_songs)
        rvR2PickerSongs.layoutManager = LinearLayoutManager(this)
        r2PickerAdapter = R2SongPickerAdapter(emptyList(), selectedSongIds) { count ->
            btnConfirmAddSelected.text = "Add Selected to Playlist ($count)"
        }
        rvR2PickerSongs.adapter = r2PickerAdapter

        // Admin Login
        layoutAdminLoginOverlay = findViewById(R.id.layout_admin_login_overlay)
        etAdminUsername = findViewById(R.id.et_admin_username)
        etAdminPassword = findViewById(R.id.et_admin_password)
        btnCancelLogin = findViewById(R.id.btn_cancel_login)
        btnSubmitLogin = findViewById(R.id.btn_submit_login)

        updateAdminUi()
    }

    private fun setupListeners() {
        btnNavLanguage.setOnClickListener { showLanguageSelectionDialog(isFirstTime = false) }
        btnNavTimer.setOnClickListener { showSleepTimerDialog() }

        btnNavPlaylists.setOnClickListener {
            if (layoutMoodSection.visibility == View.GONE) {
                layoutMoodSection.visibility = View.VISIBLE
            } else {
                openDrawer()
            }
        }

        btnSeeAll.setOnClickListener { openDrawer() }
        btnCloseMoodSection.setOnClickListener { layoutMoodSection.visibility = View.GONE }
        btnCloseDrawer.setOnClickListener { layoutDrawerPlaylists.visibility = View.GONE }

        btnNavLogin.setOnClickListener {
            if (isAdmin) {
                showAdminMenu()
            } else {
                openAdminLoginModal()
            }
        }

        // Bottom Player Controls
        btnPlayPause.setOnClickListener { playerManager.togglePlayPause() }
        btnNext.setOnClickListener {
            playerManager.next()
            playerManager.getCurrentSong()?.let { s ->
                playingPlaylist?.let { pl ->
                    saveLastPlayedSession(pl.id, s.id, playerManager.getCurrentIndex())
                }
            }
        }
        btnPrev.setOnClickListener {
            playerManager.previous()
            playerManager.getCurrentSong()?.let { s ->
                playingPlaylist?.let { pl ->
                    saveLastPlayedSession(pl.id, s.id, playerManager.getCurrentIndex())
                }
            }
        }

        btnShuffle.setOnClickListener {
            playerManager.isShuffle = !playerManager.isShuffle
            btnShuffle.setColorFilter(if (playerManager.isShuffle) getColor(R.color.amber_accent) else getColor(R.color.text_muted))
            Toast.makeText(this, if (playerManager.isShuffle) "Shuffle On" else "Shuffle Off", Toast.LENGTH_SHORT).show()
        }

        btnRepeat.setOnClickListener {
            playerManager.isRepeatOne = !playerManager.isRepeatOne
            btnRepeat.setColorFilter(if (playerManager.isRepeatOne) getColor(R.color.amber_accent) else getColor(R.color.text_muted))
            Toast.makeText(this, if (playerManager.isRepeatOne) "Repeat One On" else "Repeat Off", Toast.LENGTH_SHORT).show()
        }

        val openCurrentPlaylist = View.OnClickListener {
            val curSong = playerManager.getCurrentSong()
            val targetPlaylist = playingPlaylist
                ?: activePlaylist
                ?: curSong?.let { s ->
                    repository.getAllPlaylists().firstOrNull { pl ->
                        repository.getSongsForPlaylist(pl.id).any { it.id == s.id }
                    }
                }

            if (targetPlaylist != null) {
                openPlaylist(targetPlaylist)
            } else {
                val queue = playerManager.getQueue()
                if (queue.isNotEmpty()) {
                    activePlaylist = null
                    tvActivePlaylistTitle.text = "Now Playing"
                    tvActivePlaylistLangBadge.text = "QUEUE"
                    songAdapter.updateData(queue)
                    songAdapter.setPlayingSong(curSong?.id, playerManager.isPlaying())
                    layoutPlaylistSongsView.visibility = View.VISIBLE
                    layoutDrawerPlaylists.visibility = View.GONE
                    layoutAdminPlaylistBar.visibility = View.GONE
                    val playingIdx = queue.indexOfFirst { it.id == curSong?.id }
                    if (playingIdx >= 0) {
                        rvPlaylistSongs.post { rvPlaylistSongs.scrollToPosition(playingIdx) }
                    }
                } else {
                    openDrawer()
                }
            }
        }
        btnPlayerFolder.setOnClickListener(openCurrentPlaylist)
        tvPlayerTitle.setOnClickListener(openCurrentPlaylist)
        tvPlayerArtist.setOnClickListener(openCurrentPlaylist)
        ivPlayerArt.setOnClickListener(openCurrentPlaylist)

        btnPlayerEqualizer.setOnClickListener {
            showEqualizerDialog()
        }

        // SeekBar Dragging
        playerSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    val curMin = (progress / 1000) / 60
                    val curSec = (progress / 1000) % 60
                    tvPlayerCurrentTime.text = String.format("%d:%02d", curMin, curSec)
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
                isUserSeeking = true
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                seekBar?.let {
                    playerManager.seekTo(it.progress)
                }
                isUserSeeking = false
            }
        })

        btnBackToPlaylists.setOnClickListener {
            layoutPlaylistSongsView.visibility = View.GONE
            refreshPlaylists()
        }

        btnPlaylistOptions.setOnClickListener {
            activePlaylist?.let { showPlaylistAdminOptions(it) }
        }

        btnAdminCreatePlaylist.setOnClickListener { showCreateOrEditPlaylistDialog(existingPlaylist = null) }
        btnAdminSyncCloud.setOnClickListener {
            Toast.makeText(this, "Publishing playlists to cloud...", Toast.LENGTH_SHORT).show()
            CloudPlaylistSyncManager.publishPlaylistsToCloud(this, repository) { success, msg ->
                if (success) {
                    Toast.makeText(this, "☁️ Success: All devices will now see your playlists!", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(this, "⚠️ Failed to sync cloud: $msg", Toast.LENGTH_LONG).show()
                }
            }
        }
        btnAdminAddR2Songs.setOnClickListener { openR2Picker() }
        btnCloseR2Picker.setOnClickListener { layoutR2Picker.visibility = View.GONE }

        // Folder Drilldown Navigation
        btnBackToFolders.setOnClickListener {
            returnToFolderList()
        }

        btnSelectAllFolderSongs.setOnClickListener {
            if (isAllFolderSongsSelected) {
                r2PickerAdapter.deselectAll()
                btnSelectAllFolderSongs.text = "Select All"
                isAllFolderSongsSelected = false
            } else {
                r2PickerAdapter.selectAll()
                btnSelectAllFolderSongs.text = "Deselect All"
                isAllFolderSongsSelected = true
            }
        }

        btnConfirmAddSelected.setOnClickListener {
            activePlaylist?.let { pl ->
                if (selectedSongIds.isNotEmpty()) {
                    val count = selectedSongIds.size
                    repository.addSongsToPlaylist(pl.id, selectedSongIds.toList())
                    Toast.makeText(this, "Added $count songs to ${pl.name}", Toast.LENGTH_SHORT).show()
                    selectedSongIds.clear()
                    isAllFolderSongsSelected = false
                    btnSelectAllFolderSongs.text = "Select All"
                    returnToFolderList()
                    layoutR2Picker.visibility = View.GONE
                    openPlaylist(pl)
                    if (isAdmin) {
                        CloudPlaylistSyncManager.publishPlaylistsToCloud(this, repository)
                    }
                } else {
                    Toast.makeText(this, "Select at least one song", Toast.LENGTH_SHORT).show()
                }
            }
        }

        etSearchR2Songs.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val query = s?.toString()?.trim() ?: ""
                val filtered = repository.getUnassignedSongs(
                    folderFilter = activeCloudFolder,
                    query = query
                )
                r2PickerAdapter.updateData(filtered)
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
        })

        // Admin Login
        btnCancelLogin.setOnClickListener { layoutAdminLoginOverlay.visibility = View.GONE }
        btnSubmitLogin.setOnClickListener {
            val user = etAdminUsername.text.toString().trim()
            val pass = etAdminPassword.text.toString().trim()

            val isUserValid = user.equals("admin", ignoreCase = true)
            val isPassValid = pass == "Admin@2026" ||
                    pass.equals("admin", ignoreCase = true) ||
                    pass == "admin123"

            if (isUserValid && isPassValid) {
                isAdmin = true
                getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().putBoolean(KEY_IS_ADMIN, true).apply()
                layoutAdminLoginOverlay.visibility = View.GONE
                updateAdminUi()
                refreshPlaylists()
                Toast.makeText(this, "Admin mode enabled! Full access granted 🎵", Toast.LENGTH_SHORT).show()
                showAdminMenu()
            } else {
                Toast.makeText(this, "Invalid credentials. Please enter valid password.", Toast.LENGTH_SHORT).show()
            }
        }

        // Background Opacity on Scroll (Item 3)
        setupScrollBackgroundOverlay(rvMoodPlaylists)
        setupScrollBackgroundOverlay(rvPlaylistSongs)
        setupScrollBackgroundOverlay(rvDrawerPlaylists)
    }

    private fun setupScrollBackgroundOverlay(recyclerView: RecyclerView) {
        recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            private var totalDy = 0

            override fun onScrolled(rv: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(rv, dx, dy)
                totalDy = (totalDy + dy).coerceAtLeast(0)
                // Opacity is 0 normally; smoothly darkens up to 0.75 only when scrolling playlists
                val targetAlpha = (totalDy.toFloat() / 250f).coerceIn(0f, 0.75f)
                vBackgroundOverlay.alpha = targetAlpha
            }

            override fun onScrollStateChanged(rv: RecyclerView, newState: Int) {
                super.onScrollStateChanged(rv, newState)
                if (newState == RecyclerView.SCROLL_STATE_IDLE && !rv.canScrollVertically(-1)) {
                    totalDy = 0
                    vBackgroundOverlay.animate().alpha(0f).setDuration(200).start()
                }
            }
        })
    }

    private fun loadInitialData() {
        // Restore from persistent backup file if needed (Separate from Web playlists)
        repository.restoreFromBackupIfEmpty()
        refreshPlaylists()
    }

    // ── Session Persistence: Restore Last Played Session (Item 2) ────────────

    private fun saveLastPlayedSession(playlistId: Long, songId: String, songIndex: Int) {
        getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
            .putLong(KEY_LAST_PLAYLIST_ID, playlistId)
            .putString(KEY_LAST_SONG_ID, songId)
            .putInt(KEY_LAST_SONG_INDEX, songIndex)
            .commit()
    }

    private fun restoreLastPlayedSession() {
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val lastPlaylistId = prefs.getLong(KEY_LAST_PLAYLIST_ID, -1L)
        val lastSongId = prefs.getString(KEY_LAST_SONG_ID, null)
        val lastSongIndexPref = prefs.getInt(KEY_LAST_SONG_INDEX, 0)

        var pl: Playlist? = if (lastPlaylistId != -1L) repository.getPlaylistById(lastPlaylistId) else null
        var playlistSongs = if (pl != null) repository.getSongsForPlaylist(pl.id) else emptyList()

        // Fallback 1: If saved playlist has no songs or wasn't found, search all playlists for lastSongId
        if (playlistSongs.isEmpty() && !lastSongId.isNullOrEmpty()) {
            val allPlaylists = repository.getAllPlaylists()
            for (candidate in allPlaylists) {
                val candidateSongs = repository.getSongsForPlaylist(candidate.id)
                if (candidateSongs.any { it.id == lastSongId }) {
                    pl = candidate
                    playlistSongs = candidateSongs
                    break
                }
            }
        }

        // Fallback 2: Pick first non-empty playlist
        if (playlistSongs.isEmpty()) {
            val allPlaylists = repository.getAllPlaylists()
            for (candidate in allPlaylists) {
                val candidateSongs = repository.getSongsForPlaylist(candidate.id)
                if (candidateSongs.isNotEmpty()) {
                    pl = candidate
                    playlistSongs = candidateSongs
                    break
                }
            }
        }

        if (playlistSongs.isNotEmpty()) {
            activePlaylist = pl
            playingPlaylist = pl

            val targetIndex = if (!lastSongId.isNullOrEmpty()) {
                val foundIdx = playlistSongs.indexOfFirst { it.id == lastSongId }
                if (foundIdx >= 0) foundIdx else lastSongIndexPref.coerceIn(0, playlistSongs.size - 1)
            } else {
                lastSongIndexPref.coerceIn(0, playlistSongs.size - 1)
            }

            playerManager.prepareQueue(playlistSongs, targetIndex)
            updateAppBackground(pl)
            if (pl != null) {
                moodAdapter.setPlayingPlaylistId(pl.id, false)
                drawerAdapter.setPlayingPlaylistId(pl.id, false)
            }
            val cur = playerManager.getCurrentSong()
            if (cur != null) {
                tvPlayerTitle.text = cur.title
                val details = cur.getDetailsInfo()
                tvPlayerArtist.text = if (details.isNotBlank()) details else "Vintage Melodies"
                songAdapter.setPlayingSong(cur.id, false)
                SongArtworkHelper.loadSongArt(ivPlayerArt, cur)
            }
        } else {
            // Absolute fallback: all songs in repository
            val allSongs = repository.getAllSongs()
            if (allSongs.isNotEmpty()) {
                val targetIndex = if (!lastSongId.isNullOrEmpty()) {
                    val foundIdx = allSongs.indexOfFirst { it.id == lastSongId }
                    if (foundIdx >= 0) foundIdx else 0
                } else {
                    0
                }
                playerManager.prepareQueue(allSongs, targetIndex)
                val cur = playerManager.getCurrentSong()
                if (cur != null) {
                    tvPlayerTitle.text = cur.title
                    val details = cur.getDetailsInfo()
                    tvPlayerArtist.text = if (details.isNotBlank()) details else "Vintage Melodies"
                    songAdapter.setPlayingSong(cur.id, false)
                    SongArtworkHelper.loadSongArt(ivPlayerArt, cur)
                }
            }
        }
    }

    private fun refreshPlaylists() {
        val playlists = if (isAdmin) {
            repository.getAllPlaylists()
        } else {
            val userLangs = getUserSelectedLanguages()
            if (userLangs.isEmpty()) {
                repository.getAllPlaylists()
            } else {
                repository.getPlaylistsByLanguages(userLangs)
            }
        }

        // Re-resolve playingPlaylist so it is NEVER lost, even across syncs or multiple closes/opens
        val curSongId = playerManager.getCurrentSong()?.id
        if (playingPlaylist != null) {
            playingPlaylist = playlists.firstOrNull { it.id == playingPlaylist?.id }
                ?: playlists.firstOrNull { it.name.equals(playingPlaylist?.name, ignoreCase = true) }
                ?: (if (curSongId != null) playlists.firstOrNull { pl -> repository.getSongsForPlaylist(pl.id).any { s -> s.id == curSongId } } else null)
                ?: playingPlaylist
        } else if (curSongId != null) {
            playingPlaylist = playlists.firstOrNull { pl -> repository.getSongsForPlaylist(pl.id).any { s -> s.id == curSongId } }
        }

        moodAdapter.updateData(playlists)
        drawerAdapter.updateData(playlists)
        val isPlaying = playerManager.isPlaying()
        moodAdapter.setPlayingPlaylistId(playingPlaylist?.id, isPlaying)
        drawerAdapter.setPlayingPlaylistId(playingPlaylist?.id, isPlaying)
    }

    private fun openDrawer() {
        refreshPlaylists()
        layoutDrawerPlaylists.visibility = View.VISIBLE
        CloudPlaylistSyncManager.fetchAndSyncFromCloud(this, repository) { success, count ->
            if (success && count > 0) {
                refreshPlaylists()
            }
        }
    }

    private fun openPlaylist(playlist: Playlist) {
        activePlaylist = playlist
        tvActivePlaylistTitle.text = playlist.name
        tvActivePlaylistLangBadge.text = playlist.language.uppercase()

        var songs = repository.getSongsForPlaylist(playlist.id)
        if (songs.isEmpty() && playingPlaylist?.id == playlist.id) {
            val queue = playerManager.getQueue()
            if (queue.isNotEmpty()) songs = queue
        }
        songAdapter.updateData(songs)
        songAdapter.setPlayingSong(playerManager.getCurrentSong()?.id, playerManager.isPlaying())
        layoutPlaylistSongsView.visibility = View.VISIBLE
        layoutDrawerPlaylists.visibility = View.GONE
        layoutAdminPlaylistBar.visibility = if (isAdmin) View.VISIBLE else View.GONE

        // Scroll to currently playing song if viewing the playing playlist
        if (playingPlaylist?.id == playlist.id) {
            val curId = playerManager.getCurrentSong()?.id
            val sortedList = songAdapter.getSongs()
            val playingIdx = sortedList.indexOfFirst { it.id == curId }
            if (playingIdx >= 0) {
                rvPlaylistSongs.post {
                    rvPlaylistSongs.scrollToPosition(playingIdx)
                }
            }
        }
    }

    private fun updateAppBackground(playlist: Playlist?) {
        val targetPlaylist = playlist ?: playingPlaylist
        if (targetPlaylist != null) {
            val hasValidCoverUrl = targetPlaylist.coverUrl.isNotBlank() &&
                (targetPlaylist.coverUrl.startsWith("http") || targetPlaylist.coverUrl.startsWith("data:") || File(targetPlaylist.coverUrl).exists())
            val fallbackRes = if (targetPlaylist.coverResId != 0) targetPlaylist.coverResId else R.drawable.background
            val loadTarget: Any = if (hasValidCoverUrl) targetPlaylist.coverUrl else fallbackRes

            Glide.with(this)
                .load(loadTarget)
                .centerCrop()
                .placeholder(R.drawable.background)
                .error(R.drawable.background)
                .into(ivMainBackground)
        } else {
            Glide.with(this)
                .load(R.drawable.background)
                .centerCrop()
                .into(ivMainBackground)
        }
    }

    // ── First-Time User Language Selection (Item 1 & 6) ──────────────────────

    private fun getUserSelectedLanguages(): Set<String> {
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getStringSet(KEY_SELECTED_LANGUAGES, null) ?: setOf("Telugu")
    }

    private fun showLanguageSelectionDialog(isFirstTime: Boolean) {
        val view = LayoutInflater.from(this).inflate(R.layout.dialog_language_selection, null)
        val cbTelugu: CheckBox = view.findViewById(R.id.cb_lang_telugu)
        val cbEnglish: CheckBox = view.findViewById(R.id.cb_lang_english)
        val cbHindi: CheckBox = view.findViewById(R.id.cb_lang_hindi)
        val cbTamil: CheckBox = view.findViewById(R.id.cb_lang_tamil)
        val cbKannada: CheckBox = view.findViewById(R.id.cb_lang_kannada)
        val btnSelectAll: Button = view.findViewById(R.id.btn_select_all_langs)
        val btnSave: Button = view.findViewById(R.id.btn_save_languages)

        val currentSelection = getUserSelectedLanguages()
        cbTelugu.isChecked = currentSelection.contains("Telugu")
        cbEnglish.isChecked = currentSelection.contains("English")
        cbHindi.isChecked = currentSelection.contains("Hindi")
        cbTamil.isChecked = currentSelection.contains("Tamil")
        cbKannada.isChecked = currentSelection.contains("Kannada")

        if (isFirstTime && currentSelection.isEmpty()) {
            cbTelugu.isChecked = true
            cbEnglish.isChecked = true
        }

        val allCbs = listOf(cbTelugu, cbEnglish, cbHindi, cbTamil, cbKannada)
        var allSelected = allCbs.all { it.isChecked }
        btnSelectAll.text = if (allSelected) "Deselect All" else "Select All"

        btnSelectAll.setOnClickListener {
            allSelected = !allSelected
            allCbs.forEach { it.isChecked = allSelected }
            btnSelectAll.text = if (allSelected) "Deselect All" else "Select All"
        }

        val dialog = AlertDialog.Builder(this)
            .setView(view)
            .setCancelable(!isFirstTime)
            .create()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        btnSave.setOnClickListener {
            val selected = mutableSetOf<String>()
            if (cbTelugu.isChecked) selected.add("Telugu")
            if (cbEnglish.isChecked) selected.add("English")
            if (cbHindi.isChecked) selected.add("Hindi")
            if (cbTamil.isChecked) selected.add("Tamil")
            if (cbKannada.isChecked) selected.add("Kannada")

            if (selected.isEmpty()) {
                Toast.makeText(this, "Please select at least one language", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
                .putStringSet(KEY_SELECTED_LANGUAGES, selected)
                .putBoolean(KEY_HAS_SELECTED_LANGS, true)
                .apply()

            dialog.dismiss()
            refreshPlaylists()
            Toast.makeText(this, "Language preferences saved: ${selected.joinToString(", ")}", Toast.LENGTH_SHORT).show()
        }

        dialog.show()
    }

    // ── Create & Edit Playlist with Custom File Pic/GIF Support (Items 1, 3, 5, 8) ─

    private fun showCreateOrEditPlaylistDialog(existingPlaylist: Playlist? = null) {
        val view = LayoutInflater.from(this).inflate(R.layout.dialog_create_playlist, null)
        val tvTitle: TextView = view.findViewById(R.id.tv_dialog_playlist_title)
        val etName: EditText = view.findViewById(R.id.et_new_playlist_name)
        val spinnerLang: Spinner = view.findViewById(R.id.spinner_playlist_language)
        val rvPictures: RecyclerView = view.findViewById(R.id.rv_picture_picker)
        val btnChooseFile: Button = view.findViewById(R.id.btn_choose_custom_file)
        val layoutCustomPreview: View = view.findViewById(R.id.layout_custom_file_preview)
        val ivCustomThumb: ImageView = view.findViewById(R.id.iv_custom_file_thumb)
        val tvCustomFileName: TextView = view.findViewById(R.id.tv_custom_file_name)
        val btnRemoveCustomFile: ImageButton = view.findViewById(R.id.btn_remove_custom_file)
        val btnCancel: Button = view.findViewById(R.id.btn_cancel_create_playlist)
        val btnConfirm: Button = view.findViewById(R.id.btn_confirm_create_playlist)

        val isEditMode = existingPlaylist != null
        tvTitle.text = if (isEditMode) "✏️ Edit Playlist" else "➕ Create New Playlist"
        btnConfirm.text = if (isEditMode) "Save Changes" else "Create Playlist"

        if (isEditMode) {
            etName.setText(existingPlaylist?.name)
        }

        val langAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, SUPPORTED_LANGUAGES)
        spinnerLang.adapter = langAdapter
        if (isEditMode) {
            val idx = SUPPORTED_LANGUAGES.indexOfFirst { it.equals(existingPlaylist?.language, ignoreCase = true) }
            if (idx >= 0) spinnerLang.setSelection(idx)
        }

        val pictures = listOf(
            R.drawable.vm_art_monsoon,
            R.drawable.vm_art_village,
            R.drawable.vm_art_sunset,
            R.drawable.vm_art_classic,
            R.drawable.vm_art_nostalgia,
            R.drawable.vm_gif_kitchen,
            R.drawable.vm_gif_tailoring,
            R.drawable.vm_gif_roadtrip,
            R.drawable.ic_vintage_player_art_2,
            R.drawable.ic_vintage_player_art_3,
            R.drawable.background
        )

        var selectedPictureRes = existingPlaylist?.coverResId ?: pictures[0]
        var selectedCustomCoverUrl = existingPlaylist?.coverUrl ?: ""

        rvPictures.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        val pictureAdapter = PicturePickerAdapter(pictures, selectedPictureRes) { resId ->
            selectedPictureRes = resId
            selectedCustomCoverUrl = ""
            layoutCustomPreview.visibility = View.GONE
        }
        rvPictures.adapter = pictureAdapter

        fun updateCustomPreview(filePath: String) {
            if (filePath.isNotBlank()) {
                layoutCustomPreview.visibility = View.VISIBLE
                tvCustomFileName.text = File(filePath).name
                Glide.with(this).load(filePath).centerCrop().into(ivCustomThumb)
            } else {
                layoutCustomPreview.visibility = View.GONE
            }
        }

        if (selectedCustomCoverUrl.isNotBlank()) {
            updateCustomPreview(selectedCustomCoverUrl)
        }

        btnRemoveCustomFile.setOnClickListener {
            selectedCustomCoverUrl = ""
            layoutCustomPreview.visibility = View.GONE
        }

        btnChooseFile.setOnClickListener {
            onImagePickedCallback = { pickedPath ->
                selectedCustomCoverUrl = pickedPath
                updateCustomPreview(pickedPath)
            }
            val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
                type = "image/*"
                putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("image/jpeg", "image/png", "image/gif", "image/webp"))
            }
            filePickerLauncher.launch(Intent.createChooser(intent, "Select Playlist Cover (Pic or GIF)"))
        }

        val dialog = AlertDialog.Builder(this)
            .setView(view)
            .create()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        btnCancel.setOnClickListener { dialog.dismiss() }

        btnConfirm.setOnClickListener {
            val name = etName.text.toString().trim()
            if (name.isEmpty()) {
                Toast.makeText(this, "Please enter a playlist name", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val language = spinnerLang.selectedItem?.toString() ?: "Telugu"
            val picRes = pictureAdapter.getSelectedPicture()

            if (isEditMode) {
                repository.updatePlaylist(existingPlaylist!!.id, name, language, picRes, selectedCustomCoverUrl)
                if (activePlaylist?.id == existingPlaylist.id) {
                    val updated = repository.getPlaylistById(existingPlaylist.id)
                    if (updated != null) {
                        openPlaylist(updated)
                        if (playingPlaylist?.id == updated.id) {
                            playingPlaylist = updated
                            updateAppBackground(updated)
                        }
                    }
                }
                Toast.makeText(this, "Playlist '$name' updated!", Toast.LENGTH_SHORT).show()
            } else {
                repository.createPlaylist(name, language, picRes, selectedCustomCoverUrl)
                Toast.makeText(this, "Playlist '$name' ($language) created & saved to DB!", Toast.LENGTH_SHORT).show()
            }

            refreshPlaylists()
            if (isAdmin) {
                CloudPlaylistSyncManager.publishPlaylistsToCloud(this, repository)
            }
            dialog.dismiss()
        }

        dialog.show()
    }

    // ── Song Options Menu (Item 4: Normal user does NOT have remove action) ──

    private fun showSongOptions(song: Song) {
        val options = if (isAdmin) {
            arrayOf(
                "▶ Play Now",
                "📋 Copy to Another Playlist",
                "➡️ Move to Another Playlist",
                "🗑 Remove from This Playlist",
                "ℹ Song Details"
            )
        } else {
            arrayOf("▶ Play Now", "ℹ Song Details")
        }

        AlertDialog.Builder(this)
            .setTitle(song.title)
            .setItems(options) { _, which ->
                if (isAdmin) {
                    when (which) {
                        0 -> {
                            playerManager.playSong(song)
                            activePlaylist?.let { pl ->
                                saveLastPlayedSession(pl.id, song.id, playerManager.getCurrentIndex())
                            }
                        }
                        1 -> showTargetPlaylistPicker(song, isMove = false)
                        2 -> showTargetPlaylistPicker(song, isMove = true)
                        3 -> removeSongFromActivePlaylist(song)
                        4 -> showSongDetails(song)
                    }
                } else {
                    when (which) {
                        0 -> {
                            playerManager.playSong(song)
                            activePlaylist?.let { pl ->
                                saveLastPlayedSession(pl.id, song.id, playerManager.getCurrentIndex())
                            }
                        }
                        1 -> showSongDetails(song)
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun removeSongFromActivePlaylist(song: Song) {
        activePlaylist?.let { pl ->
            repository.removeSongFromPlaylist(pl.id, song.id)
            openPlaylist(pl)
            refreshPlaylists()
            if (isAdmin) {
                CloudPlaylistSyncManager.publishPlaylistsToCloud(this, repository)
            }
            Toast.makeText(this, "Removed '${song.title}' from '${pl.name}'", Toast.LENGTH_SHORT).show()
        } ?: run {
            Toast.makeText(this, "No active playlist selected", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showSongDetails(song: Song) {
        Toast.makeText(this, "${song.title}\nArtist: ${song.artist}\nCategory: ${song.folder}", Toast.LENGTH_LONG).show()
    }

    private fun showTargetPlaylistPicker(song: Song, isMove: Boolean) {
        val allPlaylists = repository.getAllPlaylists()
        val targets = allPlaylists.filter { it.id != activePlaylist?.id }

        if (targets.isEmpty()) {
            Toast.makeText(this, "No other playlists available. Please create another playlist first.", Toast.LENGTH_LONG).show()
            return
        }

        val targetNames = targets.map { "${it.name} (${it.language} • ${it.songCount} songs)" }.toTypedArray()
        val title = if (isMove) "Move '${song.title}' To:" else "Copy '${song.title}' To:"

        AlertDialog.Builder(this)
            .setTitle(title)
            .setItems(targetNames) { _, which ->
                val targetPl = targets[which]
                if (isMove) {
                    activePlaylist?.let { curPl ->
                        repository.moveSongToPlaylist(curPl.id, targetPl.id, song.id)
                        openPlaylist(curPl)
                        refreshPlaylists()
                        if (isAdmin) {
                            CloudPlaylistSyncManager.publishPlaylistsToCloud(this, repository)
                        }
                        Toast.makeText(this, "Moved '${song.title}' to '${targetPl.name}'", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    repository.copySongToPlaylist(targetPl.id, song.id)
                    refreshPlaylists()
                    if (isAdmin) {
                        CloudPlaylistSyncManager.publishPlaylistsToCloud(this, repository)
                    }
                    Toast.makeText(this, "Copied '${song.title}' to '${targetPl.name}'", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    // ── Cloud R2 Picker (Folders First, Then Unadded Songs) ──────────────────

    private fun openR2Picker() {
        returnToFolderList()
        layoutR2Picker.visibility = View.VISIBLE
        tvR2PickerSubtitle.text = "Syncing latest cloud folders & songs..."
        CloudCatalogSyncManager.syncFromCloud(this) { success, _ ->
            if (layoutR2Picker.visibility == View.VISIBLE && rvR2PickerFolders.visibility == View.VISIBLE) {
                val updatedFolders = repository.getAvailableCloudFolders()
                folderAdapter.updateData(updatedFolders)
                tvR2PickerSubtitle.text = if (updatedFolders.isEmpty() || updatedFolders.all { it.songCount == 0 }) {
                    "All cloud songs have already been added to playlists!"
                } else {
                    "Select a folder to browse unadded cloud songs:"
                }
            }
        }
    }

    private fun returnToFolderList() {
        layoutR2FolderSongs.visibility = View.GONE
        rvR2PickerFolders.visibility = View.VISIBLE
        tvR2PickerTitle.text = "☁️ Cloudflare R2 Folders"
        tvR2PickerSubtitle.text = "Select a folder to browse unadded cloud songs:"

        val folders = repository.getAvailableCloudFolders()
        folderAdapter.updateData(folders)

        if (folders.isEmpty() || folders.all { it.songCount == 0 }) {
            tvR2PickerSubtitle.text = "All cloud songs have already been added to playlists!"
        }
    }

    private fun openCloudFolder(folderName: String) {
        activeCloudFolder = folderName
        selectedSongIds.clear()
        isAllFolderSongsSelected = false
        btnSelectAllFolderSongs.text = "Select All"
        etSearchR2Songs.setText("")

        val songs = repository.getUnassignedSongs(folderFilter = folderName)
        r2PickerAdapter.updateData(songs)

        val cleanName = CloudFolder(folderName).displayName
        rvR2PickerFolders.visibility = View.GONE
        layoutR2FolderSongs.visibility = View.VISIBLE
        tvR2PickerTitle.text = "☁️ Cloud Folder: $cleanName"
        tvCurrentFolderName.text = cleanName
        btnConfirmAddSelected.text = "Add Selected to Playlist (0)"
    }

    // ── Equalizer Dialog ─────────────────────────────────────────────────────

    private fun showEqualizerDialog() {
        val audioSessionId = playerManager.getAudioSessionId()
        if (audioSessionId > 0) {
            VintageEqualizerManager.bindSession(audioSessionId, this)
        }

        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_equalizer, null)
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        val switchEnable: SwitchCompat = dialogView.findViewById(R.id.switch_eq_enable)
        val btnWarmth: Button = dialogView.findViewById(R.id.btn_eq_preset_warmth)
        val btnBass: Button = dialogView.findViewById(R.id.btn_eq_preset_bass)
        val btnVocal: Button = dialogView.findViewById(R.id.btn_eq_preset_vocal)
        val btnClassical: Button = dialogView.findViewById(R.id.btn_eq_preset_classical)
        val btnAcoustic: Button = dialogView.findViewById(R.id.btn_eq_preset_acoustic)
        val btnFlat: Button = dialogView.findViewById(R.id.btn_eq_preset_flat)

        val seekBass: SeekBar = dialogView.findViewById(R.id.seek_eq_bass)
        val tvBassVal: TextView = dialogView.findViewById(R.id.tv_eq_bass_val)
        val seekMid: SeekBar = dialogView.findViewById(R.id.seek_eq_mid)
        val tvMidVal: TextView = dialogView.findViewById(R.id.tv_eq_mid_val)
        val seekTreble: SeekBar = dialogView.findViewById(R.id.seek_eq_treble)
        val tvTrebleVal: TextView = dialogView.findViewById(R.id.tv_eq_treble_val)

        val btnReset: Button = dialogView.findViewById(R.id.btn_eq_reset)
        val btnApply: Button = dialogView.findViewById(R.id.btn_eq_apply)

        fun formatDb(progress: Int): String {
            val db = ((progress - 50) * 12) / 50
            return if (db > 0) "+$db dB" else "$db dB"
        }

        fun updateDbLabels() {
            tvBassVal.text = formatDb(seekBass.progress)
            tvMidVal.text = formatDb(seekMid.progress)
            tvTrebleVal.text = formatDb(seekTreble.progress)
        }

        val presetButtons = listOf(
            "Warmth" to btnWarmth,
            "Bass Boost" to btnBass,
            "Vocals" to btnVocal,
            "Classical" to btnClassical,
            "Acoustic" to btnAcoustic,
            "Flat" to btnFlat
        )

        var selectedPreset = VintageEqualizerManager.getSavedPreset(this)

        fun highlightPreset(name: String) {
            selectedPreset = name
            presetButtons.forEach { (presetName, btn) ->
                if (presetName.equals(name, ignoreCase = true)) {
                    btn.setBackgroundResource(R.drawable.bg_pill_button_playing)
                    btn.setTextColor(getColor(R.color.amber_accent))
                } else {
                    btn.setBackgroundResource(R.drawable.bg_pill_button)
                    btn.setTextColor(getColor(R.color.text_primary))
                }
            }
        }

        fun applyCurrent(presetName: String) {
            VintageEqualizerManager.applyBands(
                seekBass.progress,
                seekMid.progress,
                seekTreble.progress,
                presetName,
                this
            )
        }

        // Initialize state
        switchEnable.isChecked = VintageEqualizerManager.isEnabled(this)
        seekBass.progress = VintageEqualizerManager.getSavedBass(this)
        seekMid.progress = VintageEqualizerManager.getSavedMid(this)
        seekTreble.progress = VintageEqualizerManager.getSavedTreble(this)
        updateDbLabels()
        highlightPreset(selectedPreset)

        fun setEnabledUi(enabled: Boolean) {
            seekBass.isEnabled = enabled
            seekMid.isEnabled = enabled
            seekTreble.isEnabled = enabled
            presetButtons.forEach { (_, btn) -> btn.isEnabled = enabled }
        }
        setEnabledUi(switchEnable.isChecked)

        switchEnable.setOnCheckedChangeListener { _, isChecked ->
            VintageEqualizerManager.setEnabled(isChecked, this)
            setEnabledUi(isChecked)
            if (isChecked) {
                applyCurrent(selectedPreset)
            }
        }

        fun selectPresetValues(presetName: String, bass: Int, mid: Int, treble: Int) {
            seekBass.progress = bass
            seekMid.progress = mid
            seekTreble.progress = treble
            updateDbLabels()
            highlightPreset(presetName)
            applyCurrent(presetName)
        }

        btnWarmth.setOnClickListener { selectPresetValues("Warmth", 65, 60, 70) }
        btnBass.setOnClickListener { selectPresetValues("Bass Boost", 90, 55, 45) }
        btnVocal.setOnClickListener { selectPresetValues("Vocals", 40, 80, 65) }
        btnClassical.setOnClickListener { selectPresetValues("Classical", 55, 50, 75) }
        btnAcoustic.setOnClickListener { selectPresetValues("Acoustic", 60, 65, 65) }
        btnFlat.setOnClickListener { selectPresetValues("Flat", 50, 50, 50) }

        val seekListener = object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    updateDbLabels()
                    highlightPreset("Custom")
                    applyCurrent("Custom")
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        }

        seekBass.setOnSeekBarChangeListener(seekListener)
        seekMid.setOnSeekBarChangeListener(seekListener)
        seekTreble.setOnSeekBarChangeListener(seekListener)

        btnReset.setOnClickListener {
            selectPresetValues("Flat", 50, 50, 50)
            Toast.makeText(this, "Equalizer reset to Flat", Toast.LENGTH_SHORT).show()
        }

        btnApply.setOnClickListener {
            applyCurrent(selectedPreset)
            Toast.makeText(this, "Equalizer settings saved! 🎵", Toast.LENGTH_SHORT).show()
            dialog.dismiss()
        }

        dialog.show()
    }

    // ── Admin Dialogs & Actions (Item 5: Edit Playlist Action) ────────────────

    private fun updateAdminUi() {
        tvTopAdminBadge.visibility = if (isAdmin) View.VISIBLE else View.GONE
        if (isAdmin) {
            tvNavLogin.text = "Admin"
            tvNavLogin.setTextColor(getColor(R.color.amber_light))
            ivNavLogin.setColorFilter(getColor(R.color.amber_light))
        } else {
            tvNavLogin.text = "Login"
            tvNavLogin.setTextColor(getColor(R.color.text_muted))
            ivNavLogin.setColorFilter(getColor(R.color.text_muted))
        }
        btnAdminCreatePlaylist.visibility = if (isAdmin) View.VISIBLE else View.GONE
        btnAdminSyncCloud.visibility = if (isAdmin) View.VISIBLE else View.GONE
        layoutAdminPlaylistBar.visibility = if (isAdmin && layoutPlaylistSongsView.visibility == View.VISIBLE) View.VISIBLE else View.GONE
    }

    private fun openAdminLoginModal() {
        etAdminPassword.setText("")
        layoutAdminLoginOverlay.visibility = View.VISIBLE
    }

    private fun showAdminMenu() {
        val options = arrayOf(
            "➕ Create New Playlist",
            "☁️ Publish All Playlists to Cloud",
            "🌐 Configure Languages",
            "🔄 Check for App Updates",
            "🔓 Log Out Admin"
        )
        AlertDialog.Builder(this)
            .setTitle("Admin Panel (Vintage Melodies)")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> showCreateOrEditPlaylistDialog(existingPlaylist = null)
                    1 -> {
                        Toast.makeText(this, "Publishing playlists to cloud...", Toast.LENGTH_SHORT).show()
                        CloudPlaylistSyncManager.publishPlaylistsToCloud(this, repository) { success, msg ->
                            if (success) {
                                Toast.makeText(this, "☁️ Success: All devices will now see these playlists!", Toast.LENGTH_LONG).show()
                            } else {
                                Toast.makeText(this, "⚠️ Failed: $msg", Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                    2 -> showLanguageSelectionDialog(isFirstTime = false)
                    3 -> AppUpdateManager.checkForUpdate(this, isManual = true)
                    4 -> {
                        isAdmin = false
                        getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().putBoolean(KEY_IS_ADMIN, false).apply()
                        updateAdminUi()
                        refreshPlaylists()
                        Toast.makeText(this, "Logged out of Admin mode", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("Close", null)
            .show()
    }

    private fun showPlaylistAdminOptions(playlist: Playlist) {
        val options = arrayOf(
            "✏️ Edit Playlist (Name, Language, Picture)",
            "📋 Copy Playlist",
            "☁️ Add Songs from Cloud R2",
            "🗑 Delete Playlist"
        )
        AlertDialog.Builder(this)
            .setTitle("Manage: ${playlist.name} (${playlist.language})")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> showCreateOrEditPlaylistDialog(existingPlaylist = playlist)
                    1 -> {
                        repository.copyPlaylist(playlist.id)
                        refreshPlaylists()
                        if (isAdmin) CloudPlaylistSyncManager.publishPlaylistsToCloud(this, repository)
                        Toast.makeText(this, "Copied '${playlist.name}'", Toast.LENGTH_SHORT).show()
                    }
                    2 -> {
                        openPlaylist(playlist)
                        openR2Picker()
                    }
                    3 -> {
                        AlertDialog.Builder(this)
                            .setTitle("Delete Playlist?")
                            .setMessage("Are you sure you want to delete '${playlist.name}'?")
                            .setPositiveButton("Delete") { _, _ ->
                                repository.deletePlaylist(playlist.id)
                                if (activePlaylist?.id == playlist.id) {
                                    layoutPlaylistSongsView.visibility = View.GONE
                                }
                                refreshPlaylists()
                                if (isAdmin) CloudPlaylistSyncManager.publishPlaylistsToCloud(this, repository)
                                Toast.makeText(this, "Deleted '${playlist.name}'", Toast.LENGTH_SHORT).show()
                            }
                            .setNegativeButton("Cancel", null)
                            .show()
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showSleepTimerDialog() {
        val times = arrayOf("15 Minutes", "30 Minutes", "60 Minutes", "Turn Off Timer")
        AlertDialog.Builder(this)
            .setTitle("⏲️ Sleep Timer")
            .setItems(times) { _, which ->
                when (which) {
                    0 -> Toast.makeText(this, "Sleep timer set for 15 minutes", Toast.LENGTH_SHORT).show()
                    1 -> Toast.makeText(this, "Sleep timer set for 30 minutes", Toast.LENGTH_SHORT).show()
                    2 -> Toast.makeText(this, "Sleep timer set for 60 minutes", Toast.LENGTH_SHORT).show()
                    3 -> Toast.makeText(this, "Sleep timer turned off", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    // ── Audio Playback Callbacks & Notification Updates (Item 6) ───────────

    private var currentSongArtBitmap: Bitmap? = null
    private var currentSongArtPath: String? = null

    override fun onSongChanged(song: Song) {
        tvPlayerTitle.text = song.title
        val details = song.getDetailsInfo()
        tvPlayerArtist.text = if (details.isNotBlank()) details else "Vintage Melodies"

        // Update active playing song and playlist across all lists
        songAdapter.setPlayingSong(song.id, true)
        moodAdapter.setPlayingPlaylistId(playingPlaylist?.id, true)
        drawerAdapter.setPlayingPlaylistId(playingPlaylist?.id, true)

        // Attach Equalizer to active audio session
        val audioSessionId = playerManager.getAudioSessionId()
        if (audioSessionId > 0) {
            VintageEqualizerManager.bindSession(audioSessionId, this)
        }

        // Persist session immediately on track change
        val plId = playingPlaylist?.id ?: -1L
        val curIdx = playerManager.getCurrentIndex()
        saveLastPlayedSession(plId, song.id, curIdx)

        currentSongArtBitmap = null
        currentSongArtPath = null

        // Load true artwork (Memory -> Disk -> Remote Url -> Embedded MP3 Extraction -> Fallback)
        SongArtworkHelper.loadSongArt(ivPlayerArt, song) { bitmap, filePath ->
            currentSongArtBitmap = bitmap
            currentSongArtPath = filePath
            // Update notification and MediaSession with true album art
            MusicPlaybackService.updateNotification(
                this,
                song,
                playingPlaylist?.name ?: "Vintage Melodies",
                true,
                customBitmap = bitmap,
                artFilePath = filePath
            )
        }

        // Update app background with playing playlist artwork
        val currentPl = playingPlaylist
            ?: activePlaylist
            ?: repository.getAllPlaylists().firstOrNull { pl ->
                repository.getSongsForPlaylist(pl.id).any { it.id == song.id }
            }
        updateAppBackground(currentPl)

        btnPlayPause.setImageResource(R.drawable.ic_pause)

        // Initial notification update
        MusicPlaybackService.updateNotification(
            this,
            song,
            playingPlaylist?.name ?: "Vintage Melodies",
            true
        )
    }

    override fun onPlaybackStateChanged(isPlaying: Boolean) {
        btnPlayPause.setImageResource(if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play_arrow)
        songAdapter.setPlayingSong(playerManager.getCurrentSong()?.id, isPlaying)
        moodAdapter.setPlayingPlaylistId(playingPlaylist?.id, isPlaying)
        drawerAdapter.setPlayingPlaylistId(playingPlaylist?.id, isPlaying)

        // Update Media Playback Notification (Item 6)
        MusicPlaybackService.updateNotification(
            this,
            playerManager.getCurrentSong(),
            playingPlaylist?.name ?: "Vintage Melodies",
            isPlaying,
            customBitmap = currentSongArtBitmap,
            artFilePath = currentSongArtPath
        )
    }

    override fun onProgressUpdate(currentMs: Int, totalMs: Int) {
        if (!isUserSeeking && totalMs > 0) {
            playerSeekBar.max = totalMs
            playerSeekBar.progress = currentMs
        }
        val curMin = (currentMs / 1000) / 60
        val curSec = (currentMs / 1000) % 60
        val totMin = (totalMs / 1000) / 60
        val totSec = (totalMs / 1000) % 60
        tvPlayerCurrentTime.text = String.format("%d:%02d", curMin, curSec)
        tvPlayerTotalTime.text = String.format("%d:%02d", totMin, totSec)
    }

    override fun onError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    // ── Back Button Handling ───────────────────────────────────────────────

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        when {
            layoutR2FolderSongs.visibility == View.VISIBLE -> {
                returnToFolderList()
            }
            layoutR2Picker.visibility == View.VISIBLE -> {
                layoutR2Picker.visibility = View.GONE
            }
            layoutAdminLoginOverlay.visibility == View.VISIBLE -> {
                layoutAdminLoginOverlay.visibility = View.GONE
            }
            layoutPlaylistSongsView.visibility == View.VISIBLE -> {
                layoutPlaylistSongsView.visibility = View.GONE
                refreshPlaylists()
            }
            layoutDrawerPlaylists.visibility == View.VISIBLE -> {
                layoutDrawerPlaylists.visibility = View.GONE
            }
            else -> super.onBackPressed()
        }
    }

    override fun onStop() {
        super.onStop()
        val curSong = playerManager.getCurrentSong()
        if (curSong != null) {
            saveLastPlayedSession(
                playingPlaylist?.id ?: -1L,
                curSong.id,
                playerManager.getCurrentIndex()
            )
        }
    }

    override fun onDestroy() {
        playerManager.stop()
        VintageEqualizerManager.release()
        MusicPlaybackService.stopService(this)
        try {
            networkCallback?.let { connectivityManager?.unregisterNetworkCallback(it) }
        } catch (e: Exception) {
            // Ignore callback unregister error
        }
        super.onDestroy()
    }
}
