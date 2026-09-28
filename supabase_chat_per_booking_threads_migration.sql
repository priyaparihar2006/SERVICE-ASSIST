-- SUPERSEDED by v8 — do not run
-- Migration: Refactor Chat Conversations to One Thread Per Booking
-- Run this in Supabase SQL editor

-- 1. Drop the old pair-level active index if present
DROP INDEX IF EXISTS idx_chat_conversations_active_pair;

-- 2. Dedupe existing active conversations per booking_id
-- If multiple active conversations exist for the same booking_id, keep the one with latest last_message_at (or newest created_at)
-- and close the older duplicates with archived_reason = 'DEDUPE_PER_BOOKING'.
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
    archived_reason = 'DEDUPE_PER_BOOKING',
    closed_at = NOW()
WHERE id IN (
    SELECT id FROM ranked_convs WHERE rn > 1
);

-- 3. Create partial unique index: exactly one active/read-only conversation per booking
CREATE UNIQUE INDEX IF NOT EXISTS idx_chat_conversations_active_booking
ON public.chat_conversations (booking_id)
WHERE status <> 'CLOSED' AND booking_id IS NOT NULL;

-- 4. Update or recreate chat_list_summaries RPC to work seamlessly per booking
CREATE OR REPLACE FUNCTION public.chat_list_summaries(p_profile_id text)
RETURNS TABLE(
    conversation_id uuid,
    booking_id bigint,
    booking_code text,
    service_name text,
    customer_id text,
    partner_id text,
    customer_name text,
    partner_name text,
    wrapped_dek text,
    key_version integer,
    status text,
    opened_at timestamptz,
    last_message_at timestamptz,
    latest_seq bigint,
    last_msg_id uuid,
    last_msg_sender text,
    last_msg_ct text,
    last_msg_nonce text,
    unread_count bigint,
    peer_last_read bigint,
    peer_last_delivered bigint
) LANGUAGE plpgsql SECURITY DEFINER AS $$
BEGIN
    RETURN QUERY
    SELECT 
        c.id AS conversation_id,
        c.booking_id,
        b.booking_code,
        b.service_name,
        c.customer_id,
        c.partner_id,
        cust.full_name AS customer_name,
        part.full_name AS partner_name,
        c.wrapped_dek,
        c.key_version,
        c.status,
        c.opened_at,
        c.last_message_at,
        c.last_message_seq AS latest_seq,
        lm.id AS last_msg_id,
        lm.sender_id AS last_msg_sender,
        lm.ciphertext AS last_msg_ct,
        lm.nonce AS last_msg_nonce,
        GREATEST(0::bigint, COALESCE(c.last_message_seq, 0) - COALESCE(my_rs.last_read_seq, 0)) AS unread_count,
        COALESCE(peer_rs.last_read_seq, 0::bigint) AS peer_last_read,
        COALESCE(peer_rs.last_delivered_seq, 0::bigint) AS peer_last_delivered
    FROM public.chat_conversations c
    LEFT JOIN public.bookings b ON b.id = c.booking_id
    LEFT JOIN public.profiles cust ON cust.id = c.customer_id
    LEFT JOIN public.profiles part ON part.id = c.partner_id
    LEFT JOIN LATERAL (
        SELECT m.id, m.sender_id, m.ciphertext, m.nonce
        FROM public.chat_messages m
        WHERE m.conversation_id = c.id
        ORDER BY m.seq DESC
        LIMIT 1
    ) lm ON true
    LEFT JOIN public.chat_read_state my_rs 
        ON my_rs.conversation_id = c.id AND my_rs.user_id = p_profile_id
    LEFT JOIN public.chat_read_state peer_rs 
        ON peer_rs.conversation_id = c.id AND peer_rs.user_id = (CASE WHEN c.customer_id = p_profile_id THEN c.partner_id ELSE c.customer_id END)
    WHERE (c.customer_id = p_profile_id OR c.partner_id = p_profile_id)
      AND c.status <> 'CLOSED'
    ORDER BY COALESCE(c.last_message_at, c.opened_at, c.created_at) DESC;
END;
$$;
