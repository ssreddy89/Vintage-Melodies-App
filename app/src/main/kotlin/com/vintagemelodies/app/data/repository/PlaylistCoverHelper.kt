package com.vintagemelodies.app.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import com.vintagemelodies.app.R
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream

/**
 * Helper to ensure playlist cover images are 100% consistent across all users and devices.
 * Eliminates ID mismatches and local file path incompatibilities.
 */
object PlaylistCoverHelper {

    val PRESET_DRAWABLES = listOf(
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

    private val NAME_MAP = mapOf(
        "vm_art_monsoon" to R.drawable.vm_art_monsoon,
        "vm_art_village" to R.drawable.vm_art_village,
        "vm_art_sunset" to R.drawable.vm_art_sunset,
        "vm_art_classic" to R.drawable.vm_art_classic,
        "vm_art_nostalgia" to R.drawable.vm_art_nostalgia,
        "vm_gif_kitchen" to R.drawable.vm_gif_kitchen,
        "vm_gif_tailoring" to R.drawable.vm_gif_tailoring,
        "vm_gif_roadtrip" to R.drawable.vm_gif_roadtrip,
        "ic_vintage_player_art_2" to R.drawable.ic_vintage_player_art_2,
        "ic_vintage_player_art_3" to R.drawable.ic_vintage_player_art_3,
        "background" to R.drawable.background
    )

    private val RES_TO_NAME = NAME_MAP.entries.associate { (k, v) -> v to k }

    fun getDrawableName(resId: Int): String {
        return RES_TO_NAME[resId] ?: "vm_art_village"
    }

    fun getDrawableResByName(name: String, fallback: Int = R.drawable.vm_art_village): Int {
        return NAME_MAP[name] ?: fallback
    }

    /**
     * Compress an image file to a lightweight Base64 data URI so all users worldwide can display it
     * without needing access to the Admin's internal storage.
     */
    fun encodeFileToBase64DataUri(filePath: String): String? {
        return try {
            val file = File(filePath)
            if (!file.exists()) return null

            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(file.absolutePath, options)

            // Target size max 400x400 for fast cloud sync and low bandwidth
            var sampleSize = 1
            while (options.outWidth / sampleSize > 400 || options.outHeight / sampleSize > 400) {
                sampleSize *= 2
            }

            val decodeOptions = BitmapFactory.Options().apply { inSampleSize = sampleSize }
            val bitmap = BitmapFactory.decodeFile(file.absolutePath, decodeOptions) ?: return null

            val stream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 75, stream)
            val bytes = stream.toByteArray()
            "data:image/jpeg;base64," + Base64.encodeToString(bytes, Base64.NO_WRAP)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
