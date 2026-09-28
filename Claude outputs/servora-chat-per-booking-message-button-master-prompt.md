# MASTER PROMPT — Remove the Chats Tab, Turn "Track Status" into "Message", One Chat Thread per Booking, Offline-First Chat Storage Synced with Supabase (Servora / ServiceAssist)

Paste everything below the line into your coding agent. This follows the chat master prompts already applied (`servora-chat-master-prompt.md`, `servora-chat-fix-receipts-ordering-master-prompt.md`, `servora-chat-conversation-not-found-fix-master-prompt.md`, `servora-chat-failed-to-read-messages-fix-master-prompt.md`, `servora-chat-send-lists-sync-latency-fix-master-prompt.md`, `servora-chat-invalid-key-length-fix-master-prompt.md`). The real chat backend (encrypted Edge Functions + `chat_*` tables) works today. Work in the repo root `ServiceAssist/`. Change what is described here. Do not re-implement the parts that already work.

---

## 0. ROLE AND WORKING RULES

You are a staff-level engineer with 10+ years of experience in Android (Kotlin, Jetpack Compose, Room), Supabase (Postgres, Edge Functions/Deno) and offline-first sync design.

1. Read every file named in section 1 before you edit anything. If the code differs from what I describe, trust the code, say so in one line, and adapt.
2. Post a short plan first: files to touch, the migration, and risks. Then work through the phases in section 4 in order. Don't stop between phases. Report once at the end unless you are truly blocked.
3. Don't ask me questions unless you are truly blocked. State the assumption in one line and keep going.
4. Keep changes small and reviewable, with no unrelated refactors or renames. Never log message text, tokens, phone numbers, OTPs or keys.
5. **No tests.** I will test everything myself. Don't write or run unit tests, Deno tests, SQL test scripts, Compose UI tests or screenshot tests. The only check you run is `./gradlew assembleDebug` at the end of each Android phase. Keep reports short.
6. **Keep existing behaviour working.** That means encryption (envelope AES-GCM, per-conversation DEK), contact scrubbing, read/delivered receipts, message ordering by `seq`, idempotent sends (client UUID), the incoming-message banner, admin audit read, READ_ONLY/CLOSED lifecycle, and the partner per-job chat buttons.

## 1. CURRENT STATE — VERIFIED IN THE CODE

**Navigation**
- `ui/components/ServoraBottomNav.kt`: `enum class ServoraNavTab { HOME, SERVICES, SEARCH, CHATS, PROFILE }`. The bar is hard-coded to 5 slots: `activeIndex` maps `CHATS -> 3`, `selectSlotByIndex` uses indices 0..4, Search is the raised `CenterSearchSolidButton` at slot 2, and `unreadChatsCount` draws a badge on the Chats slot (~line 325).
- `ui/components/PartnerBottomNav.kt`: `enum class PartnerNavTab { DUTY_JOBS, CHATS, EARNINGS, TOOLKIT, PROFILE }`. It iterates `PartnerNavTab.entries` generically and puts the `unreadChatsCount` badge on CHATS (~line 271).
- `MainActivity.kt`:
  - `AppScreen` is a hand-rolled sealed interface (line ~119). `AppScreen.ChatThread(conversationId, isAdminView)` exists.
  - The top-bar visibility `when` (~lines 273-291) has `PartnerNavTab.CHATS -> false` and `ServoraNavTab.CHATS -> false`.
  - Bottom bars get `unreadChatsCount = unreadChatsCount` (~lines 369, 388) from `chatViewModel.unreadTotal`.
  - `PartnerNavTab.CHATS -> ChatListScreen(...)` (~line 541) and `ServoraNavTab.CHATS -> ChatListScreen(...)` (~line 690).
  - `customerTabs = listOf(HOME, SERVICES, SEARCH, CHATS, PROFILE)` (~line 593).
  - The Notifications `NEW_MESSAGE` click routes to `currentPartnerTab = PartnerNavTab.CHATS` / `currentTab = ServoraNavTab.CHATS` (~lines 940-944). **This breaks when the tabs are removed.**
  - The incoming banner already routes straight to `AppScreen.ChatThread(banner.conversationId)`. Keep that.
  - `chatViewModel.onSessionChanged(...)` and `chatViewModel.loadConversations()` are already driven by `LaunchedEffect`s in `MainActivity`, not by `ChatListScreen`. Verify this. The global sync loop must keep running without any chat tab.
  - **The partner UI already has a per-job message icon, and it becomes the only chat entry point for partners:**
    - `PartnerJobsScreen.kt` (~lines 936-954) is the "Message Action". It's a 38 dp circular `Surface` with `Icons.Default.ChatBubbleOutline` (`contentDescription = "Message"`) and `.clickable { onOpenChat(booking) }`, in the customer header next to the Call button.
    - `PartnerBookingsScreen.kt` (~lines 922-938) is the "Chat Button". It's a 36 dp circular `Box` with `Icons.Outlined.ChatBubbleOutline` (`contentDescription = "Chat Customer"`) and `.clickable { onOpenChat(booking) }`.
  - Per-booking chat entry points already exist and all call `chatViewModel.openThreadForBooking(bookingId, bookingCode, serviceName, onError) { convId -> currentScreen = AppScreen.ChatThread(convId) }`. They are in `PartnerJobsScreen.onOpenChat`, `PartnerBookingsScreen.onOpenChat`, `BookingTracking` (`BookingConfirmationScreen.onOpenChat`, which already has a "Message" button at ~line 689), and one more. Reuse this path. Don't invent a new one.

