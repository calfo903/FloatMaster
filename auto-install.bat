@echo off
REM FloatMaster — Auto Install (Windows)
REM Usage: auto-install.bat [--release] [--device <serial>]
REM Requires: git, JDK 17, Android SDK, adb in PATH
setlocal EnableDelayedExpansion
set REPO=https://github.com/calfo903/FloatMaster.git
set DIR=FloatMaster
set MODE=debug

if "%1"=="--release" set MODE=release

echo [FloatMaster] Checking prerequisites...
where git >nul 2>&1 || (echo [FAIL] git not found & exit /b 1)
where java >nul 2>&1 || (echo [FAIL] java not found & exit /b 1)
where adb >nul 2>&1 || echo [WARN] adb not found — will build only

if not exist "%DIR%\.git" (
  if not exist "%DIR%" (
    echo [FloatMaster] Cloning %REPO%...
    git clone %REPO% || exit /b 1
  )
) else (
  echo [FloatMaster] Pulling...
  pushd %DIR% && git pull --rebase origin main & popd
)

cd %DIR% 2>nul || cd .

if not exist "local.properties" (
  if defined ANDROID_HOME (
    echo sdk.dir=%ANDROID_HOME:\=/% > local.properties
  ) else if exist "%LOCALAPPDATA%\Android\Sdk" (
    echo sdk.dir=%LOCALAPPDATA%\Android\Sdk > local.properties
  ) else if exist "%USERPROFILE%\Android\Sdk" (
    echo sdk.dir=%USERPROFILE%\Android\Sdk > local.properties
  ) else (
    echo [WARN] Create local.properties with sdk.dir=C:/path/to/Android/Sdk
  )
)

echo [FloatMaster] Building (%MODE%)...
if "%MODE%"=="release" (
  call gradlew.bat assembleRelease --stacktrace || exit /b 1
  set APK=app\build\outputs\apk\release\app-release.apk
) else (
  call gradlew.bat assembleDebug --stacktrace || exit /b 1
  set APK=app\build\outputs\apk\debug\app-debug.apk
)

if not exist "%APK%" (
  echo [FAIL] APK not found: %APK%
  exit /b 1
)
echo [OK] Built ^> %APK%

adb start-server >nul 2>&1
for /f "skip=1 tokens=1" %%a in ('adb devices ^| findstr "device$"') do set SERIAL=%%a & goto found
echo [WARN] No device — copy %APK% to phone and tap to install
exit /b 0
:found
echo [FloatMaster] Installing to %SERIAL%...
adb -s %SERIAL% install -r "%APK%" || exit /b 1
echo [OK] Installed!
adb -s %SERIAL% shell am start -n com.floatmaster/.MainActivity >nul 2>&1
echo [FloatMaster] Done! Grant overlay: Settings ^> Apps ^> FloatMaster ^> Display over other apps ^> Allow
pause
