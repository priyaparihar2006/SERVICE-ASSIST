import { useCallback, useEffect, useRef, useState } from 'react';
import { ChatConversation, ChatMessage, ChatMessageStatus, ChatReportReason } from '../types';
import { chatApi } from '../services/chat';
import { useChat } from '../context/ChatContext';

const TYPING_IDLE_MS = 2000;
const TYPING_DISPLAY_MS = 5000;
const POLL_WHEN_OFFLINE_MS = 15000;
const RANK: Record<ChatMessageStatus, number> = { failed: -1, sending: 0, sent: 1, delivered: 2, read: 3 };

// crypto.randomUUID() only exists in secure contexts; fall back so plain-http LAN testing still works.
function uuid(): string {
  if (typeof crypto.randomUUID === 'function') return crypto.randomUUID();
  const bytes = crypto.getRandomValues(new Uint8Array(16));
  bytes[6] = (bytes[6] & 0x0f) | 0x40;
  bytes[8] = (bytes[8] & 0x3f) | 0x80;
  const hex = [...bytes].map((b) => b.toString(16).padStart(2, '0')).join('');
  return `${hex.slice(0, 8)}-${hex.slice(8, 12)}-${hex.slice(12, 16)}-${hex.slice(16, 20)}-${hex.slice(20)}`;
}

const order = (a: ChatMessage, b: ChatMessage) =>
  a.createdAt === b.createdAt ? a.id.localeCompare(b.id) : a.createdAt < b.createdAt ? -1 : 1;

// Inserts or replaces a message. A server message replaces its optimistic copy (matched by clientMessageId).
function upsert(list: ChatMessage[], incoming: ChatMessage): ChatMessage[] {
  const index = list.findIndex(
    (m) => m.id === incoming.id || (!!incoming.clientMessageId && m.clientMessageId === incoming.clientMessageId),
  );
  if (index === -1) return [...list, incoming].sort(order);
  const existing = list[index];
  const merged: ChatMessage = {
    ...incoming,
    // Never downgrade a delivery status that a socket event already advanced.
    status:
      incoming.status && existing.status && RANK[existing.status] > RANK[incoming.status]
        ? existing.status
        : incoming.status,
  };
  return [...list.slice(0, index), merged, ...list.slice(index + 1)].sort(order);
}

