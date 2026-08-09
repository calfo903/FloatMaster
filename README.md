# FloatMaster — Floating Multitasking Suite

**Package:** `com.floatmaster`  
**Target SDK:** 34 (Android 14) — tested up to 36 (Android 16)  
**Min SDK:** 26 (Android 8.0)  
**Language:** Kotlin 1.9 + Jetpack Compose (Material 3)  
**Architecture:** MVVM + Clean Architecture + Repository + UDF

FloatMaster replicates and improves upon *Floating Apps (LWi s.r.o.)* — a productivity overlay that lets users run **multiple resizable, movable, minimizable floating windows** simultaneously on top of any app.

---

## 1. Architecture Overview

```
┌──────────────────────────────────────────────────────────┐
│  Presentation Layer (Compose + View)                     │
│  MainActivity, Onboarding, WindowChrome, MiniApps UIs    │
├──────────────────────────────────────────────────────────┤
│  Domain Layer                                            │
│  FloatingWindowManager (singleton), WindowController,    │
│  WindowHistoryManager, UseCases                          │
├──────────────────────────────────────────────────────────┤
│  Data Layer                                              │
│  NotesRepository (Room), ClipboardRepository,            │
│  SettingsRepository (DataStore), HistoryRepository       │
├──────────────────────────────────────────────────────────┤
│  System Layer                                            │
│  FloatingService (FG Service)                            │
│  WindowManager (TYPE_APPLICATION_OVERLAY)                │
│  ComposeView / WebView / ActivityView inside overlay     │
│  AccessibilityService (optional)                         │
│  BroadcastReceivers + Notification Controls              │
└──────────────────────────────────────────────────────────┘
```

### Tech Stack

| Concern | Choice | Why |
|---|---|---|
| UI | Jetpack Compose + Material 3 | Dynamic color, dark mode, adaptive |
| Overlays | `WindowManager.addView()` + `ComposeView` | Most reliable across OEMs; not dependent on freeform mode |
| Freeform launch | `ActivityView` / `TaskOrganizer` (API 33+) + fallback `Intent.FLAG_ACTIVITY_NEW_TASK` | Turn any installed app into floating window where OS supports it |
| Persistence | Room 2.6 + DataStore Preferences | Notes, history, favorites, settings |
| DI | Hilt | Singleton managers, repositories |
| Async | Kotlin Coroutines + Flow | Window state flows |
| PDF | `PdfRenderer` (native) + fallback `AndroidPdfViewer` | No native DOC renderer → use Google Docs viewer |
| Browser | `android.webkit.WebView` with tab manager | Lightweight, supports JS, incognito |
| Permissions | Accompanist Permissions + custom onboarding | Overlay + Notifications + Battery |

---

## 2. Project Structure

```
app/src/main/java/com/floatmaster/
├── FloatMasterApp.kt               // Application + Hilt
├── MainActivity.kt                 // Hosts Compose NavGraph + Onboarding
├── model/
│   ├── FloatingWindow.kt           // Core data class
│   ├── WindowType.kt               // Enum of mini-apps
│   └── WindowState.kt              // Minimized / Maximized / Bubble / Closed
├── service/
│   ├── FloatingService.kt          // Foreground Service (TYPE_APPLICATION_OVERLAY)
│   ├── FloatingWindowManager.kt    // Singleton: create/destroy/z-order
│   └── KeepAliveService.kt         // OEM resilience
├── overlay/
│   ├── FloatingWindowContainer.kt  // ComposeView wrapper + WindowManager.LayoutParams
│   ├── WindowChrome.kt             // Title bar, drag handle, resize handles, controls
│   ├── BubbleView.kt               // Minimized bubble + edge snap
│   └── FloatingDock.kt             // Always-on-top quick-launch dock
├── apps/
│   ├── browser/FloatingBrowser.kt
│   ├── notes/FloatingNotes.kt
│   ├── calculator/FloatingCalculator.kt
│   ├── document/FloatingDocumentViewer.kt
│   ├── filemanager/FloatingFileManager.kt
│   ├── clipboard/FloatingClipboard.kt
│   ├── clock/FloatingClock.kt
│   ├── youtube/FloatingYouTube.kt
│   ├── translator/FloatingTranslator.kt
│   ├── music/FloatingMusicPlayer.kt
│   ├── quicksettings/FloatingQuickSettings.kt
│   ├── launcher/AppLauncherWindow.kt
│   └── url/UrlWindowCreator.kt
├── data/                           // Room + DataStore
├── permission/
│   ├── OverlayPermissionHandler.kt
│   └── BatteryOptimizationHelper.kt
├── manager/WindowHistoryManager.kt
└── accessibility/FloatMasterAccessibilityService.kt
```

