# FloatMaster — Quick Start

## 1. Clone & Open

```bash
git clone <this-repo> FloatMaster
cd FloatMaster
# Open in Android Studio Hedgehog+ (AGP 8.3, Kotlin 1.9, JDK 17)
```

## 2. Build

```bash
./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

## 3. First Launch Checklist

1. Accept overlay prompt → Allow.
2. Battery whitelist → “Don’t optimize” (or OEM auto-start screen).
3. Tap **Launch Dock** → floating pill appears at bottom.
4. Inside app tap **Browser**, **Notes**, **Calculator** → three floating windows appear over the Home screen.
5. Drag title bar → moves. Drag corner → resizes. Tap `○` → bubble. Tap bubble → restores. Tap `☐` → maximizes.
6. In Browser, open `https://youtube.com` → floats over Settings.

## 4. Create URL Window

Home → **New URL Window** → enter `https://notion.so` → creates Browser window with that URL.

## 5. Launch Any App Floating

Home → **All Apps** → grid of installed apps → tap Chrome → launches adjacent (freeform where supported, else normal). On tablets with `ActivityView`, it embeds directly.

## 6. Window Manager

Home → **Active** tab → list of windows → bring to front / bubble / close. Also accessible from notification shade: pull down → FloatMaster → **Close all**.

## 7. Customize

Settings tab → Dark mode, snap-to-edge, transparency defaults. Per-window transparency via title-bar `⋮` → **Transparency** slider. Border toggle in same menu.

## 8. File Locations

- `app/src/main/java/com/floatmaster/service/FloatingWindowManager.kt` — create/close/bringToFront logic
- `app/src/main/java/com/floatmaster/overlay/FloatingWindowContainer.kt` — WindowManager params + focus
- `app/src/main/java/com/floatmaster/overlay/WindowChrome.kt` — drag/resize/chrome UI
- `app/src/main/java/com/floatmaster/apps/*` — each mini-app’s Compose content

## 9. Troubleshooting

| Symptom | Fix |
|---|---|
| “Permission denied” toast | Settings → Apps → FloatMaster → Display over other apps → Allow |
| Windows close when app swiped away | Disable battery optimization + enable auto-start (see onboarding Step 2) |
| Keyboard not showing inside Notes | Tap title bar to focus window first |
| File Manager empty | Grant media permissions; on Android 11+ use SAF picker |
| YouTube shows “playback error” | Some videos block embed; try another or open in Browser window |

## 10. Next Steps

- Add your own mini-app: copy `apps/calculator/` → `apps/mywidget/`, define a `WindowType.MY_WIDGET`, add case in `WindowChrome.kt`, and `manager.create(WindowType.MY_WIDGET)`.
- Theme: edit `ui/theme/Theme.kt` + enable `dynamicColor=false` to lock brand palette.
- Release: `./gradlew assembleRelease` + sign with `keystore.properties`.
