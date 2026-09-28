# MASTER PROMPT 10: Fix "Failed to send message" (chat-send 500) for Servora / ServiceAssist

Paste everything below the line into your coding agent. Work in the repo root `ServiceAssist/`. This prompt follows Master Prompts 7, 9 and the per-booking prompt, which are already applied.

---

## 0. ROLE AND WORKING RULES

You are a staff-level engineer with 10+ years of experience in Supabase (Postgres/PL/pgSQL, PostgREST, Deno Edge Functions, supabase-js v2) and Android (Kotlin, Retrofit).

1. The root cause below was **reproduced** on a local Postgres 16 replay of every `supabase_*.sql` file in this repo. It is not a guess. Still, run the Phase 0 check against the live DB first. If live evidence contradicts this prompt, trust the evidence and say so in one line.
2. Post a 5-line plan, then work through every phase without stopping. Report once at the end.
3. Don't ask questions unless you are blocked. State any assumption in one line.
4. Keep changes small and don't refactor. Never log or print message text, `CHAT_MASTER_KEY`, the service-role key, OTPs or phone numbers.
5. **No tests.** Don't write unit, Deno, SQL or UI tests. The only checks allowed are:
   - `deno check` on each changed function (if Deno is installed)
   - `./gradlew assembleDebug` once at the end
   - the read-only SQL in Phase 0 and Phase 5
6. **Never** regenerate or overwrite `wrapped_dek`. Never change `CHAT_MASTER_KEY`. Never `DROP` or `DELETE` chat data. Legacy data is renamed, not removed.

## 1. SYMPTOM (logcat, partner `pro_rajesh_1`, conversation `4b3dc9ec-0895-4847-a120-484f0753ac27`)

```
GET  functions/v1/chat-read?...&after_seq=0   <-- 200   (every ~4 s)
GET  functions/v1/chat-sync                   <-- 200
POST functions/v1/chat-send                   <-- 500 (767ms)  sb-error-code: EDGE_FUNCTION_ERROR
W ChatRepo: chat-send 500 code= stage=
POST functions/v1/chat-send                   <-- 500 (792ms)  (every retry)
```

Every send fails, and the UI shows **"Failed to send message"**. `code=` is empty, so the body is `jsonError`'s generic fallback `{ error: "Failed to send message" }`. That means an exception that none of `jsonError`'s rules recognize. Reads, sync, auth and the master key all work.

## 2. ROOT CAUSE (verified)

### RC1 (primary): the live `chat_messages` table is the old legacy table, not the encrypted-chat table
- `supabase_schema.sql` (the "complete schema" file) starts with `DROP TABLE … chat_messages CASCADE` and recreates it in the **legacy** shape: `id BIGSERIAL`, `message_text TEXT NOT NULL`, `ciphertext TEXT`, `iv`, `tag`, `seq INTEGER DEFAULT 1`, with no `kind` and no `nonce`.
- `supabase_chat_schema.sql` uses `CREATE TABLE IF NOT EXISTS`, so it **silently skipped** creating the real table (`id UUID`, `seq BIGINT GENERATED ALWAYS AS IDENTITY`, `ciphertext BYTEA`, `nonce BYTEA`).
- The v8 migration proves the legacy table is live. It had to `ADD COLUMN kind` / `ADD COLUMN nonce TEXT` and cast `m.id::TEXT`. It also reads legacy-only `chat_conversations` columns (`booking_code`, `service_name`, `customer_name`, `professional_name`).
- What `chat-send` does against that table (reproduced exactly):
  1. `rpc("chat_append_message")` fails with `42804: column "id" is of type bigint but expression is of type uuid`.
  2. `42804` isn't `42883`/`PGRST202`, so the code falls through to the **fallback direct insert**. That fails with `22P02: invalid input syntax for type bigint: "<client uuid>"`. Even without `id`, it would still fail with `23502: null value in column "message_text"`.
  3. `throw insertError`. `jsonError` doesn't map `22P02`, so it returns a generic 500: "Failed to send message".
- Side effects of the same cause:
  - `chat-open`'s `injectEncryptedSystemMessage` has the same RPC-then-fallback pattern and swallows the error. So the "New booking accepted…" system messages have **never** been written.
  - `seq INTEGER DEFAULT 1` would give every message `seq = 1`, which breaks ordering and receipts.