---

## 3. How the Floating System Works

### 3.1 SYSTEM_ALERT_WINDOW Flow

1. **Onboarding** (`OnboardingScreen.kt`) checks `Settings.canDrawOverlays(context)` 
2. If false → launch `ACTION_MANAGE_OVERLAY_PERMISSION` with package URI + visual guide (different steps for Xiaomi/MIUI, Oppo/ColorOS, Samsung/OneUI, Huawei/EMUI)
3. Also requests `POST_NOTIFICATIONS` (Android 13+) and battery whitelist (`ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`)
4. Once granted, `FloatingService.start(context)` is called.

### 3.2 Keeping Windows Alive

* `FloatingService` is a **foreground service** with `foregroundServiceType="specialUse"` (Android 14 requirement) + persistent notification with window list + "Close All" action.
* `START_STICKY` + `BootReceiver` re-starts after reboot.
* `KeepAliveService`: lightweight second service that monitors `FloatingService` and restarts if killed (OEM task killers).
* Windows themselves are `View`s added via `WindowManager`, not Activities → survive app backgrounding.
* All window state saved to `DataStore` so they restore after process death.

### 3.3 WindowManager Params (key)

```kotlin
val params = WindowManager.LayoutParams(
    width, height,
    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY, // API 26+
    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
    WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH or
    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
    PixelFormat.TRANSLUCENT
).apply {
    gravity = Gravity.TOP or Gravity.START
    x = initialX; y = initialY
}
```

* **Focus toggle:** Tap title bar → `params.flags &= ~FLAG_NOT_FOCUSABLE` + `updateViewLayout()` + `view.requestFocus()` → keyboard appears. Tap outside content → re-add `FLAG_NOT_FOCUSABLE`.
* **Transparency:** `params.alpha = 0.3f..1f` + `params.dimAmount` for overlay dim.
* **Bring to front:** `windowManager.removeView(view)` then `addView()` again = z-order top.
* **Drag:** `OnTouchListener` on title bar computes `params.x/y` delta.
* **Resize:** 4 corner handles + edge handles; on drag recalculate `params.width/height`.
* **Snap to edge / bubble:** When minimized, replace window with 60dp `BubbleView` attached to left/right edge with `WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE`.

### 3.4 Compose Inside Overlay

Every floating window is a `ComposeView` set inside a `FrameLayout` container:

```kotlin
val composeView = ComposeView(context).apply {
    setViewTreeLifecycleOwner(lifecycleOwner)
    setViewTreeSavedStateRegistryOwner(savedStateRegistryOwner)
    setContent { FloatMasterTheme { WindowChrome(window) { MiniAppContent() } } }
}
container.addView(composeView)
windowManager.addView(container, params)
```

Custom `LifecycleOwner` tied to `FloatingService` lifecycle ensures Compose recomposition works inside overlay.

---

## 4. Mini-Apps Implemented

