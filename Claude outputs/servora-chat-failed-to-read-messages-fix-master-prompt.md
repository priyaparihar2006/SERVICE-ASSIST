# MASTER PROMPT 7: Fix "Failed to read messages" (chat-read 500) and the Follow-on Send / Receipt Bugs (Servora / ServiceAssist)

Paste everything below the line into your coding agent. Work in the repo root `ServiceAssist/`. This follows Master Prompt 6 (`servora-chat-send-lists-sync-latency-fix-master-prompt.md`), which has been applied: `_shared/http.ts`, `_shared/chatStatus.ts`, `supabase_chat_fix_v6.sql` and the new RPC-based functions all exist. Master Prompt 6 introduced the regressions below. Fix them. Don't redesign anything.

---

## 0. ROLE AND WORKING RULES

You are a staff-level engineer with 10+ years in Android (Kotlin, Compose, Moshi, Retrofit) and Supabase (Deno Edge Functions, supabase-js v2, PostgREST, PL/pgSQL, Web Crypto AES-GCM). You debug from evidence.

1. Read every file named in section 2 first. If the code differs from what's described here, trust the code and say so.
2. Post a 5-line plan, then do all phases without stopping. Report once at the end.
3. Don't ask questions unless you are blocked. State assumptions in one line.
4. Keep changes small. No refactors. Never log message text, keys or tokens.
5. **No tests.** Don't write or run unit, Deno, SQL or UI tests. The only checks you run are `deno check` on each changed function (if Deno is installed) and `./gradlew assembleDebug` once at the end.

## 1. SYMPTOM AND EVIDENCE

The customer (`user_priya_1`) opens the "Support & Pro Chat" thread. A red banner says **"Failed to read messages"**, and the thread stays empty. It never recovers.

Logcat:
```
--> GET .../functions/v1/chat-list                       <-- 200 (627ms)
--> GET .../functions/v1/chat-read?conversation_id=4b3dc9ec-0895-4847-a120-484f0753ac27&after_seq=0
<-- 500 (2509ms)   sb-error-code: EDGE_FUNCTION_ERROR
--> GET .../functions/v1/chat-sync                       <-- 200 (569ms)   (repeats every ~15s)
```
"Failed to read messages" is the `fallbackMessage` in `chat-read/index.ts` line 196 (`return jsonError(err, "Failed to read messages")`). So the function **threw an exception** that `jsonError` couldn't classify, and it returned a generic 500. `chat-list` and `chat-sync` work, so auth, env vars and the conversation row are fine. The crash is inside `chat-read` itself.

## 2. ROOT CAUSES (verified in current code)

### RC1 (the crash): `supabaseClient.rpc(...).catch(...)` is not a function
`chat-read/index.ts` lines 148-175:
```ts
if (highestReturnedSeq > afterSeq && !identity.isAdmin) {
  await supabaseClient.rpc("chat_bump_read_state", {...}).catch(async () => { ...fallback upsert... });
}
```
- `supabase.rpc()` returns a PostgREST builder. In supabase-js 2.39 (postgrest-js 1.x, up to the latest 1.21.4) that builder only implements `then()`. It has **no `.catch()` method**, so `.catch(...)` throws `TypeError: ...catch is not a function` synchronously.
- The outer `catch` turns that into `jsonError(err, "Failed to read messages")` → 500.
- This runs whenever the request returns at least one message (`highestReturnedSeq > afterSeq`). With `after_seq=0`, that's **every thread that has any messages, on every open**. Empty threads open fine, which is why it looks random.
- Separately, `rpc()` never *rejects* on a database error. It resolves to `{ data, error }`. So even on a builder that had `.catch`, the "fallback if the RPC is missing" would never run.
- **The same bug is in `chat-ack/index.ts` lines 43-72**, so read receipts fail on every ack too.

### RC2 (the next crash, and it destroys data): missing imports and a key-regeneration path that wipes history
- `chat-read/index.ts` line 3 imports only `getMasterKey, unwrapDek, decryptMessage, hexToBytes`. Lines 96-106 then call `generateAndWrapDek(...)` and `bytesToHex(...)`, which aren't imported. That's a `ReferenceError` the moment a DEK fails to unwrap.
- `chat-send/index.ts` has the same problem: line 3 doesn't import `generateAndWrapDek`, but lines 91-100 call it.
- Worse, if that import *were* present, the code would **overwrite `chat_conversations.wrapped_dek` with a brand-new key and bump `key_version`**. `key_version` is part of every message's AES-GCM AAD, and the old DEK is thrown away. That permanently destroys every existing message in the thread. It turns one bad key into permanent data loss, and hides the real configuration problem.
- **This must be removed, not fixed by adding the import.**

### RC3: `getMasterKey()` silently accepts a wrong key
`_shared/crypto.ts` lines 33-49: if `CHAT_MASTER_KEY` doesn't base64-decode to exactly 32 bytes, it now falls back to `SHA-256(keyStr)` and carries on.
- A mistyped or rotated secret therefore never fails loudly. It produces a *different* master key, so every existing conversation fails to unwrap. That sends execution into RC2's destructive path.
- Master Prompts 5 and 6 required a strict 32-byte check. Restore it.

