# MASTER PROMPT 9 — Fix "Failed to read messages" (chat-read 500 + chat-list 500) After the Per-Booking Migration (Servora / ServiceAssist)

Paste everything below the line into your coding agent. Work in the repo root `ServiceAssist/`. These have already been applied locally:

- Master Prompt 7 (`servora-chat-failed-to-read-messages-fix-master-prompt.md`)
- The per-booking prompt (`servora-chat-per-booking-message-button-master-prompt.md`)

The per-booking migration and the edge function changes it produced introduced new server bugs. Fix them from evidence. Don't redesign anything.

---

## 0. ROLE AND WORKING RULES

You are a staff-level engineer with 10+ years in Supabase (Postgres/PL/pgSQL, PostgREST, Deno Edge Functions, supabase-js v2) and Android (Kotlin, Retrofit, OkHttp). You debug from evidence, never from guesses.

1. **Phase 0 comes first.** Get the actual server error before changing code. If the evidence contradicts this prompt, trust the evidence and say so.
2. Post a 5-line plan, then do every phase without stopping. Report once at the end.
3. Don't ask questions unless you are blocked. State assumptions in one line.
4. Keep changes small, with no refactors. Never log or print message text, `CHAT_MASTER_KEY`, the service-role key, OTPs or phone numbers.
5. **No tests.** Don't write or run unit, Deno, SQL or UI tests. The only checks are:
   - `deno check` on each changed function (if Deno is installed)
   - `./gradlew assembleDebug` once at the end
   - the read-only diagnostic SQL/curl calls in Phase 0 and Phase 5
6. **Never** regenerate or overwrite `wrapped_dek`, never change `CHAT_MASTER_KEY`, and never delete chat rows.

## 1. SYMPTOM AND EVIDENCE (logcat, customer `user_priya_1`)

The customer taps Message and the "Support & Pro Chat" thread shows **"Failed to read messages"**. It never recovers.

```
POST functions/v1/chat-open                                   <-- 200 (2559ms)
GET  functions/v1/chat-list                                   <-- 500 (454ms)   sb-error-code: EDGE_FUNCTION_ERROR
GET  functions/v1/chat-read?conversation_id=67e31306-867d-4a3a-9059-c77a0ec2351d&after_seq=0
                                                              <-- 500 (1084ms)  sb-error-code: EDGE_FUNCTION_ERROR
     (chat-read retried every ~4-10 s → 500 every time)
GET  functions/v1/chat-sync                                   <-- 200 (every poll)
```

What this tells us:

- **The master key is fine.** `chat-open` returned 200, and both of its code paths (the existing-conv check and new-conv creation) call `getMasterKey()` successfully. A MASTER_KEY failure would have made `chat-open` fail too.
- **Auth and identity are fine.** `chat-sync` returns 200 with the same headers.
- **Unwrap/decrypt isn't the cause.** A `CONVERSATION_KEY` failure returns **409**, not 500 (`_shared/http.ts`). Per-message decrypt errors are caught inside the loop.
- The app's OkHttp logger only prints headers, so **the JSON error body was never captured**. Neither was the `[ServerError]` line that `jsonError` writes to the function logs. Phase 0 fixes that.

## 2. ROOT CAUSES — VERIFIED IN THE REPO

### RC1: `supabase_chat_per_booking_threads_migration.sql` is broken and can't run cleanly
- **Step 2** runs `UPDATE … SET closed_at = NOW()`. **No `closed_at` column exists** in any schema file (`grep -l closed_at *.sql` only matches this file). That's error 42703. Depending on how it was run, the script aborted here or partway through.
- **Step 4** has three problems:
  - It does `CREATE OR REPLACE FUNCTION public.chat_list_summaries(...) RETURNS TABLE(...)` with a **different column list** from the live v6/v7 function. Postgres refuses that with `42P13 cannot change return type of existing function`. It needs a `DROP FUNCTION` first.
  - It joins `public.profiles` / `full_name`. **That table doesn't exist.** The real table is `public.user_profiles` with a `name` column (`supabase_schema.sql:30`; v7 uses `user_profiles p … p.name`).
  - It declares `wrapped_dek text`, `last_msg_ct text` and `last_msg_nonce text`, but the columns are `BYTEA`. `RETURN QUERY` fails with `42804 structure of query does not match function result type`. **v6 and v7 have the same `wrapped_dek TEXT` vs `BYTEA` mismatch.** Before Master Prompt 7, `chat-list` swallowed RPC errors and returned `[]` with a 200, which hid this. Now it surfaces as a 500.
