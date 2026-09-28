# MASTER PROMPT 13: Status bar always visible and attached to the header, never hiding on scroll (Servora / ServiceAssist)

Paste everything below the line into your coding agent. Work in the repo root `ServiceAssist/`. Master Prompt 12 (fixed header with stable insets) is applied, so the header no longer jumps. This prompt **turns off status-bar hiding completely**, so the status bar and header stay together, like WhatsApp or Swiggy. It replaces the behavior from `servora-scroll-hide-status-bar-master-prompt.md`.

---

## 0. ROLE AND WORKING RULES

You are a senior Android engineer (Kotlin, Jetpack Compose, WindowInsets, edge-to-edge).

1. Post a 5-line plan, then work through every phase without stopping. Report once at the end.
2. Don't ask questions unless you are blocked.
3. **No visual change** other than the status bar staying visible. Keep headers, colors, paddings, the bottom nav and status-bar icon colors as they are.
4. **No tests.** The only checks are `./gradlew assembleDebug` once at the end and the manual checks in Phase 4.
5. Android only.

## 1. SYMPTOM
The header is now fixed. But when the user scrolls, the **status bar (time, battery, network) still slides away** and comes back, so it detaches from the header. Goal: the status bar is **always visible**, sitting on top of the header in the header's color, on every screen, while scrolling.

## 2. ROOT CAUSE (verified in the repo)
Two separate places still call `hide(WindowInsetsCompat.Type.statusBars())` on scroll:

1. **`MainActivity.kt` ~line 265-279:** the root `nestedScrollConnection` attached to the main `Scaffold` (`.nestedScroll(nestedScrollConnection)`, ~line 344). It hides the **bottom nav and the status bar** when scrolling down, and shows both when scrolling up.
2. **`ui/components/SystemBarsAppearance.kt` → `Modifier.hideStatusBarOnScroll()`:** used on about 20 screens (`grep -rn "hideStatusBarOnScroll()" app/src/main/java`). It hides and shows the status bar with hysteresis.

Also, `MainActivity.onCreate` (~line 147) and `hideStatusBarOnScroll` set `BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE`, a mode meant for apps that hide the bars.

## 3. THE FIX

### Phase 1: `MainActivity.kt`
1. In the root `nestedScrollConnection`, **delete only the two status-bar lines**:
   - `insetsController?.hide(WindowInsetsCompat.Type.statusBars())`
   - `insetsController?.show(WindowInsetsCompat.Type.statusBars())`

   **Keep** `isBottomNavVisible = false/true`, because the bottom-nav hide-on-scroll stays exactly as it is now.
2. Keep the `LaunchedEffect(currentTab, …, currentScreen) { … insetsController?.show(statusBars) }` as a safety net, so the bar is visible after any screen change.
3. In `onCreate`, replace `BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE` with `WindowInsetsControllerCompat.BEHAVIOR_DEFAULT`. Right after that, call `insetsController.show(WindowInsetsCompat.Type.statusBars())`.
4. Keep `enableEdgeToEdge()` and `StatusBarIcons(darkIcons = !isDarkHeader)` as they are.

### Phase 2: `SystemBarsAppearance.kt`
Turn `Modifier.hideStatusBarOnScroll()` into a **no-op** that keeps the same signature, so none of the ~20 call sites need editing:
```kotlin
/**
 * Status bar is intentionally always visible (attached to the header).
 * Kept as a no-op so existing call sites compile; safe to remove later.
 */
@Suppress("UnusedReceiverParameter")
@Composable
fun Modifier.hideStatusBarOnScroll(): Modifier = this
```
- Delete `statusBarScrollConnection` and the `DisposableEffect`/threshold code if nothing else uses them (check with grep). Remove any imports that are no longer used.
- **Don't** remove `.hideStatusBarOnScroll()` from the screens. It's harmless, and leaving it keeps this change to two files.
- Keep `StatusBarIcons` unchanged.

### Phase 3: Make sure nothing else hides the status bar
- Run `grep -rn "Type.statusBars()\|Type.systemBars()\|FLAG_FULLSCREEN\|systemBarsBehavior" app/src/main`. Only `show(...)` calls and `BEHAVIOR_DEFAULT` may remain. There must be no `hide(...)` of status bars anywhere.
- Check `app/src/main/res/values*/themes.xml` for `android:windowFullscreen` = `true` or `windowTranslucentStatus` = `true`. They must not be set. Report what you find without changing anything else.
- Keep the Master Prompt 12 changes (`stableStatusBarsPadding()`, `WindowInsets.stableStatusBars` on TopAppBars). They still give a correct, stable header height and are harmless now that the bar never hides.

### Phase 4: Verify (manual, on device)
1. `./gradlew assembleDebug`.
2. Chat thread: scroll up and down (quick flings and slow drags). The status bar (time, battery) **never disappears**. The header and status bar stay attached and don't move.
3. Home (green customer header): the status-bar area is green with light icons and stays put while scrolling. The **bottom nav still hides and shows on scroll** as before.
4. Bookings, Explore, Profile, Partner Jobs, Partner Earnings, Service Detail and Booking Flow: same result.
5. Dark mode: the status bar stays visible, with correct icon contrast.
6. Leave and re-enter screens, and open and close the keyboard in chat. The status bar is always visible and the layouts are unchanged.

## 4. ACCEPTANCE CRITERIA
- The status bar is visible at all times on every screen. It never hides on scroll.
- The header stays directly under the status bar with no gap and no movement.
- Bottom-nav hide-on-scroll still works.
- No `hide(WindowInsetsCompat.Type.statusBars())` is left in the codebase. Behavior is `BEHAVIOR_DEFAULT`.
- No other visual change. The build passes.

## 5. FINAL REPORT FORMAT
Plan (5 lines), files changed (one line each), the Phase 3 grep and themes.xml results, and pass/fail for Phase 4 steps 2-6.