### RC4 (the next bug after reading works): the `chat-send` response doesn't match the Android DTO
- `chat-send` now returns `{ id, seq, created_at }` (lines ~149, 168, 176).
- `ChatDtos.kt` `ChatSendResponse` requires `success: Boolean` and `message_id: String`, both non-null.
- Moshi throws `JsonDataException` ("Required value 'messageId' missing") on a **200**. The message is stored on the server, but the app marks it FAILED. Retrying hits the idempotent 23505 path, gets the same shape back, and fails again.

### RC5: `chat-read` returns `latest_seq`, which the Android DTO ignores
That's harmless on its own. But `ChatViewModel.openThread` must ack the **highest seq received** (`messages.maxOf { it.seq }`), which equals `last_seq` in the new response. Confirm it does. Add an optional `latestSeq: Long = 0L` field to `ChatReadResponse` so the value isn't lost.

### RC6: a failed first read leaves the thread dead
In `ChatViewModel.openThread` (non-admin branch), `.onFailure` only sets `_errorMessage`. `startThreadPolling(conversationId)` only runs on success. So a single transient 500 leaves the thread empty until the user backs out and reopens it. That's exactly what the screenshot shows.

### RC7: `chat-list` hides missing-migration errors
`chat-list/index.ts` lines 20-27: if the `chat_list_summaries` RPC errors (for example, `supabase_chat_fix_v6.sql` wasn't run), it returns `{ conversations: [] }` with 200. The chat list looks empty instead of reporting an error. `chat-send`, `chat-open` (`chat_append_message`) and `chat-sync` have similar "silently fall back" branches.

## 3. THE FIX

### Phase 1: Fix the RPC call pattern everywhere
- In `chat-read` and `chat-ack`, replace every `await supabaseClient.rpc(...).catch(...)` with:
  ```ts
  const { error: bumpErr } = await supabaseClient.rpc("chat_bump_read_state", { p_conv, p_user, p_read, p_delivered });
  if (bumpErr) {
    console.error("[chat-read] chat_bump_read_state failed:", bumpErr.code, bumpErr.message);
    // non-fatal: reading must still succeed; do NOT throw
  }
  ```
  A failed receipt bump must **never** fail the read or ack response. Delete the manual read-then-upsert fallback. The RPC is the single source of truth, and the fallback is racy.
- Run `grep -rn "\.rpc(" supabase/functions` and `grep -rn "\.catch(" supabase/functions`. The only acceptable `.catch(` is on real Promises such as `req.json().catch(() => ({}))`. Fix any other builder-level `.catch` or `.finally`.

### Phase 2: Remove key regeneration and restore the strict master key
- In `chat-read/index.ts` (lines ~86-106) and `chat-send/index.ts` (lines ~80-100), delete the whole `if (!dek) { generateAndWrapDek ... update wrapped_dek/key_version }` block.
  - In `chat-read`: if `unwrapDek` throws, let the `ChatCryptoError("CONVERSATION_KEY")` propagate to `jsonError`, which returns a clear message and logs details on the server.
  - In `chat-send`: same.
  - Run `grep -rn "key_version: (" supabase/functions`. It must return nothing.
- Fix the imports so each file imports exactly what it uses. Run `deno check` on every changed function.
- `_shared/crypto.ts` `getMasterKey()`: remove the SHA-256 fallback. If `atob` fails or the result isn't 32 bytes, throw `ChatCryptoError("MASTER_KEY", "CHAT_MASTER_KEY must be 32 random bytes, base64-encoded")`. Keep the caching.
- `_shared/http.ts` `jsonError`: for `CONVERSATION_KEY`, return 409 with `{ error: "This conversation can't be decrypted. Please start a new chat.", code: "CONVERSATION_KEY" }`. For `MASTER_KEY`, return 500 with `code: "MASTER_KEY"`. In the generic branch, always `console.error` the error's `name`, `message` and first stack line, so the next 500 is diagnosable from Supabase logs.

### Phase 3: Make the `chat-send` response match the client
- Every success path in `chat-send` (the RPC path, the fallback insert, and the idempotent 23505 path) returns exactly:
  `{ success: true, message_id: <uuid>, seq: <number>, created_at: <ISO string> }`
- Check `chat-open`'s response against `ChatOpenResponse`, `chat-list`/`chat-sync` against `ConversationSummaryDto` / `ChatSyncResponse`, and `chat-ack` against `ChatAckResponse` (`success`, `last_read_seq`). Every non-nullable Kotlin field must always be present with the right type.
  - Watch for Postgres `BIGINT` coming back as a string from the RPCs. Wrap with `Number(...)`.
  - Watch for `null` where Kotlin expects a non-null `String`, such as `booking_code` or `service_name`. Default to `""`.
  - List every mismatch you fix.

### Phase 4: Stop hiding missing-migration errors
- `chat-list`, `chat-sync`, `chat-send` and `chat-open`: if the v6 RPC fails with Postgres code `42883` or PostgREST `PGRST202` (function not found), return 500 with `{ error: "Chat server is being updated. Please try again shortly.", code: "MIGRATION_MISSING" }` and `console.error("[<fn>] RPC <name> missing — run supabase_chat_fix_v6.sql")`.
  - Remove the "return empty list" branch in `chat-list`.
  - Keep direct-insert fallbacks only if they are fully correct: monotonic `last_message_seq` and the right response shape. Otherwise delete them.

### Phase 5: Android resilience (small)
- `ChatViewModel.openThread` (non-admin):
  - On failure, still call `startThreadPolling(conversationId)`, using `afterSeq = 0` until the first success, so the thread recovers by itself.
  - In the polling loop, if no messages are loaded yet, the first successful poll must replace the list, not merge into it.
  - Clear the banner on the first successful read.
- In the error banner in `ChatThreadScreen.kt`, add a small "Retry" action that calls `chatViewModel.openThread(conversationId)`.
- Add `@Json(name = "latest_seq") val latestSeq: Long = 0L` to `ChatReadResponse`.
- `ackRead` uses the max seq actually received, never `latestSeq`.
- Make sure a Moshi parse failure on a 2xx response surfaces as "Unexpected server response", not a raw exception string. Handle this in `RemoteChatRepository`, around `response.body()`.

### Phase 6: Deploy and verify (give me the commands; run them if the CLI is logged in)
```
# 1. Make sure the v6 SQL is applied (run supabase_chat_fix_v6.sql in the SQL editor if unsure), then:
select proname from pg_proc where proname in ('chat_append_message','chat_bump_read_state','chat_list_summaries','chat_recompute_pair_status');
#    -> must return all 4 rows

# 2. Confirm the secret is a real 32-byte key (do NOT print it). If unsure, keep the existing value
#    (changing it orphans all existing conversations).
supabase secrets list | grep CHAT_MASTER_KEY

# 3. Redeploy everything that imports _shared/*
supabase functions deploy chat-read chat-ack chat-send chat-open chat-list chat-sync chat-lifecycle chat-admin-read chat-admin-list booking-create booking-update-status

# 4. Smoke test the exact failing call (debug headers only work when ENVIRONMENT != production):
curl -s "$SUPABASE_URL/functions/v1/chat-read?conversation_id=4b3dc9ec-0895-4847-a120-484f0753ac27&after_seq=0" \
  -H "apikey: $SUPABASE_ANON_KEY" -H "Authorization: Bearer $SUPABASE_ANON_KEY" \
  -H "X-Dev-Profile-Id: user_priya_1" -H "X-Dev-User-Role: CUSTOMER"
#    -> 200 with messages[], or 409 CONVERSATION_KEY (see note below), never 500
```
**If the smoke test returns 409 `CONVERSATION_KEY`** for conversation `4b3dc9ec-…`, its DEK was wrapped under a different master key (or was already overwritten by the old regeneration code). Those messages can't be recovered. Tell me, and give me this SQL to see how many conversations are affected (don't delete anything):
```sql
select id, key_version, created_at from chat_conversations where key_version > 1 order by created_at desc;
```
`key_version > 1` means the regeneration code already ran on that row. Its older messages are lost. The fix for affected threads is to close them and let `chat-open` create a new conversation. Describe how, but don't run it.

