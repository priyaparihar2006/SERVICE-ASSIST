# MASTER PROMPT 14: Replace numbered red badges on icons with a simple red dot (Servora / ServiceAssist)

Paste everything below the line into your coding agent. Work in the repo root `ServiceAssist/`.

---

## 0. ROLE AND WORKING RULES

You are a senior Android engineer (Kotlin, Jetpack Compose).

1. Post a 5-line plan, then work through every phase without stopping. Report once at the end.
2. Don't ask questions unless you are blocked.
3. UI only. **Don't change any counting logic, ViewModels, repositories or the server.** The counts still exist; only how they're shown changes.
4. Keep icon sizes, positions and nav-bar layout the same. Only the badge changes.
5. **No tests.** The only checks are `./gradlew assembleDebug` once at the end and the manual checks in Phase 4.

## 1. GOAL
Every red number badge on an icon ("3", "9+", "99+") becomes a **small red dot with no number**, like Instagram or WhatsApp's "new" dot. It just tells the user there's something to see. The dot shows when count > 0 and disappears at 0.

## 2. INVENTORY (verified in the repo)

| # | File / location | Current | Change |
|---|---|---|---|
| 1 | `ui/components/ServoraBottomNav.kt` ~575-610, `BookingsCalendarOutlineIcon` (customer **Bookings** tab) | red circle + number / "99+" | → dot |
| 2 | `ui/components/PartnerBottomNav.kt` ~340-370 (partner **Jobs** tab) | red circle + number / "99+" | → dot |
| 3 | `ui/components/AdminBottomNav.kt` ~255-270 (admin **Bookings** tab, `pendingBookingsCount`) | orange `Badge` + number / "9+" | → red dot (consistent everywhere) |
| 4 | `ui/screens/PartnerJobsScreen.kt` ~223-240 (partner header **bell**, `unreadNotificationsCount`) | red circle + number / "9+" | → dot |
| 5 | `ui/components/ServoraTopBar.kt` ~160-175 (customer header **bell**) | **no badge at all**, even though `unreadNotificationsCount` is passed in | **add** the dot when `unreadNotificationsCount > 0` |
| 6 | `ui/components/PartnerJobSections.kt` ~469 (chat icon on the job card) | already a 9dp red dot, hard-coded `Color(0xFFE53935)` | switch to the shared component |
| 7 | `ui/screens/PartnerBookingsScreen.kt` ~971 (chat icon on the booking card) | already an 8dp red dot, hard-coded color | switch to the shared component |

Before editing, run `grep -rn "99+\|\"9+\"\|badgeRed\|BadgedBox\|Badge(\|0xFFE53935\|0xFFEF4444" app/src/main/java` and add any other **icon** badge you find to this table.

**Out of scope (leave these as they are):** these aren't red icon badges; they're in-list counts, like WhatsApp's chat list.
- the green unread count bubble on each chat row (`ChatListScreen.kt` ~473-490)
- the "Active" filter-tab count (`ChatListScreen.kt` ~244-260)
- the "Message (n)" button text (`BookingsListScreen.kt` ~667)
- the "N unread in active chats" subtitle (`ChatListScreen.kt` ~132)

## 3. THE FIX

### Phase 1: One shared dot component
Create `ui/components/NotificationDot.kt`:
```kotlin
package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.ServoraTheme

/**
 * Small red "something new" dot. No number by design.
 * ringColor = the background the icon sits on, so the dot separates cleanly from the icon.
 */
@Composable
fun NotificationDot(
    modifier: Modifier = Modifier,
    size: Dp = 9.dp,
    ringColor: Color? = null,
    contentDescription: String = "New updates"
) {
    val base = modifier
        .size(size)
        .clip(CircleShape)
        .semantics { this.contentDescription = contentDescription }
    Box(
        modifier = (if (ringColor != null) base.border(1.5.dp, ringColor, CircleShape) else base)
            .background(ServoraTheme.colors.badgeRed)
    )
}
```
- Put the border **before** the background so the ring sits on the outer edge.
- Use `ServoraTheme.colors.badgeRed` everywhere (it already exists in both themes). Remove the hard-coded `0xFFE53935` / `0xFFEF4444` at these call sites.

