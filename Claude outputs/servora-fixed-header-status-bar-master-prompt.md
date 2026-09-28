# MASTER PROMPT 12: Keep headers fixed when the status bar hides on scroll (Servora / ServiceAssist)

Paste everything below the line into your coding agent. Work in the repo root `ServiceAssist/`. The "hide status bar on scroll" feature (`servora-scroll-hide-status-bar-master-prompt.md`) stays. Only the header jump gets fixed.

---

## 0. ROLE AND WORKING RULES

You are a senior Android engineer (Kotlin, Jetpack Compose, WindowInsets, edge-to-edge).

1. Post a 5-line plan, then work through every phase without stopping. Report once at the end.
2. Don't ask questions unless you are blocked. State any assumption in one line.
3. **Zero visual change** other than removing the jump. Keep the same colors, heights, paddings, header designs and bottom bars. Don't redesign anything, and don't remove the hide-on-scroll feature.
4. **No tests.** The only checks are `./gradlew assembleDebug` once at the end and the manual checks in Phase 5.
5. Android only. Don't touch the server, chat logic or navigation.

## 1. SYMPTOM (screen recording, chat thread "Priya · Client")

When the user drags the chat list, the status bar hides. The **whole header (back arrow, title, "Active Booking" strip) jumps up** by the status-bar height, into the area the status bar used. When the status bar comes back, the header **jumps down again**. On small drags it flickers up and down repeatedly. The same thing happens on every screen that uses `hideStatusBarOnScroll()` (Home, Bookings, Explore, Profile, Partner screens and so on).

## 2. ROOT CAUSE (verified in the repo)

### RC1: the header's top padding follows the status bar's visibility
- `MainActivity` runs `enableEdgeToEdge()`, and `ui/components/SystemBarsAppearance.kt` → `Modifier.hideStatusBarOnScroll()` calls `controller.hide(WindowInsetsCompat.Type.statusBars())` on scroll.
- Every header reserves space for the status bar with **visibility-dependent** insets:
  - `ChatThreadScreen.kt:~134` and `ChatListScreen.kt`: Material3 `TopAppBar(...)` with the default `windowInsets = TopAppBarDefaults.windowInsets`, which means `WindowInsets.statusBars`
  - about 20 places use `Modifier.statusBarsPadding()`, including `ServoraTopBar.kt:67`, `MainActivity.kt:1093` (incoming banner), `HomeScreen`, `BookingsListScreen`, `ExploreScreen`, `ProfileScreen`, `OffersScreen`, `Partner*Screen`, `ServiceDetailScreen`, `BookingFlowScreen`, `BookingConfirmationScreen`, `PaymentCollectionScreen`, `NotificationsScreen`, `LoginScreen`, `AdminDashboardScreen`, `SearchOverlay`, `LocationPickerModal`. Get the full list with `grep -rn "statusBarsPadding()\|TopAppBar(" app/src/main/java`.
- When the status bar is hidden, `WindowInsets.statusBars.top` becomes **0**. The padding collapses, so the header moves up. When the bar is shown, it becomes about 24-32dp again, so the header moves down. That is exactly the jump in the video.

### RC2: the hide/show toggle has no hysteresis
`statusBarScrollConnection` calls `hide()`/`show()` on **every** pre-scroll event where `|dy| > 8px`, even if the bar is already in that state. Small back-and-forth drags flip it repeatedly, which makes the jump look like flicker.

## 3. THE FIX

### Phase 1: One stable status-bar inset helper
Create `ui/components/StableInsets.kt`:
```kotlin
package com.example.ui.components

import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBarsIgnoringVisibility
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed

/** Status-bar height that does NOT change when the bar is hidden/shown. */
@OptIn(ExperimentalLayoutApi::class)
val WindowInsets.Companion.stableStatusBars: WindowInsets
    @Composable get() = WindowInsets.statusBarsIgnoringVisibility

/** Drop-in replacement for statusBarsPadding() that never jumps. */
@OptIn(ExperimentalLayoutApi::class)
fun Modifier.stableStatusBarsPadding(): Modifier = composed {
    windowInsetsPadding(WindowInsets.statusBarsIgnoringVisibility)
}
```
- `statusBarsIgnoringVisibility` exists in the current Compose BOM (`2024.09.00` → foundation 1.7). On minSdk 24-29 it's backed by the stable insets through `WindowInsetsCompat.getInsetsIgnoringVisibility`, so it works on all supported versions.
- If the build says it's unavailable, bump **only** the Compose BOM to the nearest version that has it. Report that change.

