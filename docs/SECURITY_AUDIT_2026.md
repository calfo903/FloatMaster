# FloatMaster — Security / Production Audit (2026)

## Scope

Audit covered the Android application architecture, floating-window state boundary, overlay WindowManager boundary, foreground-service lifecycle, boot recovery, AccessibilityService surface, WebViews/AI providers, manifest permissions, persistence, release/R8 configuration, tests and Play compliance documentation.

## High-impact findings fixed

### P0/P1 — Build integrity

- Removed duplicate Hilt binding for `FloatingWindowManager`.
- Removed invalid unused `SessionRestoreWorker` whose constructor could not be created by WorkManager/Hilt.
- Removed dead KeepAlive foreground service.
- Removed dead WebView pool.
- Removed dependency on the JitPack-only PDF viewer after migrating local PDF rendering to `PdfRenderer`.
- Added missing `androidx.webkit` dependency.
- Added test dependencies for MockK, AndroidX lifecycle and instrumentation.
- Replaced a non-functional Gradle-wrapper workflow with an explicit Gradle 9.3.1 CI setup.

### P0/P1 — Overlay state

- Serialized all `FloatingWindowManager` mutations under one lock.
- Made rate limiting and max-window checks atomic.
- Added URL/scheme validation before state mutation.
- Added exact AI-provider hostname validation.
- Clamped window geometry to display bounds.
- Added bounded session restoration.
- Fixed z-order change detection in the overlay container.
- Stopped recreating the Compose composition on every overlay update.
- Introduced `WindowOverlayController` as the exception-containing WindowManager boundary.
- Implemented pinned-window state instead of a no-op menu action.
- Added snap/tiling interaction coverage.

### P0/P1 — WebView security

- AI navigation is exact-host allowlisted.
- AI WebViews reject non-HTTPS URLs.
- AI WebViews disable file/content access and mixed content.
- AI WebViews enable Safe Browsing.
- AI WebViews disable automatic JavaScript windows and multiple windows.
- AI prompts/selectors are JSON-encoded before JavaScript injection.
- Removed file-access WebView rendering from the document viewer.
- Hardened encrypted autofill storage and rejected unsafe values that could become JavaScript injection payloads.
- Added runtime WebView setting and navigation-policy tests.

### P1 — Service/recovery

- Removed the inert watchdog FGS.
- `FloatingService` remains `START_STICKY` for OS service recreation.
- Persisted window state is debounced to DataStore.
- Service restart with a null Intent restores the persisted session.
- Boot/package-replacement recovery starts only when overlay access is still granted and saved session state exists.
- Restore jobs are cancellable to prevent close/restore races.
- Added boot-recovery policy tests and repository recreation persistence tests.

### P1 — Permissions / Play surface

Removed unused permissions and components, including AccessibilityService, legacy media/storage permissions, Wi-Fi/Bluetooth state permissions, wake lock and battery-optimization exemption.

Retained only permissions tied to shipped functionality: overlay, FGS/specialUse, notifications, boot recovery, optional WRITE_SETTINGS brightness control, and INTERNET.

## Residual risks requiring device validation

1. OEM firmware may suppress or delay `START_STICKY` service recreation.
2. Overlay permission can be revoked by the user or OEM settings; all UI paths must degrade gracefully.
3. AI sites can change their DOM and break Ask All selectors without creating an Android security vulnerability.
4. Third-party AI websites receive data entered by the user; this must remain explicit in privacy/Play disclosures.
5. General browser mode intentionally loads arbitrary HTTPS sites; it must never be treated as trusted UI.
6. WebView renderer crashes should be covered with `onRenderProcessGone` before final lint-clean release if the current WebKit lint flags it.

## Guardian re-audit checklist

Before merge/release:

- [ ] `lintDebug` clean.
- [ ] Unit tests clean.
- [ ] `assembleRelease` clean with R8/resource shrinking.
- [ ] Connected tests pass on API 29, 34 and 37.
- [ ] No unexpected manifest permission changes.
- [ ] No AccessibilityService component.
- [ ] No broad R8 keep rules.
- [ ] No `!!` in production Kotlin.
- [ ] No unvalidated external URL reaches an AI WebView.
- [ ] No plaintext autofill fallback.
- [ ] No FileProvider path broader than the dedicated share cache.
- [ ] Boot recovery cannot start the FGS without both overlay access and saved session state.
