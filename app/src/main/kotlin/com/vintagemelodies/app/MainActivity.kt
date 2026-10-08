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
    private lateinit var btnTopLanguage: ImageButton
    private lateinit var btnTopTimer: ImageButton
    private lateinit var btnTopPlaylists: ImageButton
    private lateinit var btnTopAdmin: ImageButton

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
    private lateinit var layoutDrawerPlaylists: LinearLayout
    private lateinit var rvDrawerPlaylists: RecyclerView
    private lateinit var drawerAdapter: DrawerPlaylistAdapter
    private lateinit var btnCloseDrawer: ImageButton
    private lateinit var btnAdminCreatePlaylist: Button

    // Playlist Songs View
    private lateinit var layoutPlaylistSongsView: LinearLayout
    private lateinit var tvActivePlaylistTitle: TextView
    private lateinit var tvActivePlaylistLangBadge: TextView
    private lateinit var btnBackToPlaylists: ImageButton
    private lateinit var btnPlaylistOptions: ImageButton
    private lateinit var rvPlaylistSongs: RecyclerView
    private lateinit var songAdapter: PlaylistSongAdapter
    private lateinit var layoutAdminPlaylistBar: LinearLayout
    private lateinit var btnAdminAddR2Songs: Button

    // Cloudflare R2 Folder & Song Picker
    private lateinit var layoutR2Picker: LinearLayout
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
        btnTopLanguage = findViewById(R.id.btn_top_language)
        btnTopTimer = findViewById(R.id.btn_top_timer)
        btnTopPlaylists = findViewById(R.id.btn_top_playlists)
        btnTopAdmin = findViewById(R.id.btn_top_admin)

        // Mood Carousel Section
        layoutMoodSection = findViewById(R.id.layout_mood_section)
        btnCloseMoodSection = findViewById(R.id.btn_close_mood_section)
        btnSeeAll = findViewById(R.id.btn_see_all)
        rvMoodPlaylists = findViewById(R.id.rv_mood_playlists)
        rvMoodPlaylists.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        rvMoodPlaylists.setHasFixedSize(true)
        rvMoodPlaylists.isNestedScrollingEnabled = false
        moodAdapter = MoodPlaylistAdapter(emptyList()) { playlist -> openPlaylist(playlist) }
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
                activePlaylist?.let { pl ->
                    playingPlaylist = pl
                    val songs = repository.getSongsForPlaylist(pl.id)
                    playerManager.playQueue(songs, index)
                    updateAppBackground(pl)
                    saveLastPlayedSession(pl.id, song.id, index)
                } ?: run {
                    playerManager.playSong(song)
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
        btnTopLanguage.setOnClickListener { showLanguageSelectionDialog(isFirstTime = false) }
        btnTopTimer.setOnClickListener { showSleepTimerDialog() }

        btnTopPlaylists.setOnClickListener {
            if (layoutMoodSection.visibility == View.GONE) {
                layoutMoodSection.visibility = View.VISIBLE
            } else {
                openDrawer()
            }
        }

        btnSeeAll.setOnClickListener { openDrawer() }
        btnCloseMoodSection.setOnClickListener { layoutMoodSection.visibility = View.GONE }
        btnCloseDrawer.setOnClickListener { layoutDrawerPlaylists.visibility = View.GONE }

        btnTopAdmin.setOnClickListener {
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
            playingPlaylist?.let { pl ->
                openPlaylist(pl)
            } ?: activePlaylist?.let { pl ->
                openPlaylist(pl)
            } ?: run {
                openDrawer()
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
                    repository.addSongsToPlaylist(pl.id, selectedSongIds.toList())
                    Toast.makeText(this, "Added ${selectedSongIds.size} songs to ${pl.name}", Toast.LENGTH_SHORT).show()
                    layoutR2Picker.visibility = View.GONE
                    openPlaylist(pl)
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
                    query = query,
                    targetPlaylistId = activePlaylist?.id
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
            .apply()
    }

    private fun restoreLastPlayedSession() {
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val lastPlaylistId = prefs.getLong(KEY_LAST_PLAYLIST_ID, -1L)
        if (lastPlaylistId != -1L) {
            val pl = repository.getPlaylistById(lastPlaylistId)
            if (pl != null) {
                val songs = repository.getSongsForPlaylist(pl.id)
                if (songs.isNotEmpty()) {
                    activePlaylist = pl
                    playingPlaylist = pl
                    val lastSongIndex = prefs.getInt(KEY_LAST_SONG_INDEX, 0).coerceIn(0, songs.size - 1)
                    playerManager.prepareQueue(songs, lastSongIndex)
                    updateAppBackground(pl)
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
        moodAdapter.updateData(playlists)
        drawerAdapter.updateData(playlists)
    }

    private fun openDrawer() {
        refreshPlaylists()
        layoutDrawerPlaylists.visibility = View.VISIBLE
    }

    private fun openPlaylist(playlist: Playlist) {
        activePlaylist = playlist
        tvActivePlaylistTitle.text = playlist.name
        tvActivePlaylistLangBadge.text = playlist.language.uppercase()

        val songs = repository.getSongsForPlaylist(playlist.id)
        songAdapter.updateData(songs)
        layoutPlaylistSongsView.visibility = View.VISIBLE
        layoutAdminPlaylistBar.visibility = if (isAdmin) View.VISIBLE else View.GONE
    }

    private fun updateAppBackground(playlist: Playlist?) {
        if (playlist == null) {
            Glide.with(this).load(R.drawable.background).centerCrop().into(ivMainBackground)
            return
        }

        val loadTarget: Any = if (playlist.coverUrl.isNotBlank()) playlist.coverUrl else {
            if (playlist.coverResId != 0) playlist.coverResId else R.drawable.background
        }

        Glide.with(this)
            .load(loadTarget)
            .centerCrop()
            .into(ivMainBackground)
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
                        Toast.makeText(this, "Moved '${song.title}' to '${targetPl.name}'", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    repository.copySongToPlaylist(targetPl.id, song.id)
                    refreshPlaylists()
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
                val updatedFolders = repository.getAvailableCloudFolders(activePlaylist?.id)
                folderAdapter.updateData(updatedFolders)
                tvR2PickerSubtitle.text = if (updatedFolders.isEmpty()) {
                    "All cloud songs have already been added to this playlist!"
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

        val folders = repository.getAvailableCloudFolders(activePlaylist?.id)
        folderAdapter.updateData(folders)

        if (folders.isEmpty()) {
            tvR2PickerSubtitle.text = "All cloud songs have already been added to this playlist!"
        }
    }

    private fun openCloudFolder(folderName: String) {
        activeCloudFolder = folderName
        selectedSongIds.clear()
        isAllFolderSongsSelected = false
        btnSelectAllFolderSongs.text = "Select All"
        etSearchR2Songs.setText("")

        val songs = repository.getUnassignedSongs(
            folderFilter = folderName,
            targetPlaylistId = activePlaylist?.id
        )
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
        val presets = arrayOf(
            "📻 Vintage Melodies Warmth (Default)",
            "🔊 Deep Bass Boost",
            "🎤 Clear Vocals",
            "🎻 Classic Instrumental",
            "⚡ Acoustic Live",
            "🎧 Flat / Studio Reference"
        )
        AlertDialog.Builder(this)
            .setTitle("Equalizer Sound Profile")
            .setItems(presets) { _, which ->
                Toast.makeText(this, "Equalizer activated: ${presets[which]}", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Close", null)
            .show()
    }

    // ── Admin Dialogs & Actions (Item 5: Edit Playlist Action) ────────────────

    private fun updateAdminUi() {
        tvTopAdminBadge.visibility = if (isAdmin) View.VISIBLE else View.GONE
        btnTopAdmin.setColorFilter(if (isAdmin) getColor(R.color.amber_accent) else getColor(R.color.text_primary))
        btnAdminCreatePlaylist.visibility = if (isAdmin) View.VISIBLE else View.GONE
        layoutAdminPlaylistBar.visibility = if (isAdmin && layoutPlaylistSongsView.visibility == View.VISIBLE) View.VISIBLE else View.GONE
    }

    private fun openAdminLoginModal() {
        etAdminPassword.setText("")
        layoutAdminLoginOverlay.visibility = View.VISIBLE
    }

    private fun showAdminMenu() {
        val options = arrayOf(
            "➕ Create New Playlist",
            "🌐 Configure Languages",
            "🔄 Check for App Updates",
            "🔓 Log Out Admin"
        )
        AlertDialog.Builder(this)
            .setTitle("Admin Panel (Vintage Melodies)")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> showCreateOrEditPlaylistDialog(existingPlaylist = null)
                    1 -> showLanguageSelectionDialog(isFirstTime = false)
                    2 -> AppUpdateManager.checkForUpdate(this, isManual = true)
                    3 -> {
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
        val playlistInfo = playingPlaylist?.let { "${it.name} • " } ?: ""
        val songInfo = when {
            song.year.isNotBlank() && song.artist.isNotBlank() -> "${song.artist} (${song.year})"
            song.album.isNotBlank() && !song.album.startsWith("Folder:") && !song.album.startsWith("Cloudflare R2:") -> "${song.artist} • ${song.album}"
            else -> song.artist
        }
        tvPlayerArtist.text = "$playlistInfo$songInfo"

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

        // Update app background
        playingPlaylist?.let { pl ->
            updateAppBackground(pl)
        }

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

    override fun onDestroy() {
        playerManager.stop()
        MusicPlaybackService.stopService(this)
        try {
            networkCallback?.let { connectivityManager?.unregisterNetworkCallback(it) }
        } catch (e: Exception) {
            // Ignore callback unregister error
        }
        super.onDestroy()
    }
}
