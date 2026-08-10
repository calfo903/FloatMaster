#!/usr/bin/env bash
# FloatMaster — Auto Install Script
# One-command build + install: ./auto-install.sh  [--device] [--release] [--no-build]
# Works on: Linux, macOS, Termux (Android), WSL, and Chocopy IDE terminal
# Requires: git, JDK 17, Android SDK (or Android Studio), adb
set -euo pipefail

REPO="https://github.com/calfo903/FloatMaster.git"
DIR="FloatMaster"
APK_DEBUG="app/build/outputs/apk/debug/app-debug.apk"
APK_RELEASE="app/build/outputs/apk/release/app-release.apk"

# ── helpers ──────────────────────────────────────────────────────────────
log()  { echo -e "\033[1;34m[FloatMaster]\033[0m $*"; }
ok()   { echo -e "\033[1;32m[OK]\033[0m $*"; }
warn() { echo -e "\033[1;33m[WARN]\033[0m $*"; }
die()  { echo -e "\033[1;31m[FAIL]\033[0m $*"; exit 1; }

need() { command -v "$1" >/dev/null 2>&1 || die "Missing '$1'. Install it first. See HOW_TO_BUILD.md"; }

# ── args ─────────────────────────────────────────────────────────────────
MODE="debug"
NO_BUILD=false
DEVICE=""
for a in "$@"; do
  case "$a" in
    --release) MODE="release" ;;
    --no-build) NO_BUILD=true ;;
    --device) DEVICE="next" ;; # placeholder
    --device=*) DEVICE="${a#*=}" ;;
    --help|-h) echo "Usage: ./auto-install.sh [--release] [--no-build] [--device=<serial>]"; exit 0 ;;
  esac
done

# ── 1. Prereqs ───────────────────────────────────────────────────────────
log "Checking prerequisites..."
need git
need java
need adb || warn "adb not found — will build but not install. Install platform-tools."

JAVA_VER=$(java -version 2>&1 | head -n1 | grep -oE '[0-9]+\.[0-9]+' | head -n1 || echo "0")
log "Java: $(java -version 2>&1 | head -n1)"
if ! java -version 2>&1 | grep -q "17\."; then
  warn "JDK 17 recommended. Found: $JAVA_VER — build may fail."
  warn "Fix: export JAVA_HOME=/path/to/jdk-17  (Android Studio: /Applications/Android Studio.app/Contents/jbr/Contents/Home)"
fi

# Android SDK check via local.properties or ANDROID_HOME
if [ -f "$DIR/local.properties" ]; then
  log "Found $DIR/local.properties"
elif [ -n "${ANDROID_HOME:-}" ] && [ -d "$ANDROID_HOME" ]; then
  log "ANDROID_HOME=$ANDROID_HOME"
elif [ -n "${ANDROID_SDK_ROOT:-}" ] && [ -d "$ANDROID_SDK_ROOT" ]; then
  log "ANDROID_SDK_ROOT=$ANDROID_SDK_ROOT"
else
  # Try common locations
  for p in "$HOME/Android/Sdk" "$HOME/Library/Android/sdk" "/opt/android-sdk" "/storage/emulated/0/Android/Sdk" "/sdcard/Android/Sdk"; do
    if [ -d "$p" ]; then
      log "Auto-detected SDK at $p → writing local.properties"
      mkdir -p "$DIR" 2>/dev/null || true
      echo "sdk.dir=$p" > "$DIR/local.properties"
      break
    fi
  done
  if [ ! -f "$DIR/local.properties" ] && [ ! -f "local.properties" ]; then
    warn "No SDK found. Create local.properties with: sdk.dir=/path/to/Android/Sdk"
    warn "Chocopy: Settings → SDK location → copy path → echo \"sdk.dir=...\" > local.properties"
  fi
fi

# ── 2. Clone / Pull ──────────────────────────────────────────────────────
if [ -d "$DIR/.git" ]; then
  log "Repo exists → git pull --rebase"
  (cd "$DIR" && git pull --rebase origin main || warn "pull failed — continuing with local")
else
  if [ -d "$DIR" ]; then
    log "Folder $DIR exists but not git → using it"
  else
    log "Cloning $REPO ..."
    git clone "$REPO" || die "git clone failed"
  fi
fi

cd "$DIR" 2>/dev/null || cd .

