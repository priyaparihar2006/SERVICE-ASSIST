# MASTER PROMPT — Masked, Encrypted, Admin-Auditable In-App Chat (Servora / ServiceAssist)

Paste everything below the line into your coding agent (Claude Code, AI Studio, Cursor). Work in the repo root `ServiceAssist/`.

---

## 0. ROLE AND WORKING RULES

You are a staff-level engineer with 10+ years of experience across Android (Kotlin, Jetpack Compose), Postgres/Supabase security, and trust-and-safety design for two-sided service marketplaces. You are pragmatic, security-first, and you never ship something you have not built and tested.

Rules for this task:

1. Read before you write. Open every file named in section 1 and confirm it matches what I describe. If reality differs, trust the code and say so.
2. Post a short plan first (files to touch, migrations, risks, open assumptions), then implement phase by phase (section 9). Do not stop between phases: run straight through and report once at the end, unless you are truly blocked.
3. Do not ask me questions unless you are truly blocked. Otherwise pick the sensible default, write the assumption down in one line, and keep going.
4. Keep changes small and reviewable. Do not refactor unrelated code. Do not grow `ServoraViewModel` (it is already a god-class); chat gets its own ViewModel.
5. Never log, print, commit or hard-code secrets, message text, phone numbers or keys. Do not open or echo `.env`.
6. **No tests.** I will test everything myself. Do not write or run unit tests, Deno tests, SQL/RLS test scripts, Compose UI or screenshot tests, and do not fix existing tests. The only check you run is `./gradlew assembleDebug` once at the end of each phase, just to confirm it compiles. Keep every report to a few lines. (AGP 9.1.1, Gradle 9.3.1, Kotlin 2.2.10.)

## 1. PROJECT CONTEXT (verified against the repo)

- Android app, package `com.example`, single `MainActivity`, Compose Material 3, Room 2.7.0, Retrofit + Moshi + OkHttp against Supabase PostgREST (no supabase-kt).
- Three roles in one app: `UserRole.CUSTOMER`, `PROFESSIONAL` (called "Partner"), `ADMIN`. Enum in `data/model/Models.kt`.
- Navigation is hand-rolled: `sealed interface AppScreen` in `MainActivity.kt` (line ~81) plus `remember` tab state (`currentTab`, `currentPartnerTab`, `currentAdminTab`, lines ~113-115).
- Bottom navs live in `ui/components/`:
  - `ServoraBottomNav.kt` — `enum ServoraNavTab`: `HOME("SA")` (green square logo), `EXPLORE("Native")`, `OFFERS("Rewards")`, `PROFILE("Account")`. Test tags `nav_home`, `nav_explore`, `nav_offers`, `nav_profile`. The `BadgedBox(badge = {})` is currently empty.
  - `PartnerBottomNav.kt` — `PartnerNavTab`: Duty & Jobs, Earnings, Toolkit, Pro Profile.
  - `AdminBottomNav.kt` — `AdminNavTab`: Ops Control, Customers, Partners, Bookings, Admin.
- Tab content is rendered in `MainActivity.kt` `when (currentTab)` (~line 401), partner `when` (~line 355) and admin block (~line 335).
- Admin "account view" already exists: `AdminDashboardScreen.kt` shows a "Switch Active Account?" dialog (~line 300) and `viewModel.adminSwitchToUser(...)` / `adminReturnToDashboard()`; `MainActivity.kt` shows an indigo "Viewing as {name} ({role}) — Return to Admin" banner (~line 291). Today this is purely client-side.
- Booking lifecycle: `BookingStatus` = PENDING > CONFIRMED > ASSIGNED > ON_THE_WAY > ARRIVED > STARTED > COMPLETED / CANCELLED. `Booking` has `professionalId`, `customerId`, `customerPhone`.
- Supabase schema is `supabase_schema.sql` (root). Tables: `user_profiles`, `saved_addresses`, `bookings`, `reviews`, `service_categories`.

Where phone numbers currently leak between the two parties (all must be removed for the customer/partner experience):

- `PartnerJobsScreen.kt` ~line 354 (`ACTION_DIAL` intent) and ~line 637 (`onCall("+919876543210")`, "Call Client" button, hard-coded number).
- `BookingConfirmationScreen.kt` ~line 478: a green `Icons.Default.Call` circle beside the professional card (currently decorative, not wired to an intent) — it invites a call, so replace it with a Message action. Also check every place `Professional.phone` could be rendered to a customer.
- `Professional.phone` (`Models.kt` ~line 131) with hard-coded numbers in `ServoraRepository.kt` (~lines 587-626); `Booking.customerPhone` (`Models.kt` ~line 52) and its DTO fields in `SupabaseModels.kt` — the partner's device downloads and stores the customer's phone in Room via `SupabaseSyncManager`.
- Admin screens (`AdminDashboardScreen.kt`) legitimately show phones. Leave those.

