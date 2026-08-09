# FloatMaster — Play Store Compliance Guide

This doc lists every sensitive permission / API and how to stay compliant.

## 1. SYSTEM_ALERT_WINDOW

- **Why:** Core feature = floating windows over other apps. Without it, multitasking is impossible.
- **Disclosure:** Onboarding screen explains *in plain language* why the permission is needed, with a Lottie animation of dragging a window. User must tap “Grant” — no pre-granted state.
- **Review video:** Record a 30-sec screen capture showing: launch app → onboarding → system overlay dialog → floating browser + notes on top of Chrome → drag/resize. Upload to Play Console “App content → Sensitive permissions”.

## 2. FOREGROUND_SERVICE + specialUse

- Android 14+ requires `foregroundServiceType`. We use `specialUse` with `<property android:name="android.app.PROPERTY_SPECIAL_USE_FGS_SUBTYPE" android:value="multitaskingOverlay"/>`.
- Notification is **low importance**, ongoing, with “Close all” + “Stop” actions. User can dismiss service via notification.
- Declare in Console: “Data safety → Foreground service” → purpose = “Multitasking overlay keeps windows alive; user explicitly starts it”.

## 3. QUERY_ALL_PACKAGES — AVOID IT

- **Do NOT** add `<uses-permission android:name="android.permission.QUERY_ALL_PACKAGES"/>` unless you absolutely need full inventory. Play will reject non-qualifying apps.
- Instead use `<queries>`:

```xml
<queries>
  <intent><action android:name="android.intent.action.MAIN"/><category android:name="android.intent.category.LAUNCHER"/></intent>
</queries>
```

Then `queryIntentActivities(launcherIntent, 0)` still returns all launchable apps on Android 11+ for that intent, which is sufficient for “turn any app into floating”.

- If you later need full package list for enterprise, submit “Declaration → QUERY_ALL_PACKAGES” with video justification (file manager / launcher use-case qualifies, but expect extra review days).

## 4. ACCESSIBILITY SERVICE

- Declare only if you truly need it. FloatMaster’s accessibility is **optional** — core floating works without it.
- `accessibility_service_config.xml` must set `android:description` that matches Play Console declaration. Description must say: “This service is used to detect foreground app for auto-minimize and to enable swipe gestures on floating windows. No data is collected.”
- Provide a separate toggle in Settings: “Enable advanced gestures (requires accessibility)”. Don’t enable by default.
- Play now requires a **privacy policy** URL if you declare accessibility.

## 5. REQUEST_IGNORE_BATTERY_OPTIMIZATIONS

- Safer to use `ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS` (opens list) rather than direct `ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` (dialog). Direct request is flagged as “high risk” unless your core function is alarm/clock.
- FloatMaster’s core function *is* “keep windows alive”, so direct request is defensible, but show a bottom-sheet rationale first: “Android may close floating windows when battery saver is on. Allow FloatMaster to run unrestricted?”
- Alternative: guide user to OEM auto-start screens via `BatteryOptimizationHelper.openOemAutoStartSettings()`.

## 6. STORAGE / MEDIA

- Target SDK 34 uses scoped storage. Request `READ_MEDIA_IMAGES/VIDEO/AUDIO` (API 33+) and legacy `READ_EXTERNAL_STORAGE` with `maxSdkVersion=32`.
- Use `Storage Access Framework` (SAF) for File Manager on Android 11+ where direct file paths are restricted. Fallback to `MANAGE_EXTERNAL_STORAGE` only if you are a genuine file manager — otherwise Play will reject.

## 7. WEBVIEW + YOUTUBE

- YouTube iframe via `WebView` is allowed, but do **not** enable background playback without user gesture that bypasses ads — that violates YouTube ToS and Play’s “Deceptive behavior”.
- Add disclosure: “YouTube content is loaded via youtube.com embed; background play respects site policy.”

## 8. DATA SAFETY FORM

- Declare:
  - Clipboard: “Collected, not shared, ephemeral, user-initiated”
  - Files: “Accessed only when user picks a document”
  - No location, no contacts unless you add them.

## 9. TESTING CHECKLIST BEFORE SUBMIT

- [ ] Test on Pixel (stock), Xiaomi MIUI 14, Samsung One UI 6, Oppo ColorOS 13, Huawei EMUI 12.
- [ ] Kill app from recents → windows + notification must survive (or gracefully restore).
- [ ] Rotate device → windows reposition correctly.
- [ ] Dark mode toggle → Material You dynamic colors apply.
- [ ] Deny overlay → app shows onboarding, doesn’t crash.
- [ ] Deny notifications (Android 13+) → service still starts, but warn user they won’t see controls.
- [ ] Record reviewer video (no debug banner).

## 10. COMMON REJECTION REASONS & FIXES

| Rejection | Fix |
|---|---|
| “QUERY_ALL_PACKAGES not allowed” | Remove permission, use `<queries>` |
| “Accessibility not core” | Make accessibility optional, hide behind toggle + disclosure |
| “Foreground service not declared” | Add `foregroundServiceType` + property for Android 14 |
| “Battery optimization request without rationale” | Show bottom sheet before requesting |
| “WebView uploads file without prompt” | Ensure file picker uses SAF intent |

---

**Bottom line:** Keep the app honest — every sensitive API maps to a visible user action. If the reviewer can see *why* in 30 seconds, you pass.