# Ensure local.properties exists in project root
if [ ! -f "local.properties" ] && [ -f "../local.properties" ]; then cp ../local.properties local.properties; fi
if [ ! -f "local.properties" ]; then
  for p in "$HOME/Android/Sdk" "$HOME/Library/Android/sdk" "/opt/android-sdk" "/storage/emulated/0/Android/Sdk"; do
    if [ -d "$p" ]; then echo "sdk.dir=$p" > local.properties; log "Created local.properties → $p"; break; fi
  done
fi

# Make gradlew executable (Chocopy/Termux loses +x)
chmod +x gradlew 2>/dev/null || true
chmod +x ./gradlew 2>/dev/null || true

# ── 3. Build ─────────────────────────────────────────────────────────────
if [ "$NO_BUILD" = true ]; then
  log "Skipping build (--no-build)"
else
  log "Building ($MODE) — first build downloads ~500MB, keep WiFi on..."
  if [ "$MODE" = "release" ]; then
    ./gradlew assembleRelease --stacktrace || die "Release build failed (need signing config). Try --debug."
  else
    ./gradlew assembleDebug --stacktrace || die "Build failed. See HOW_TO_BUILD.md Troubleshooting."
  fi
  APK="$APK_DEBUG"
  [ "$MODE" = "release" ] && APK="$APK_RELEASE"
  if [ ! -f "$APK" ]; then
    # Search fallback
    APK=$(find app/build/outputs -name "*.apk" | head -n1 || true)
    [ -z "$APK" ] && die "APK not found after build"
  fi
  ok "Built → $APK ($(du -h "$APK" | cut -f1))"
else
  APK="$APK_DEBUG"
  [ "$MODE" = "release" ] && APK="$APK_RELEASE"
  [ ! -f "$APK" ] && APK=$(find app/build/outputs -name "*.apk" 2>/dev/null | head -n1 || echo "$APK")
fi

# ── 4. Device / Install ──────────────────────────────────────────────────
log "Checking adb devices..."
adb start-server >/dev/null 2>&1 || true
DEVICES=$(adb devices | grep -v "List" | grep "device$" | awk '{print $1}' || true)
if [ -z "$DEVICES" ]; then
  warn "No device connected. Connect phone via USB (USB debugging ON) or start emulator."
  warn "Install manually: copy $APK to phone → tap to install"
  log "To grant overlay without device, enable on phone: Settings → Apps → FloatMaster → Display over other apps → Allow"
  exit 0
fi

# Pick device
if [ -n "$DEVICE" ] && [ "$DEVICE" != "next" ]; then
  SERIAL="$DEVICE"
else
  SERIAL=$(echo "$DEVICES" | head -n1)
  if [ "$(echo "$DEVICES" | wc -l)" -gt 1 ]; then
    log "Multiple devices:"
    echo "$DEVICES" | cat -n
    log "Using first: $SERIAL (pass --device=<serial> to choose)"
  fi
fi

log "Installing to $SERIAL ..."
adb -s "$SERIAL" install -r "$APK" || die "adb install failed. Try: adb -s $SERIAL install -r $APK"

ok "Installed!"

# ── 5. Post-install — grant overlay + launch ─────────────────────────────
log "Launching FloatMaster..."
adb -s "$SERIAL" shell am start -n com.floatmaster/.MainActivity >/dev/null 2>&1 || warn "am start failed"

log "Trying to auto-grant overlay (Android 8-13; 14+ needs manual)..."
# This only works on rooted or via adb shell settings? We try, but warn if fails.
adb -s "$SERIAL" shell appops set com.floatmaster SYSTEM_ALERT_WINDOW allow >/dev/null 2>&1 && ok "Overlay auto-granted via appops" || warn "Auto-grant failed — grant manually: Settings → Apps → FloatMaster → Display over other apps → Allow"

log "Battery whitelist hint (OEMs kill overlays):"
adb -s "$SERIAL" shell dumpsys deviceidle whitelist +com.floatmaster >/dev/null 2>&1 && ok "Battery whitelisted" || warn "Whitelist manually: Settings → Battery → Unrestricted"

ok "Done! On phone: open FloatMaster → Launch Dock → AI Group → Launch 12"
log "Uninstall: adb -s $SERIAL uninstall com.floatmaster"
log "Logs: adb -s $SERIAL logcat | grep -i floatmaster"
log "APK path: $(pwd)/$APK"

# ── 6. Optional: open overlay settings ───────────────────────────────────
read -r -p "Open overlay settings now? [y/N] " ans 2>/dev/null || ans="n"
if [[ "$ans" =~ ^[Yy] ]]; then
  adb -s "$SERIAL" shell am start -a android.settings.action.MANAGE_OVERLAY_PERMISSION -d package:com.floatmaster >/dev/null 2>&1 || true
fi