**My Bookings "Track Status" button**
- `ui/screens/BookingsListScreen.kt` (~line 620-652): for non-completed, non-cancelled bookings, the card shows `OutlinedButton("View Details", onClick = onSelectBooking)` plus `Button("Track Status", onClick = onSelectBooking)`. Both do the same thing: `MainActivity` sends them to `AppScreen.BookingTracking(booking.id)` (or `PartnerBookings` for a professional). Completed or cancelled bookings show "Book Again" instead.
- The `BookingsListScreen(...)` signature (~line 81) has no chat callback.

**Chat client**
- `ui/viewmodel/ChatViewModel.kt` exposes `conversations: StateFlow<List<ConversationUi>>` and `unreadTotal` (the sum of unread counts over ACTIVE conversations). It runs a global `chat-sync` poll loop (4 s in list view, 15 s in a thread) and per-thread polling. `openThread()` always calls `readMessages(conversationId, afterSeq = 0)`, so it re-downloads the whole history every time.
- `data/model/ChatUiModels.kt`: `ConversationUi` already carries `bookingId`, `bookingCode`, `serviceName`, `unreadCount`, `status`, `latestSeq`.
- `data/repository/ChatRepository.kt`: `RemoteChatRepository` calls Edge Functions through `data/remote/chat/ChatApiService.kt` (`chat-open`, `chat-list`, `chat-read`, `chat-send`, `chat-ack`, `chat-sync`, `chat-admin-list`, `chat-admin-read`). `FakeChatRepository` / `FakeChatStore` stay debug-only.
- **There is no local chat persistence.** `data/db/ServoraDatabase.kt` (Room, `version = 5`, migrations 2→3→4→5, plus `fallbackToDestructiveMigration()`) has no chat entities. Chats exist only in memory, so they vanish on process death and nothing shows offline.
- `AndroidManifest.xml` already has `allowBackup="false"`. Keep it that way.

**Chat backend (Supabase)**
- `supabase_chat_schema.sql`: `chat_conversations(id uuid, booking_id bigint NOT NULL, customer_id, partner_id, status ACTIVE|READ_ONLY|CLOSED, wrapped_dek, key_version, closes_at, last_message_at, …)`, `chat_messages(id uuid client-generated, conversation_id, seq identity, sender_id, sender_role, kind, ciphertext, nonce, created_at)`, and `chat_read_state(conversation_id, user_id) PK`.
- **The thread model today is one conversation per customer–partner pair, shared across bookings.** `supabase_chat_fix_v7.sql` defines `CREATE UNIQUE INDEX idx_chat_conversations_active_pair ON chat_conversations (customer_id, partner_id) WHERE status <> 'CLOSED'`. `chat-open/index.ts` (~line 115) looks up the conversation by `(customer_id, partner_id)`, then re-points `booking_id` and posts an "announcement" when a new booking comes in (`last_announced_booking_id`). `_shared/chatStatus.ts → recomputePairChatStatus(customerId, partnerId)` sets status from *any* active booking of the pair and moves `booking_id` to the newest one. Callers are `booking-update-status/index.ts` (~line 87) and `chat-lifecycle/index.ts` (~lines 34, 45, 91; that includes partner reassignment).
- `chat_list_summaries(p_profile_id)` (v7) and `chat_sync(p_profile_id)` (1-to-1 migration) are SECURITY DEFINER RPCs used by `chat-list` / `chat-sync`.
- `chat_messages` and `chat_conversations` are in the `supabase_realtime` publication. The app doesn't use Realtime for chat because payloads are ciphertext only the Edge Functions can decrypt. **Keep polling. Don't add Realtime.**

