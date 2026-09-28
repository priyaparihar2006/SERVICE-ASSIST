-- =========================================================================
-- SERVICE ASSIST — CHAT & SYNC FIX MIGRATION V6 (Idempotent)
-- =========================================================================

-- 1. Add last_announced_booking_id column if not present
DO $do$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_schema = 'public' 
          AND table_name = 'chat_conversations' 
          AND column_name = 'last_announced_booking_id'
    ) THEN
        ALTER TABLE public.chat_conversations ADD COLUMN last_announced_booking_id BIGINT;
    END IF;
END;
$do$;

-- 2. Add updated_at column and auto-timestamp trigger on bookings if missing
DO $do$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_schema = 'public' 
          AND table_name = 'bookings' 
          AND column_name = 'updated_at'
    ) THEN
        ALTER TABLE public.bookings ADD COLUMN updated_at TIMESTAMPTZ DEFAULT NOW();
    END IF;
END;
$do$;

CREATE OR REPLACE FUNCTION public.set_bookings_updated_at()
RETURNS TRIGGER AS $func$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$func$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_bookings_updated_at ON public.bookings;
CREATE TRIGGER trg_bookings_updated_at
BEFORE UPDATE ON public.bookings
FOR EACH ROW
EXECUTE FUNCTION public.set_bookings_updated_at();

-- 3. Optimized Indices for fast queries and scans
CREATE INDEX IF NOT EXISTS idx_bookings_cust_pro_status 
ON public.bookings (customer_id, professional_id, status);

CREATE INDEX IF NOT EXISTS idx_chat_messages_conv_seq_desc 
ON public.chat_messages (conversation_id, seq DESC);

-- 4. RPC: chat_append_message
-- Atomically inserts message, updates conversation last_message_seq monotonically, and updates sender read state
CREATE OR REPLACE FUNCTION public.chat_append_message(
    p_conv UUID,
    p_id UUID,
    p_sender TEXT,
    p_role TEXT,
    p_kind TEXT,
    p_ct BYTEA,
    p_nonce BYTEA
)
RETURNS TABLE (
    id UUID,
    seq BIGINT,
    created_at TIMESTAMPTZ
)
LANGUAGE plpgsql
SECURITY DEFINER
AS $func$
DECLARE
    v_msg_id UUID;
    v_seq BIGINT;
    v_created_at TIMESTAMPTZ;
    v_existing RECORD;
BEGIN
    -- Try to insert
    BEGIN
        INSERT INTO public.chat_messages (
            id,
            conversation_id,
            sender_id,
            sender_role,
            kind,
            ciphertext,
            nonce
        ) VALUES (
            p_id,
            p_conv,
            p_sender,
            p_role,
            p_kind,
            p_ct,
            p_nonce
        )
        RETURNING chat_messages.id, chat_messages.seq, chat_messages.created_at
        INTO v_msg_id, v_seq, v_created_at;
    EXCEPTION WHEN unique_violation THEN
        -- Check if it's an idempotent retry from same sender and conversation
        SELECT m.id, m.seq, m.created_at, m.conversation_id, m.sender_id 
        INTO v_existing
        FROM public.chat_messages m
        WHERE m.id = p_id;

        IF FOUND AND v_existing.conversation_id = p_conv AND v_existing.sender_id = p_sender THEN
            RETURN QUERY SELECT v_existing.id, v_existing.seq, v_existing.created_at;
            RETURN;
        ELSE
            RAISE EXCEPTION 'Message ID conflict across conversation or sender' USING ERRCODE = '23505';
        END IF;
    END;

    -- Update conversation last_message_seq and last_message_at monotonically
    UPDATE public.chat_conversations
    SET last_message_seq = GREATEST(COALESCE(last_message_seq, 0), v_seq),
        last_message_at = GREATEST(COALESCE(last_message_at, v_created_at), v_created_at)
    WHERE public.chat_conversations.id = p_conv;

    -- Upsert sender's read and delivery state monotonically
    INSERT INTO public.chat_read_state (
        conversation_id,
        user_id,
        last_read_seq,
        last_delivered_seq,
        updated_at
    ) VALUES (
        p_conv,
        p_sender,
        v_seq,
        v_seq,
        NOW()
    )
    ON CONFLICT (conversation_id, user_id) DO UPDATE
    SET last_read_seq = GREATEST(chat_read_state.last_read_seq, EXCLUDED.last_read_seq),
        last_delivered_seq = GREATEST(chat_read_state.last_delivered_seq, EXCLUDED.last_delivered_seq),
        updated_at = NOW();

    RETURN QUERY SELECT v_msg_id, v_seq, v_created_at;
