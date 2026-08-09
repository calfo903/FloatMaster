# FloatMaster — Play Store Compliance Pack

## Release posture

- Target SDK: **37 (Android 17)**
- Minimum SDK: **26**
- Overlay permission: `SYSTEM_ALERT_WINDOW`
- Foreground service: one exported=false `specialUse` service, started only for the user-controlled floating-window feature
- Accessibility service: **removed** — FloatMaster has no AccessibilityService dependency
- `QUERY_ALL_PACKAGES`: **not requested**
- Storage/media permissions: **not requested**; documents use Storage Access Framework
- Battery-optimization exemption: **not requested**
- WebView: hardened; AI pods use exact HTTPS provider-host allowlists

## 1. Sensitive permissions

| Permission/API | Status | User-facing justification |
|---|---|---|
| `SYSTEM_ALERT_WINDOW` | Required | Core feature: user-created floating windows over other apps. |
| `FOREGROUND_SERVICE` | Required | Keeps the user-visible overlay service alive while windows are active. |
| `FOREGROUND_SERVICE_SPECIAL_USE` | Required | Declares the overlay service's `specialUse` FGS type on Android 14+. |
| `POST_NOTIFICATIONS` | Required on Android 13+ for normal notification UX | Shows active-window count and Stop/Close All controls. |
| `RECEIVE_BOOT_COMPLETED` | Required for opt-in session recovery behavior | Restores a previously persisted floating session after reboot/update when overlay access is still granted. |
| `WRITE_SETTINGS` | Feature-specific | Only the Quick Settings brightness control uses this special access. Request it only from the explicit brightness action. |
| `INTERNET` | Required | Browser, translator, YouTube and AI WebViews. |

Removed from the manifest: Wi-Fi/Bluetooth state permissions, wake lock, battery-optimization exemption, legacy storage/media permissions, accessibility service declarations, and unused services.

## 2. Foreground-service declaration

Manifest declaration:

```xml
<service
    android:name=".service.FloatingService"
    android:exported="false"
    android:foregroundServiceType="specialUse">
    <property
        android:name="android.app.PROPERTY_SPECIAL_USE_FGS_SUBTYPE"
        android:value="persistent_user_initiated_floating_overlay_windows" />
</service>
```

Reviewer explanation:

> FloatMaster provides user-controlled floating windows above other applications. The foreground service owns those overlay windows and displays a persistent low-importance notification with Stop and Close All actions. The service is not used for silent background work.

## 3. Overlay disclosure

The onboarding flow must show, before requesting the permission:

> **Allow FloatMaster to appear over other apps?**
> FloatMaster needs this permission to display the floating windows you create while you use other apps. It does not grant FloatMaster control of other apps.

The user must explicitly choose the system permission action.

### Reviewer video

Record a short release-build video showing:

1. Launch FloatMaster.
2. Explain the overlay permission in-app.
3. Grant the system overlay permission.
4. Create a Browser and Notes window.
5. Drag, resize, minimize and close a window.
6. Show the persistent FGS notification and Stop action.
7. Reboot with a saved session and show session recovery.

## 4. Accessibility API

**No AccessibilityService is shipped.**

Do not declare or describe Accessibility API usage in Play Console. Gesture handling is implemented by the app's own Compose/overlay touch events.

## 5. Package visibility

Do not add `QUERY_ALL_PACKAGES`.

The manifest uses a narrow launcher `<queries>` intent for the app-launcher feature. No unrestricted package inventory is requested.

## 6. Files and media

FloatMaster uses the Storage Access Framework (`ACTION_GET_CONTENT`) when the user chooses a document or file upload.

- No `READ_EXTERNAL_STORAGE`.
- No `READ_MEDIA_*`.
- No `MANAGE_EXTERNAL_STORAGE`.
- FileProvider exposes only `cache/shares/`.
- Selected documents are copied into app-private cache before local rendering.

## 7. WebView security

### AI pods

All 12 AI providers use:

- HTTPS only.
- Exact hostname allowlisting; suffix/substring matches are rejected.
- Main-frame navigation validation on start, finish and navigation requests.
- JavaScript enabled only because the AI sites require it.
- File access disabled.
- Content access disabled.
- Mixed content disabled.
- Safe Browsing enabled.
- Automatic JavaScript windows disabled.
- Multiple windows disabled.
- No native JavaScript bridge.
- Prompt injection strings are JSON-encoded before `evaluateJavascript`.

### General browser

The general browser intentionally supports arbitrary HTTPS sites. It must never treat generic browser content as trusted application content. It uses SAF for file selection and does not request broad storage permissions.

### Third-party AI services

AI prompts and account activity are transmitted directly to the selected third-party provider's website through its WebView. FloatMaster does not proxy those prompts through a FloatMaster server. The Play listing/privacy materials must identify this behavior clearly.

## 8. Data Safety preparation

Declare only data flows actually present in the release build.

Potential user-controlled data flows:

- **Clipboard:** stored locally when the user uses Clipboard history; not uploaded by FloatMaster.
- **Documents/files:** accessed only after user selection; stored temporarily in app-private cache.
- **Browser history:** stored locally in DataStore.
- **Autofill usernames:** encrypted locally; never uploaded by FloatMaster.
- **AI prompts/account data:** sent directly to the third-party provider selected by the user because the provider's website is loaded in the WebView.

Before publishing, reconcile these statements with the exact Play Console Data Safety questionnaire and the privacy policy.

## 9. Privacy policy minimum disclosures

The published privacy policy should explain:

- What FloatMaster stores locally.
- That overlay permission is required for floating windows.
- That selected documents are copied temporarily to private app storage.
- That browser history/clipboard/autofill data remain local unless the user intentionally shares them.
- That AI provider websites receive content entered into those sites and are governed by their own privacy policies.
- How users delete local history, clipboard and autofill data.
- Contact method for privacy requests.

## 10. Release validation matrix

| Area | Required before release |
|---|---|
| Android 10 / API 29 | Unit + connected tests |
| Android 14 / API 34 | Unit + connected tests + FGS checks |
| Current Android / API 37 | Unit + connected tests + release R8 build |
| R8 | `assembleRelease` must pass; inspect mapping and runtime smoke test |
| Overlay denied | App remains usable and does not crash |
| Notification denied | FGS remains policy-compliant and user can recover via app UI |
| Service killed | `START_STICKY` restarts the service and persisted state can restore |
| OEM/task-killer scenario | Reboot/restart recovery path must be tested on representative OEMs |
| WebView | Navigation, settings and render-process failure tests pass |
| Play policy | Permission declarations match shipped manifest exactly |

## 11. OEM test matrix

At minimum test one current device/emulator for:

- Pixel / AOSP
- Samsung One UI
- Xiaomi HyperOS/MIUI
- OPPO/ColorOS
- One additional OEM with aggressive background process management

Record whether overlay access survives reboot, whether the FGS is restarted, and whether saved session state is restored.

## 12. Reviewer notes

FloatMaster's core purpose is **user-controlled floating multitasking**. Every sensitive capability should be demonstrated as a direct consequence of that feature. Avoid describing removed Accessibility, battery-exemption, or broad-storage capabilities in the Play listing or reviewer notes.