## 2. WHAT I WANT (requirements)

1. **Remove the Chats tab** from both the Customer bottom bar and the Partner bottom bar. No screen, state, badge or route should still point to it.
2. **Rename "Track Status" to "Message"** on active booking cards in My Bookings. Tapping it opens the chat for **that booking only**.
3. **One chat thread per booking** (per service order). Two bookings with the same professional get two separate threads.
4. **Nothing is lost:** tracking stays reachable ("View Details" and tapping the card still open `BookingTracking`), existing chat features keep working, and every other chat entry point (partner job cards, tracking screen, banner, notifications, admin audit) keeps working and lands in the right per-booking thread.
5. **Every chat is stored and synced with Supabase.** Supabase stays the source of truth. The app also keeps an offline-first local copy in Room: cached threads open instantly and work offline for reading, unsent messages survive app restarts and retry on their own, and after that everything reconciles with Supabase by `seq`.
6. Unread indicators move from the removed tab to where the chats now live: the customer Message button and the partner's existing message icon.
7. **Partner side: use the existing message icon only.** Don't add a new "Message" button, text button, FAB or screen for partners. The existing message icons on the job cards (section 1) are the partner's way into each booking's chat. Keep their size, shape, colors, icon and position exactly as they are.

## 3. LOCKED DESIGN DECISIONS (don't revisit)

- **Thread key = `booking_id`.** Uniqueness is `UNIQUE (booking_id) WHERE status <> 'CLOSED'`. Drop the active-pair index.
- **Legacy pair threads are not split.** Their messages are encrypted with one DEK per conversation, so moving them between conversations isn't worth the risk. Each existing conversation stays attached to the `booking_id` it currently holds. If an older booking of the same pair has no thread yet, it gets a fresh empty one when opened. State this in the migration comments.
- **Status comes from the booking's own status:** active booking → `ACTIVE`; COMPLETED/CANCELLED → `READ_ONLY` with `closes_at = now() + 24h` (same window as today); then `CLOSED` by the existing lifecycle job.
- **Partner reassignment:** the old partner's conversation for that booking goes to `CLOSED` with `archived_reason = 'REASSIGNED'`. The new partner gets a fresh conversation the next time anyone opens chat for it. The old partner must lose access.
- **When chat is available:** only when the booking has a real professional (`professional_id` not blank) and status is not `PENDING`. Otherwise the Message button shows disabled with the helper text "Chat opens once a professional is assigned". `chat-open` enforces the same rule server-side (409 + readable message).
- **Transport stays as it is:** Edge Functions + polling. Room is a cache and an outbox, not a second source of truth. Server `seq` decides order. The client UUID `id` makes retries idempotent.

## 4. THE WORK

### Phase 1 — Backend: per-booking threads (Supabase)

1. New idempotent migration `supabase_chat_per_booking_threads_migration.sql` in the repo root:
   - `DROP INDEX IF EXISTS idx_chat_conversations_active_pair;` and drop the old `idx_chat_conv_cust_part` index / pair constraint if they still exist.
   - Before creating the new index, dedupe safely. If more than one non-CLOSED conversation shares a `booking_id`, keep the one with the latest `last_message_at` and set the rest to `CLOSED` with `archived_reason = 'DEDUPE_PER_BOOKING'`.
   - `CREATE UNIQUE INDEX IF NOT EXISTS idx_chat_conversations_active_booking ON public.chat_conversations (booking_id) WHERE status <> 'CLOSED';`
   - Add `CREATE INDEX IF NOT EXISTS` on `(customer_id)` and `(partner_id)` for list queries if they're missing.
   - Recreate `chat_list_summaries` and `chat_sync` if they assume one row per pair (check the join/group logic). They must return **one row per conversation** with that conversation's own `booking_id`, `booking_code`, `service_name` and `booking_status`, and still exclude CLOSED rows the same way they do now.
   - Leave `last_announced_booking_id` in place (harmless) but stop using it.
