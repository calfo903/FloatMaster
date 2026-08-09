package com.floatmaster.accessibility

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent

/**
 * Optional accessibility service.
 * Required only for advanced features:
 *  - Detecting foreground app to auto-minimize on certain apps
 *  - Gesture: swipe to minimize
 *  - Pin window to stay above specific apps
 *
 * Declare minimal capabilities in accessibility_service_config.xml.
 * Play Store requires disclosure and video for overlay+accessibility.
 */
class FloatMasterAccessibilityService : AccessibilityService() {

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Example: log foreground package, could be used to trigger auto-actions
        // val pkg = event?.packageName?.toString() ?: return
        // if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) { }
    }

    override fun onInterrupt() {}

    override fun onServiceConnected() {
        super.onServiceConnected()
        // configure via AccessibilityServiceInfo if needed
    }

    companion object {
        var instance: FloatMasterAccessibilityService? = null
            private set
    }

    override fun onCreate() { super.onCreate(); instance = this }
    override fun onDestroy() { instance = null; super.onDestroy() }
}
