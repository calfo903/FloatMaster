# FloatMaster — Floating Multitasking Suite

[![Android CI](https://github.com/calfo903/FloatMaster/actions/workflows/android.yml/badge.svg)](https://github.com/calfo903/FloatMaster/actions)
![Kotlin](https://img.shields.io/badge/Kotlin-2.4-7F52FF?logo=kotlin)
![Compose](https://img.shields.io/badge/Compose-Material3-4285F4?logo=android)
![Min SDK 26](https://img.shields.io/badge/minSdk-26-brightgreen)
![Target 36](https://img.shields.io/badge/target-36-blue)
![AI Pods 12](https://img.shields.io/badge/AI%20pods-12-6750A4)
![License MIT](https://img.shields.io/badge/License-MIT-lightgrey)

> **AI Chat Group — 12 floating AI pods + Ask All, snap tiling, persisted session restore, and hardened WebViews.**

FloatMaster provides user-controlled, resizable, draggable floating windows over other apps, with a desktop-style AI group and practical mini-apps.

## Features

- **Floating Engine** — `TYPE_APPLICATION_OVERLAY`, drag/resize, bubble, maximize, alpha 0.3–1, border, z-order, pinning, snap tiling and PiP
- **AI Group** — Dashboard/Tabs/Tiled, **Ask All** broadcast, 12 providers, desktop UA and exact-host allowlisting
- **Mini-Apps** — Browser (tabs), Notes, Calculator, Document/PDF, File Manager (SAF), Clipboard, Clock, YouTube, Translator, Music and Quick Settings
- **System** — one `specialUse` foreground service, `START_STICKY` recovery, reboot/package-update session restore, encrypted autofill and hardened FileProvider

## Quick Start

Use Gradle 9.3.1 with JDK 17:

```bash
gradle :app:assembleDebug
gradle :app:testDebugUnitTest
```

Then install `app/build/outputs/apk/debug/app-debug.apk`, grant overlay access, and launch the Dock.

## Architecture

`Compose M3 → FloatingWindowManager (SSOT, atomic Result envelope) → WindowOverlayController → WindowManager`

Session state is persisted through DataStore. The foreground service owns overlay lifecycle and restores saved state after OS service recreation or eligible reboot/update broadcasts.

See `docs/OVERLAY_INTERNALS.md` and `docs/AI_CHATS_GROUP.md`.

## Security posture

- No AccessibilityService.
- No `QUERY_ALL_PACKAGES`.
- No broad storage/media permissions.
- No battery-optimization exemption.
- AI WebViews: HTTPS + exact provider host, Safe Browsing, no file/content access, no mixed content, no popup windows and no native JS bridge.
- User-controlled JavaScript injection is JSON-encoded and bounded.
- FileProvider exposes only the dedicated share cache.
- R8/resource shrinking is enabled for release.

See `docs/SECURITY_AUDIT_2026.md` for the production audit and residual-risk register.

## Play Store

See `docs/PLAY_STORE_COMPLIANCE.md` for permission declarations, reviewer video steps, Data Safety preparation and OEM testing requirements.

## Contributing

See `CONTRIBUTING.md` — `// WHY:` required for security-sensitive changes, UUID/Instant identifiers, and explicit `Result` error handling.

MIT © calfo903