2. `_shared/chatStatus.ts`: add `recomputeBookingChatStatus(supabaseClient, bookingId)`, which reads that booking's status and updates only that booking's non-CLOSED conversation. Switch `booking-update-status` and `chat-lifecycle` to it. For reassignment in `chat-lifecycle` (~line 91), close the old conversation as described in section 3. Delete `recomputePairChatStatus` once nothing calls it.
3. `chat-open/index.ts`:
   - Keep the booking lookup (by `booking_id`, fallback `booking_code`) and the participant/admin authorization exactly as they are.
   - If `professional_id` is blank or status is `PENDING`, return 409 `{ "error": "Chat opens once a professional is assigned" }`.
   - Look up with `.eq("booking_id", booking.id).neq("status", "CLOSED").maybeSingle()`. Create it if missing (new DEK, `customer_id`/`partner_id` from the booking, initial status from section 3). On `23505`, re-select by `booking_id`. Remove the pair re-pointing and the "new booking" announcement branch. One optional SYSTEM intro message ("Chat for <service> · <booking code>") on create is fine if the current code already posts one.
   - Response shape unchanged (`conversation_id`, `status`, `booking_id`, …).
4. `chat-send`, `chat-read`, `chat-ack`, `chat-sync`, `chat-list`, `chat-admin-*`: check that none of them resolves a conversation by pair. Anything that does must go by `conversation_id` / `booking_id`. The participant check stays: the sender must be the conversation's `customer_id` or `partner_id`.
5. At the end of this phase, list the exact deploy commands for me: the SQL file to run in the SQL editor, then `supabase functions deploy chat-open chat-list chat-sync chat-lifecycle booking-update-status` (plus any other function you touched).

### Phase 2 — Local persistence: Room cache + outbox

1. New entities in `data/db/`:
   - `ChatConversationEntity(conversationId PK, bookingId (indexed, unique), bookingCode, serviceName, counterpartName, status, lastMessagePreview, lastMessageAtMillis, lastMessageFromMe, unreadCount, latestSeq, peerReadSeq, peerDeliveredSeq, ownerProfileId, updatedAtMillis)`
   - `ChatMessageEntity(id PK = server/client UUID, conversationId (indexed), seq Long? , senderId, senderRole, kind, text, createdAtMillis, localStatus (SENDING|SENT|FAILED), ownerProfileId)` with an index on `(conversationId, seq)`.
2. `ChatDao`: `observeConversations(owner): Flow<List<…>>`, `observeConversationByBooking(bookingId)`, `observeMessages(conversationId): Flow<List<…>>` ordered by `seq` (nulls/pending last, then `createdAtMillis`), `maxSeq(conversationId)`, `upsertConversations`, `upsertMessages` (merge by `id`; a server row replaces the pending local row with the same client UUID), `pendingOutbox(owner)`, `markStatus`, `clearForOwner(owner)`.
3. `ServoraDatabase`: `version = 6`. Add an explicit `MIGRATION_5_6` that creates the two tables and indexes, and register it in `addMigrations(...)`. **Don't rely on `fallbackToDestructiveMigration()`**, because it would wipe bookings/addresses on upgrade.
4. `ChatRepository`: keep the interface. Add a local layer (`ChatLocalStore`, or put it inside `RemoteChatRepository`, whichever keeps the diff smaller) that writes every successful `listConversations` / `syncChat` / `readMessages` / `sendMessage` result into Room.
5. Privacy: text is stored in plaintext only in app-private Room storage. Call `clearForOwner` on logout and whenever `onSessionChanged` switches to a different profile, including admin impersonation start/stop, so one user never sees another's cached chats. Never write decrypted text to logs.

### Phase 3 — ViewModel: offline-first reads, delta sync, durable sends