## 4. MANUAL TEST CHECKLIST (I run this myself)
1. Open a thread that already has messages. It loads with no banner, and the newest messages are at the bottom.
2. Send a message. The ticks go to SENT, the message doesn't flip to FAILED, and the other device receives it.
3. The other device opens the thread. Read receipts (blue ticks) appear on the sender's side within one poll.
4. With airplane mode on, open a thread. You see the banner with Retry. Turn airplane mode off. The thread fills in by itself within a few seconds, without reopening.
5. Supabase → Edge Functions → Logs for `chat-read` and `chat-ack` show no `TypeError` and no `ReferenceError`.

## 5. ACCEPTANCE CRITERIA
1. `grep -rn "rpc(.*)\.catch\|\.rpc([^)]*)[^;]*\.catch" supabase/functions` returns nothing, and no function calls `.catch` or `.finally` on a PostgREST builder.
2. No function regenerates or overwrites `wrapped_dek` for an existing conversation.
3. `getMasterKey` rejects any key that isn't exactly 32 base64-decoded bytes.
4. Every Edge Function response matches its Kotlin DTO (list any fixes).
5. `deno check` passes on all changed functions (or say Deno isn't available), and `assembleDebug` passes.

## 6. OUTPUT FORMAT
One report at the end, 15 lines max:
- which RCs you confirmed,
- files changed,
- DTO mismatches fixed,
- whether the v6 RPCs exist and whether you deployed,
- the smoke-test result for `4b3dc9ec-…`,
- the `key_version > 1` count, if you could run it.