## 2. WHAT I WANT (requirements)

1. A private chat between a customer and the partner assigned to their booking, so they can coordinate before the partner arrives.
2. Neither side can see the other's phone number (or email, or full profile). They must use the app to coordinate. No `tel:`, no WhatsApp deep links, no phone in any API response, DTO, Room row, notification or log that reaches the other party.
3. Chat is encrypted. Only the customer, the assigned partner and the Admin can ever read it.
4. Admin can read any chat, read-only, through the existing account view (impersonation). Admin access is logged.
5. A new "Chats" section in the customer bottom navbar alongside SA, Native, Rewards, Account. Partners get a Chats tab too. Unread badge on both.

## 3. ENCRYPTION MODEL (decided — do not relitigate)

I do NOT want end-to-end encryption. The Admin must be able to read every chat, so the chat is **admin-monitored and server-mediated**, encrypted in transit and at rest:

- TLS for all traffic. Message bodies are never stored in plaintext: envelope encryption (AES-256-GCM) at rest, section 6.
- Clients never read the chat tables directly. Every read and write goes through Supabase Edge Functions that check membership, then encrypt/decrypt server-side.
- Customer and partner can read only their own booking's thread. The Admin reads any thread through an admin-only function, read-only, and every open writes an audit row.
- Do NOT call this "end-to-end encrypted" anywhere in the UI, docs or store listing. Use: "Private between you and your professional. Servora may review chats for safety and support."
- Do not build client-side key management, sealed boxes or escrow keys. Keep it simple.

## 4. PHASE 0 BLOCKER — REAL IDENTITY AND CLOSED RLS

Chat cannot ship on the current backend. Today there is no real authentication (login builds a local profile from any phone/email, `switchRole()` can make anyone ADMIN) and every RLS policy is `USING (true)` for anon. Anyone with the anon key could read chats or make themselves admin.

Minimum required before any chat data touches production:

- Supabase Auth with a real session (JWT) per user. Map `user_profiles.id` to `auth.uid()` (uuid). Migrate the string ids (`user_priya_1` etc.) or add an `auth_user_id uuid unique` column.
- Role is stored server-side in `auth.users.raw_app_meta_data->>'role'` (`customer` | `partner` | `admin`), set only by service role or the dashboard. Never read the role from `user_metadata` or from a client-writable column.
- Resolve the local-vs-remote booking id mismatch (Room autoincrement `id` vs Supabase `BIGSERIAL`). Chat keys on the Supabase `bookings.id`; add a `remoteId` mapping in Room or key by `booking_code`.
- Tighten policies on `bookings` and `user_profiles` so a customer sees only their rows, a partner sees only bookings assigned to them, and neither can update `role`. Partners must read bookings through a view/RPC that excludes `customer_phone`.
- The OkHttp client currently sends only the anon key. Add an interceptor that sends `Authorization: Bearer <user access token>`.
- Set OkHttp logging to `Level.NONE` in release and never `BODY` for chat endpoints. Set `android:allowBackup="false"` (or exclude chat data).

If the user cannot finish auth now, build the UI against a `FakeChatRepository` behind a debug-only BuildConfig flag with fake data, and clearly mark chat as NOT production-ready. Never point that fake at real keys.

## 5. DATA MODEL (new file `supabase_chat_schema.sql`, idempotent)

