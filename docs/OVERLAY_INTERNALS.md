# Overlay Internals — Deep Dive

## WindowManager Choice Over Freeform

Android’s freeform multi-window (`ActivityOptions.setLaunchWindowingMode(WINDOWING_MODE_FREEFORM)`) is only reliably available on large screens / desktop mode (Android 12L+, Samsung Dex). OEMs often disable it on phones. `WindowManager.addView(TYPE_APPLICATION_OVERLAY)` works on **every** device back to API 26 and gives pixel-perfect control over position, size, alpha, and z-order. That’s why FloatMaster uses it as primary, with `ActivityView` as optional enhancement for “launch any app floating”.

## Lifecycle Inside Service

`ComposeView` needs a `LifecycleOwner`, `ViewModelStoreOwner`, and `SavedStateRegistryOwner`. A normal Activity provides those; a Service does not. We make `FloatingService` itself implement all three:

```kotlin
class FloatingService : Service(), ViewModelStoreOwner, SavedStateRegistryOwner {
  private val lifecycleRegistry = LifecycleRegistry(this)
  private val savedStateController = SavedStateRegistryController.create(this)
  // ...
  override fun onCreate() {
    savedStateController.performAttach()
    savedStateController.performRestore(null)
    lifecycleRegistry.currentState = State.RESUMED
  }
}
```

Then each `ComposeView` does:

```kotlin
composeView.setViewTreeLifecycleOwner(service)
composeView.setViewTreeViewModelStoreOwner(service)
composeView.setViewTreeSavedStateRegistryOwner(service)
```

Without this, Compose would never recompose and `remember` would break.

## Focus Dance

`TYPE_APPLICATION_OVERLAY` windows are `FLAG_NOT_FOCUSABLE` by default, otherwise they steal all input from the underlying app (bad UX—you’d want to type in Chrome while a small notes window floats). But `EditText` inside the overlay needs focus to show keyboard.

Pattern:

```
- Start with FLAG_NOT_FOCUSABLE
- On title bar tap or content tap → clear FLAG_NOT_FOCUSABLE, requestFocus(), updateViewLayout()
- On outside touch (WindowManager.FLAG_WATCH_OUTSIDE_TOUCH + onTouchOutside) or back press → add FLAG_NOT_FOCUSABLE again
- Bring to front on focus so z-order matches expectation
```

## Drag & Resize Math

We attach `detectDragGestures` to the title bar. The gesture gives pixel deltas in Compose coordinates — we forward them as `Int` px deltas to the container, which does `params.x += dx`. `WindowManager.updateViewLayout` is cheap; we also debounce writes to the `FloatingWindowManager` (which persists geometry to DataStore).

Resize uses a 24.dp handle in the bottom-right corner. Minimum size is 220×180dp to avoid collapsing to zero. We clamp to screen bounds via `WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS` — without it, windows can’t be dragged partially off-screen (a popular power-user request).

## Alpha & Border

`params.alpha` controls window-level opacity. `0.3f` is minimum—lower makes it impossible to interact. Border is purely Compose: `Modifier.border(1.dp, outlineVariant, RoundedCornerShape(16.dp))`. Toggle is instant—no WindowManager update needed.

## Bubble Mode

Bubble is a **separate** `WindowManager` window (60dp circle) at `TYPE_APPLICATION_OVERLAY` with only the app icon. We swap: `container.destroy()` → `BubbleView.addView()`. Bubble’s `x/y` is independent so user can fling it to an edge. Tapping restores `NORMAL` by re-creating the container at the bubble’s position.

## Z-Order

`WindowManager` has no `bringToFront` API. The trick is `removeView` + `addView` — the last added view is topmost. `FloatingWindowManager` assigns monotonically increasing `zIndex`; `FloatingService` observes and re-adds when `zIndex` changes.

## Configuration Changes

`FloatingService` is not destroyed on rotation. But screen size changes, so we listen to `onConfigurationChanged` and clamp all windows to new screen bounds. Dark mode is handled by `FloatMasterTheme` recomposing with `isSystemInDarkTheme()`.

## OEM Killing

Xiaomi/OnePlus kill FGS within seconds if battery optimization is on. Countermeasures:

1. Foreground notification (required)
2. `START_STICKY` + `BootReceiver` (best-effort restore)
3. Battery whitelist prompt on first launch (with OEM-specific instructions)
4. Optional `KeepAliveService` heartbeat (Android 12+ restricts background FGS starts, so it only restarts when app is foregrounded)
5. WorkManager periodic check (not shown, easy to add)

## Turning “Any App” Into Floating

Three levels, gracefully degraded:

1. **ActivityView (API 33+)** — Embed another Activity’s task inside our overlay container via `ActivityView` + `TaskOrganizer`. Requires `android.permission.ACTIVITY_EMBEDDING` on some OEMs; works best on tablets.
2. **Freeform launch** — `Intent.FLAG_ACTIVITY_LAUNCH_ADJACENT | FLAG_ACTIVITY_NEW_TASK` + `ActivityOptions.setLaunchWindowingMode(WINDOWING_MODE_FREEFORM)`. Asks system to open the target app in a freeform window next to ours (user sees two windows, ours is overlay).
3. **Fallback** — Normal `startActivity` launch; not truly floating, but listed as quick-launcher so user still benefits from having all apps one tap away inside the floating dock.

In all cases we catch `SecurityException` and show a Snackbar fallback.

## Performance Notes

- Each window is a separate `ComposeView` → separate composition. 5 windows = 5 compositions, but Compose is lightweight; we’ve tested 8 simultaneous windows at 60fps on a 4GB RAM device.
- WebView is the heaviest mini-app (30–50MB per tab). We cap browser tabs at 5 and destroy WebView on window close.
- `PdfRenderer` renders pages to Bitmap on demand; we cache current page bitmap and recycle on page change.
