# MASTER PROMPT 2 — Fix Chat Delivery, Newest-First Ordering, Timestamps and Message Ticks (Servora / ServiceAssist)

Paste everything below the line into your coding agent. This is a follow-up to `servora-chat-master-prompt.md`, which is already applied. Work in the repo root `ServiceAssist/`. Do not re-implement what already exists; fix it.

---

## 0. ROLE AND WORKING RULES

You are a staff-level engineer with 10+ years of experience in Android (Kotlin, Compose), Supabase/Postgres/Edge Functions, and real-time messaging UX. You debug from evidence, not guesses.

1. Read every file named in section 1 first. If the code differs from what I describe, trust the code and say so.
2. Post a short plan (root causes confirmed, files to touch, migrations, risks), then work phase by phase (section 5). Do not stop between phases: run straight through and report once at the end, unless you are truly blocked.
3. Do not ask me questions unless truly blocked. State an assumption in one line and continue.
4. Small, reviewable changes. No unrelated refactors. Never log message text, tokens, phone numbers or keys. Do not open or echo `.env`.
5. **No tests.** I will test everything myself. Do not write or run unit tests, Deno tests, SQL/RLS test scripts, Compose UI or screenshot tests, and do not fix existing tests. The only check you run is `./gradlew assembleDebug` once at the end of each phase, just to confirm it compiles. Keep every report to a few lines.

## 1. WHAT IS BROKEN — ROOT CAUSES ALREADY VERIFIED IN THE CODE

The symptom: Priya sends a message to Rajesh Sharma, but nothing shows up in Rajesh's Chats section. Confirm each cause below yourself, then fix all of them.

**RC1 — Every device chats with itself (the main cause).**
- `ui/viewmodel/ChatViewModel.kt` line ~34: `repository: ChatRepository = FakeChatRepository()`.
- `MainActivity.kt` line ~118: `chatViewModel: ChatViewModel = viewModel()` — no factory, so the default (the fake) is always used.
- `RemoteChatRepository` and `ChatApiService` are never instantiated anywhere. `SupabaseClient.kt` only builds `apiService` (PostgREST); there is no Retrofit instance for the chat Edge Functions.
- `FakeChatRepository` is an in-memory map private to each ViewModel instance, hard-codes the sender as `user_priya_1` / `CUSTOMER`, and returns `"Just now"` strings. Nothing ever leaves the phone, so Rajesh cannot receive anything.

**RC2 — Even if the remote repository were wired, the server would reject every call.**
- The app has no real login (`ServoraViewModel.loginWithCredentials` builds a local profile). `SupabaseClient.userAccessToken` is never set, so the interceptor sends the anon key as the bearer token, and every Edge Function's `supabaseClient.auth.getUser(jwt)` returns 401.
- Identity is derived from `user.user_metadata?.profile_id` in `chat-list` (line ~31), `chat-open` (~53-54), `chat-read` (~42), `chat-send` (~54). `user_metadata` is editable by the user, so any signed-in user could impersonate another profile.
- `chat-admin-list` and `chat-admin-read` (line ~30) accept `user.app_metadata?.role || user.user_metadata?.role`. Falling back to `user_metadata` lets any signed-in user set `role: "admin"` on themselves and read every chat. **Critical.**
- Every function has `Deno.env.get("CHAT_MASTER_KEY") ?? "QUFBQUFB…"` (a hard-coded all-`A` key) in `chat-open`, `chat-list`, `chat-read`, `chat-send`, `chat-admin-read`, `chat-lifecycle`. If the secret is missing, chats are silently encrypted with a publicly known key. **Critical: fail closed.**
- Rajesh's demo login profile is `user_rajesh_pro` (`ServoraRepository.kt` ~line 137, `demoPartner`), but the professional record and `bookings.professional_id` use `pro_rajesh_1` (~line 585), and `chat_conversations.partner_id` copies that value. Nothing maps Rajesh's account to `pro_rajesh_1`, so his chat list would be empty even with a working backend. (Manual `loginWithCredentials` accounts get `user_<digits>` ids, which match neither.)

**RC3 — Wrong names.** `chat-open` hard-codes `"Rajesh · Your Professional"` for every customer, `chat-list` returns `"Your Professional"`.