### Phase 2: Replace each badge (keep each badge's current anchor position)
1. **Customer Bookings tab** (`ServoraBottomNav.kt`): in `BadgedBox`'s `badge = { … }`, replace the whole numbered `Box { Text(...) }` with:
   ```kotlin
   if (isBadgeVisible) NotificationDot(
       modifier = Modifier.offset(x = 2.dp, y = (-2).dp),
       ringColor = ServoraTheme.colors.navBarBackground.copy(alpha = 1f),
       contentDescription = "Bookings has updates"
   )
   ```
2. **Partner Jobs tab** (`PartnerBottomNav.kt`): the same, with `contentDescription = "Jobs has updates"`.
3. **Admin Bookings tab** (`AdminBottomNav.kt`): replace `Badge(containerColor = navColors.warning){ Text(...) }` with `NotificationDot(ringColor = <nav bar background>, contentDescription = "Pending bookings")`.
4. **Partner header bell** (`PartnerJobsScreen.kt`): replace the 16dp numbered `Box { Text }` with
   `NotificationDot(Modifier.align(Alignment.TopEnd).padding(top = 7.dp, end = 7.dp), ringColor = <header green>, contentDescription = "Unread notifications")`.
   Use the same header color token the header already uses. Don't hard-code it.
5. **Customer header bell** (`ServoraTopBar.kt`): inside the existing 38dp bell `Box`, after the `Icon`, add
   `if (unreadNotificationsCount > 0) NotificationDot(Modifier.align(Alignment.TopEnd).padding(top = 8.dp, end = 8.dp), ringColor = ServoraTheme.colors.headerBackgroundStart, contentDescription = "Unread notifications")`.
6. **Chat icon on the job card / booking card** (`PartnerJobSections.kt`, `PartnerBookingsScreen.kt`): replace the hand-made dot `Box` with `NotificationDot(Modifier.align(Alignment.TopEnd).padding(3.dp), size = 8.dp, contentDescription = "Unread messages")`.
- Remove imports that are no longer used (`TextAlign`, `PlatformTextStyle`, `defaultMinSize`, and so on) only if nothing else in the file uses them.
- Keep `BadgedBox` where it already exists, since it handles the anchoring.

### Phase 3: Accessibility
- Each dot has a meaningful `contentDescription` (see above). The **parent icon's** own description stays unchanged.
- Don't read out numbers. The count is intentionally hidden.

### Phase 4: Verify (manual, on device)
1. `./gradlew assembleDebug`.
2. Customer with active bookings: the Bookings tab shows a red dot, with no number. With 0 active, there's no dot.
3. Customer with unread notifications: the bell shows a red dot. Open Notifications → Mark all read, and the dot disappears.
4. Partner: the Jobs tab and the header bell show dots. The job card chat icon shows a dot when there are unread messages, and it clears after opening the chat.
5. Admin: the Bookings tab shows a red dot when there are pending bookings.
6. Light and dark mode: the dot is clearly visible, and the ring separates it from the icon. Icons and the nav layout haven't shifted.
7. Chat list rows still show their green unread counts (unchanged, as intended).

## 4. ACCEPTANCE CRITERIA
- No red or orange numbered badge remains on any icon. `grep -rn "99+\|\"9+\"" app/src/main/java` returns 0 results.
- All icon badges use the single `NotificationDot` component with the theme `badgeRed`.
- The dot shows exactly when the underlying count is > 0. No counting logic was changed.
- The customer bell now shows a dot for unread notifications.
- In-list counts (chat rows, filter tab, "Message (n)") are unchanged. The build passes.

## 5. FINAL REPORT FORMAT
Plan (5 lines), files changed (one line each), any extra badges found by the grep, and pass/fail for Phase 4 steps 2-7.
