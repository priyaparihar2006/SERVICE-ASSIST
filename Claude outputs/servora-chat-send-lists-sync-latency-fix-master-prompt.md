# MASTER PROMPT 6: Fix Chat Messages Not Reaching the Server, Broken Active/Recent Chat Lists, and Slow Server Updates (Servora / ServiceAssist)

Paste everything below the line into your coding agent. Work in the repo root `ServiceAssist/`. Earlier chat prompts (1–5) were applied at some point, but **several of their fixes have been reverted or never deployed**. This prompt is based on a fresh read of the current code. Where the code differs from what's written here, trust the code and say so.

---

## 0. ROLE AND WORKING RULES

You are a staff-level engineer with 10+ years in Android (Kotlin, Compose, Retrofit/OkHttp, Room), Supabase (Postgres, PL/pgSQL, PostgREST, Deno Edge Functions) and applied crypto (Web Crypto AES-GCM). You debug from evidence, not guesses.

1. Read every file named in section 2 before changing anything.
2. Post a short plan (which root causes you confirmed, files to touch, risks). Then work through the phases in section 4 without stopping. Report once at the end unless you are truly blocked.
3. Don't ask questions unless you are blocked. If you have to assume something, state it in one line and continue.
4. Keep changes small and reviewable. No unrelated refactors or UI redesigns. Never log message text, tokens, phone numbers or key material.
5. **No tests.** I test everything myself. Don't write or run unit, Deno, SQL/RLS, UI or screenshot tests. The only checks you run are `./gradlew assembleDebug` at the end of each Android phase and `deno check` on each changed Edge Function if Deno is available. If it isn't, say so.

## 1. SYMPTOMS

- **A. Messages don't reach the server.** A message looks "sent" in the chat thread (ticks shown), but the other user never gets it. After restarting the app, some messages have disappeared.
- **B. Active and Recent chat lists are wrong.** They show phantom or duplicate conversations (demo chats like "Rajesh" / "Amit", or two threads for the same person). Some chats stay under **Active** forever after the job is completed, and **Recent** is empty or shows fake read-only chats. Opening some chats shows errors or "[Decryption Error]" bubbles.
- **C. The server is slow to pick up changes made in the app.** Booking status changes, sent messages and new bookings take many seconds to show on the server or on the other device. Sometimes a status change flips back to the old value for a moment.

## 2. ROOT CAUSES (verified in current code)

### A. Message sending (symptom A)

**RC-A1: `RemoteChatRepository` silently falls back to the in-memory `FakeChatStore` on every failure.** Master Prompt 4 removed this, but it has come back. In `app/src/main/java/com/example/data/repository/ChatRepository.kt`:
- `openConversation()` (lines ~62-108): on any HTTP error or exception it logs a warning and returns `FakeChatStore.openConversation(...)`. That returns a fake id like `conv_booking_<id>` or `conv_<cust>_<pro>`, which doesn't exist in Supabase and isn't even a valid UUID.
- `sendMessage()` (~148-163): if `chat-send` fails for any reason (404, 403 read-only, 500 crypto, timeout, contact-scrubber 400), it returns `FakeChatStore.sendMessage(...)` as a **success**. The ViewModel marks the message SENT with a fake seq. **The server never received it, and nobody is told.** This is the main cause of symptom A.
- `readMessages()`, `ackRead()` and `syncChat()` do the same, and so do the admin methods (`listAdminConversations`, `readAdminConversation`). The server error body is thrown away, so the user never sees a real error.
- `private val localFallbackRepo = FakeChatRepository()` and `setIdentity` feed this path.
- Result: once you are in a fake conversation, every send goes to local memory, and every poll reads local memory. It looks like it works, but nothing reaches the server.

**RC-A2: The ChatCryptoError fix (Master Prompt 5) isn't in the code.** Nothing matches `grep -rn ChatCryptoError supabase`. `unwrapDek` / `generateAndWrapDek` in `_shared/crypto.ts` have no guard. `chat-send`, `chat-read`, `chat-open` and `chat-admin-read` return the raw `err.message` (Web Crypto or Postgres text) as `{error}` with status 500. Combined with RC-A1, this 500 then becomes a fake success.