**RC4 — Ordering.** `chat-list` orders by `last_message_at` with `nullsFirst:false`, so a conversation with no message sorts last. The client never sorts (`ChatListScreen.kt` `items(conversations…)` uses list order), the fake returns `convMap.values` in insertion order and never bumps a conversation on send, and `last_message_at` is a display string (`"Just now"`), which cannot be sorted.

**RC5 — State leaks between accounts.** `ChatViewModel` is created once (`viewModel()` at Activity scope) and `init { loadConversations() }` runs once. Logging out of Priya and into Rajesh (or admin impersonation) keeps the previous user's conversations, thread and unread count in memory, and polling keeps running with the old identity.

**RC6 — Merge bug that will crash and duplicate.** `startThreadPolling` does `current.messages + newBatch.messages`. The sender's own optimistic message (client UUID) comes back from the server with the same id, so it is duplicated, and `ChatThreadScreen.kt` uses `items(messages, key = { it.id })` — duplicate keys throw `IllegalArgumentException` in LazyColumn. The optimistic message also uses `seq = lastSeq + 1` (real `seq` is a global identity, so this collides) and `senderRole = "CUSTOMER"` even when a partner sends.

**RC7 — Timestamps are wrong.** `ChatComponents.kt` `formatMessageTime` (line ~328) parses with `SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss")` in the device time zone and strips a trailing `Z`. Server times are UTC, so IST users would see UTC clock times (5 h 30 min off) and `+00:00`/fractional formats are handled by accident. Any non-ISO string shows "Now".

**RC8 — Ticks are fake.** `ChatBubble` shows `DoneAll` (double tick) for every outgoing message that is not SENDING/FAILED. There is no data model for delivered/read: `chat_read_state` has only `last_read_seq`, and only messages sent in the current session even have a status (`pendingMessages` map).

**RC9 — "Read" is marked as a side effect.** `chat-read` upserts `last_read_seq` on every call, including background polling while the screen is not visible, which would produce false blue ticks.

**RC10 — Slow and expensive refresh.** The list polls every 20 s, and `chat-list` runs 3 queries per conversation (read state, unread count, last message) plus a decrypt. Too slow for "instant popup", and it does not scale.

**RC11 — Time APIs.** `minSdk = 24` and there is no core library desugaring, so `java.time` is unsafe below API 26.

## 2. WHAT I WANT (requirements)

