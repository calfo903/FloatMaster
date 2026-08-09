package com.floatmaster.util

import android.app.PictureInPictureParams
import android.os.Build
import android.util.Rational
import androidx.activity.ComponentActivity

/**
 * WHY: PiP keeps video playing when window bubbled — user gesture required (ToS-safe).
 */
object PipHelper {
    fun enterPip(activity: ComponentActivity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val params = PictureInPictureParams.Builder()
                .setAspectRatio(Rational(16,9))
                .build()
            activity.enterPictureInPictureMode(params)
        }
    }
}