**RC-A3: `chat-send` upsert is unsafe and makes `last_message_seq` go backwards.** `chat-send/index.ts` lines ~116-144:
- It does `.upsert({... id: client_message_id ...}, { onConflict: "id" })`. A retry re-encrypts the message and overwrites the row. A client could also send a `client_message_id` that belongs to *another* conversation and overwrite that message.
- It then does `update chat_conversations set last_message_seq = insertedMsg.seq`. On a retry of an old message, or when two sends race, this can set `last_message_seq` **lower** than the real latest. `chat-sync` and `chat-list` then report a stale `latest_seq`, and the other device doesn't notice the new message.
- It makes 6+ sequential round-trips per send: auth, conversation select, key import, unwrap, upsert, update, read-state upsert. That makes sending slow.

**RC-A4: `chat-read` returns the oldest 100 messages, but acks everything as read.** `chat-read/index.ts` lines ~70-76 and 143:
- `order seq asc limit 100` with `after_seq=0` means a long 1-to-1 thread opens on its **first** 100 messages. The newest messages then arrive 100 at a time every 3 seconds through polling.
- The response returns `last_seq = conv.last_message_seq` (the real latest, not the highest seq actually returned). `ChatViewModel.openThread` then calls `ackRead(last_seq)`, which marks unseen messages as read.

### B. Active / Recent lists (symptom B)

**RC-B1: `listConversations()` merges fake local conversations into the real list.** In `ChatRepository.kt` ~110-134:
- On every call it runs `FakeChatStore.syncFromBookings(RoomBookings)`. That creates a local `conv_<cust>_<pro>` for every Room booking.
- It then appends every local conversation whose id isn't in the remote list. Because the ids never match the server's UUIDs, **every real chat appears twice** (real and fake).
- The seeded demo chats `conv_ac_rajesh_1` (ACTIVE) and `conv_cleaning_amit_2` (READ_ONLY) in `FakeChatStore.resetToDefault()` also show up for `user_priya_1`.
- If `chat-list` fails or returns an empty list, the **whole** list becomes fake.
- Tapping a fake row sends `conversation_id=conv_...` to `chat-read`. That fails ("invalid input syntax for type uuid" or "Conversation not found") and falls back to fake data again (see RC-A1).

**RC-B2: Nothing ever moves a conversation to READ_ONLY, and `chat-open` re-activates closed ones.**
- `chat-lifecycle` is the only code that sets `READ_ONLY`, and **nothing calls it**. It isn't called from the Android app (`grep -rn chat-lifecycle app/` finds nothing), and `booking-update-status` doesn't touch chat. So server conversations stay `ACTIVE` forever, and **Recent Chats** (`ChatListScreen.kt` lines 94-98, `status != "ACTIVE"`) only ever contains fake rows.
- `chat-open` (lines ~171-208) sets `status: "ACTIVE"` whenever the thread is opened, even from a COMPLETED or CANCELLED booking.

**RC-B3: The 1-to-1 thread migration conflicts with per-booking lifecycle and breaks encryption.**
- `supabase_chat_1to1_threads_migration.sql` step 1 merges duplicate conversations by `UPDATE chat_messages SET conversation_id = primary_conv_id`. Each message was encrypted with **its own conversation's DEK**, and the conversation id and message id are part of the AES-GCM AAD. After the move, `chat-read` can't decrypt them, so they show as `"[Decryption Error]"`, and `chat-list` shows the preview "Encrypted message". The duplicate conversation's `wrapped_dek` is deleted, so this can't be undone after the fact. The data has to be re-encrypted or kept separate.
- With one thread per (customer, partner), `chat_conversations.booking_id` is a single "current booking". `chat-open` swaps it every time the thread is opened from a different booking, and posts a new `"New booking accepted…"` system message each time. Opening chat from booking 1, then 2, then 1 again spams system messages and keeps changing which booking the thread belongs to.
- `chat-lifecycle` closes by `.eq("booking_id", booking_id)`. Completing an older booking does nothing. Completing the current booking marks the whole thread READ_ONLY, even if the pair has another active booking.
- `chat-lifecycle`'s `PARTNER_REASSIGNED` path inserts a new (customer, new_partner) row. That hits the unique constraint if the pair already has a thread. It also hard-codes `last_message_seq: 1`.