### RC2 (hidden by RC1, would break the next step): `chat_list_summaries` assumes text columns
v8's `last_msgs` CTE does `COALESCE(m.ciphertext, '')` / `COALESCE(m.nonce, '')` and declares `last_msg_ct TEXT`, `last_msg_nonce TEXT`. Once `chat_messages` is fixed to `BYTEA`, `chat-list` fails with `42804 structure of query does not match function result type` (reproduced). It must return hex strings: `'\x' || encode(col,'hex')`. `chat-list` already passes these through `hexToBytes()`, which accepts that format.

### RC3 (separate bug, same area): the v6 pair-level booking trigger breaks booking updates for repeat pairs
`trg_bookings_chat_status_sync` → `chat_recompute_pair_status()` (from `supabase_chat_fix_v6.sql`) runs `UPDATE chat_conversations SET booking_id = <latest booking>, status = … WHERE customer_id = … AND partner_id = …`. It has **no booking filter and no `status <> 'CLOSED'` filter**. With per-booking threads, any customer–partner pair with 2+ conversations hits `23505 duplicate key … idx_chat_conversations_active_booking` (reproduced). The **whole booking status UPDATE rolls back**: accept, on-the-way, cancel and so on. The trigger also reopens CLOSED conversations and moves them onto the wrong booking. Nothing else calls `chat_recompute_pair_status` (grep checked), and the edge functions already call the per-booking `recomputeBookingChatStatus`.

### RC4 (why this took so long to find): errors are hidden
- `chat-send` falls back to a direct insert on **any** RPC error, which hides the real error.
- `jsonError` returns a code-less 500 for unmapped Postgres errors, even though the client already logs `code` and `stage`.

## 3. THE FIX

### Phase 0: Confirm against live (read-only)
Run this in the Supabase SQL editor and quote the results:
```sql
select column_name, data_type, is_nullable from information_schema.columns
 where table_schema='public' and table_name='chat_messages' order by ordinal_position;
select count(*) total, count(*) filter (where coalesce(nonce,'') <> '') encrypted_rows from public.chat_messages;
select tgname from pg_trigger where tgrelid='public.bookings'::regclass and not tgisinternal;
```
Expected results:
- `id bigint`, `message_text text NOT NULL`, `nonce text`
- `encrypted_rows = 0`, because no encrypted row could ever have been inserted
- `trg_bookings_chat_status_sync` is present

If `id` is already `uuid`, stop at Phase 1. Skip steps 1-3 and report instead.

### Phase 1: New migration `supabase_chat_messages_fix_v9.sql` (idempotent, one transaction)
Create it with exactly these parts. Parts A, C and D were run and verified on the local replay. Build part B as described.

