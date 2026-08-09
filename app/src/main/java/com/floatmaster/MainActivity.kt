package com.floatmaster

import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.floatmaster.permission.OverlayPermissionHandler
import com.floatmaster.service.FloatingWindowManager
import com.floatmaster.ui.screens.HomeScreen
import com.floatmaster.ui.screens.OnboardingScreen
import com.floatmaster.ui.theme.FloatMasterTheme
import com.floatmaster.util.PipHelper
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject lateinit var windowManager: FloatingWindowManager
    private var pipReceiver: PipHelper.PipActionReceiver? = null
    private var overlayGranted by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        overlayGranted = OverlayPermissionHandler.hasPermission(this)
        pipReceiver = PipHelper.PipActionReceiver(
            onPlayPause = { playing -> PipHelper.updateActions(this, playing) },
            onClose = { if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) finishAndRemoveTask() else finish() }
        ).register(this)

        setContent {
            FloatMasterTheme {
                Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    if (!overlayGranted) {
                        OnboardingScreen(onComplete = { overlayGranted = OverlayPermissionHandler.hasPermission(this@MainActivity) })
                    } else {
                        HomeScreen(manager = windowManager)
                    }
                }
            }
        }
    }

    override fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean, newConfig: Configuration) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        // WHY: Only a video window is eligible for automatic PiP; AI chats must never unexpectedly leave the app.
        if (windowManager.allWindows().any { it.type.name == "YOUTUBE" } && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            PipHelper.enterPip(this, aspect = android.util.Rational(16, 9), autoEnter = true)
        }
    }

    override fun onResume() {
        super.onResume()
        overlayGranted = OverlayPermissionHandler.hasPermission(this)
    }

    override fun onDestroy() {
        runCatching { pipReceiver?.let(::unregisterReceiver) }
        pipReceiver = null
        super.onDestroy()
    }
}
