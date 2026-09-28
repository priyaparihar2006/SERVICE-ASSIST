# MASTER PROMPT 11: Sent messages on the right, received on the left, with distinct bubble colors (Servora / ServiceAssist)

Paste everything below the line into your coding agent. Work in the repo root `ServiceAssist/`. Master Prompt 10 (the chat-send 500 fix) is applied and sending works.

---

## 0. ROLE AND WORKING RULES

You are a senior Android engineer (Kotlin, Jetpack Compose, Room, MVVM).

1. Post a 5-line plan, then work through every phase without stopping. Report once at the end.
2. Don't ask questions unless you are blocked. State any assumption in one line.
3. Keep changes small. **Android only.** No Supabase or edge-function changes.
4. **No tests.** Don't write unit or UI tests. The only check is `./gradlew assembleDebug` once at the end, plus the manual checks in Phase 5.
5. Don't touch send, receipts, polling or encryption logic beyond what's listed here.

## 1. GOAL (WhatsApp-style)

- **My messages** are on the **right**, in the brand-green bubble, with ticks.
- **The other person's messages** are on the **left**, in a clearly different (white) bubble, with no ticks.
- **Booking/system cards** stay **centered**.
- This must hold for both roles (customer `user_priya_1` and partner `pro_rajesh_1`), in light and dark mode, after an app restart, and after switching profiles on the same phone.

## 2. ROOT CAUSE (verified in the repo)

`ChatBubble` in `ui/components/ChatComponents.kt` already does left/right correctly (`Alignment.End` vs `Alignment.Start`, `fillMaxWidth()` parent). `ChatThreadScreen.kt:~338` passes `isOutgoing = msg.isMine`. **The bug is in the value of `isMine`, not the layout.**

### RC1: `isMine` is stored in the shared Room cache and reused for the wrong user
- `data/db/ChatEntities.kt` → `ChatMessageEntity` stores `isFromMe: Boolean`, keyed only by message `id`. There's **no owner column**. `chat_conversations` has `ownerProfileId`, but messages don't.
- `ChatViewModel.openThread` (~line 362-385) first renders the cached rows with `isMine = entity.isFromMe`.
- It then only fetches **newer** messages (`afterSeq = cachedMaxSeq`, ~line 419), so older cached rows are **never corrected**.
- When the customer and partner are tested on the same phone (dev profile switcher → `MainActivity` → `chatViewModel.onSessionChanged`), whoever cached a message first decides the side for everyone. The other user then sees received messages on the right, as if they had sent them.
- `onSessionChanged` resets in-memory state but never clears the Room chat cache. `ChatDao.clearForOwner` exists but **is never called**.

### RC2: "mine" has three different sources of truth
`isMine` comes from `dto.isMine` (server), `entity.isFromMe` (Room), or is forced to `true` (optimistic send). The fix is to have **one rule**, computed at mapping time from data that doesn't depend on who is viewing:
```
isMine = message.senderId == currentUserId
```

### RC3: the received bubble barely differs from the background
Light mode: `chatBubbleTheirs = #F3F4F6` on a white screen has a contrast of **1.10:1**, so it's nearly invisible. Dark mode: `#1F2723` is almost the same as the card background. There's also no chat wallpaper to separate bubbles from the page.

## 3. THE FIX

### Phase 1: One rule for "mine" (`ui/viewmodel/ChatViewModel.kt`)
1. Add a private helper:
   ```kotlin
   private fun isMine(senderId: String): Boolean = senderId == currentUserId
   ```
2. Use it everywhere a `ChatMessageUi` is built:
   - the Room cache mapping (~line 374): `isMine = isMine(entity.senderId)` instead of `entity.isFromMe`
   - `mapDtoToUi` (~line 622): `isMine = isMine(dto.senderId)`. Use the same value for the status branch (`if (!mine) SENT else …`) instead of `dto.isMine`.
   - the optimistic message (~line 669): leave it as `true`, since `senderId = currentUserId` already makes it true
   - every other place that sets `isMine =` or `isFromMe =` from `msg.isMine` (~458, ~539): write `isFromMe = isMine(msg.senderId)`
   - admin view: keep `isMine = false` (see Phase 3 for admin layout)
3. Messages with `senderRole == "SYSTEM"` or `kind == "BOOKING_UPDATE"` are never "mine". `ChatBubble` already routes `BOOKING_UPDATE` to the centered card. Make `kind == "SYSTEM"` also render centered (Phase 3).
4. Keep the `isFromMe` column in Room (no schema change or migration), but **nothing may read it for layout anymore**.

### Phase 2: Stop cross-profile cache bleed
1. In `ChatDao`, add:
   ```kotlin
   @Query("DELETE FROM chat_messages") suspend fun clearAllMessages()
   ```
2. In `ChatViewModel.onSessionChanged(newProfileId, newRole)`: if `newProfileId != currentUserId` (compare **before** assigning), run `chatDao?.clearAllMessages()` on `Dispatchers.IO` inside `viewModelScope.launch`, **before** `loadConversations()`.
   - The server is the source of truth, and the next open does a full fetch (`afterSeq = 0`) because the list is empty.
   - Don't clear if the id is unchanged, such as a re-login as the same user.
3. Don't touch `chat_conversations` cache logic, which is already scoped by `ownerProfileId`.