In `ChatViewModel`:
1. `conversations` and `activeThreadMessages` are backed by Room `Flow`s (`stateIn(viewModelScope, …)`) scoped to the current profile. The network only writes to Room.
2. `openThread(conversationId)` shows cached messages immediately, then calls `readMessages(conversationId, afterSeq = dao.maxSeq(conversationId))` for a **delta** instead of `afterSeq = 0`. Keep a full re-fetch only as a fallback when the cache is empty or the server reports a gap/reset. Ack read with the highest `seq` received, as today.
3. `sendMessage` inserts a `SENDING` row with the client UUID **before** the network call. Success replaces it with the server row (`seq`, `createdAt`). Failure marks it `FAILED`, and `retryMessage` reuses the same UUID. Flush the outbox (`SENDING`/`FAILED` older than a few seconds) on app start, on `onSessionChanged`, and on each global sync tick. Keep the existing `pendingMessages` map only if something still needs it. Otherwise derive it from Room.
4. Add `val unreadByBookingId: StateFlow<Map<Long, Int>>` derived from conversations (ACTIVE and READ_ONLY both count). Keep `unreadTotal` because the banner logic uses it.
5. Make sure the global sync loop starts on session start and doesn't depend on any chat list screen being visible (it used a 4 s "list view" cadence. Keep 4 s when no thread is open).

### Phase 4 — UI: remove the Chats tab, add the Message button

