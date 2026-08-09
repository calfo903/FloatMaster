# Contributing to FloatMaster
1. Fork → branch `feat/your-feature` → PR to `main`
2. `./gradlew testDebugUnitTest` must pass, no `!!`, `// WHY:` on every change
3. KDoc on public APIs, `UUID`/`Instant`, `Result<T>` envelope
4. WebView changes must stay allowlisted + `mixedContent=NEVER_ALLOW`