- **R1 Delivery.** A message sent from Priya's account appears in Rajesh's account (and vice versa) within about 5 seconds while his app is open, on a different device.
- **R2 Popup and top-of-list.** When a new message arrives from either side: the Chats tab badge increments, the conversation moves to the top of the Chats list, and if the user is not inside that thread a heads-up banner appears in the app ("Priya · Deep Cleaning — Gate is open…", tap to open).
- **R3 Newest first.** The Chats list is ordered by latest message time, newest at the top, for customers and partners. Inside a thread messages stay oldest to newest (newest at the bottom), like WhatsApp.
- **R4 Timestamps.** Every message bubble shows its time in the device's local time zone, 12-hour, e.g. "11:15 AM". Threads show day dividers (Today, Yesterday, 21 Sep 2026). List rows show the time of the last message (today: time; yesterday: "Yesterday"; older: "21 Sep").
- **R5 Ticks** (sender's side only, on messages I sent):

| State | Icon | Meaning |
|---|---|---|
| Sending | grey clock | not yet accepted by the server |
| Sent | one grey tick | server stored it |
| Delivered | two grey ticks | recipient's app has synced it |
| Read | two blue ticks | recipient had the thread open in the foreground and it was on screen |
| Failed | red exclamation + Retry | server rejected or network failed |

Ticks also appear on the last-message preview in the Chats list when the last message is mine.

Assumptions I am making (state them in your plan if you disagree): "popup" means an in-app heads-up banner plus badge and bump-to-top now, and a system push notification later (Phase E, needs Firebase); "delivered" means the recipient's app has synced while in the foreground; a conversation with zero messages is hidden from the person who did not open it.

## 3. NON-NEGOTIABLE SECURITY FIXES (ship in the same change)

1. Delete every use of `user_metadata` for identity or role in `supabase/functions/`. Role comes only from `app_metadata.role` (`customer` | `partner` | `admin`), set server-side.
2. Resolve the caller's profile id server-side via a shared helper `_shared/identity.ts`: look up `user_profiles` where `auth_user_id = auth.uid()` and return `{ profileId, role }`. Every function calls it; nobody trusts a client-supplied profile id.
3. Remove every `?? "QUFB…"` fallback. `getMasterKey()` must throw if `CHAT_MASTER_KEY` is missing or not 32 bytes, and each function returns 500 without leaking why.
4. Receipt writes (delivered/read) must never happen from admin functions. Admin views are strictly read-only, including cursors.

## 4. DESIGN

### 4.1 Data model (new migration `supabase_chat_receipts_migration.sql`, idempotent)

```sql
alter table public.chat_read_state
  add column if not exists last_delivered_seq bigint not null default 0;

alter table public.chat_conversations
  add column if not exists opened_by text,
  add column if not exists last_message_seq bigint not null default 0;

create index if not exists idx_chat_conv_customer_recent
  on public.chat_conversations (customer_id, coalesce(last_message_at, opened_at) desc);
create index if not exists idx_chat_conv_partner_recent
  on public.chat_conversations (partner_id, coalesce(last_message_at, opened_at) desc);

-- Link auth accounts to app profiles (Phase A). Partner rows use the professional id (e.g. pro_rajesh_1).
alter table public.user_profiles
  add column if not exists auth_user_id uuid unique;
```

Add a SQL function `chat_sync(p_profile_id text)` (SECURITY DEFINER, `revoke … from anon, authenticated`, called only by Edge Functions) that returns, in ONE query, for every conversation the profile belongs to and is allowed to see: `conversation_id, booking_id, status, last_message_at, latest_seq, unread_count, peer_read_seq, peer_delivered_seq`. `unread_count` counts messages with `seq > my last_read_seq` and `sender_id <> me`. All cursor updates use `greatest(existing, new)` so they can never move backwards.

`seq` is a global identity, so it is only ever compared within one conversation. Never invent a seq on the client.

### 4.2 Edge Functions

| Function | Change |
|---|---|
| `chat-sync` (new) | Cheap heartbeat for foreground polling. Returns the `chat_sync` rows, total unread, and server time. Also sets my `last_delivered_seq = latest_seq` for each conversation (this is what turns the sender's tick grey-double). Never called for admin. |
| `chat-list` | One query (or the RPC), no per-row queries. Order by `coalesce(last_message_at, opened_at) desc`. Hide conversations with `last_message_seq = 0` from the person who is not `opened_by`. Decrypt only the last message of each conversation. Return `last_message_at` as ISO-8601 UTC with `Z`, `latest_seq`, `last_message_from_me`, `peer_read_seq`, `peer_delivered_seq`, real counterpart first name and role label (fix RC3). |
| `chat-read` | Stop marking read. Mark delivered up to the highest seq returned. Always include `peer_delivered_seq` and `peer_read_seq` in the response, even when `messages` is empty, so old messages' ticks can update. `created_at` in ISO-8601 UTC with `Z`. |
| `chat-ack` (new) | `POST {conversation_id, read_seq}`. Membership check, reject admin (403), clamp `read_seq` to the conversation's latest seq, set `last_read_seq` and `last_delivered_seq` with `greatest()`. |
| `chat-send` | After insert: update `last_message_at` and `last_message_seq`; set the sender's own `last_read_seq`/`last_delivered_seq` to the new seq (so their own unread stays 0). Idempotent on client message id (already upsert). |
| `chat-open` | Store `opened_by`. Use the real professional first name from `user_profiles`/professional data (fix RC3). |
| `chat-admin-list`, `chat-admin-read` | Read-only. Return both parties' cursors so the admin UI can show ticks. No receipt writes. Keep the audit row. |

Return times as strict ISO-8601 UTC (`2026-09-21T06:15:32.123Z`). The client never sends timestamps that the server trusts; ordering is by server `created_at` then `seq`.

### 4.3 Android architecture

- Domain models with epoch millis (do not pass strings to the UI): `ChatMessageUi(id, seq: Long?, text, kind, createdAtMillis, isMine, status: MessageStatus)`, `ConversationUi(id, bookingId, title, counterpartName, lastMessagePreview, lastMessageAtMillis, lastMessageFromMe, unreadCount, status, peerReadSeq, peerDeliveredSeq, latestSeq)`. Map DTOs to these at the repository boundary. `MessageStatus = SENDING, SENT, DELIVERED, READ, FAILED`.
- `ChatTime` util (pure): parse ISO-8601 with `java.time.Instant`, format in `ZoneId.systemDefault()` with `DateTimeFormatter.ofPattern("h:mm a", Locale.getDefault())`, day-label logic with an injectable `Clock`. Enable core library desugaring (`isCoreLibraryDesugaringEnabled = true` in `compileOptions`, add `desugar_jdk_libs` to `gradle/libs.versions.toml` and `coreLibraryDesugaring(...)` in `app/build.gradle.kts`). Delete `formatMessageTime`.
- Status derivation for my messages: `seq != null && seq <= peerReadSeq` → READ; `seq <= peerDeliveredSeq` → DELIVERED; `seq != null` → SENT; local `SENDING`/`FAILED` override until the server confirms.

## 5. PHASES

### Phase A — Make it real (fixes R1, RC1, RC2, RC3, RC5)

A1. **Real identity.** Implement Supabase Auth sign-in (email + password is fine for Priya, Rajesh and the admin, no SMS cost) and store the access token in `SupabaseClient.userAccessToken`, refresh it before expiry, clear it on logout. Set `app_metadata.role` server-side. Populate `user_profiles.auth_user_id` for each account; the partner's `user_profiles.id` must equal the professional id (`pro_rajesh_1`; also reconcile `demoPartner` `user_rajesh_pro` so there is one id per person) so `bookings.professional_id` and `chat_conversations.partner_id` match. Seed those rows.
   *Staging-only fallback if Auth cannot be finished in this pass:* the Edge Functions may accept an `X-Dev-Profile-Id` header only when `Deno.env.get("ENVIRONMENT") === "staging"`, and the app sends it only when `BuildConfig.DEBUG`. It must be impossible to enable in production (server default off, documented, with a test). Say clearly that chat is not production-ready in that mode.
A2. **Wire the real repository.** Add `SupabaseClient.chatApiService` (same OkHttp client and base URL as `apiService`). Add `ChatViewModelFactory` and create `ChatViewModel` through it in `MainActivity`. Use `RemoteChatRepository` whenever Supabase is configured; use the fake only if `BuildConfig.DEBUG && BuildConfig.CHAT_USE_FAKE` (default false). The fake must never be the default when Supabase keys exist.
A3. **Identity-aware fake.** Replace the per-instance fake with a process-wide `FakeChatStore` keyed by profile id and role, supporting the same receipt cursors, so a single-emulator account switch behaves like the real backend.
A4. **Session handling.** In `MainActivity`, `LaunchedEffect(currentUser.id, currentUser.role, impersonatingAdminProfile != null)` calls `chatViewModel.onSessionChanged(...)`, which cancels all polling jobs, clears conversations, active thread, pending messages, unread count and errors, then reloads. On logout also clear `userAccessToken`.
A5. **Server fixes from section 3** and the corrected names (RC3).

### Phase B — Server receipts and ordering (R3, R5 server side, RC4, RC9, RC10)

Apply the migration, `chat_sync` RPC, new `chat-sync` and `chat-ack`, and the changes in 4.2. Behaviours I will verify manually, so get them right: cursors never decrease; a non-participant gets 403; admin calls never change any cursor; `chat-list` is newest first; missing `CHAT_MASTER_KEY` returns 500 and stores nothing; `user_metadata.role` is never trusted.

### Phase C — Android: models, time, ordering, ticks, popup (R2, R3, R4, R5, RC4-RC8, RC11)

C1. Domain models, mappers and `ChatTime` (section 4.3). Enable desugaring.
C2. **`ChatViewModel` rewrite of the merge and send paths.** Merge by message id (a server copy replaces the optimistic one), sort by `createdAtMillis` then `seq`, never throw on duplicate keys, always apply `peerReadSeq`/`peerDeliveredSeq` from every read even when no new messages arrived. Optimistic messages get `seq = null`, the real sender role from the session, and `createdAtMillis = System.currentTimeMillis()`; they are replaced by the server's `created_at` on confirmation.
C3. **Sync loop and lifecycle.** Foreground only, using `repeatOnLifecycle(Lifecycle.State.STARTED)`: `chat-sync` about every 4 s (exponential backoff to 30 s on failure, reset on success); when a thread is open, `chat-read` every 2 to 3 s. Fetch the full `chat-list` only when `chat-sync` shows a changed `latest_seq`, `unread_count` or a new conversation. Pause everything when the app is backgrounded.
C4. **Read receipts done right.** In `ChatThreadScreen`, call `chatViewModel.ackRead(conversationId, maxVisibleSeq)` only while the thread is RESUMED (use `LifecycleResumeEffect`) and the newest incoming message is on screen; debounce 300 ms; never in admin view; never in the background.
C5. **List ordering.** Sort `ConversationUi` in the ViewModel by `lastMessageAtMillis ?: openedAtMillis` descending, tie-break by conversation id, as a safety net on top of the server order. `LazyColumn` with stable keys and `Modifier.animateItem()` so a conversation visibly jumps to the top. Unread rows: bold preview, accent time, count badge. Own last message: "You: …" prefix with the tick icon.
C6. **In-app popup.** `IncomingMessageBanner` composable hosted in `MainActivity` above the content. Trigger when a conversation's `latest_seq` grows, `last_message_from_me` is false, and the user is not inside that thread. Shows sender first name, service name and a preview (first 60 characters), tap opens the thread, auto-dismiss after 4 s, one banner at a time (newest replaces older). No banner for the admin audit view.
C7. **Ticks and timestamps UI.** `MessageStatusTicks(status)` implements the table in R5 (`Icons.Default.Schedule`, `Icons.Default.Done`, `Icons.Default.DoneAll` grey, `DoneAll` blue `#34B7F1`, error icon) with TalkBack descriptions ("Sending", "Sent", "Delivered", "Read", "Failed to send"). The outgoing bubble is currently solid `ServoraGreen`, where grey and blue ticks will not be legible; change outgoing bubbles to a light green tint with dark text (for example `#E7F8EF`) or otherwise guarantee at least 3:1 contrast for both tick colours, and say what you chose. Replace the hard-coded "Encrypted in transit & at rest" `DayDivider` text with real day dividers, and keep the safety banner separate. Every bubble shows its time.
C8. **Admin view.** Show timestamps and, for each side's messages, ticks derived from both cursors. Read-only.

### Phase D — Manual test checklist (I will run this myself; do not write tests or docs for it)

 1) Priya sends, Rajesh's tab badge goes 0 to 1 and the row jumps to the top with banner. 2) Priya's tick is one grey. 3) Rajesh's app in foreground on the Jobs tab: Priya's tick turns two grey. 4) Rajesh opens the thread: Priya's ticks turn two blue within 3 s. 5) Rajesh replies; Priya's list reorders. 6) Two conversations: message the older one, it moves above the newer one. 7) Set the phone to another time zone and confirm times change. 8) Logout and login as Rajesh on Priya's device: no Priya data visible.

