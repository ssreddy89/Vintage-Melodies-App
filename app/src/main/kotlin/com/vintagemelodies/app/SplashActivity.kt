package com.vintagemelodies.app

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

/**
 * Splash screen that displays Vintage Melodies branding and checks network
 * availability before launching the main WebView activity.
 *
 * No artificial delay — transitions immediately when network is available.
 */
class SplashActivity : AppCompatActivity() {

    private lateinit var brandingView: View
    private lateinit var errorView: View
    private lateinit var retryButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        brandingView = findViewById(R.id.splash_branding)
        errorView = findViewById(R.id.splash_error)
        retryButton = findViewById(R.id.splash_retry_btn)

        retryButton.setOnClickListener {
            checkNetworkAndProceed()
        }

        checkNetworkAndProceed()
    }

    /**
     * Checks network availability and either proceeds to MainActivity
     * or shows the error state with a retry button.
     */
    private fun checkNetworkAndProceed() {
        if (NetworkUtils.isNetworkAvailable(this)) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        } else {
            showErrorState()
        }
    }

    private fun showErrorState() {
        brandingView.visibility = View.GONE
        errorView.visibility = View.VISIBLE
    }
}