1. `ServoraBottomNav.kt`: remove `CHATS` from `ServoraNavTab`. The bar becomes 4 destinations: Home, Services, Search, Profile. Update `activeIndex`, `selectSlotByIndex`, `itemCount`, the drag/hover slot math and the test tags so every index maps correctly. Search stays visually centered: if the raised `CenterSearchSolidButton` depends on being the middle of an odd slot count, lay it out as Home | Services | [Search] | Profile with Search in a fixed-width center slot and equal weights on each side, matching the current look. Remove the `unreadChatsCount` parameter and its badge.
2. `PartnerBottomNav.kt`: remove `CHATS` from `PartnerNavTab` (4 tabs: Duty & Jobs, Earnings, Toolkit, Pro Profile) and remove `unreadChatsCount` and its badge.
3. `MainActivity.kt`:
   - Remove both `CHATS -> ChatListScreen(...)` branches, the `CHATS` cases in the top-bar visibility `when`, `CHATS` from `customerTabs`, and the `unreadChatsCount` arguments. Fix every other exhaustive `when` the compiler flags.
   - Notifications `NEW_MESSAGE` click: if the notification has a `bookingId`, call `chatViewModel.openThreadForBooking(...)` and navigate to `AppScreen.ChatThread(convId)`. If not, go to `AppScreen.MyBookings` (customer) or `AppScreen.PartnerBookings` (partner). Check the notification builder too: if message notifications don't carry `bookingId` yet, populate it from the conversation.
   - `MyBookings`: pass the new callbacks to `BookingsListScreen` (below), wired through the same `openThreadForBooking` + Toast-on-error + `AppScreen.ChatThread(convId)` flow the other screens use. Keep `onSelectBooking` → `BookingTracking` unchanged.
   - If `ChatListScreen` has no remaining references after this (grep, and check that admin doesn't use it; admin uses `chat-admin-list` from `AdminDashboardScreen`), delete `ChatListScreen.kt` and any components only it used. If anything still references it, leave it alone.
4. `BookingsListScreen.kt`:
   - New params: `onMessageClick: (Booking) -> Unit`, `unreadByBookingId: Map<Long, Int> = emptyMap()`, `openingChatBookingId: Long? = null`.
   - Replace the "Track Status" button with **"Message"**: same colors, shape and height, with a leading chat-bubble icon (`Icons.AutoMirrored.Filled.Chat` or the icon already used by the chat UI), `contentDescription = "Message professional"`, and `testTag("booking_message_${booking.id}")`. Show a small red count badge when `unreadByBookingId[booking.id] > 0` (cap display at "9+"). While `openingChatBookingId == booking.id`, show a 14 dp progress indicator and ignore taps (no double-open). Disable it with the helper text from section 3 when chat isn't available yet.
   - "View Details" and tapping the card still open tracking. Completed/cancelled cards keep "Book Again". Their read-only chat stays reachable from the tracking screen's existing Message button.
   - Check the button row still fits at 360 dp width with the price. Use `maxLines = 1`, `softWrap = false` and the existing padding.
5. Partner side: **reuse the existing message icons. Don't add a new button.** The icons are in `PartnerJobsScreen.kt` (~line 936, "Message Action") and `PartnerBookingsScreen.kt` (~line 922, "Chat Button").
   - Keep their size, shape, colors, icon, position and `onOpenChat(booking)` callback as they are. They already go through `openThreadForBooking`, which now resolves the per-booking thread, so no rewiring is needed.
   - Only add small things to them:
     - An unread dot/count badge from `unreadByBookingId[booking.id]`, drawn in the icon's top-end corner with `BadgedBox` or an overlaid `Box`. It must not change the icon's footprint. Pass `unreadByBookingId` down as a new parameter with default `emptyMap()`.
     - A tap guard (ignore taps while `openingChatBookingId == booking.id`) so it doesn't double-open.
     - Unify the `contentDescription` to "Message customer" and add `testTag("partner_message_${booking.id}")`.
   - If a job has no customer chat available yet (section 3 rule), dim the icon (alpha 0.4) and show the helper text as a Toast on tap instead of calling `onOpenChat`.
6. Leave `ChatThreadScreen` as is, except that its header should show the service name and booking code of *its* booking (it's per-booking now). Check whether it already does.

### Phase 5 — Final pass

- Run `grep -rn "CHATS\|unreadChatsCount\|recomputePairChatStatus\|active_pair" app/src supabase` and confirm nothing is left.
- Run `./gradlew assembleDebug`.
- Confirm there are no `Log` calls with message text.

## 5. MANUAL TEST CHECKLIST (I will run this myself — don't write tests)

1. Customer and partner bottom bars show 4 items, no Chats. Search is centered on the customer bar. Every tab still navigates correctly, including hold-and-drag selection.
2. My Bookings → an active assigned booking shows "View Details" and "Message". Message opens that booking's chat. View Details / tapping the card opens tracking.
3. Book two services with the same professional. Each booking's Message opens a **different** thread, and messages don't cross over.
4. A PENDING booking or one with no professional shows Message disabled with the helper text. Calling `chat-open` for it directly returns the readable 409.
5. Partner: the Chats tab is gone. The existing message icon on the Duty & Jobs card and on the Partner Bookings card looks exactly as before and opens that job's chat. No new button was added.
6. Send from the customer, reply from the partner (using the job card's message icon). Receipts, ordering and the incoming banner still work. The unread badge shows on the right booking's Message button and on the partner job chat button, and clears after the thread is opened.
6. Put the device in airplane mode, open a previously opened thread: history shows from cache. Send a message: it shows as pending/failed. Kill the app, turn the network back on, reopen the app: the message sends by itself exactly once (no duplicate in Supabase `chat_messages`).
7. Complete a booking: its chat turns read-only, and it closes after the retention window. Reassign a booking to another partner: the old partner can no longer open it, and the new partner gets a fresh thread.
8. Tap a new-message notification: it opens the correct booking thread.
9. Log out and log in as a different user: no cached chats from the previous user are visible. Admin audit view still opens conversations and logs to the audit trail.
10. Upgrade install over the current build: bookings and addresses are still there (the Room 5→6 migration didn't wipe data), and legacy pair threads still open under their booking.

## 6. ACCEPTANCE CRITERIA

1. No Chats tab or any reference to it remains on either role. The Message button replaces Track Status and opens a per-booking thread. Tracking is still reachable.
2. The database enforces at most one non-CLOSED conversation per `booking_id`. All Edge Functions resolve threads by booking/conversation, never by pair.
3. Every message is persisted in Supabase `chat_messages`. The app mirrors conversations and messages in Room and syncs by `seq` delta. Pending sends survive restarts and are idempotent.
4. Encryption, scrubbing, receipts, lifecycle, banner and admin audit behave exactly as before.
5. Room migration 5→6 is explicit and non-destructive. `assembleDebug` passes. Every changed file is listed.

## 7. OUTPUT FORMAT

One short report at the very end (20 lines max) with:

- plan vs. what you actually did
- every file changed or added
- assumptions you made
- the exact steps I must run myself: the SQL migration, the `supabase functions deploy …` list, and the order to run them in (SQL first, then functions, then install the app build)
