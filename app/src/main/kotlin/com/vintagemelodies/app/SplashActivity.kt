package com.vintagemelodies.app

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

/**
 * Splash Screen Activity.
 * Displays the artwork "Vintage Melodies, Endless Memories" for 4 seconds
 * (between 3 to 5 seconds) before proceeding to MainActivity.
 */
class SplashActivity : AppCompatActivity() {

    private lateinit var splashImageView: View
    private lateinit var errorView: View
    private lateinit var retryButton: Button

    private val handler = Handler(Looper.getMainLooper())
    private val splashDurationMs = 4000L // 4 seconds (3 to 5 seconds)
    private var hasNavigated = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        splashImageView = findViewById(R.id.iv_splash_image)
        errorView = findViewById(R.id.splash_error)
        retryButton = findViewById(R.id.splash_retry_btn)

        retryButton.setOnClickListener {
            errorView.visibility = View.GONE
            splashImageView.visibility = View.VISIBLE
            checkNetworkAndProceed()
        }

        // Display the splash image for 4 seconds, then proceed
        handler.postDelayed({
            if (!isFinishing && !isDestroyed && !hasNavigated) {
                checkNetworkAndProceed()
            }
        }, splashDurationMs)
    }

    private fun checkNetworkAndProceed() {
        if (NetworkUtils.isNetworkAvailable(this)) {
            hasNavigated = true
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        } else {
            showErrorState()
        }
    }

    private fun showErrorState() {
        errorView.visibility = View.VISIBLE
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        super.onDestroy()
    }
}