**RC-B4: `chat-open` fabricates bookings with hard-coded demo users.** `chat-open/index.ts` lines ~109-151: if the booking isn't found, it **inserts a new booking** with `customer_id "user_priya_1"` / `professional_id "pro_rajesh_1"`, name "Priya Sharma" and an Agra address. If that insert also fails, it builds an in-memory booking with id `99999`. This creates junk bookings and conversations between the wrong people, which show up in both users' lists. The conversation insert also isn't race-safe: two users opening at the same moment trigger a unique violation, and the raw Postgres text is returned.

**RC-B5: The chat opens with a local Room `booking_id` that doesn't match Supabase.** `openThreadForBooking(bookingId)` passes the **Room** id. Room ids and Supabase `bookings.id` are separate sequences (`SupabaseSyncManager` inserts remote rows with `id = remote.id`, and local bookings use Room autoincrement). So `chat-open` can match a **different** Supabase booking with the same number, and open a chat with the wrong customer or partner. `booking_code` is the only stable shared key.

### C. Slow server updates (symptom C)

**RC-C1: `booking-create` and `booking-update-status` Edge Functions can't start.** Both have `import { resolveIdentity } from "./_shared/identity.ts";`. The correct path is **`../_shared/identity.ts`** (every other function uses `../`). The module can't be found, so every call fails:
- New local bookings never reach Supabase. That feeds RC-B4 and RC-B5 ("booking not found", fake bookings).
- `SupabaseSyncManager.updateBookingStatus()` first waits for this call to fail (up to 15s timeout), then falls back to a direct REST PATCH. On failure, `ServoraRepository.updateBookingStatus` retries the whole thing after 500 ms.

**RC-C2: A full sync runs every 5 seconds, forever.** `ServoraViewModel.startLiveSyncLoop()` (lines ~66-77) calls `performFullSync()` every 5s. Each run upserts the profile, GETs **all** bookings, reviews, addresses and categories, and pushes each missing local booking one by one. There is no guard against overlapping runs (`triggerSupabaseSync()` can start a second one), and nothing pauses it when the app is in the background.

**RC-C3: The OkHttp per-host limit makes user actions wait behind background polling.** All traffic uses one `OkHttpClient` (`SupabaseClient.kt`) with OkHttp's default `Dispatcher.maxRequestsPerHost = 5`, and it all goes to the same Supabase host. At the same moment the app is running:
- the full sync every 5s (~6+ calls),
- chat `chat-sync` every 4s (which calls `chat-list` whenever anything changes),
- the thread poll every 3s,
- the booking poll every 12s,
- plus `LaunchedEffect(allBookings) { chatViewModel.loadConversations() }` in `MainActivity.kt` ~218. Because the 5s sync keeps writing to Room, `allBookings` keeps emitting, so `chat-list` is also triggered continuously.

A user's `chat-send` or status update sits **in a queue** behind these calls. That's why "the server takes time to take updates from my app".

**RC-C4: The sync pull overwrites newer local changes.** `ServoraRepository.updateBookingStatus` writes Room first, then pushes asynchronously. If `performFullSync` pulls before the push lands, `existing.copy(status = remoteStatus)` overwrites the new local status with the old server one. The UI flips back, then forward again on the next sync.

**RC-C5: Heavy chat endpoints.**
- `chat-list` selects **every message of every conversation** (`chat_messages.in(convIds)` with no limit) just to get the last message and the unread count. It also unwraps a DEK per conversation, one after another. Cost grows with total chat history, and 1-to-1 persistent threads make that history grow forever.
- `chat-sync` runs **one `chat_read_state` upsert per conversation, sequentially, on every 4s poll**, even when nothing changed.
- `chat-list` builds a PostgREST `.or()` filter from `profileId`, which comes from the `X-Dev-Profile-Id` header in non-prod. That's a filter-injection risk.