```sql
create extension if not exists pgcrypto;

create table if not exists public.chat_conversations (
  id            uuid primary key default gen_random_uuid(),
  booking_id    bigint not null references public.bookings(id) on delete cascade,
  customer_id   uuid   not null,
  partner_id    uuid   not null,
  status        text   not null default 'ACTIVE' check (status in ('ACTIVE','READ_ONLY','CLOSED')),
  key_version   int    not null default 1,
  wrapped_dek   bytea  not null,           -- per-conversation data key, AES-GCM-wrapped by the master key
  opened_at     timestamptz not null default now(),
  closes_at     timestamptz,
  last_message_at timestamptz,
  created_at    timestamptz not null default now(),
  unique (booking_id, partner_id)          -- reassignment => new conversation, old partner loses access
);

create table if not exists public.chat_messages (
  id              uuid primary key,        -- client-generated UUID => idempotent retries
  conversation_id uuid not null references public.chat_conversations(id) on delete cascade,
  seq             bigint generated always as identity,   -- cursor for polling: ?after_seq=
  sender_id       uuid not null,
  sender_role     text not null check (sender_role in ('CUSTOMER','PARTNER','SYSTEM')),
  kind            text not null default 'TEXT' check (kind in ('TEXT','QUICK_REPLY','ETA','SYSTEM')),
  ciphertext      bytea not null,
  nonce           bytea not null,
  created_at      timestamptz not null default now()
);
create index on public.chat_messages (conversation_id, seq);

create table if not exists public.chat_read_state (
  conversation_id uuid references public.chat_conversations(id) on delete cascade,
  user_id         uuid,
  last_read_seq   bigint not null default 0,
  updated_at      timestamptz not null default now(),
  primary key (conversation_id, user_id)
);

-- Blocked-send attempts (contact-sharing). Stores the reason only, never the text.
create table if not exists public.chat_flags (
  id uuid primary key default gen_random_uuid(),
  conversation_id uuid not null references public.chat_conversations(id) on delete cascade,
  sender_id uuid not null,
  reason text not null check (reason in ('PHONE','EMAIL','EXTERNAL_APP','ABUSE')),
  created_at timestamptz not null default now()
);

-- Every admin read, forever (append-only).
create table if not exists public.chat_admin_audit (
  id uuid primary key default gen_random_uuid(),
  admin_id uuid not null,
  conversation_id uuid,
  target_user_id uuid,
  action text not null check (action in ('LIST','OPEN','EXPORT')),
  created_at timestamptz not null default now()
);

alter table public.chat_conversations enable row level security;
alter table public.chat_messages      enable row level security;
alter table public.chat_read_state    enable row level security;
alter table public.chat_flags         enable row level security;
alter table public.chat_admin_audit   enable row level security;

-- Default deny: NO policies for anon/authenticated. Only Edge Functions (service role) touch these tables.
revoke all on public.chat_conversations, public.chat_messages, public.chat_read_state,
              public.chat_flags, public.chat_admin_audit from anon, authenticated;
```

Add a retention job (pg_cron): hard-delete messages 90 days after a conversation is CLOSED (make the number a config value). Add an update-blocking trigger on `chat_admin_audit` (no UPDATE/DELETE).

## 6. SERVER: SUPABASE EDGE FUNCTIONS (Deno/TypeScript, `supabase/functions/`)

Envelope encryption: master key `CHAT_MASTER_KEY` (32 random bytes, base64) lives only in Edge Function secrets. Each conversation gets a random 256-bit DEK at creation, stored wrapped (AES-256-GCM). Each message: AES-256-GCM, fresh 96-bit random nonce, AAD = `conversation_id | message_id | sender_id | key_version`, so ciphertext cannot be replayed into another thread. Use Web Crypto (`crypto.subtle`). Support `key_version` for master-key rotation.

Every function: verify the JWT (`Authorization` header), derive `uid` and `role` from the token (`app_metadata.role`), return generic errors, never log bodies.

| Function | Who | What |
|---|---|---|
| `chat-open` | customer or partner | Idempotently create/return the conversation for a booking. Verify caller is that booking's customer or assigned partner and status is ASSIGNED..STARTED. |
| `chat-list` | customer, partner | Their conversations: booking id, service name, counterpart **display name only** (first name + role label, e.g. "Rajesh · Your Professional"), last-message preview (decrypted server-side), unread count, status. No phone, email or full address. |
| `chat-read` | customer, partner | `after_seq` cursor, page of decrypted messages. Membership check every call. Also upserts `chat_read_state`. |
| `chat-send` | customer, partner | Validate membership and status is ACTIVE; max 1000 chars; rate limit 20 msgs/min/user and 500 msgs/conversation; run the contact-info scrubber (below); encrypt; insert; update `last_message_at`; trigger a push that contains NO message text ("New message from your professional"). Idempotent on client message id. |
| `chat-admin-list` | admin only | Conversations for `?user_id=` or `?booking_id=`. Writes a `chat_admin_audit` row. |
| `chat-admin-read` | admin only | Full thread, both sides, decrypted, read-only. Writes an audit row per open. |
| `chat-lifecycle` (DB webhook/cron) | system | On booking status change: COMPLETED/CANCELLED => conversation `READ_ONLY` and `closes_at = now() + 24h`; after that `CLOSED`. On partner reassignment: close old conversation, open new one, post a SYSTEM message. |

