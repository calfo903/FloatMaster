# AI Chats Group — 12 Floating WebView Pods

FloatMaster now includes a first-class **AI Chat Group** that lets you float **10+ AI chats simultaneously**, just like the original request: *"Floating at least 10 ai chats (using iframe or WebView) in group"*.

## Providers (12)

| # | Chat | URL | WindowType | Short ID |
|---|---|---|---|---|
| 1 | **ChatGPT** | https://chatgpt.com/ | `AI_CHATGPT` | gpt |
| 2 | **Claude** | https://claude.ai/ | `AI_CLAUDE` | claude |
| 3 | **Gemini** | https://gemini.google.com/ | `AI_GEMINI` | gemini |
| 4 | **Perplexity** | https://www.perplexity.ai/ | `AI_PERPLEXITY` | pplx |
| 5 | **Grok** | https://grok.com/ | `AI_GROK` | grok |
| 6 | **DeepSeek** | https://chat.deepseek.com/ | `AI_DEEPSEEK` | deepseek |
| 7 | **Copilot** | https://copilot.microsoft.com/ | `AI_COPILOT` | copilot |
| 8 | **Meta AI** | https://www.meta.ai/ | `AI_META` | meta |
| 9 | **Poe** | https://poe.com/ | `AI_POE` | poe |
| 10 | **You.com** | https://you.com/ | `AI_YOU` | you |
| 11 | **Mistral** | https://chat.mistral.ai/chat | `AI_MISTRAL` | mistral |
| 12 | **Character.AI** | https://character.ai/ | `AI_CHARACTER` | char |

All are `WebView` pods (Android equivalent of iframe) with desktop UA so they render like a desktop browser.

## Three Ways to Float

### 1. Dashboard Group Window (`AI_GROUP`)
The hero window `WindowType.AI_GROUP` (420×640dp) is itself a floating window. Inside:

- **Dashboard tab** — hero card + grid of 12 providers

  Each card has **Float** (opens that single AI as its own window) and **Tab** (opens inside grouped tabs).

  **Launch All 12 · Cascade** — creates 12 separate `FloatingWindow`s with `manager.create(...)` and cascaded geometry (`x + idx%4*28, y + idx/4*36`) so they tile diagonally and don't fully overlap.

  **Launch 10 · Tiled set** — creates 10 windows with grid-calculated `x/y` for a 2-column tiling.

  **Inside Tabs** — switches to Tab mode inside the same group window.

- **Tabs tab** — `ScrollableTabRow` with 12 tabs, one WebView visible at a time (lazy Key). Per-tab toolbar shows URL + **Desktop/Mobile UA** toggle. Cookies/third-party enabled so logins persist.

- **Tiled tab** — `LazyVerticalGrid(columns=2)` where each cell is a live WebView pod (240dp tall). First 4 auto-load, rest have **Load** button to save RAM. Demonstrates "12 at once inside one floating window".

### 2. Separate Floating Pods (10+ simultaneous)
You can also bypass the group and create any single AI directly:

```kotlin
manager.create(WindowType.AI_CHATGPT, title="ChatGPT", url="https://chatgpt.com/")
manager.create(WindowType.AI_CLAUDE, ...)
 // repeat 10× → you get 10 independent draggable/resizable WebView windows over any app
```

Or one-tap from the home screen: **Home → AI Chats ×12 → Launch 10 Now** does exactly that cascade without opening the group window first.

The Dock also has a primary **AI ×12** pill that opens the group, plus a ChatGPT shortcut.

### 3. Home Strip & App Grid
- **Home → AI strip** (`LazyRow`) shows all 12 as 96dp cards; tap to float.
- **Active tab** lists every AI pod alongside other windows; you can bring to front, bubble, minimize per-pod.

## WebView Hardening

Each pod (`FloatingAiChatContent`) does:

```kotlin
CookieManager.getInstance().setAcceptCookie(true)
CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true)
settings.javaScriptEnabled = true
settings.domStorageEnabled = true
settings.mixedContentMode = MIXED_CONTENT_ALWAYS_ALLOW
settings.userAgentString = if (desktopMode) DESKTOP_UA else MOBILE_UA
// Chrome 124 desktop UA: fixes Claude/Gemini "unsupported browser"
settings.javaScriptCanOpenWindowsAutomatically = true
webViewClient.shouldOverrideUrlLoading → stay inside
webChromeClient.onProgressChanged → LinearProgressIndicator
```

- **Desktop vs Mobile toggle** per pod — many AI sites block mobile WebView UA with 403. Toggling reloads with alternate UA.
- **_blank handling** → loads in same WebView (prevents popup loss).
- **Focus dance** → tap title bar → `FLAG_NOT_FOCUSABLE` cleared → keyboard shows; outside tap restores.

## Geometry & Z-Order

- Default size per pod: 380×600dp (via `WindowType` default). Group window is larger: 420×640.
- Cascade offset 28/36dp ensures all 10 remain visible and draggable.
- `FloatingWindowManager` assigns monotonic `zIndex`; `FloatingWindowContainer` re-adds the view to bring to front on focus/tap.
- Each pod independently supports minimize → title bar, bubble → 60dp edge circle, maximize → fullscreen, alpha 0.3–1.0, border toggle.

## RAM Considerations

12 simultaneous WebViews ≈ 300–600MB. The **Tiled** mode lazily loads: only 4 pods load immediately, others wait for **Load** tap. The **Tabs** mode only keeps the selected provider's WebView alive (keyed `AndroidView`). For **Cascade** (12 separate windows) Android will WebView-throttle off-screen pods; user can close any to reclaim memory.

## Code Map

```
apps/aichat/
  AiChatProvider.kt          // enum + DESKTOP_UA constant
  FloatingAiChat.kt          // single pod WebView + FloatingAiChatRoutedContent
  FloatingAiChatGroup.kt     // AI_GROUP Dashboard/Tabs/Tiled
model/WindowType.kt          // +13 AI entries
model/MiniApp.kt             // AI_GROUP in top grid, aiSingles helper
overlay/WindowChrome.kt      // + manager param, routes AI_* → aichat
overlay/FloatingWindowContainer.kt // passes manager
overlay/FloatingDock.kt      // hero AI ×12 pill
ui/screens/HomeScreen.kt     // AI hero card + strip + Launch 10 button
docs/AI_CHATS_GROUP.md       // this doc
```

## Adding a New AI

1. Add entry to `AiChatProvider` and `WindowType`.
2. No other code change — `FloatingAiChatRoutedContent` routes by `window.type`.
3. Optional: add to `MiniAppCatalog.aiSingles` or Dock shortcuts.

## Testing Checklist

- Grant overlay, then Home → **Open AI Group** → verify 12 cards show.
- Tap **Launch All 12 Cascade** → 12 windows appear offset; drag one, toggle alpha, bubble one.
- Inside group → Tabs → switch ChatGPT ↔ Claude → each retains login.
- Tabs → toggle Desktop/Mobile → reload works.
- Tiled → tap Load on You.com tile → live WebView appears inside grid, scroll, resize group window → grid reflows.
- Kill one AI pod → Active tab count decrements, notification badge updates.

## Why WebView and not iframe?

Android has no iframe; `WebView` is the native iframe equivalent. For a web demo, wrap `FloatingAiChatGroup` content with `<iframe src="https://chatgpt.com">` same way — the architecture is identical; only the embedding tag differs.