### Phase 3: Bubble layout polish (`ui/components/ChatComponents.kt` → `ChatBubble`)
Keep the existing structure and change only this:
- **Width:** replace `widthIn(max = 290.dp)` with a max of **78% of the screen width**. Wrap the Column content in `BoxWithConstraints`, or pass `maxWidth * 0.78f`.
- **Side gutter:** outgoing gets `padding(start = 48.dp)`, incoming gets `padding(end = 48.dp)`, so a bubble never spans the full width.
- **Corner "tail" by side (WhatsApp-style):**
  - outgoing: `RoundedCornerShape(topStart = 16, topEnd = 4, bottomStart = 16, bottomEnd = 16)` (already correct)
  - incoming: `RoundedCornerShape(topStart = 4, topEnd = 16, bottomStart = 16, bottomEnd = 16)` (already correct)
- **Grouping:** add a param `isFirstInGroup: Boolean = true`. Only the first bubble of a run from the same sender gets the 4.dp tail corner. Later bubbles use 16.dp on all corners. Vertical padding is `2.dp` inside a group and `8.dp` before a new group. Compute it in `ChatThreadScreen` inside `itemsIndexed`:
  ```kotlin
  val prev = activeThreadMessages.getOrNull(index - 1)
  val isFirstInGroup = showDateDivider || prev == null || prev.isMine != msg.isMine || prev.kind == "BOOKING_UPDATE" || prev.kind == "SYSTEM"
  ```
- **Incoming bubble:**
  - light mode: `BorderStroke(1.dp, colors.chatBubbleTheirsBorder)` + `shadowElevation = 1.dp`
  - dark mode: no shadow, keep the 1dp border
  - outgoing bubbles have no border in either mode (remove the current dark-mode border on outgoing)
- **Ticks** stay only on outgoing bubbles (already the case). The time on incoming uses `colors.chatBubbleTheirsMeta`.
- **SYSTEM kind:** render as a centered small pill using the existing `chatSystemBubble` / `onChatSystemBubble` colors, like `DayDivider`.
- **Admin view** (`isAdminView`): instead of forcing everything left, show `senderRole == "PARTNER"` on the right and `"CUSTOMER"` on the left. Use the same colors and **no ticks**. Pass `isOutgoing = msg.senderRole == "PARTNER"` and add `showTicks: Boolean = isOutgoing && !isAdminView`.
- Accessibility: set `Modifier.semantics { contentDescription = (if (isOutgoing) "You" else counterpartName) + ", " + time + ", " + message.text }` on the bubble Surface. Keep the existing `testTag`s.

### Phase 4: Colors (`ui/theme/Theme.kt` + `ServoraColors`)
Add three tokens to the `ServoraColors` data class (and to both palettes): `chatBubbleTheirsBorder`, `chatBubbleTheirsMeta`, `chatWallpaper`. Set the values:

| Token | Light | Dark | Why |
|---|---|---|---|
| `chatBubbleMine` | `#007A45` | `#0F6B43` | brand green; white text passes AA (5.43:1 light, 5.57:1 dark). Current `#009051` is only 4.11:1 |
| `onChatBubbleMine` | `#FFFFFF` | `#E8EEEA` | unchanged in spirit |
| `chatBubbleTheirs` | `#FFFFFF` | `#26302B` | white vs green: different hue, not just shade |
| `onChatBubbleTheirs` | `#111827` | `#E8EEEA` | 17.7:1 / 11.6:1 |
| `chatBubbleTheirsBorder` | `#E3E8E5` | `#34403A` | edge definition |
| `chatBubbleTheirsMeta` | `#6B7280` | `#A9B7B0` | timestamp, 4.8:1 / 6.5:1 |
| `chatWallpaper` | `#EEF3EF` | `#0E1411` | thread background so white bubbles stand out |

- Outgoing timestamp stays `onChatBubbleMine.copy(alpha = 0.75f)`. The read-tick blue `chatReadTick` is unchanged. On the new darker green, make sure the tick color is still visible, or use `#8FD3FF` for the read tick inside outgoing bubbles only.
- In `ChatThreadScreen`, give the messages `Box` (the `weight(1f)` one, ~line 304) `.background(colors.chatWallpaper)`. The top bar and composer keep `surface`.
- Don't hard-code colors in composables. Use tokens only. Don't change any other screen's colors.

### Phase 5: Verify (manual, on device)
1. Build: `./gradlew assembleDebug`.
2. As `pro_rajesh_1`, open the booking chat and send "hi from partner". It appears **right, green**, with ticks.
3. Switch the profile to `user_priya_1` on the **same phone** and open the same chat. "hi from partner" appears **left, white**, with no ticks. Reply "hi from customer": it goes right, green.
4. Switch back to `pro_rajesh_1`. The partner's message is on the right, the customer's on the left. Kill the app, reopen it, and check the sides are unchanged.
5. The booking-accepted card is centered. Three quick messages in a row group together (only the first bubble has a tail).
6. Toggle dark mode. The received bubble is clearly distinct from both the wallpaper and the sent bubble.
7. A long message wraps at about 78% width, and a one-word message hugs its content.

## 4. ACCEPTANCE CRITERIA
- For every message, side = `senderId == currentUserId` → right, otherwise left. System/booking messages are centered.
- No layout decision reads `ChatMessageEntity.isFromMe` or `dto.isMine`.
- Switching profiles clears cached messages. No wrong-side messages appear after a switch or a restart.
- Sent and received bubbles differ in color (green vs white), side, tail and ticks, in both themes. Text contrast is at least 4.5:1.
- The build passes. No server changes.

## 5. FINAL REPORT FORMAT
Plan (5 lines), files changed (one line each), the color tokens added, and results of Phase 5 steps 2-7 (pass/fail each).