**RC-C6 (flag it, don't fix it in this pass): auth is effectively debug-only.** `SupabaseClient.userAccessToken` is never set to a real user JWT (it's only ever set to `null`). `X-Dev-Profile-Id` headers are only sent when `BuildConfig.DEBUG` is true. A **release** build sends the anon key as the bearer token, so `resolveIdentity` → `auth.getUser` → `"Unauthorized"`, and every chat call fails (which RC-A1 then hides). In the report, just confirm this and tell me what's needed.

## 3. REQUIREMENTS

1. A message is shown as SENT only after `chat-send` returns 200 with a server `seq`. Any failure shows FAILED with a readable reason and a working retry. There is never a silent local substitute.
2. The chat list shows **only** server conversations, one row per real thread. No demo or fake rows unless `BuildConfig.DEBUG && BuildConfig.CHAT_USE_FAKE` is true (default false). The ViewModel is then constructed with `FakeChatRepository` from the start, at the one existing construction point in `MainActivity.kt`.
3. **Active vs Recent** follows the real booking state. A thread is ACTIVE while the pair has at least one non-completed, non-cancelled booking. Otherwise it is READ_ONLY. Opening chat from a completed booking must not re-activate it.
4. Messages merged by the 1-to-1 migration must be readable again, or clearly marked as unrecoverable. No new "[Decryption Error]" can be created.
5. A user's own writes (send message, status change, new booking) reach the server in well under 2s on a normal network, and are never queued behind background polling.
6. No Edge Function ever returns raw Web Crypto or Postgres error text. Errors are short sentences, and the details go to `console.error` on the server.

## 4. THE FIX

### Phase 1: Remove every silent fake fallback (Android)
- `ChatRepository.kt` → `RemoteChatRepository`:
  - Delete `localFallbackRepo`. Remove every `FakeChatStore.*` call from every method, including `syncFromBookings` and the list merge. Also remove the `bookingDao`-driven fake-conversation creation.
  - On a non-2xx response, `throw ChatApiException(code, extractErrorMessage(raw, code, "<action> failed"))`. On `IOException`, throw `"No connection — message not sent"` or similar.
  - Keep `extractErrorMessage`. Map known server codes to friendly text: `CONTACT_SHARING_RESTRICTED` → the server's own message; 403 read-only → "This chat is closed."; 404 → "This chat no longer exists."
- Keep `FakeChatStore` / `FakeChatRepository` only for the gated debug path. Add `buildConfigField("boolean", "CHAT_USE_FAKE", "false")` in `app/build.gradle.kts` if it's missing. In `MainActivity.kt` (~164-170), use `FakeChatRepository()` only when `BuildConfig.DEBUG && BuildConfig.CHAT_USE_FAKE`, or when Supabase isn't configured. In the not-configured case, show a clear "Chat unavailable — server not configured" state instead of demo data.
- Afterwards, run `grep -rn "FakeChatStore\|FakeChatRepository" app/src/main/java`. It must only match the gated construction point and the fake classes themselves.
- `ChatViewModel.sendMessage` / `retryMessage`:
  - The optimistic `senderRole` should be the current role, not a hard-coded `"CUSTOMER"`.
  - Show `failureReason` in the per-message failed state.
  - Don't show a global `errorMessage` banner for a single failed send. The per-message state is enough.
  - The contact-scrubber block should appear as an inline message under the composer.
- `openThread`: after `readMessages(afterSeq=0)`, ack **the highest seq actually received** (`response.messages.maxOf { it.seq }`), not `response.lastSeq`.