```sql
-- =========================================================================
-- SERVICE ASSIST — CHAT MESSAGES TABLE FIX V9 (Idempotent)
-- Replaces the legacy chat_messages (BIGSERIAL/plaintext) with the encrypted schema.
-- =========================================================================
BEGIN;

-- A1. Move the legacy table aside (data kept, never deleted)
DO $do$
BEGIN
  IF EXISTS (SELECT 1 FROM information_schema.columns
             WHERE table_schema='public' AND table_name='chat_messages'
               AND column_name='id' AND data_type='bigint') THEN
    ALTER TABLE public.chat_messages RENAME TO chat_messages_legacy_v1;
    ALTER INDEX IF EXISTS public.idx_chat_messages_conv_id        RENAME TO idx_chat_messages_legacy_v1_conv_id;
    ALTER INDEX IF EXISTS public.idx_chat_messages_conv_seq_desc  RENAME TO idx_chat_messages_legacy_v1_conv_seq_desc;
    ALTER INDEX IF EXISTS public.idx_chat_messages_conv_seq       RENAME TO idx_chat_messages_legacy_v1_conv_seq;
    REVOKE ALL ON public.chat_messages_legacy_v1 FROM anon, authenticated;
  END IF;
END $do$;

-- A2. Canonical encrypted table
CREATE TABLE IF NOT EXISTS public.chat_messages (
  id              UUID PRIMARY KEY,
  conversation_id UUID NOT NULL REFERENCES public.chat_conversations(id) ON DELETE CASCADE,
  seq             BIGINT GENERATED ALWAYS AS IDENTITY,
  sender_id       TEXT NOT NULL,
  sender_role     TEXT NOT NULL CHECK (sender_role IN ('CUSTOMER','PARTNER','SYSTEM')),
  kind            TEXT NOT NULL DEFAULT 'TEXT' CHECK (kind IN ('TEXT','QUICK_REPLY','ETA','SYSTEM','BOOKING_UPDATE')),
  ciphertext      BYTEA NOT NULL,
  nonce           BYTEA NOT NULL,
  created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_chat_messages_conv_seq      ON public.chat_messages (conversation_id, seq);
CREATE INDEX IF NOT EXISTS idx_chat_messages_conv_seq_desc ON public.chat_messages (conversation_id, seq DESC);
ALTER TABLE public.chat_messages ENABLE ROW LEVEL SECURITY;
REVOKE ALL ON public.chat_messages FROM anon, authenticated;
GRANT ALL ON public.chat_messages TO service_role;

-- A3. seq must start above every cursor already stored (so old read cursors can't mark new messages read)
ALTER TABLE public.chat_conversations ALTER COLUMN last_message_seq TYPE BIGINT;
DO $do$
DECLARE v_start BIGINT;
BEGIN
  SELECT GREATEST(
    COALESCE((SELECT MAX(last_message_seq) FROM public.chat_conversations), 0),
    COALESCE((SELECT MAX(GREATEST(last_read_seq, last_delivered_seq)) FROM public.chat_read_state), 0),
    COALESCE((SELECT MAX(seq) FROM public.chat_messages), 0)) + 1 INTO v_start;
  EXECUTE format('ALTER TABLE public.chat_messages ALTER COLUMN seq RESTART WITH %s', v_start);
END $do$;

-- A4. Realtime publication parity with the old table (harmless if absent)
DO $do$
BEGIN
  IF EXISTS (SELECT 1 FROM pg_publication WHERE pubname='supabase_realtime') THEN
    ALTER PUBLICATION supabase_realtime ADD TABLE public.chat_messages;
  END IF;
EXCEPTION WHEN duplicate_object THEN NULL;
END $do$;

-- B. chat_list_summaries: see instructions below

-- C. Per-booking chat-status trigger (replaces the pair-level v6 version)
CREATE OR REPLACE FUNCTION public.trg_bookings_sync_chat_status()
RETURNS TRIGGER LANGUAGE plpgsql SECURITY DEFINER SET search_path = public AS $func$
BEGIN
  IF TG_OP = 'UPDATE' AND OLD.status IS NOT DISTINCT FROM NEW.status THEN
    RETURN NEW;
  END IF;
  UPDATE public.chat_conversations
     SET status    = CASE WHEN NEW.status IN ('COMPLETED','CANCELLED') THEN 'READ_ONLY' ELSE 'ACTIVE' END,
         closes_at = CASE WHEN NEW.status IN ('COMPLETED','CANCELLED') THEN NOW() + INTERVAL '24 hours' ELSE NULL END
   WHERE booking_id = NEW.id
     AND status <> 'CLOSED';
  RETURN NEW;
END;
$func$;
DROP FUNCTION IF EXISTS public.chat_recompute_pair_status(text, text);
-- (trigger trg_bookings_chat_status_sync already points at this function; keep it)

-- D. chat_append_message: unchanged body, but pin search_path
ALTER FUNCTION public.chat_append_message(uuid, uuid, text, text, text, bytea, bytea) SET search_path = public;

COMMIT;
```

**Part B, instructions:** copy section 4 of `supabase_chat_per_booking_fix_v8.sql` **verbatim**. That runs from `DROP FUNCTION IF EXISTS public.chat_list_summaries(text);` through its `GRANT EXECUTE … TO service_role;`. Change only these two lines in the `last_msgs` CTE:
```sql
-- before
COALESCE(m.ciphertext, '') AS ciphertext,
COALESCE(m.nonce, '') AS nonce
-- after
('\x' || encode(m.ciphertext, 'hex')) AS ciphertext,
('\x' || encode(m.nonce, 'hex')) AS nonce
```
Keep the return columns as `TEXT`. `chat-sync` (v8 section 5) doesn't read ciphertext and needs no change.

