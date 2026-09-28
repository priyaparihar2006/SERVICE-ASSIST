-- =========================================================================
-- SERVICE ASSIST — CHAT PER-BOOKING THREADS & RPC FIX V8 (Idempotent)
-- =========================================================================

-- 1. Ensure required columns exist on chat tables
ALTER TABLE public.chat_conversations ADD COLUMN IF NOT EXISTS archived_reason TEXT;
ALTER TABLE public.chat_conversations ADD COLUMN IF NOT EXISTS last_message_at TIMESTAMPTZ;
ALTER TABLE public.chat_conversations ADD COLUMN IF NOT EXISTS opened_at TIMESTAMPTZ DEFAULT NOW();

ALTER TABLE public.chat_messages ADD COLUMN IF NOT EXISTS kind TEXT DEFAULT 'TEXT';
ALTER TABLE public.chat_messages ADD COLUMN IF NOT EXISTS nonce TEXT DEFAULT '';

-- 2. Dedupe non-CLOSED conversations per booking_id
-- Keep the newest conversation (by last_message_at/opened_at/created_at) and close duplicates
WITH ranked_convs AS (
    SELECT 
        id,
        booking_id,
        ROW_NUMBER() OVER (
            PARTITION BY booking_id 
            ORDER BY COALESCE(last_message_at, opened_at, created_at) DESC, id DESC
        ) as rn
    FROM public.chat_conversations
    WHERE status <> 'CLOSED' AND booking_id IS NOT NULL
)
UPDATE public.chat_conversations
SET status = 'CLOSED',
    archived_reason = 'DEDUPE_PER_BOOKING'
WHERE id IN (
    SELECT id FROM ranked_convs WHERE rn > 1
);

-- 3. Drop obsolete pair indexes and enforce exactly 1 active conversation per booking
DROP INDEX IF EXISTS idx_chat_conversations_active_pair;
DROP INDEX IF EXISTS idx_chat_conv_cust_part;

CREATE UNIQUE INDEX IF NOT EXISTS idx_chat_conversations_active_booking
ON public.chat_conversations (booking_id)
WHERE status <> 'CLOSED' AND booking_id IS NOT NULL;

-- 4. Recreate chat_list_summaries RPC with correct tables (user_profiles), columns & types
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
            COALESCE(m.ciphertext, '') AS ciphertext,
            COALESCE(m.nonce, '') AS nonce
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

-- 5. Recreate chat_sync RPC
DROP FUNCTION IF EXISTS public.chat_sync(text);

CREATE OR REPLACE FUNCTION public.chat_sync(p_profile_id TEXT)
RETURNS TABLE (
    conversation_id UUID,
    booking_id BIGINT,
    status TEXT,
    last_message_at TIMESTAMPTZ,
    latest_seq BIGINT,
    unread_count BIGINT,
    peer_read_seq BIGINT,
    peer_delivered_seq BIGINT
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
            c.status AS c_status,
            COALESCE(c.last_message_at, c.opened_at, c.created_at) AS c_last_msg_at,
            COALESCE(c.last_message_seq, 0)::BIGINT AS c_latest_seq,
            CASE WHEN c.customer_id = p_profile_id THEN c.partner_id ELSE c.customer_id END AS peer_id
        FROM public.chat_conversations c
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
        uc.c_status AS status,
        uc.c_last_msg_at AS last_message_at,
        uc.c_latest_seq AS latest_seq,
        COALESCE(u.count, 0)::BIGINT AS unread_count,
        COALESCE(pr.last_read_seq, 0)::BIGINT AS peer_read_seq,
        COALESCE(pr.last_delivered_seq, 0)::BIGINT AS peer_last_delivered
    FROM user_convs uc
    LEFT JOIN peer_reads pr ON pr.conversation_id = uc.conv_id
    LEFT JOIN unreads u ON u.conversation_id = uc.conv_id
    ORDER BY uc.c_last_msg_at DESC NULLS LAST;
END;
$func$;

REVOKE ALL ON FUNCTION public.chat_sync(text) FROM PUBLIC, anon, authenticated;
GRANT EXECUTE ON FUNCTION public.chat_sync(text) TO service_role;

-- 6. Verification
-- SELECT * FROM public.chat_list_summaries('user_priya_1');