END;
$func$;

REVOKE ALL ON FUNCTION public.chat_append_message FROM PUBLIC, anon, authenticated;
GRANT EXECUTE ON FUNCTION public.chat_append_message TO service_role;

-- 5. RPC: chat_bump_read_state
CREATE OR REPLACE FUNCTION public.chat_bump_read_state(
    p_conv UUID,
    p_user TEXT,
    p_read BIGINT,
    p_delivered BIGINT
)
RETURNS VOID
LANGUAGE plpgsql
SECURITY DEFINER
AS $func$
BEGIN
    INSERT INTO public.chat_read_state (
        conversation_id,
        user_id,
        last_read_seq,
        last_delivered_seq,
        updated_at
    ) VALUES (
        p_conv,
        p_user,
        COALESCE(p_read, 0),
        COALESCE(p_delivered, 0),
        NOW()
    )
    ON CONFLICT (conversation_id, user_id) DO UPDATE
    SET last_read_seq = GREATEST(chat_read_state.last_read_seq, COALESCE(EXCLUDED.last_read_seq, 0)),
        last_delivered_seq = GREATEST(chat_read_state.last_delivered_seq, COALESCE(EXCLUDED.last_delivered_seq, 0)),
        updated_at = NOW();
END;
$func$;

GRANT EXECUTE ON FUNCTION public.chat_bump_read_state TO service_role;

-- 6. RPC: chat_list_summaries
-- Returns single concise summary row per conversation with last message and unread count
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

-- 7. RPC & Trigger: chat_recompute_pair_status
CREATE OR REPLACE FUNCTION public.chat_recompute_pair_status(
    p_customer TEXT,
    p_partner TEXT
)
RETURNS TEXT
LANGUAGE plpgsql
SECURITY DEFINER
AS $func$
DECLARE
    v_active_booking RECORD;
    v_latest_booking RECORD;
    v_target_status TEXT;
    v_closes_at TIMESTAMPTZ;
    v_booking_id BIGINT;
BEGIN
    IF p_customer IS NULL OR p_partner IS NULL THEN
        RETURN 'NOOP';
    END IF;

    -- Look for any non-completed, non-cancelled booking
    SELECT id INTO v_active_booking
    FROM public.bookings
    WHERE customer_id = p_customer
      AND professional_id = p_partner
      AND status NOT IN ('COMPLETED', 'CANCELLED')
    ORDER BY created_at DESC
    LIMIT 1;

    IF FOUND THEN
        v_target_status := 'ACTIVE';
        v_closes_at := NULL;
        v_booking_id := v_active_booking.id;
    ELSE
        v_target_status := 'READ_ONLY';
        v_closes_at := NOW() + INTERVAL '24 hours';

        SELECT id INTO v_latest_booking
        FROM public.bookings
        WHERE customer_id = p_customer
          AND professional_id = p_partner
        ORDER BY created_at DESC
        LIMIT 1;

        v_booking_id := v_latest_booking.id;
    END IF;

    UPDATE public.chat_conversations
    SET status = v_target_status,
        closes_at = v_closes_at,
        booking_id = COALESCE(v_booking_id, booking_id)
    WHERE customer_id = p_customer
      AND partner_id = p_partner;

    RETURN v_target_status;
END;
$func$;

-- Trigger on bookings table to keep chat status synchronized automatically
CREATE OR REPLACE FUNCTION public.trg_bookings_sync_chat_status()
RETURNS TRIGGER AS $func$
BEGIN
    IF (TG_OP = 'UPDATE' AND OLD.status IS DISTINCT FROM NEW.status) OR TG_OP = 'INSERT' THEN
        PERFORM public.chat_recompute_pair_status(NEW.customer_id, NEW.professional_id);
    END IF;
    RETURN NEW;
END;
$func$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_bookings_chat_status_sync ON public.bookings;
CREATE TRIGGER trg_bookings_chat_status_sync
AFTER INSERT OR UPDATE OF status ON public.bookings
FOR EACH ROW
EXECUTE FUNCTION public.trg_bookings_sync_chat_status();

-- 8. Backfill recomputation for all existing conversations
DO $do$
DECLARE
    r RECORD;
BEGIN
    FOR r IN SELECT customer_id, partner_id FROM public.chat_conversations LOOP
        PERFORM public.chat_recompute_pair_status(r.customer_id, r.partner_id);
    END LOOP;
END;
$do$;