Also:
- Add a header to `supabase_schema.sql`: `-- ⚠ DO NOT RUN ON AN EXISTING PROJECT. Drops every table (bookings, payments, chat…) and recreates the LEGACY chat tables. Chat tables are owned by supabase_chat_schema.sql + v9.` Don't change the rest of the file.
- Add `-- SUPERSEDED for chat_messages by v9` at the top of `supabase_chat_schema.sql`.

### Phase 2: `chat-send/index.ts`: remove the error-hiding fallback and add stage tracking
1. Add `let stage = "init";` at the top of the handler. Set `stage` before each step: `"auth"`, `"load_conv"`, `"scrub"`, `"crypto"`, `"append"`, `"respond"`.
2. Replace the whole `if (rpcError) { … fallback direct insert … }` block with:
   ```ts
   if (rpcError) {
     if (rpcError.code === "23505") {
       return jsonResponse({ error: "Message ID conflict", code: "MESSAGE_ID_CONFLICT" }, 409);
     }
     throw rpcError;
   }
   ```
   The RPC already handles idempotent retries by returning the existing row. A direct insert bypasses the atomic seq/read-state update, so don't keep it.
3. Guard the result: `const row = Array.isArray(appended) ? appended[0] : appended; if (!row?.id) throw new Error("append returned no row");`
4. Catch block: `return jsonError(err, "Failed to send message", defaultCorsHeaders, stage);`

### Phase 3: `_shared/http.ts`: make unmapped errors diagnosable
- Add an optional `stage?: string` param to `jsonError`. Include `stage` in **every** JSON body it returns.
- Add a schema-mismatch branch before the generic fallback: pg codes `22P02`, `23502`, `42804`, `42P13`, `PGRST203` → 500 `{ error: "Chat server is being updated. Please try again shortly.", code: "SCHEMA_MISMATCH", stage }`.
- Generic fallback: `{ error: fallbackMessage, code: errCode || "INTERNAL", stage }`.
- Keep the existing `console.error("[ServerError]"…)` line and add `stage` to it. Never log `text`.
- The Android `extractErrorMessage` already shows `json.error` and logs `code`/`stage`. **No Android change is needed.**

### Phase 4: `chat-open/index.ts` `injectEncryptedSystemMessage`
Delete the fallback direct-insert block. On `rpcError`, only `console.error("[chat-open] system message append failed", rpcError.code, rpcError.message)`. Don't throw, because opening the chat must still succeed.

### Phase 5: Deploy and verify
1. Run `supabase_chat_messages_fix_v9.sql` in the SQL editor. It must finish with `COMMIT` and no error.
2. Deploy: `supabase functions deploy chat-send chat-open chat-list`
3. Read-only checks:
   ```sql
   select data_type from information_schema.columns where table_name='chat_messages' and column_name in ('id','ciphertext','nonce'); -- uuid, bytea, bytea
   select pg_get_function_result('public.chat_list_summaries(text)'::regprocedure);
   select * from public.chat_list_summaries('pro_rajesh_1') limit 1;   -- no error
   select to_regprocedure('public.chat_recompute_pair_status(text,text)'); -- null
   ```
4. On the device, send a message as `pro_rajesh_1` in conversation `4b3dc9ec-…`. Expect `chat-send <-- 200`, the tick turns to SENT, and the customer (`user_priya_1`) sees it on the next poll with the unread badge. Send one reply back.
5. Move a booking for a pair that already had an earlier booking to the next status (e.g. ON_THE_WAY). It must succeed. Only that booking's chat changes status.
6. `./gradlew assembleDebug` once.

## 4. ACCEPTANCE CRITERIA
- `chat-send` returns 200 with `{ success, message_id, seq, created_at }`. The message shows SENT, then DELIVERED/READ.
- Both participants see messages in order with increasing `seq`. The chat list preview decrypts the last message.
- A retry with the same `client_message_id` returns the same `seq` and creates no duplicate.
- Opening a brand-new booking chat shows the "New booking accepted…" system message.
- Booking status changes for repeat customer–partner pairs no longer fail. Other bookings' chats are untouched.
- Any future chat-send 500 carries `code` and `stage` in its body and in logcat (`chat-send 500 code=… stage=…`).
- No `wrapped_dek` changed, no chat data deleted (`chat_messages_legacy_v1` still exists).

## 5. FINAL REPORT FORMAT
Phase 0 results (3 lines), files changed (one line each), migration output, deploy output, the Phase 5 check results, and anything that didn't match this prompt.
