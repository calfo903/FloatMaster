# FloatMaster — Floating Multitasking Suite

[![Android CI](https://github.com/calfo903/FloatMaster/actions/workflows/android.yml/badge.svg)](https://github.com/calfo903/FloatMaster/actions)
![Kotlin](https://img.shields.io/badge/Kotlin-1.9-7F52FF?logo=kotlin)
![Compose](https://img.shields.io/badge/Compose-Material3-4285F4?logo=android)
![Min SDK 26](https://img.shields.io/badge/minSdk-26-brightgreen)
![Target 34](https://img.shields.io/badge/target-34-blue)
![License MIT](https://img.shields.io/badge/license-MIT-lightgrey)
![AI Pods 12](https://img.shields.io/badge/AI%20pods-12-6750A4)

> **Now with AI Chat Group — 12 floating AI pods (WebView/iframe) + Ask All broadcast, snap tiling, session restore, hardened 9.4**

FloatMaster replicates and improves *Floating Apps (LWi s.r.o.)* — resizable, draggable, minimizable floating windows over any app. New: one-tap **AI Group** with ChatGPT, Claude, Gemini, Perplexity, Grok, DeepSeek, Copilot, Meta AI, Poe, You.com, Mistral, Character.AI.

![Demo](docs/demo.gif)

## Features
- **Floating Engine** — `TYPE_APPLICATION_OVERLAY`, drag/resize, bubble (60dp), maximize, alpha 0.3-1, border, z-order, snap to half/quarter, PiP
- **AI Group** — Dashboard/Tabs/Tiled, **Ask All** broadcast, cascade 12, desktop UA, allowlist
- **11 Mini-Apps** — Browser (tabs), Notes (Room), Calculator (scientific), Document (PdfRenderer), FileManager (SAF), Clipboard (history), Clock (stopwatch/timer), YouTube (iframe), Translator (MyMemory), Music (MediaSession), QuickSettings
- **System** — FG `specialUse` service, `START_STICKY`, KeepAlive + WorkManager restore, OEM battery guide, Session restore, WebView pool

## Quick Start
```bash
./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
# Grant overlay → Launch Dock → AI Group → Launch 12
```

## Architecture
`Compose M3 → FloatingWindowManager (SSOT, Result<WindowId>, rate-limit 8/2s) → WindowOverlayController → WindowManager`
See `docs/OVERLAY_INTERNALS.md` and `docs/AI_CHATS_GROUP.md`

## Hardening 9.4
`WindowId` value class, `Instant`, `sealed Result`, no `!!`, `suspend` DataStore, WebView least-privilege, allowlist, `filterTouchesWhenObscured`, `// WHY:` on every change, MockK + Turbine tests

## Play Store
See `docs/PLAY_STORE_COMPLIANCE.md` — `queries` not `QUERY_ALL_PACKAGES`, `specialUse` FGS, disclosure video required.

## Contributing
See `CONTRIBUTING.md` — `// WHY:` required, `UUID`/`Instant`, `Result` envelope

MIT © calfo903
