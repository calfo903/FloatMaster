# How to Build FloatMaster

> **Repo:** `https://github.com/calfo903/FloatMaster` · **Branch:** `main` · **Last pull:** `71f1631` (ci: code quality + Hilt 2.60.1) · **Tech:** Kotlin 1.9, AGP 8.3, Gradle 8.x, JDK 17, minSdk 26, targetSdk 34, Jetpack Compose Material3

This guide covers **every verified way** to build FloatMaster — **Chocopy IDE (phone)**, **Android Studio (recommended)**, **CLI**, and **GitHub Actions artifact**. All produce the same `app-debug.apk`.

---

## 0. Prerequisites (all methods)

| Tool | Version | Check |
|---|---|---|
| **JDK** | 17 (not 8/11/21) | `java -version` → `17` |
| **Android SDK** | Platform 34 + Build Tools 34.0.0 | `sdkmanager --list` |
| **Gradle** | 8.3 (wrapper) | `./gradlew --version` |
| **Kotlin** | 1.9.22 | from `build.gradle.kts` |
| **Git** | any | `git --version` |

`local.properties` must exist in project root:
```properties
sdk.dir=/opt/android-sdk          # Android Studio default: /Users/<you>/Library/Android/sdk (macOS) or /home/<you>/Android/Sdk (Linux)
# OR on phone Chocopy: sdk.dir=/storage/emulated/0/Android/Sdk  (see Chocopy → Settings → SDK location)
```

---

## 1. Chocopy IDE — Build on Your Phone (No PC)

**Use when:** you only have an Android phone. Chocopy IDE is a mobile Gradle IDE (like AIDE/CodeAssist). Steps are identical for `AIDE`, `CodeAssist`, `Sketchware Pro`.

### Step-by-step

1. **Install Chocopy IDE** — Play Store → `Chocopy IDE` → Open → `Allow files` → `Allow install unknown apps` (for APK install later).

2. **Clone:**
   - Chocopy → `Open → Clone from GitHub` → paste `https://github.com/calfo903/FloatMaster.git` → `Clone` → wait 60s.
   - *If Clone button fails:* Install `Termux` → `pkg install git -y && git clone https://github.com/calfo903/FloatMaster.git /storage/emulated/0/Download/FloatMaster` → back in Chocopy `Open → /Download/FloatMaster → Open as Gradle Project → Trust`.

3. **Open as Gradle Project:** `Open → Select folder FloatMaster → Open as Gradle Project`.

4. **First Sync (3-5 min, keep screen on + WiFi):**
   Chocopy auto-runs `Download SDK 34 + JDK 17 + Gradle 8.3`.
   - **Error `SDK not found`:** `Settings → SDK Manager → Install Android SDK Platform 34 + Build Tools 34.0.0` → `Apply`.
   - **Error `local.properties`:** `Files → FloatMaster → New File: local.properties` → paste `sdk.dir=/storage/emulated/0/Android/Sdk` (verify via `Settings → SDK location`).
   - **Error `Kotlin 1.9 not found`:** `Settings → Clear Gradle cache → Resync`.
   - **OOM on 4GB phones:** `Settings → Gradle → JVM args: -Xmx1536m` → `Invalidate & Restart`.

5. **Build APK:**
   Bottom bar → `Build → assembleDebug` (or `▶ Run → Debug`).
   Success → `app/build/outputs/apk/debug/app-debug.apk`

6. **Install & Grant Overlay (critical):**
   Tap `app-debug.apk` → `Install` → Open `FloatMaster` → `Grant Overlay Permission` → `Allow display over other apps` → Back → `Launch Dock` → `Open AI Group → Launch 12`.
   *If bubbles don’t appear:* `Settings → Apps → FloatMaster → Battery → Unrestricted` + `Autostart → Allow` (Xiaomi/Oppo/Vivo/Huawei).

7. **Terminal alternative (inside Chocopy):**
   `Terminal → cd /storage/emulated/0/Download/FloatMaster && ./gradlew assembleDebug --stacktrace && ls app/build/outputs/apk/debug/`

---

## 2. Android Studio — Recommended (2 min)

