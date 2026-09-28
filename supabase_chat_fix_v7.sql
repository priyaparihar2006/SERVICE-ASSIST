-- =========================================================================
-- SUPABASE CHAT FIX V7: Key Recovery, Clean Partial Unique Index & Resilience
-- =========================================================================

-- 1. Add archived_reason column for key loss tracking
ALTER TABLE public.chat_conversations ADD COLUMN IF NOT EXISTS archived_reason TEXT;

-- 2. Convert strict (customer_id, partner_id) unique constraint to partial unique index
-- This allows archiving unreadable (KEY_LOST) legacy conversations while keeping only 1 active thread per pair.
DO $do$
BEGIN
    IF EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'chat_conversations_customer_id_partner_id_key'
    ) THEN
        ALTER TABLE public.chat_conversations DROP CONSTRAINT chat_conversations_customer_id_partner_id_key;
    END IF;
END;
$do$;

DROP INDEX IF EXISTS idx_chat_conversations_active_pair;
CREATE UNIQUE INDEX IF NOT EXISTS idx_chat_conversations_active_pair 
ON public.chat_conversations (customer_id, partner_id) 
WHERE status <> 'CLOSED';

-- 3. Update chat_list_summaries to ignore CLOSED / KEY_LOST conversations
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
    booking_status TEXT,
    partner_name TEXT,
    my_last_read BIGINT,
    my_last_delivered BIGINT,
    peer_last_read BIGINT,
    peer_last_delivered BIGINT,
    unread_count BIGINT,
    last_msg_id UUID,
    last_msg_seq BIGINT,
    last_msg_sender TEXT,
    last_msg_kind TEXT,
    last_msg_ct BYTEA,
    last_msg_nonce BYTEA
)
LANGUAGE plpgsql
SECURITY DEFINER
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
            c.opened_at AS c_opened_at,
            c.last_message_at AS c_last_msg_at,
            COALESCE(c.last_message_seq, 0) AS c_latest_seq,
            c.wrapped_dek AS c_wrapped_dek,
            c.key_version AS c_key_version,
            CASE WHEN c.customer_id = p_profile_id THEN c.partner_id ELSE c.customer_id END AS peer_id,
            b.booking_code AS b_code,
            b.service_name AS b_service_name,
            b.customer_name AS b_customer_name,
            b.status AS b_status,
            p.name AS p_partner_name
        FROM public.chat_conversations c
        LEFT JOIN public.bookings b ON b.id = c.booking_id
        LEFT JOIN public.user_profiles p ON p.id = c.partner_id
        WHERE (c.customer_id = p_profile_id OR c.partner_id = p_profile_id)
          AND c.status <> 'CLOSED'
          AND NOT (COALESCE(c.last_message_seq, 0) = 0 AND c.opened_by <> p_profile_id)
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
            m.id,
            m.seq,
            m.sender_id,
            m.kind,
            m.ciphertext,
            m.nonce
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
        uc.b_status AS booking_status,
        uc.p_partner_name AS partner_name,
        COALESCE(mr.last_read_seq, 0) AS my_last_read,
        COALESCE(mr.last_delivered_seq, 0) AS my_last_delivered,
        COALESCE(pr.last_read_seq, 0) AS peer_last_read,
        COALESCE(pr.last_delivered_seq, 0) AS peer_last_delivered,
        COALESCE(u.count, 0) AS unread_count,
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

GRANT EXECUTE ON FUNCTION public.chat_list_summaries TO service_role;