| Window | Tech | Highlights |
|---|---|---|
| **Browser** | WebView | Address bar, tabs, incognito, desktop mode, downloads via DownloadManager |
| **Notes** | Room + Compose TextField | Multi-page, rich text (bold/italic), auto-save, export TXT |
| **Calculator** | Compose state | Scientific functions, history, copy result |
| **Document Viewer** | PdfRenderer + WebView | PDF via PdfRenderer, DOC/TXT via Google Docs viewer fallback |
| **File Manager** | Storage Access Framework | List, copy, move, delete, open with |
| **Clipboard History** | ClipboardManager listener | Auto-capture, pin, copy back |
| **Clock** | Compose animation | Analog/digital, stopwatch, timer with notification |
| **YouTube** | WebView iframe | `youtube.com/embed` + background playback via WebView |
| **Translator** | Google Translate web + MyMemory free API | 100+ languages, auto-detect |
| **Music Player** | MediaSession + MediaController | Controls for any playing app via `MediaSessionManager` |
| **Quick Settings** | Settings.System + AudioManager | Brightness slider, volume, WiFi/Bluetooth toggles (via intents where restricted) |
| **App Launcher** | PackageManager + ActivityView | Grid of installed apps → launch in floating container |
| **URL Creator** | Dialog + WebView | Any URL → floating window |
| **Widget** | AppWidgetHost | Embed home-screen widgets in floating window |

---

## 5. Permissions & Manifest (see `AndroidManifest.xml`)

* `SYSTEM_ALERT_WINDOW` — core
* `FOREGROUND_SERVICE` + `FOREGROUND_SERVICE_SPECIAL_USE`
* `POST_NOTIFICATIONS` (Android 13+)
* `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` (optional, with Play Store disclosure)
* `QUERY_ALL_PACKAGES` (needed to list installed apps — Play Store requires declaration; alternative declared: `queries` tag with `CATEGORY_LAUNCHER`)
* `READ_MEDIA_*`, `READ_EXTERNAL_STORAGE` for File Manager / Document Viewer
* `BIND_ACCESSIBILITY_SERVICE` (optional)

---

## 6. Play Store Compliance

1. **Overlay disclosure:** Onboarding explains *why* overlay is needed (multitasking). No pre-ticked consent.
2. **QUERY_ALL_PACKAGES:** Use `<queries>` for launcher apps; only request `QUERY_ALL_PACKAGES` if you truly need full list — otherwise Play Console will reject. This project uses `<queries>` with `LAUNCHER` intent to avoid the sensitive permission.
3. **Battery whitelist:** Don't auto-request without user action; show rationale bottom sheet first. Safer to use `ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS` rather than direct request on some OEMs.
4. **AccessibilityService:** Declare `accessibilityServiceInfo` with minimal flags. Google tightly reviews overlay + accessibility combos — provide video demo for reviewer.
5. **Foreground service types:** Android 14+ requires explicit `foregroundServiceType`. Use `specialUse` with `property` description in manifest.
6. **WebView YouTube:** Use iframe, don't violate YouTube ToS by background-playing without user gesture — document it.
7. **Data safety form:** Declare clipboard, file access.

---

## 7. Build & Run

```bash
./gradlew assembleDebug
adb install app/build/outputs/apk/debug/app-debug.apk
# Grant overlay: Settings → Apps → FloatMaster → Display over other apps → Allow
```

On first launch, follow onboarding. Then tap dock or notification to create windows.

---

## 8. Key Classes Deep Dive

See source files:
* `service/FloatingWindowManager.kt` — authoritative singleton
* `service/FloatingService.kt` — foreground service + notification
* `overlay/FloatingWindowContainer.kt` — WindowManager params + lifecycle
* `overlay/WindowChrome.kt` — drag/resize/transparency UI
* `apps/browser/FloatingBrowser.kt` — tabbed WebView example
* `permission/BatteryOptimizationHelper.kt` — OEM handling

---

## 9. OEM Battery Guide (Xiaomi, Oppo, Vivo, Huawei, Samsung)

`BatteryOptimizationHelper` detects manufacturer and deep-links to auto-start / background-settings screens. See `OemBatteryScreen.kt` for per-OEM instructions with screenshots.

---

## 10. Future Improvements

* Picture-in-Picture fallback for video
* S Pen / stylus support for Notes
* Taskbar integration (Android 12L+)
* Cloud sync for Notes via WorkManager
* Window snapping grid + Aero Snap

---

MIT — built as reference implementation.
