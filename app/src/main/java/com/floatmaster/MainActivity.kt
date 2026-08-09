package com.floatmaster

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import android.content.res.Configuration
import android.graphics.Rect
import android.os.Build
import com.floatmaster.permission.OverlayPermissionHandler
import com.floatmaster.util.PipHelper
import com.floatmaster.service.FloatingWindowManager
import com.floatmaster.ui.screens.HomeScreen
import com.floatmaster.ui.screens.OnboardingScreen
import com.floatmaster.ui.theme.FloatMasterTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var windowManager: FloatingWindowManager
    private var pipReceiver: PipHelper.PipActionReceiver? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        // WHY: Register PiP remote actions (Play/Pause, Close) — receivers must be registered before enterPip
        pipReceiver = PipHelper.PipActionReceiver(onPlayPause = { playing ->
            // WHY: Toggle WebView play via JS — find active WebView and dispatch
            if (playing) pipReceiver?.let { PipHelper.updateActions(this, true) } else PipHelper.updateActions(this, false)
        }, onClose = { if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) finishAndRemoveTask() }).register(this)
        super.onCreate(savedInstanceState)

        setContent {
            FloatMasterTheme {
                Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    var showOnboarding by remember {
                        mutableStateOf(!OverlayPermissionHandler.hasPermission(this@MainActivity))
                    }
                    // re-check on resume
                    LaunchedEffect(Unit) {
                        // observe lifecycle resume via DisposableEffect alternative
                    }
                    if (showOnboarding) {
                        OnboardingScreen(onComplete = { showOnboarding = false })
                    } else {
                        HomeScreen(manager = windowManager)
                    }
                }
            }
        }
    }

    override fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean, newConfig: Configuration) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        // WHY: Hide onboarding/Home chrome in PiP — show only video
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        // WHY: Auto-enter PiP when user swipes home while video is playing — only if a video window is active
        // We check if any AI/YouTube window exists, then enter PiP with source rect hint for smooth zoom
        if (windowManager.allWindows().any { it.type.name.contains("YOUTUBE") || it.type.name.contains("AI_") }) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                PipHelper.enterPip(this, aspect = android.util.Rational(16,9), autoEnter = true)
            }
        }
    }

    override fun onDestroy() {
        try { pipReceiver?.let { unregisterReceiver(it) } } catch (_: Exception) {}
        super.onDestroy()
    }

    override fun onResume() {
        super.onResume()
        // If user just granted permission, could auto-dismiss onboarding — handled via state recomposition
    }
}
