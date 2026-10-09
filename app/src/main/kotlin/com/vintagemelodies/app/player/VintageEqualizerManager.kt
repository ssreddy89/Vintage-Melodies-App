package com.vintagemelodies.app.player

import android.content.Context
import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer

object VintageEqualizerManager {

    private const val PREFS_NAME = "vm_equalizer_prefs"
    private const val KEY_ENABLED = "eq_enabled"
    private const val KEY_PRESET = "eq_preset"
    private const val KEY_BASS = "eq_bass"
    private const val KEY_MID = "eq_mid"
    private const val KEY_TREBLE = "eq_treble"

    private var equalizer: Equalizer? = null
    private var bassBoost: BassBoost? = null
    private var currentSessionId: Int = 0

    fun bindSession(audioSessionId: Int, context: Context) {
        if (audioSessionId <= 0 || audioSessionId == currentSessionId) return
        release()
        currentSessionId = audioSessionId

        try {
            equalizer = Equalizer(0, audioSessionId).apply {
                enabled = isEnabled(context)
            }
            bassBoost = BassBoost(0, audioSessionId).apply {
                enabled = isEnabled(context)
            }
            applySavedSettings(context)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun isEnabled(context: Context): Boolean {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_ENABLED, true)
    }

    fun setEnabled(enabled: Boolean, context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
            .putBoolean(KEY_ENABLED, enabled)
            .apply()

        try {
            equalizer?.enabled = enabled
            bassBoost?.enabled = enabled
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun getSavedPreset(context: Context): String {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_PRESET, "Warmth") ?: "Warmth"
    }

    fun getSavedBass(context: Context): Int {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getInt(KEY_BASS, 65)
    }

    fun getSavedMid(context: Context): Int {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getInt(KEY_MID, 60)
    }

    fun getSavedTreble(context: Context): Int {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getInt(KEY_TREBLE, 70)
    }

    fun applyBands(bass: Int, mid: Int, treble: Int, presetName: String, context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
            .putInt(KEY_BASS, bass)
            .putInt(KEY_MID, mid)
            .putInt(KEY_TREBLE, treble)
            .putString(KEY_PRESET, presetName)
            .apply()

        val eq = equalizer ?: return
        try {
            val numBands = eq.numberOfBands
            val minLevel = eq.bandLevelRange[0]
            val maxLevel = eq.bandLevelRange[1]
            val range = maxLevel - minLevel

            // Map 0-100 to minLevel..maxLevel
            fun progressToLevel(progress: Int): Short {
                return (minLevel + (progress / 100.0f) * range).toInt().toShort()
            }

            if (numBands >= 1) eq.setBandLevel(0, progressToLevel(bass))
            if (numBands >= 3) eq.setBandLevel((numBands / 2).toShort(), progressToLevel(mid))
            if (numBands >= 2) eq.setBandLevel((numBands - 1).toShort(), progressToLevel(treble))

            bassBoost?.setStrength((bass * 10).toShort().coerceIn(0, 1000))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun applySavedSettings(context: Context) {
        val bass = getSavedBass(context)
        val mid = getSavedMid(context)
        val treble = getSavedTreble(context)
        val preset = getSavedPreset(context)
        applyBands(bass, mid, treble, preset, context)
    }

    fun release() {
        try {
            equalizer?.release()
            bassBoost?.release()
        } catch (e: Exception) {
            // Ignore
        }
        equalizer = null
        bassBoost = null
        currentSessionId = 0
    }
}