- **Unread math is wrong.** `unread_count = last_message_seq - my last_read_seq`, but `chat_messages.seq` is one **global** identity across all conversations, not per conversation. This over-counts wildly and also counts your own messages. v7's `COUNT(*) WHERE seq > last_read AND sender_id <> me` is the correct approach.
- **Net effect: `chat-list` gets a DB error on every call, so it returns 500.** It's also unknown which parts of the migration actually took effect: is the pair index dropped, and does the booking index exist?

### RC2: `chat-open` resets everyone's read receipts on every open
`chat-open/index.ts` "Initialize read states" `upsert`s both participants with `last_read_seq: 0, last_delivered_seq: 0` and `onConflict: "conversation_id, user_id"`. That **overwrites existing cursors with 0 every time anyone taps Message**, so:
- blue ticks vanish,
- the unread badge resets to the full thread,
- every message looks undelivered.

It must create missing rows only.

### RC3: `chat-open` archives a healthy conversation on a master-key error
The existing-conv check does `catch (err) { if (err?.kind === "CONVERSATION_KEY" || err?.name === "ChatCryptoError") → status CLOSED, archived_reason KEY_LOST }`. `err.name === "ChatCryptoError"` is also true for **`MASTER_KEY`** errors. So a transient or misconfigured secret silently closes the conversation and orphans its history. Only `kind === "CONVERSATION_KEY"` may archive. `MASTER_KEY` must re-throw.