```bash
git clone https://github.com/calfo903/FloatMaster.git
cd FloatMaster
# Open Android Studio → File → Open → FloatMaster → Sync (auto)
# Select device: Pixel 7 API 34 (or physical device with USB debugging)
# Run ▶ (Shift+F10) → installs app-debug.apk
```

**Verified:** Android Studio Hedgehog+ (AGP 8.3, Kotlin 1.9 plugin).

---

## 3. CLI — `gradlew` (Linux/macOS/Termux)

```bash
git clone https://github.com/calfo903/FloatMaster.git
cd FloatMaster
echo "sdk.dir=$HOME/Android/Sdk" > local.properties   # or /opt/android-sdk

# Build
./gradlew assembleDebug --stacktrace          # → app/build/outputs/apk/debug/app-debug.apk
./gradlew testDebugUnitTest                    # unit tests (MockK + Turbine)
./gradlew lintDebug                            # lint

# Install to connected device/emulator
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb logcat | grep FloatMaster
```

**Termux on phone (no Chocopy):**
```bash
pkg install openjdk-17 git -y
git clone https://github.com/calfo903/FloatMaster.git
cd FloatMaster
./gradlew assembleDebug
```

---

## 4. GitHub Actions — No Build Needed (Download APK)

Every push to `main` builds an APK via `.github/workflows/android.yml` (+ `code-quality.yml` added in `71f1631`).

1. Open `https://github.com/calfo903/FloatMaster/actions`
2. Click latest `Android CI` run (green ✓) → `Artifacts` → `FloatMaster-debug` → Download ZIP → unzip → `app-debug.apk`
3. Transfer to phone → Install → Grant overlay.

---

## 5. After Install — First Run Checklist

1. Open FloatMaster → `Onboarding` → `Grant Overlay Permission` → `Allow`.
2. `Battery whitelist` → `Don’t optimize` (or OEM auto-start: Xiaomi → Autostart, Samsung → Never sleeping apps).
3. Tap `Launch Dock` → floating pill appears.
4. `Home → AI Group → Launch 12` → 12 floating WebViews (ChatGPT, Claude, Gemini, …) appear over any app.
5. `Browser` → test `Back/Forward/Refresh/Stop`, `Desktop` chip, `⋮ → Downloads / Ad-block / Reader / History`, pinch zoom, `⋮ → Save as PDF`.

---

## 6. Pull Review (what changed since your last build)

```bash
git fetch origin
git log --oneline --graph -8          # you should see 71f1631 as HEAD after pull
git pull --rebase origin main         # fast-forwarded c395aec → 71f1631
```

**Pulled 4 commits:**
- `71f1631` `ci: add GitHub Actions workflow for code quality and security analysis` (.github/workflows/code-quality.yml)
- `df9a183` `fix: update Hilt plugin from 2.50 to 2.60.1 in root build.gradle.kts` (Hilt 2.60.1, androidx.core 1.19.0, espresso 3.7.0)
- `c7736da` `fix: update all critical dependencies` 
- `dfedf63` `Merge pull request #5 from calfo903/dependabot/gradle/...`

No conflict with your local `c395aec Browser polish` — fast-forward succeeded.

---

## 7. Troubleshooting

| Error | Fix |
|---|---|
| `SDK not found` | `echo "sdk.dir=/path/to/Sdk" > local.properties` |
| `Unsupported Java 21` | Switch to JDK 17: `export JAVA_HOME=/path/to/jdk-17` |
| `AAPT2 error` | `SDK Manager → reinstall Build Tools 34.0.0` |
| `Kotlin 1.9 not found` | `rm -rf ~/.gradle/caches` → Sync again |
| `Overlay not granted` | Must tap system dialog → `Settings → Apps → FloatMaster → Display over other apps → Allow` |
| `Windows close on swipe` | `Battery → Unrestricted` + `Autostart → Allow` + `KeepAlive` service |
| `Push 403` | `gh auth login` or `git remote set-url origin https://<PAT>@github.com/calfo903/FloatMaster.git` |

---

## 8. Useful Commands

```bash
./gradlew clean
./gradlew :app:dependencies --configuration debugRuntimeClasspath | grep window
adb shell settings can-draw-overlays com.floatmaster  # check overlay
adb shell dumpsys window | grep -i floatmaster
```

**Need help for a specific Chocopy error?** Paste the exact `Build Output` log and I’ll give the one-line fix.

