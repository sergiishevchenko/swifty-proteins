package com.music42.swiftyprotein

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.fragment.app.FragmentActivity
import com.music42.swiftyprotein.ui.navigation.SwiftyProteinNavHost
import com.music42.swiftyprotein.ui.theme.SwiftyProteinTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : FragmentActivity() {

    var shouldShowLogin by mutableStateOf(false)
        private set

    private var wasInBackground = false

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()

        var keepSplash = true
        splashScreen.setKeepOnScreenCondition { keepSplash }
        window.decorView.postDelayed({ keepSplash = false }, 2000L)

        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            SwiftyProteinTheme {
                SwiftyProteinNavHost(
                    shouldShowLogin = shouldShowLogin,
                    onLoginShown = { shouldShowLogin = false }
                )
            }
        }
    }

    override fun onStop() {
        super.onStop()
        wasInBackground = true
    }

    override fun onResume() {
        super.onResume()
        if (wasInBackground) {
            wasInBackground = false
            shouldShowLogin = true
        }
    }
}