Contact-info scrubber (shared module): block the send and return a friendly error when the text contains an Indian mobile pattern in any spacing/format (`+91`, `0091`, 10 digits with spaces/dots/dashes, digits split by words), spelled-out digit sequences, emails, UPI ids, or phrases such as "whatsapp", "call me", "my number", "telegram", "insta id". On block: insert a `chat_flags` row (reason only, no content) and tell the sender "For your safety, contact details can't be shared here. Keep chatting in the app." Quick replies and ETA messages bypass the scrubber because they are server-defined.

## 7. ANDROID IMPLEMENTATION

New files (keep the existing package layout):

- `data/remote/chat/ChatApiService.kt` (Retrofit interface to `/functions/v1/chat-*`), `ChatDtos.kt` (Moshi). No supabase-kt needed.
- `data/repository/ChatRepository.kt` — interface + real implementation + `FakeChatRepository` (debug only). Encryption is entirely server-side; the Android client sends and receives plain text over TLS and holds no chat keys.
- `ui/viewmodel/ChatViewModel.kt` — separate from `ServoraViewModel`. State: conversations, active thread, unread total, send state per message (Sending / Sent / Failed + retry).
- `ui/screens/ChatListScreen.kt`, `ui/screens/ChatThreadScreen.kt`.
- `ui/components/`: `ChatBubble`, `QuickReplyRow`, `ChatSafetyBanner`, `DayDivider`, `UnreadBadge`.

Realtime strategy: Phase 1 uses foreground polling of `chat-read?after_seq=` every 3 s while a thread is open and every 20 s for the list/badge while the app is foregrounded, with exponential backoff on failure. Design the repository so Supabase Realtime or FCM can replace polling later (FCM is declared in Gradle but there is no `google-services.json`; note this, do not fake it).

Local storage: do not persist decrypted chat text in Room in plaintext. Keep the thread in memory. If an offline cache is wanted, use SQLCipher (or Room + Keystore-wrapped key) — decide and explain in the plan. Apply `FLAG_SECURE` on the thread screen only if I confirm.

Thread UX:

- Header shows counterpart display name and role label only, plus service name and booking code. No phone icon anywhere.
- Persistent `ChatSafetyBanner`: "Chats are private between you and your professional. Servora may review chats for safety and support."
- Quick-reply chips. Partner: "On my way", "Running 10 min late", "I've arrived", "Please share a landmark", "Please keep the area ready". Customer: "Gate is open", "Please call the bell", "Landmark: …", "Running late, please wait".
- One-tap "Share ETA" for partner (server-defined message).
- Optimistic send with client UUID, sent/read ticks, retry on failure, day dividers, empty state ("Chat unlocks once a professional is assigned"), read-only state after completion ("This booking is complete. Chat is closed").
- Disabled composer and clear message when status is READ_ONLY/CLOSED.
- Accessibility: content descriptions, 48dp touch targets, TalkBack labels; respect the existing theme (`ServoraGreen`, `ServoraCharcoal`, `ServoraMuted`).

## 8. NAVBAR AND SCREEN WIRING

Customer nav (`ServoraBottomNav.kt`): add `CHATS("Chats", Icons.AutoMirrored.Filled.Chat, Icons.AutoMirrored.Outlined.Chat (or nearest available), "nav_chats")`. Final order: **SA, Native, Chats, Rewards, Account**. Add a parameter `unreadChatCount: Int = 0` and render the badge in the currently empty `BadgedBox`. Verify five tabs fit at 360dp width without label truncation; if not, reduce horizontal padding, not label text.

Partner nav (`PartnerBottomNav.kt`): add `CHATS("Chats", …, "partner_nav_chats")` after Duty & Jobs, with the unread badge (reuse the existing `Badge` styling).

Admin nav: do NOT add a sixth tab (already five). Admin gets chats two ways:

1. **Account view (primary, as I asked):** when the admin impersonates a customer or partner, that account's Chats tab is shown from `chat-admin-list?user_id=<target>` and threads open from `chat-admin-read`. The composer is hidden, the top of the thread shows "Read-only · Admin audit view · This access is logged", and the impersonation banner stays. The admin can never send as the user. Extend the existing "Switch Active Account?" dialog copy with "Viewing chats is logged."
2. **Booking shortcut (small extra):** an "Open chat" button on the admin booking detail that opens the same read-only thread.

