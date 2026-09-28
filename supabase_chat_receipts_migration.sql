-- =========================================================================
-- SERVICE ASSIST — CHAT SYSTEM RECEIPTS, ORDERING & SYNC MIGRATION
-- =========================================================================

-- 1. Read and delivery cursors
ALTER TABLE public.chat_read_state
  ADD COLUMN IF NOT EXISTS last_delivered_seq BIGINT NOT NULL DEFAULT 0;

-- 2. Conversation metadata for ordering and discovery
ALTER TABLE public.chat_conversations
  ADD COLUMN IF NOT EXISTS opened_by TEXT,
  ADD COLUMN IF NOT EXISTS last_message_seq BIGINT NOT NULL DEFAULT 0;

-- 3. Composite indices for newest-first sorting
CREATE INDEX IF NOT EXISTS idx_chat_conv_customer_recent
  ON public.chat_conversations (customer_id, COALESCE(last_message_at, opened_at) DESC);

CREATE INDEX IF NOT EXISTS idx_chat_conv_partner_recent
  ON public.chat_conversations (partner_id, COALESCE(last_message_at, opened_at) DESC);

-- 4. Auth account link to app profiles
ALTER TABLE public.user_profiles
  ADD COLUMN IF NOT EXISTS auth_user_id UUID UNIQUE;

-- 5. Backfill last_message_seq and opened_by on existing conversations if empty
UPDATE public.chat_conversations c
SET last_message_seq = COALESCE((
  SELECT MAX(m.seq) FROM public.chat_messages m WHERE m.conversation_id = c.id
), 0)
WHERE c.last_message_seq = 0;

UPDATE public.chat_conversations
SET opened_by = customer_id
WHERE opened_by IS NULL;

-- 6. RPC: chat_sync(p_profile_id text)
-- Returns all active/read-only conversations, unread counts, and peer cursors in ONE query.
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
AS $$
BEGIN
  RETURN QUERY
  WITH user_convs AS (
    SELECT
      c.id AS c_id,
      c.booking_id AS b_id,
      c.status AS c_status,
      c.last_message_at AS c_last_msg_at,
      c.opened_at AS c_opened_at,
      c.opened_by AS c_opened_by,
      c.last_message_seq AS c_latest_seq,
      CASE WHEN c.customer_id = p_profile_id THEN c.partner_id ELSE c.customer_id END AS peer_id
    FROM public.chat_conversations c
    WHERE (c.customer_id = p_profile_id OR c.partner_id = p_profile_id)
      AND NOT (c.last_message_seq = 0 AND c.opened_by <> p_profile_id)
  ),
  my_read AS (
    SELECT
      r.conversation_id,
      r.last_read_seq,
      r.last_delivered_seq
    FROM public.chat_read_state r
    WHERE r.user_id = p_profile_id
  ),
  peer_read AS (
    SELECT
      r.conversation_id,
      r.last_read_seq AS peer_r_seq,
      r.last_delivered_seq AS peer_d_seq
    FROM public.chat_read_state r
    JOIN user_convs uc ON uc.c_id = r.conversation_id AND uc.peer_id = r.user_id
  ),
  unread_counts AS (
    SELECT
      m.conversation_id,
      COUNT(*)::BIGINT AS cnt
    FROM public.chat_messages m
    JOIN user_convs uc ON uc.c_id = m.conversation_id
    LEFT JOIN my_read mr ON mr.conversation_id = m.conversation_id
    WHERE m.seq > COALESCE(mr.last_read_seq, 0)
      AND m.sender_id <> p_profile_id
    GROUP BY m.conversation_id
  )
  SELECT
    uc.c_id AS conversation_id,
    uc.b_id AS booking_id,
    uc.c_status AS status,
    uc.c_last_msg_at AS last_message_at,
    uc.c_latest_seq AS latest_seq,
    COALESCE(unr.cnt, 0) AS unread_count,
    COALESCE(pr.peer_r_seq, 0) AS peer_read_seq,
    COALESCE(pr.peer_d_seq, 0) AS peer_delivered_seq
  FROM user_convs uc
  LEFT JOIN peer_read pr ON pr.conversation_id = uc.c_id
  LEFT JOIN unread_counts unr ON unr.conversation_id = uc.c_id
  ORDER BY COALESCE(uc.c_last_msg_at, uc.c_opened_at) DESC, uc.c_id DESC;
END;
$$;

REVOKE ALL ON FUNCTION public.chat_sync(TEXT) FROM PUBLIC, anon, authenticated;