### Phase 2: Fix the chat Edge Functions
- `_shared/crypto.ts`: add `ChatCryptoError(kind: "MASTER_KEY" | "CONVERSATION_KEY")` exactly as Master Prompt 5 specified.
  - `getMasterKey` throws `MASTER_KEY`.
  - Wrap `unwrapDek` and `generateAndWrapDek` in try/catch and throw `CONVERSATION_KEY`.
  - Cache the imported master `CryptoKey` in a module-level variable, so it isn't re-imported on every request.
- Add a small shared helper, `_shared/http.ts`, with `jsonError(status, clientMsg, logDetail?)`. Use it in every chat function's outer catch:
  - `ChatCryptoError` → a friendly message (use the wording from Master Prompt 5).
  - Postgres error → "Something went wrong, please retry", with the detail logged only.
  - `Unauthorized` → 401.
- `chat-send`:
  - Replace the upsert with an insert. On unique violation (`23505`), look up the existing row. If it belongs to the **same** conversation and sender, return it (idempotent retry). Otherwise return 409.
  - Update `last_message_seq` monotonically, via a new SQL function (Phase 3) or `.lt("last_message_seq", seq)`, so it can never go backwards.
  - Best option: move steps 3-5 into a single RPC `chat_append_message(...)` that inserts, bumps the conversation with `GREATEST`, and upserts the sender's read state in one round-trip.
- `chat-read`:
  - When `after_seq = 0`, return the **latest** page: `order seq desc limit 50`, then reverse. Also accept an optional `before_seq` for loading older messages.
  - Return `last_seq` = the highest seq in the returned batch, plus a separate `latest_seq` = `conv.last_message_seq`.
  - Read-state updates use `GREATEST` (Phase 3 RPC).
- `chat-list`: replace the "fetch every message" query with an RPC, `chat_list_summaries(p_profile_id)`, that returns one row per conversation with:
  - the last message (`DISTINCT ON (conversation_id) ... ORDER BY conversation_id, seq DESC`),
  - the unread count (`COUNT(*) FILTER (WHERE seq > my_last_read AND sender_id <> p)`),
  - peer cursors, booking fields and the counterpart name.
  Decrypt only the returned last messages, using `Promise.all`. This removes the `.or()` string filter (and its injection risk). Remove the hard-coded `"Rajesh Sharma"` fallback.
- `chat-sync`: remove the per-conversation upsert loop. Either have `chat_sync` mark delivered inside the RPC (one `INSERT ... ON CONFLICT DO UPDATE SET last_delivered_seq = GREATEST(...)` over all rows), or do one bulk upsert for only the rows where `latest_seq > my_delivered`.
- `chat-open`:
  - **Delete** the fake-booking insert and the `99999` in-memory booking. If the booking isn't found by `booking_code` (preferred) or `booking_id`, return 404 `"This booking hasn't synced to the server yet — pull to refresh and try again."`.
  - Make the conversation create race-safe: insert, and on `23505` re-select the existing row.
  - Don't set `ACTIVE` blindly. Compute status from the pair's bookings (see Phase 3).
  - Post the "New booking accepted" system message **only once per booking**. Track it with `chat_conversations.last_announced_booking_id`, or check for an existing BOOKING_UPDATE message with that booking reference. Don't post it on every open.
  - Use real identity for the participant check in non-prod too. Today `isCustomer`/`isPartner` are true for any role, and that should go.
- `chat-lifecycle`:
  - Recompute the pair's status instead of `.eq("booking_id", ...)`.
  - Handle `PARTNER_REASSIGNED` by opening or reusing the (customer, new_partner) thread with the same race-safe insert. Remove `last_message_seq: 1`.
- Fix the import path in **`booking-create/index.ts` and `booking-update-status/index.ts`**: `./_shared/identity.ts` → `../_shared/identity.ts`.
- In `booking-update-status`, after a successful status change, call the shared "recompute chat status for this pair" logic directly (a shared helper in `_shared/chatStatus.ts`), so COMPLETED or CANCELLED moves the thread to **Recent** automatically.

