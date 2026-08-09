package com.floatmaster

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.floatmaster.permission.OverlayPermissionHandler
import com.floatmaster.service.FloatingWindowManager
import com.floatmaster.ui.screens.HomeScreen
import com.floatmaster.ui.screens.OnboardingScreen
import com.floatmaster.ui.theme.FloatMasterTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var windowManager: FloatingWindowManager

    override fun onCreate(savedInstanceState: Bundle?) {
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

    override fun onResume() {
        super.onResume()
        // If user just granted permission, could auto-dismiss onboarding — handled via state recomposition
    }
}