### Phase 2: Replace every visibility-dependent status-bar inset
1. **Every** `.statusBarsPadding()` in `app/src/main/java` → `.stableStatusBarsPadding()`. Add the import. Don't change the modifier order or any other padding.
2. **Every** `TopAppBar(` / `CenterAlignedTopAppBar(` (ChatThreadScreen, ChatListScreen): add
   `windowInsets = WindowInsets.stableStatusBars,`
3. **Every** `Scaffold(` (`MainActivity.kt:341`, `ChatListScreen.kt:113`, `ChatThreadScreen.kt:128`, `PartnerJobDetailScreen.kt:133`):
   - If it passes `contentWindowInsets = WindowInsets(0,0,0,0)`, leave it alone (MainActivity).
   - Otherwise set `contentWindowInsets = WindowInsets.stableStatusBars.union(WindowInsets.navigationBars)`. If the screen already applies `navigationBarsPadding()` on the Scaffold modifier (ChatThreadScreen does), use `WindowInsets.stableStatusBars` only, to avoid double bottom padding.
   - This keeps `innerPadding.top` stable on screens where the body reads it.
4. Search for anything else reading status-bar insets and switch it to the stable version: `grep -rn "WindowInsets.statusBars\|WindowInsets.systemBars\|safeDrawing\|safeContent" app/src/main/java`.
5. Leave navigation-bar and IME insets unchanged. The bottom composer and `imePadding()` in ChatThreadScreen stay as they are.

Result: when the status bar hides, the header stays exactly where it is. The status-bar strip shows the header's own background (it's edge-to-edge and the header Surface/TopAppBar already draws behind it), so it looks the same as now, just without the jump.

### Phase 3: Hysteresis for hide/show (`SystemBarsAppearance.kt`)
Rewrite `statusBarScrollConnection` / `hideStatusBarOnScroll` so that:
- It keeps an internal `var hidden = false` and a running `accumulated` distance (px).
- In `onPreScroll`: add `available.y` to `accumulated`. Reset `accumulated` to 0 when the scroll direction flips.
- Hide only if `!hidden && accumulated < -threshold`. Show only if `hidden && accumulated > threshold`. Use `threshold = 24.dp` converted to px with `LocalDensity`. After a toggle, set `hidden` and reset `accumulated = 0`.
- Never call `hide()`/`show()` when the state wouldn't change.
- Add a `DisposableEffect(Unit) { onDispose { controller.show(WindowInsetsCompat.Type.statusBars()) } }` so leaving a screen always restores the status bar. That stops the next screen from opening with the bar hidden.
- Keep `BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE` and the public API `Modifier.hideStatusBarOnScroll()` unchanged, so no call sites change.

### Phase 4: Don't break these
- Status-bar icon colors (`StatusBarIcons`) and dark-mode behavior are unchanged.
- The green customer header (`ServoraTopBar`) still fills behind the status bar with no white gap.
- `LocationPickerModal` / `SearchOverlay` full-screen overlays still sit below the status bar area.
- The incoming-message banner (`MainActivity.kt:~1093`) doesn't jump when the bar toggles.

### Phase 5: Verify (manual, on device)
1. `./gradlew assembleDebug`.
2. Chat thread (Priya · Client): drag up and down slowly and quickly. The status bar hides and shows, but the back arrow, title and "Active Booking" strip **don't move a single pixel**. Screen-record it and compare the header's Y position in the frames.
3. Tiny back-and-forth drags don't flicker the status bar.
4. Repeat on Home (green customer header), Bookings, Explore, Profile, Partner Jobs and Partner Earnings. The header stays fixed and the status-bar area keeps the header color.
5. Leave a screen while the bar is hidden. The next screen opens with the status bar visible.
6. Light and dark mode, gesture and 3-button navigation, and with the keyboard open in chat. Layouts are identical to before, apart from the missing jump.

## 4. ACCEPTANCE CRITERIA
- No header, top bar or banner changes position when the status bar hides or shows, on any screen.
- Hide-on-scroll still works, with no flicker from small drags, and the bar is always restored when leaving a screen.
- No other visual change: same colors, sizes, paddings and bottom/keyboard behavior.
- `grep -rn "statusBarsPadding()" app/src/main/java` returns 0 results (all replaced), and every `TopAppBar` passes `windowInsets = WindowInsets.stableStatusBars`.
- The build passes.

## 5. FINAL REPORT FORMAT
Plan (5 lines), files changed (one line each, with the number of replacements), any BOM change, and results of Phase 5 steps 2-6 (pass/fail each).