### Phase 3: SQL migration, `supabase_chat_fix_v6.sql` (idempotent)
1. `chat_append_message(p_conv uuid, p_id uuid, p_sender text, p_role text, p_kind text, p_ct bytea, p_nonce bytea) returns (id, seq, created_at)`. It inserts, then does `UPDATE chat_conversations SET last_message_seq = GREATEST(last_message_seq, new_seq), last_message_at = GREATEST(...)`, then upserts the sender's read state with `GREATEST`. Make it SECURITY DEFINER and revoke it from anon and authenticated.
2. `chat_bump_read_state(p_conv, p_user, p_read, p_delivered)`, using `GREATEST` on both cursors.
3. `chat_list_summaries(p_profile_id text)` as described in Phase 2.
4. `chat_recompute_pair_status(p_customer text, p_partner text)`:
   - If any booking for the pair has a status not in (`COMPLETED`, `CANCELLED`), set status `ACTIVE`, `closes_at` null, and `booking_id` = the most recent such booking.
   - Otherwise set `READ_ONLY`, `closes_at = now() + interval '24 hours'`, and `booking_id` = the most recent booking.
   - Run it once for every existing conversation, as a backfill.
   - Also add an `AFTER UPDATE OF status ON bookings` trigger that calls it. That way the lists stay correct even when status changes arrive through the direct REST path.
5. Add `last_announced_booking_id BIGINT` to `chat_conversations`.
6. Add an index on `bookings (customer_id, professional_id, status)`. Also add an index on `chat_messages (conversation_id, seq DESC)` if the existing ascending one isn't used for DESC scans. Check with `EXPLAIN`.
7. **Undecryptable merged messages (RC-B3):**
   - Give me a read-only query that counts messages whose `conversation_id` changed during the 1-to-1 merge. These are messages that fail to decrypt, since the old DEKs were deleted.
   - Change `chat-read` to return `text: "This older message can't be displayed."` and `kind: "SYSTEM"` for decrypt failures, instead of `"[Decryption Error]"`.
   - **Don't** try to recover keys. They're gone. Say so plainly in the report.
   - Fix the migration file itself so it can never be re-run destructively. Re-encrypting inside SQL is impossible (the master key only exists in the Edge runtime), so the dedupe block becomes a no-op guard with a comment explaining why.

### Phase 4: Fix sync latency (Android)
- `SupabaseClient.kt`: give **user-initiated writes** their own OkHttp client or dispatcher, so they never queue behind polling.
  - Create `writeClient` for `chat-send`, `chat-open`, booking create and status update, and a separate `pollClient` for sync, list, poll and read.
  - Raise `dispatcher.maxRequestsPerHost` to about 10 on both. Share the connection pool.
  - Expose `chatWriteApi` and `chatPollApi`, or add an OkHttp tag, whichever is smaller.
- `ServoraViewModel.startLiveSyncLoop()`:
  - Change the full sync from every **5s** to on app start, on resume (`ProcessLifecycleOwner` STARTED), on pull-to-refresh, and then every **60s**.
  - Guard it with a `Mutex` (`tryLock`) so runs never overlap, and stop it while the app is in the background.
  - Keep the existing 12s single-booking poll for the live-tracking screen.
- `SupabaseSyncManager.performFullSync()`:
  - Pull bookings incrementally (`updated_at=gt.<lastSync>`, if the column exists; if not, add it with a trigger in the Phase 3 migration).
  - Push missing local bookings concurrently (`coroutineScope { map { async { } }.awaitAll() }`, capped at 4). Skip reviews and addresses unless there's a local change or it's been more than 10 minutes.
- **Stop remote pulls overwriting newer local edits (RC-C4).**
  - Add `pendingSync: Boolean` and `localUpdatedAt: Long` to the Room `Booking` (with a Room migration, and bump the DB version).
  - `updateBookingStatus` sets `pendingSync = true`. The pull skips rows where `pendingSync` is true. A successful push clears it.
- `SupabaseSyncManager.updateBookingStatus()`:
  - Call the (now fixed) Edge Function **first and only**. Fall back to direct REST only on a network or 5xx error, not after every attempt.
  - Remove the `is_paid = true` / `paid_at = millis-string` side effect on COMPLETED. Payment status belongs to the payment flow, and writing epoch millis as text into a timestamp column fails.