export function useConversation(conversationId: string | null) {
  const { subscribe, emit, connection } = useChat();
  const [conversation, setConversation] = useState<ChatConversation | null>(null);
  const [messages, setMessages] = useState<ChatMessage[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [hasMore, setHasMore] = useState(false);
  const [nextBefore, setNextBefore] = useState<string | null>(null);
  const [loadingOlder, setLoadingOlder] = useState(false);
  const [theyAreTyping, setTheyAreTyping] = useState(false);

  const idRef = useRef(conversationId);
  idRef.current = conversationId;
  const loadedRef = useRef(false);
  const unreadRef = useRef(false);
  const typingActive = useRef(false);
  const typingIdle = useRef<number | undefined>(undefined);
  const typingDisplay = useRef<number | undefined>(undefined);

  const markReadIfVisible = useCallback(() => {
    const id = idRef.current;
    if (!id || !unreadRef.current || document.visibilityState !== 'visible') return;
    unreadRef.current = false;
    chatApi.markRead(id).catch(() => {
      unreadRef.current = true;
    });
  }, []);

  const load = useCallback(async () => {
    const id = conversationId;
    if (!id) return;
    setLoading(true);
    setError('');
    try {
      const [conv, page] = await Promise.all([chatApi.get(id), chatApi.messages(id)]);
      if (idRef.current !== id) return;
      setConversation(conv);
      setMessages(page.messages);
      setHasMore(page.hasMore);
      setNextBefore(page.nextBefore);
      loadedRef.current = true;
      unreadRef.current = conv.unreadCount > 0;
      markReadIfVisible();
    } catch (e: any) {
      if (idRef.current === id) setError(e.message || 'Unable to load this conversation');
    } finally {
      if (idRef.current === id) setLoading(false);
    }
  }, [conversationId, markReadIfVisible]);

  // Re-fetch the latest page without disturbing older history or unsent messages (after reconnects).
  const sync = useCallback(async () => {
    const id = idRef.current;
    if (!id) return;
    try {
      const [conv, page] = await Promise.all([chatApi.get(id), chatApi.messages(id)]);
      if (idRef.current !== id) return;
      setConversation(conv);
      setMessages((prev) => page.messages.reduce(upsert, prev));
      if (conv.unreadCount > 0) {
        unreadRef.current = true;
        markReadIfVisible();
      }
    } catch {
      /* the next reconnect or poll tries again */
    }
  }, [markReadIfVisible]);

  const refreshConversation = useCallback(async () => {
    const id = idRef.current;
    if (!id) return;
    try {
      const conv = await chatApi.get(id);
      if (idRef.current === id) setConversation(conv);
    } catch (e: any) {
      // Access ended (e.g. the professional was reassigned): stop showing this thread.
      if (idRef.current === id && e.status === 404) {
        setConversation(null);
        setMessages([]);
        setError('This conversation is no longer available.');
      }
    }
  }, []);

  useEffect(() => {
    setConversation(null);
    setMessages([]);
    setError('');
    setHasMore(false);
    setNextBefore(null);
    setTheyAreTyping(false);
    loadedRef.current = false;
    unreadRef.current = false;
    if (conversationId) void load();
  }, [conversationId, load]);

  useEffect(() => {
    if (!conversationId) return;
    const offs = [
      subscribe('message:new', ({ conversationId: cid, message }) => {
        if (cid !== idRef.current) return;
        setMessages((prev) => upsert(prev, message));
        if (!message.isMine) {
          setTheyAreTyping(false);
          emit('message:delivered', { conversationId: cid, messageIds: [message.id] });
          unreadRef.current = true;
          markReadIfVisible();
        }
      }),
      subscribe('message:status', ({ conversationId: cid, messageIds, status }) => {
        if (cid !== idRef.current) return;
        const ids = new Set<string>(messageIds);
        setMessages((prev) =>
          prev.map((m) =>
            m.isMine && ids.has(m.id) && m.status && RANK[m.status] < RANK[status as ChatMessageStatus]
              ? { ...m, status: status as ChatMessageStatus }
              : m,
          ),
        );
      }),
      subscribe('message:deleted', ({ conversationId: cid, messageId }) => {
        if (cid !== idRef.current) return;
        setMessages((prev) => prev.map((m) => (m.id === messageId ? { ...m, deleted: true, content: null } : m)));
      }),
      subscribe('typing', ({ conversationId: cid, isTyping }) => {
        if (cid !== idRef.current) return;
        window.clearTimeout(typingDisplay.current);
        setTheyAreTyping(!!isTyping);
        if (isTyping)
          typingDisplay.current = window.setTimeout(() => setTheyAreTyping(false), TYPING_DISPLAY_MS);
      }),
      subscribe('presence', ({ conversationId: cid, online }) => {
        if (cid !== idRef.current) return;
        setConversation((c) => (c ? { ...c, counterpart: { ...c.counterpart, online: !!online } } : c));
      }),
      subscribe('conversation:updated', ({ conversationId: cid, reason }) => {
        if (cid === idRef.current && reason !== 'read') void refreshConversation();
      }),
      subscribe('connected', () => {
        if (loadedRef.current) void sync();
      }),
    ];
    const onVisible = () => markReadIfVisible();
    document.addEventListener('visibilitychange', onVisible);
    return () => {
      offs.forEach((off) => off());
      document.removeEventListener('visibilitychange', onVisible);
      window.clearTimeout(typingDisplay.current);
    };
  }, [conversationId, subscribe, emit, markReadIfVisible, refreshConversation, sync]);

  // Without a live socket, poll so the thread still catches up.
  useEffect(() => {
    if (!conversationId || connection === 'connected') return;
    const timer = window.setInterval(() => void sync(), POLL_WHEN_OFFLINE_MS);
    return () => window.clearInterval(timer);
  }, [conversationId, connection, sync]);

  const stopTyping = useCallback(() => {
    window.clearTimeout(typingIdle.current);
    if (typingActive.current && idRef.current) {
      typingActive.current = false;
      emit('typing', { conversationId: idRef.current, isTyping: false });
    }
  }, [emit]);

  // Leaving a conversation (or unmounting) tells the other side we stopped typing in *that* thread.
  useEffect(() => {
    const id = conversationId;
    return () => {
      window.clearTimeout(typingIdle.current);
      if (typingActive.current && id) {
        typingActive.current = false;
        emit('typing', { conversationId: id, isTyping: false });
      }
    };
  }, [conversationId, emit]);

  const notifyTyping = useCallback(() => {
    const id = idRef.current;
    if (!id) return;
    if (!typingActive.current) {
      typingActive.current = true;
      emit('typing', { conversationId: id, isTyping: true });
    }
    window.clearTimeout(typingIdle.current);
    typingIdle.current = window.setTimeout(stopTyping, TYPING_IDLE_MS);
  }, [emit, stopTyping]);

  const send = useCallback(
    async (text: string, retryOf?: string): Promise<boolean> => {
      const id = idRef.current;
      if (!id) return false;
      stopTyping();
      const clientMessageId = retryOf ?? uuid();
      const pending: ChatMessage = {
        id: `local-${clientMessageId}`,
        conversationId: id,
        isMine: true,
        type: 'TEXT',
        content: text,
        deleted: false,
        createdAt: new Date().toISOString(),
        clientMessageId,
        status: 'sending',
      };
      setMessages((prev) =>
        retryOf
          ? prev.map((m) =>
              m.clientMessageId === retryOf ? { ...m, status: 'sending', failure: undefined } : m,
            )
          : [...prev, pending].sort(order),
      );
      try {
        const saved = await chatApi.send(id, text, clientMessageId);
        setMessages((prev) => upsert(prev, saved));
        return true;
      } catch (e: any) {
        const retryable = e.code !== 'CONTACT_INFO_NOT_ALLOWED' && (!e.status || e.status >= 500 || e.status === 429);
        setMessages((prev) =>
          prev.map((m) =>
            m.clientMessageId === clientMessageId
              ? { ...m, status: 'failed', failure: { message: e.message || 'Message not sent', retryable } }
              : m,
          ),
        );
        if (e.status === 403 || e.status === 404) void refreshConversation();
        return false;
      }
    },
    [refreshConversation, stopTyping],
  );

  const discard = useCallback((clientMessageId: string) => {
    setMessages((prev) => prev.filter((m) => m.clientMessageId !== clientMessageId));
  }, []);

  const loadOlder = useCallback(async () => {
    const id = idRef.current;
    if (!id || !nextBefore || loadingOlder) return;
    setLoadingOlder(true);
    try {
      const page = await chatApi.messages(id, nextBefore);
      if (idRef.current !== id) return;
      setMessages((prev) => page.messages.reduce(upsert, prev));
      setHasMore(page.hasMore);
      setNextBefore(page.nextBefore);
    } catch (e: any) {
      setError(e.message || 'Unable to load earlier messages');
    } finally {
      setLoadingOlder(false);
    }
  }, [nextBefore, loadingOlder]);

  const remove = useCallback(async (messageId: string) => {
    await chatApi.deleteMessage(messageId);
    setMessages((prev) => prev.map((m) => (m.id === messageId ? { ...m, deleted: true, content: null } : m)));
  }, []);

  const setBlocked = useCallback(async (blocked: boolean) => {
    const id = idRef.current;
    if (id) setConversation(await chatApi.setBlocked(id, blocked));
  }, []);

  const report = useCallback(async (reason: ChatReportReason, details: string) => {
    const id = idRef.current;
    if (id) await chatApi.report(id, reason, details);
  }, []);

  return {
    conversation,
    messages,
    loading,
    error,
    hasMore,
    loadingOlder,
    theyAreTyping,
    connection,
    reload: load,
    loadOlder,
    send,
    discard,
    remove,
    notifyTyping,
    stopTyping,
    setBlocked,
    report,
  };
}