### RC4: `chat-lifecycle` writes a column that doesn't exist
`chat-lifecycle/index.ts` also references `closed_at`. Every close/reassign update it makes fails with 42703. (Check how it handles the error; it's probably silently ignored.)

### RC5: `chat-read` 500 — cause not yet proven; Phase 0 decides
In the current local `chat-read/index.ts`, no statement obviously throws on a healthy row. The leading hypotheses, in order:
- **H1: the deployed `chat-read` is stale.** The local file was edited at 08:54 today by Master Prompt 7, but the deployed version may still be the old one with `.rpc(...).catch(...)` (TypeError) or the missing `generateAndWrapDek` import (ReferenceError). Both return exactly the generic `"Failed to read messages"`. Deploy drift is likely, because `chat-open` already runs the new per-booking code while other functions may not.
- **H2: a DB error surfaced by the migration state.** For example, `chat_messages` / `chat_read_state` / the `last_message_seq` column is missing or has the wrong type, `msgError` is thrown, and the generic 500 comes back.
- **H3: `new Date(msg.created_at).toISOString()` throws `RangeError: Invalid time value`** for a row with a null or odd `created_at`. For example, a row inserted by the `chat_append_message` fallback path in `chat-open`'s `injectEncryptedSystemMessage`.

## 3. THE FIX

### Phase 0: Get the real error (read-only, do this first)
1. **Function logs.** Get the most recent `chat-read` and `chat-list` errors from the Supabase Dashboard → Edge Functions → *function* → Logs, or with the CLI if it's logged in. `jsonError` prints `[ServerError] <name> <message> <stack line>` and `[chat-list] chat_list_summaries RPC error: {code, message}`. Quote the **error name, message and Postgres code**, never any message text.
2. **Response body.** Replay the exact failing calls (debug headers work because `ENVIRONMENT != production`):
   ```bash
   H=(-H "apikey: $SUPABASE_ANON_KEY" -H "Authorization: Bearer $SUPABASE_ANON_KEY" -H "X-Dev-Profile-Id: user_priya_1" -H "X-Dev-User-Role: CUSTOMER")
   curl -s "$SUPABASE_URL/functions/v1/chat-read?conversation_id=67e31306-867d-4a3a-9059-c77a0ec2351d&after_seq=0" "${H[@]}"; echo
   curl -s "$SUPABASE_URL/functions/v1/chat-list" "${H[@]}"; echo
   ```
3. **Deploy drift.** Run `supabase functions list` and compare each chat function's deployed `updated_at`/version with its local file time. Anything older than the local edit is stale.
4. **DB state** (read-only SQL; report the results):
   ```sql
   -- which chat indexes exist
   select indexname, indexdef from pg_indexes where tablename = 'chat_conversations';
   -- which columns exist
   select column_name, data_type from information_schema.columns
    where table_schema='public' and table_name in ('chat_conversations','chat_read_state','chat_messages') order by table_name, ordinal_position;
   -- current RPC signature and whether it runs
   select pg_get_function_result('public.chat_list_summaries(text)'::regprocedure);
   select * from public.chat_list_summaries('user_priya_1') limit 1;   -- expect an error today; quote its code
   -- the failing conversation
   select id, booking_id, status, key_version, archived_reason, last_message_seq, length(wrapped_dek) dek_len from chat_conversations where id = '67e31306-867d-4a3a-9059-c77a0ec2351d';
   select count(*), min(seq), max(seq), count(*) filter (where created_at is null) null_created from chat_messages where conversation_id = '67e31306-867d-4a3a-9059-c77a0ec2351d';
   -- more than one non-CLOSED conversation per booking?
   select booking_id, count(*) from chat_conversations where status <> 'CLOSED' group by 1 having count(*) > 1;
   ```
5. Map the evidence to RC5's H1/H2/H3 (or a new cause) and state it in one line before you continue. If you can't reach Supabase logs, the DB or the CLI, say so and ask me to paste the curl output and the SQL results. That's the one allowed stop.

### Phase 1: Replace the broken migration with a correct, idempotent one
Create `supabase_chat_per_booking_fix_v8.sql`. Don't edit the old file; add a header comment to it saying "SUPERSEDED by v8 — do not run". The new script must work whether or not the old one partly ran:
1. `ALTER TABLE public.chat_conversations ADD COLUMN IF NOT EXISTS archived_reason TEXT;`. Don't add `closed_at`; use `closes_at`/`archived_reason`, which already exist.
2. Dedupe non-CLOSED conversations per `booking_id`: keep the latest by `COALESCE(last_message_at, opened_at, created_at)` and close the rest with `archived_reason = 'DEDUPE_PER_BOOKING'`, **without** `closed_at`.
3. `DROP INDEX IF EXISTS idx_chat_conversations_active_pair; DROP INDEX IF EXISTS idx_chat_conv_cust_part;`, then `CREATE UNIQUE INDEX IF NOT EXISTS idx_chat_conversations_active_booking ON public.chat_conversations (booking_id) WHERE status <> 'CLOSED';`.
4. `DROP FUNCTION IF EXISTS public.chat_list_summaries(text);`, then recreate it:
   - **Output columns** must be exactly what `chat-list/index.ts` reads: `conversation_id, booking_id, customer_id, partner_id, status, opened_by, opened_at, last_message_at, latest_seq, wrapped_dek, key_version, booking_code, service_name, customer_name, partner_name, booking_status, my_last_read, my_last_delivered, peer_last_read, peer_last_delivered, unread_count, last_msg_id, last_msg_seq, last_msg_sender, last_msg_kind, last_msg_ct, last_msg_nonce`.
   - **Types must match the source columns exactly.** Declare `wrapped_dek BYTEA`, `last_msg_ct BYTEA`, `last_msg_nonce BYTEA` (supabase-js returns BYTEA as a `\x…` hex string, which `hexToBytes` already handles). Cast every count/seq to `BIGINT` and `key_version` to `INT`.
   - **Profiles:** join `public.user_profiles` and use `.name`, for both the customer and the partner.
   - **Unread:** `COUNT(*) FROM chat_messages m WHERE m.conversation_id = c.id AND m.seq > COALESCE(my.last_read_seq,0) AND m.sender_id <> p_profile_id`.
   - **Last message:** `LATERAL … ORDER BY seq DESC LIMIT 1`.
   - **Filter:** `(c.customer_id = p_profile_id OR c.partner_id = p_profile_id) AND c.status <> 'CLOSED'`. Keep v7's "hide empty threads the other side opened" filter only if the Android list still needs it. Since the Chats tab is gone, prefer to drop it.
   - `SECURITY DEFINER`, `SET search_path = public`, `REVOKE ALL … FROM anon, authenticated` (only the service role inside the edge function calls it).
5. Check `chat_sync(p_profile_id)` for the same issues: return types, `profiles`, and pair assumptions. Fix it the same way (drop, then recreate) if needed.
6. End the file with the verification queries from Phase 0 step 4 (commented), plus `select * from public.chat_list_summaries('user_priya_1');`, which must now succeed.

### Phase 2: Edge-function fixes
- **`chat-open` (RC2).** Initialize read state with **insert-if-missing only**: `upsert([...], { onConflict: "conversation_id,user_id", ignoreDuplicates: true })`. Never write 0 over existing cursors.
- **`chat-open` (RC3).** Archive as `KEY_LOST` only when `err instanceof ChatCryptoError && err.kind === "CONVERSATION_KEY"`. Re-throw any other error (including `MASTER_KEY`) so `jsonError` returns 500 with `code: "MASTER_KEY"`.
- **`chat-lifecycle` (RC4).** Remove every `closed_at` write. Use `closes_at` / `status` / `archived_reason`. Check each `.update()` result's `error` and `console.error` it (code + message) instead of ignoring it.
- **`chat-read` (RC5 hardening, regardless of which hypothesis was confirmed):**
  - Tag each stage so the next failure explains itself. Keep a `let stage = "conv"` variable and update it as you go: `"peer_state"`, `"messages"`, `"master_key"`, `"unwrap"`, `"decrypt"`, `"bump"`, `"respond"`. In the outer `catch`, log `[chat-read] stage=<stage> <err.name> <err.code> <err.message>` and return `{ error: "Failed to read messages", code: "READ_FAILED", stage }`. `stage` isn't sensitive.
  - Make timestamps safe: `const t = Date.parse(msg.created_at); created_at: Number.isFinite(t) ? new Date(t).toISOString() : new Date().toISOString()`.
  - Coerce `seq` with `Number(msg.seq)` before comparing.
- Apply the same `stage` pattern to `chat-list`. Add `42P13`, `42804`, `42703` and `42P01` to `jsonError`'s schema-problem branch, returning `code: "MIGRATION_MISSING"` with the Postgres code in the server log.
- Run `grep -rn "closed_at\|from(\"profiles\")\|public.profiles\|full_name" supabase/ *.sql`. Only the superseded file may still match.

### Phase 3: Deploy everything that shares `_shared/*`
There must be no drift between local and deployed code. Run the new SQL first, then:
```bash
supabase functions deploy chat-open chat-read chat-send chat-ack chat-list chat-sync chat-lifecycle chat-admin-list chat-admin-read booking-create booking-update-status
supabase functions list   # confirm every updated_at is now
```
Deploy if the CLI is logged in. Otherwise give me the exact commands in order.

### Phase 4: Android, small and diagnostic only
- In `RemoteChatRepository`'s error extraction, parse the optional `code` and `stage` fields. In **debug builds only**, `Log.w("ChatRepo", "<endpoint> <http> code=<code> stage=<stage>")`. Never log the body's message text or any chat content. The user still sees only the friendly `error` sentence.
- `ChatViewModel` read polling: after 3 consecutive `chat-read` failures, back off to 15 s, not ~4 s, and keep the Retry banner. Reset to normal on the first success.
- Leave the OkHttp logging level alone (no BODY logging).

### Phase 5: Verify (read-only)
- Re-run the Phase 0 curls: `chat-read` → **200** with a `messages[]` array (or 409 `CONVERSATION_KEY` if that thread's key is truly lost; report it), and `chat-list` → **200** with one row per booking thread and sane `unread_count`.
- Re-run the Phase 0 SQL. Exactly one non-CLOSED conversation per booking, `chat_list_summaries` runs, and no `closed_at` references remain.

## 4. MANUAL TEST CHECKLIST (I run this myself)
1. Customer → My Bookings → **Message** on an active booking. The thread loads its history with no red banner.
2. Send a message, then reply from the partner's job-card message icon. Ticks go SENT → DELIVERED → READ, and they **stay** after either side re-taps Message (RC2).
3. The unread badge on the Message button shows the real number of unread partner messages, and it clears after the thread is opened.
4. Two bookings with the same pro open two different threads.
5. Complete a booking. Its chat becomes read-only, and the Supabase logs for `chat-lifecycle` show no `closed_at` errors.
6. Supabase Edge Function logs for `chat-read` / `chat-list` show no 500s during all of the above.

## 5. ACCEPTANCE CRITERIA
1. The Phase 0 evidence is quoted in the report: the chat-read error name/message/code, and the confirmed hypothesis.
2. `chat_list_summaries('user_priya_1')` executes, and `chat-list` and `chat-read` return 200 for `user_priya_1`.
3. `chat-open` never resets read cursors and never archives on a `MASTER_KEY` error.
4. No code or SQL (other than the superseded file) references `closed_at`, `public.profiles` or `full_name`.
5. The deployed functions match the local code. `deno check` passes (or say Deno isn't available), and `assembleDebug` passes.

## 6. OUTPUT FORMAT
One report at the end, 15 lines max:
- the Phase 0 findings (error + code + which hypothesis)
- which RCs you confirmed
- files changed
- whether the SQL was run and the functions deployed (or the exact commands for me, in order)
- the Phase 5 curl status codes
- anything I must do by hand