- Room id vs Supabase id (RC-B5): when the chat opens from a booking, send `booking_code` as the primary key (`ChatOpenRequest.bookingCode` is already there). `chat-open` should look up by `booking_code` **first** and only then by `id`.
  - In `performFullSync`, stop inserting remote rows with `id = remote.id` when that id already belongs to a local booking with a different `booking_code`. `insertBooking` uses `OnConflictStrategy.REPLACE` and would silently delete the local booking. Match on `booking_code` instead, and store the server id in a separate `remoteId` column.
- `MainActivity.kt` ~217: replace `LaunchedEffect(allBookings) { chatViewModel.loadConversations() }` with a key that only changes when the set of booking ids or statuses changes, for example `allBookings.map { it.bookingCode to it.status }`.
- `ChatViewModel`:
  - Polling: global `chat-sync` every 4s while the chat list is visible, and every 15s otherwise. Thread poll every 2–3s only while a thread is open. Pause everything when the app is backgrounded.
  - Call `listConversations()` only when `chat-sync` reports a changed `latest_seq`, `unread_count` or cursor for a known conversation, or a new conversation id.
  - Optional stretch (only if it's small): subscribe to Supabase Realtime `postgres_changes` on `chat_conversations` filtered by participant, as a wake-up signal that triggers an immediate `chat-sync`. Keep polling as the fallback. Skip this if it needs RLS changes that aren't already in place.

### Phase 5: Deploy
Give me the exact commands, and run them yourself if the Supabase CLI is logged in here:
```
supabase db push            # or run supabase_chat_fix_v6.sql in the SQL editor
supabase functions deploy chat-open chat-send chat-read chat-list chat-sync chat-ack chat-lifecycle chat-admin-read chat-admin-list booking-create booking-update-status
supabase secrets list       # confirm CHAT_MASTER_KEY exists; if not: openssl rand -base64 32 → supabase secrets set CHAT_MASTER_KEY=...
```
Tell me explicitly whether you ran these or whether I need to.

## 5. MANUAL TEST CHECKLIST (I run this myself)
1. Turn off Wi-Fi and send a message. It shows FAILED with "No connection". Turn Wi-Fi back on and tap retry. It shows SENT, and the other device receives it within about 3s.
2. Send from a completed (read-only) chat. The composer is disabled, or you get "This chat is closed". There is no fake success.
3. The chat list shows no demo rows, and each customer–partner pair appears once.
4. Complete the booking. Within one sync the chat moves from **Active** to **Recent**. Book the same partner again, and it moves back to Active with **one** "New booking accepted" message.
5. Open a long thread. It opens on the newest messages, and the unread badge doesn't clear messages you haven't scrolled to.
6. Change a booking status on device A. Device B and the Supabase table update within about 2s, and the status on A never flips back.
7. Create a new booking and open chat right away. It opens, or shows "hasn't synced yet" with retry. It never opens a chat with the wrong person.
8. Logcat: no more than one full sync per minute while idle, and no overlapping syncs.

## 6. ACCEPTANCE CRITERIA
1. `grep -rn "FakeChatStore\|FakeChatRepository" app/src/main/java` matches only the gated debug construction and the fake classes.
2. No chat or booking Edge Function returns raw crypto or Postgres text, and all functions deploy (fixed `../_shared` imports).
3. `last_message_seq` and read-state cursors can never decrease.
4. Active vs Recent is driven by booking status on the server (trigger plus recompute).
5. User writes don't share a queue with background polling, and the full sync runs no more often than every 60s while idle.
6. `assembleDebug` passes. List every changed file.

## 7. OUTPUT FORMAT
One short report at the end, 20 lines max:
- which RCs you confirmed or corrected,
- files changed,
- the migration file name,
- the count query result or instructions for undecryptable merged messages,
- whether you ran the deploy and secret commands,
- what's still needed for release auth (RC-C6).
