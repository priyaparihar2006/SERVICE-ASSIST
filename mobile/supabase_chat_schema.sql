-- =========================================================================
-- SERVICE ASSIST — CHAT SYSTEM SCHEMA (Idempotent & Default Deny)
-- Server-Mediated Envelope Encryption (AES-256-GCM) with Admin Audit
-- =========================================================================

CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- 1. CHAT CONVERSATIONS
CREATE TABLE IF NOT EXISTS public.chat_conversations (
  id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  booking_id      BIGINT NOT NULL REFERENCES public.bookings(id) ON DELETE CASCADE,
  customer_id     TEXT NOT NULL,
  partner_id      TEXT NOT NULL,
  status          TEXT NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE','READ_ONLY','CLOSED')),
  key_version     INT NOT NULL DEFAULT 1,
  wrapped_dek     BYTEA NOT NULL,           -- per-conversation data key, AES-GCM-wrapped by CHAT_MASTER_KEY
  opened_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
  closes_at       TIMESTAMPTZ,
  last_message_at TIMESTAMPTZ,
  created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
  UNIQUE (booking_id, partner_id)          -- reassignment => new conversation, old partner loses access
);

-- 2. CHAT MESSAGES
CREATE TABLE IF NOT EXISTS public.chat_messages (
  id              UUID PRIMARY KEY,        -- client-generated UUID => idempotent retries
  conversation_id UUID NOT NULL REFERENCES public.chat_conversations(id) ON DELETE CASCADE,
  seq             BIGINT GENERATED ALWAYS AS IDENTITY,   -- cursor for polling: ?after_seq=
  sender_id       TEXT NOT NULL,
  sender_role     TEXT NOT NULL CHECK (sender_role IN ('CUSTOMER','PARTNER','SYSTEM')),
  kind            TEXT NOT NULL DEFAULT 'TEXT' CHECK (kind IN ('TEXT','QUICK_REPLY','ETA','SYSTEM')),
  ciphertext      BYTEA NOT NULL,
  nonce           BYTEA NOT NULL,
  created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_chat_messages_conv_seq 
ON public.chat_messages (conversation_id, seq);

-- 3. CHAT READ STATE
CREATE TABLE IF NOT EXISTS public.chat_read_state (
  conversation_id UUID REFERENCES public.chat_conversations(id) ON DELETE CASCADE,
  user_id         TEXT NOT NULL,
  last_read_seq   BIGINT NOT NULL DEFAULT 0,
  updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
  PRIMARY KEY (conversation_id, user_id)
);

-- 4. CHAT FLAGS (Blocked contact-sharing attempts, reason only, never raw text)
CREATE TABLE IF NOT EXISTS public.chat_flags (
  id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  conversation_id UUID NOT NULL REFERENCES public.chat_conversations(id) ON DELETE CASCADE,
  sender_id       TEXT NOT NULL,
  reason          TEXT NOT NULL CHECK (reason IN ('PHONE','EMAIL','EXTERNAL_APP','ABUSE')),
  created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- 5. CHAT ADMIN AUDIT (Append-only audit trail of every admin inspection)
CREATE TABLE IF NOT EXISTS public.chat_admin_audit (
  id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  admin_id        TEXT NOT NULL,
  conversation_id UUID,
  target_user_id  TEXT,
  action          TEXT NOT NULL CHECK (action IN ('LIST','OPEN','EXPORT')),
  created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- 6. IMMUTABILITY TRIGGER ON ADMIN AUDIT (NO UPDATE / DELETE ALLOWED)
CREATE OR REPLACE FUNCTION public.prevent_audit_tampering()
RETURNS TRIGGER
LANGUAGE plpgsql
SECURITY DEFINER
AS $$
BEGIN
  RAISE EXCEPTION 'Audit records are immutable and append-only. Modification or deletion is strictly prohibited.';
END;
$$;

DROP TRIGGER IF EXISTS trg_prevent_audit_tampering ON public.chat_admin_audit;
CREATE TRIGGER trg_prevent_audit_tampering
BEFORE UPDATE OR DELETE ON public.chat_admin_audit
FOR EACH ROW
EXECUTE FUNCTION public.prevent_audit_tampering();

-- 7. ENABLE ROW LEVEL SECURITY (DEFAULT DENY)
ALTER TABLE public.chat_conversations ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.chat_messages      ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.chat_read_state    ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.chat_flags         ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.chat_admin_audit   ENABLE ROW LEVEL SECURITY;

-- Explicitly revoke all direct client permissions from anon and authenticated
REVOKE ALL ON public.chat_conversations, public.chat_messages, public.chat_read_state,
              public.chat_flags, public.chat_admin_audit FROM anon, authenticated;

-- 8. 90-DAY RETENTION CLEANUP PROCEDURE FOR CLOSED CONVERSATIONS
CREATE OR REPLACE FUNCTION public.purge_expired_closed_chat_messages(retention_days INT DEFAULT 90)
RETURNS INT
LANGUAGE plpgsql
SECURITY DEFINER
AS $$
DECLARE
  deleted_count INT := 0;
BEGIN
  WITH expired_convs AS (
    SELECT id FROM public.chat_conversations
    WHERE status = 'CLOSED' 
      AND closes_at IS NOT NULL 
      AND closes_at < (now() - (retention_days || ' days')::INTERVAL)
  )
  DELETE FROM public.chat_messages
  WHERE conversation_id IN (SELECT id FROM expired_convs);
  
  GET DIAGNOSTICS deleted_count = ROW_COUNT;
  RETURN deleted_count;
END;
$$;
