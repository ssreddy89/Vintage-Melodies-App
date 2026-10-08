package com.vintagemelodies.app

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import kotlin.concurrent.thread

/**
 * Handles In-App Update checks for Vintage Melodies.
 * Launches browser to download the latest APK without requiring dangerous install permissions.
 */
object AppUpdateManager {

    private val UPDATE_URLS = listOf(
        "https://raw.githubusercontent.com/ssreddy89/Vintage-Melodies-App/main/version.json",
        "https://vintage-melodies-kappa.vercel.app/api/version",
        "https://pub-e7f4f743d98b43b89671fe068ee1ee05.r2.dev/update/version.json"
    )

    data class UpdateInfo(
        val versionCode: Int,
        val versionName: String,
        val apkUrl: String,
        val changelog: String,
        val forceUpdate: Boolean = false
    )

    fun checkForUpdate(
        activity: Activity,
        isManual: Boolean = false,
        customUrl: String? = null
    ) {
        val currentVersionCode = try {
            val pInfo = activity.packageManager.getPackageInfo(activity.packageName, 0)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                pInfo.longVersionCode.toInt()
            } else {
                @Suppress("DEPRECATION")
                pInfo.versionCode
            }
        } catch (e: Exception) {
            1
        }

        val urlsToTry = if (!customUrl.isNullOrBlank()) listOf(customUrl) else UPDATE_URLS

        thread {
            var fetchedUpdate: UpdateInfo? = null
            var lastError: Exception? = null

            for (endpoint in urlsToTry) {
                try {
                    val url = URL(endpoint)
                    val conn = (url.openConnection() as HttpURLConnection).apply {
                        connectTimeout = 4000
                        readTimeout = 4000
                        requestMethod = "GET"
                        setRequestProperty("User-Agent", "Mozilla/5.0 (Android; VintageMelodies)")
                    }

                    if (conn.responseCode == 200) {
                        val jsonStr = conn.inputStream.bufferedReader().use { it.readText() }
                        val obj = JSONObject(jsonStr)
                        val remoteVersionCode = obj.optInt("versionCode", currentVersionCode)
                        val remoteVersionName = obj.optString("versionName", "1.0.1")
                        val apkUrl = obj.optString("apkUrl", "")
                        val changelog = obj.optString("changelog", "Bug fixes and performance improvements.")
                        val minVersionCode = obj.optInt("minVersionCode", 0)
                        val isForce = obj.optBoolean("forceUpdate", false) || (currentVersionCode < minVersionCode)

                        if (remoteVersionCode > currentVersionCode && apkUrl.isNotBlank()) {
                            fetchedUpdate = UpdateInfo(
                                versionCode = remoteVersionCode,
                                versionName = remoteVersionName,
                                apkUrl = apkUrl,
                                changelog = changelog,
                                forceUpdate = isForce
                            )
                            break
                        } else {
                            // Up to date
                            fetchedUpdate = null
                            break
                        }
                    }
                } catch (e: Exception) {
                    lastError = e
                }
            }

            Handler(Looper.getMainLooper()).post {
                if (activity.isFinishing || activity.isDestroyed) return@post

                if (fetchedUpdate != null) {
                    showUpdateDialog(activity, fetchedUpdate)
                } else if (isManual) {
                    Toast.makeText(
                        activity,
                        "Vintage Melodies is already up to date! (v$currentVersionCode)",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    private fun showUpdateDialog(activity: Activity, updateInfo: UpdateInfo) {
        if (activity.isFinishing || activity.isDestroyed) return

        val builder = AlertDialog.Builder(activity)
            .setTitle(if (updateInfo.forceUpdate) "⚠️ Mandatory Update Required" else "🚀 New Update Available!")
            .setMessage(
                if (updateInfo.forceUpdate) {
                    "A required update (v${updateInfo.versionName}) is available.\n\nYou must update to continue using Vintage Melodies.\n\nWhat's New:\n${updateInfo.changelog}"
                } else {
                    "Version ${updateInfo.versionName} is ready to install.\n\nWhat's New:\n${updateInfo.changelog}"
                }
            )
            .setPositiveButton("Update Now") { dialog, _ ->
                try {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(updateInfo.apkUrl))
                    activity.startActivity(intent)
                } catch (e: Exception) {
                    Toast.makeText(activity, "Failed to open link: ${e.message}", Toast.LENGTH_SHORT).show()
                }
                if (updateInfo.forceUpdate) {
                    // Re-present or exit if user does not actually install/leaves
                    dialog.dismiss()
                    showUpdateDialog(activity, updateInfo)
                }
            }

        if (updateInfo.forceUpdate) {
            builder.setCancelable(false)
            builder.setNegativeButton("Exit App") { _, _ ->
                activity.finishAffinity()
            }
        } else {
            builder.setCancelable(true)
            builder.setNegativeButton("Later", null)
        }

        val dialog = builder.create()
        dialog.setCanceledOnTouchOutside(!updateInfo.forceUpdate)
        dialog.show()
    }
}