### Phase E — Later (do not start unless I ask)

FCM push for the background case (needs a Firebase project and `google-services.json`, which the repo does not have): data-only message that wakes the app to call `chat-sync`; payload contains no message text and no names. Optionally replace polling with a Supabase Realtime broadcast channel per profile that carries only `{conversation_id, latest_seq}`. Until then, when the recipient's app is closed the sender's tick stays a single grey tick. Say this limitation plainly in the release notes.

## 6. ACCEPTANCE CRITERIA

1. Two different devices: a message from Priya shows in Rajesh's Chats within 5 s, and a reply shows in Priya's within 5 s.
2. On each new message the conversation is at the top of the list for both roles, the Chats badge updates, and the banner shows when the user is elsewhere in the app.
3. Every bubble has a correct local-time timestamp; the list shows correct relative times; no "Just now" or "Now" placeholders remain.
4. Ticks: one grey after send, two grey after the recipient's app syncs, two blue only after the recipient actually viewed the thread; never blue from background polling; admin viewing never changes ticks.
5. No duplicate messages, no LazyColumn key crash, no leaked state after switching accounts.
6. No `user_metadata` in any Edge Function, no hard-coded master key fallback, admin role only from `app_metadata`.
7. `chat-list` uses a constant number of queries; foreground sync interval is about 4 s and stops in the background.
8. `assembleDebug` passes. List every file changed.

## 7. OUTPUT FORMAT

One short report at the very end (15 lines max): root causes confirmed, files changed, assumptions, and anything I must do myself (secrets, migrations, functions to deploy).
