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

-- B. chat_list_summaries
DROP FUNCTION IF EXISTS public.chat_list_summaries(text);

CREATE OR REPLACE FUNCTION public.chat_list_summaries(p_profile_id TEXT)
RETURNS TABLE (
    conversation_id UUID,
    booking_id BIGINT,
    customer_id TEXT,
    partner_id TEXT,
    status TEXT,
    opened_by TEXT,
    opened_at TIMESTAMPTZ,
    last_message_at TIMESTAMPTZ,
    latest_seq BIGINT,
    wrapped_dek TEXT,
    key_version INT,
    booking_code TEXT,
    service_name TEXT,
    customer_name TEXT,
    partner_name TEXT,
    booking_status TEXT,
    my_last_read BIGINT,
    my_last_delivered BIGINT,
    peer_last_read BIGINT,
    peer_last_delivered BIGINT,
    unread_count BIGINT,
    last_msg_id TEXT,
    last_msg_seq BIGINT,
    last_msg_sender TEXT,
    last_msg_kind TEXT,
    last_msg_ct TEXT,
    last_msg_nonce TEXT
)
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $func$
BEGIN
    RETURN QUERY
    WITH user_convs AS (
        SELECT
            c.id AS conv_id,
            c.booking_id AS b_id,
            c.customer_id AS cust_id,
            c.partner_id AS part_id,
            c.status AS c_status,
            c.opened_by AS c_opened_by,
            COALESCE(c.opened_at, c.created_at) AS c_opened_at,
            COALESCE(c.last_message_at, c.opened_at, c.created_at) AS c_last_msg_at,
            COALESCE(c.last_message_seq, 0)::BIGINT AS c_latest_seq,
            c.wrapped_dek AS c_wrapped_dek,
            COALESCE(c.key_version, 1)::INT AS c_key_version,
            CASE WHEN c.customer_id = p_profile_id THEN c.partner_id ELSE c.customer_id END AS peer_id,
            COALESCE(b.booking_code, c.booking_code, '') AS b_code,
            COALESCE(b.service_name, c.service_name, 'Doorstep Service') AS b_service_name,
            COALESCE(cust.name, c.customer_name, 'Client') AS b_customer_name,
            COALESCE(b.status, 'CONFIRMED') AS b_status,
            COALESCE(part.name, c.professional_name, 'Professional') AS p_partner_name
        FROM public.chat_conversations c
        LEFT JOIN public.bookings b ON b.id = c.booking_id
        LEFT JOIN public.user_profiles cust ON cust.id = c.customer_id
        LEFT JOIN public.user_profiles part ON part.id = c.partner_id
        WHERE (c.customer_id = p_profile_id OR c.partner_id = p_profile_id)
          AND c.status <> 'CLOSED'
    ),
    my_reads AS (
        SELECT r.conversation_id, r.last_read_seq, r.last_delivered_seq
        FROM public.chat_read_state r
        WHERE r.user_id = p_profile_id
    ),
    peer_reads AS (
        SELECT r.conversation_id, r.last_read_seq, r.last_delivered_seq
        FROM public.chat_read_state r
        JOIN user_convs uc ON uc.conv_id = r.conversation_id AND uc.peer_id = r.user_id
    ),
    last_msgs AS (
        SELECT DISTINCT ON (m.conversation_id)
            m.conversation_id,
            m.id::TEXT AS id,
            m.seq::BIGINT AS seq,
            m.sender_id,
            COALESCE(m.kind, 'TEXT') AS kind,
            ('\x' || encode(m.ciphertext, 'hex')) AS ciphertext,
            ('\x' || encode(m.nonce, 'hex')) AS nonce
        FROM public.chat_messages m
        JOIN user_convs uc ON uc.conv_id = m.conversation_id
        ORDER BY m.conversation_id, m.seq DESC
    ),
    unreads AS (
        SELECT
            m.conversation_id,
            COUNT(*)::BIGINT AS count
        FROM public.chat_messages m
        JOIN user_convs uc ON uc.conv_id = m.conversation_id
        LEFT JOIN my_reads mr ON mr.conversation_id = m.conversation_id
        WHERE m.seq > COALESCE(mr.last_read_seq, 0)
          AND m.sender_id <> p_profile_id
        GROUP BY m.conversation_id
    )
    SELECT
        uc.conv_id AS conversation_id,
        uc.b_id AS booking_id,
        uc.cust_id AS customer_id,
        uc.part_id AS partner_id,
        uc.c_status AS status,
        uc.c_opened_by AS opened_by,
        uc.c_opened_at AS opened_at,
        uc.c_last_msg_at AS last_message_at,
        uc.c_latest_seq AS latest_seq,
        uc.c_wrapped_dek AS wrapped_dek,
        uc.c_key_version AS key_version,
        uc.b_code AS booking_code,
        uc.b_service_name AS service_name,
        uc.b_customer_name AS customer_name,
        uc.p_partner_name AS partner_name,
        uc.b_status AS booking_status,
        COALESCE(mr.last_read_seq, 0)::BIGINT AS my_last_read,
        COALESCE(mr.last_delivered_seq, 0)::BIGINT AS my_last_delivered,
        COALESCE(pr.last_read_seq, 0)::BIGINT AS peer_last_read,
        COALESCE(pr.last_delivered_seq, 0)::BIGINT AS peer_last_delivered,
        COALESCE(u.count, 0)::BIGINT AS unread_count,
        lm.id AS last_msg_id,
        lm.seq AS last_msg_seq,
        lm.sender_id AS last_msg_sender,
        lm.kind AS last_msg_kind,
        lm.ciphertext AS last_msg_ct,
        lm.nonce AS last_msg_nonce
    FROM user_convs uc
    LEFT JOIN my_reads mr ON mr.conversation_id = uc.conv_id
    LEFT JOIN peer_reads pr ON pr.conversation_id = uc.conv_id
    LEFT JOIN last_msgs lm ON lm.conversation_id = uc.conv_id
    LEFT JOIN unreads u ON u.conversation_id = uc.conv_id
    ORDER BY uc.c_last_msg_at DESC NULLS LAST, uc.c_opened_at DESC NULLS LAST;
END;
$func$;

REVOKE ALL ON FUNCTION public.chat_list_summaries(text) FROM PUBLIC, anon, authenticated;
GRANT EXECUTE ON FUNCTION public.chat_list_summaries(text) TO service_role;

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