Wiring in `MainActivity.kt`: add `ServoraNavTab.CHATS ->` in the customer `when`, `PartnerNavTab.CHATS ->` in the partner `when`, `AppScreen.ChatThread(conversationId: String)` to the sealed interface (BackHandler already covers non-MainTabs screens), and pass `unreadChatCount` from `ChatViewModel` into both navs. The `LaunchedEffect(currentTab, …)` at ~line 138 only restores bottom-nav visibility, so it needs no change (just make sure the thread screen keeps the keyboard and nav behaving correctly). Add entry points from booking tracking/confirmation (customer) and `PartnerJobCard` (partner): a "Message" button that calls `chat-open` then navigates to the thread.

Remove phone exposure (section 1 list):

- Delete the `onCall` parameter, the `ACTION_DIAL` intent and the "Call Client" button in `PartnerJobsScreen.kt`; replace with "Message customer".
- Replace the decorative Call circle in `BookingConfirmationScreen.kt` (~line 478) with a working "Message professional" action.
- Split models: `Professional` used by customer UI becomes phone-free (`ProfessionalPublic`), the phone stays in an admin-only model. Remove hard-coded numbers from customer-reachable data.
- Partner device: stop syncing `customer_phone` into the partner's Room. Partner reads bookings from a Supabase view/RPC (`partner_booking_view`) that omits it, and `BookingDto` for the partner path has no phone field. Bump the Room version with a real migration (remove `fallbackToDestructiveMigration` from this path or justify it) and wipe any phone values already stored on partner devices.
- The partner still sees the service address (needed to arrive). Say in the plan if you think any address field should also be trimmed until status is ON_THE_WAY.

## 9. PHASES

- **Phase 0** — Auth, roles, closed RLS, id mapping, OkHttp hardening (section 4).
- **Phase 1** — `supabase_chat_schema.sql`, Edge Functions, scrubber, encryption, lifecycle.
- **Phase 2** — Android data layer (`ChatApiService`, `ChatRepository`, `ChatViewModel`, fake repo).
- **Phase 3** — UI: list, thread, quick replies, navbar tabs, badges, entry points, phone removal.
- **Phase 4** — Admin account-view chats + booking shortcut + audit trail.
- **Phase 5** — Hardening: docs, privacy copy, retention job.

## 10. MANUAL TEST CHECKLIST (I will run this myself — do not write tests)

1. Customer and partner accounts on two devices exchange messages within a few seconds.
2. Neither side sees a phone number, a call button or a WhatsApp link anywhere.
3. A message containing a phone number, email or "whatsapp" is blocked with a friendly error.
4. A customer cannot open another customer's chat; a partner cannot open a booking they are not assigned to.
5. Admin, through the account view, can read a chat but cannot type, and the open is recorded in `chat_admin_audit`.
6. The Chats tab and its unread badge show for customer and partner; the other tabs still work.

## 11. PRIVACY, SAFETY AND COMPLIANCE NOTES

- Users must be told chats can be reviewed by Servora: banner in every thread, plus a line for the privacy policy/Terms and the sign-up consent. Recommend I get the privacy policy reviewed by a lawyer against India's DPDP Act 2023 (this is not legal advice).
- Push notifications must never contain message text or names of the other party's contact details.
- Add a "Report this chat" action that creates an admin-visible flag.
- Admin access must be by role, from the JWT, never from a client flag. The admin audit table is append-only.

## 12. ACCEPTANCE CRITERIA

1. From the customer app and partner app, no screen, API response, DTO, Room row, notification or log contains the other party's phone number.
2. Customer and assigned partner can exchange messages in near real time (≤3 s while the thread is open); non-participants cannot read them.
3. Database rows contain only ciphertext + nonce; no plaintext anywhere; secrets are not in the repo; the UI never claims "end-to-end encrypted".
4. Admin opens any chat through the account view, read-only, and each open is in `chat_admin_audit`.
5. Customer navbar shows SA, Native, Chats, Rewards, Account; partner navbar has Chats; unread badges work; existing tabs and test tags unchanged.
6. Contact-sharing attempts are blocked and flagged without storing the content.
7. `assembleDebug` passes and you list every file changed.

## 13. OUTPUT FORMAT

One short report at the very end (15 lines max): files changed, assumptions made, and anything I must do myself (secrets to set, migrations to run, functions to deploy).
