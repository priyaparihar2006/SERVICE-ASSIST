import React, { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { AlertCircle, ArrowLeft, MessageSquare } from 'lucide-react';
import { ChatConversation } from '../types';
import { chatApi } from '../services/chat';
import { useChat } from '../context/ChatContext';
import { useAuth } from '../context/AuthContext';
import { ChatInbox } from '../components/chat/ChatInbox';
import { ChatWindow } from '../components/chat/ChatWindow';

interface MessagesPageProps {
  currentPath: string;
  onNavigate: (path: string) => void;
}

// Updates the address bar without adding a history entry, then tells the app router to re-read it.
function replacePath(path: string) {
  window.history.replaceState({}, '', path);
  window.dispatchEvent(new PopStateEvent('popstate'));
}

export const MessagesPage: React.FC<MessagesPageProps> = ({ currentPath, onNavigate }) => {
  const { user } = useAuth();
  const { subscribe, connection } = useChat();
  const params = useMemo(() => new URL(currentPath, window.location.origin).searchParams, [currentPath]);
  const selectedId = params.get('c');
  const bookingParam = params.get('booking');

  const [conversations, setConversations] = useState<ChatConversation[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [opening, setOpening] = useState(false);
  const [openError, setOpenError] = useState('');

  const load = useCallback(async (silent = false) => {
    if (!silent) setLoading(true);
    try {
      setConversations(await chatApi.list());
      setError('');
    } catch (e: any) {
      if (!silent) setError(e.message || 'Unable to load conversations');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    void load();
  }, [load, user?.id]);

  // Keep the inbox current from live events (debounced) and presence changes.
  useEffect(() => {
    let timer: number | undefined;
    const refresh = () => {
      window.clearTimeout(timer);
      timer = window.setTimeout(() => void load(true), 250);
    };
    const offs = ['message:new', 'message:deleted', 'conversation:updated', 'connected'].map((event) => subscribe(event, refresh));
    offs.push(
      subscribe('presence', ({ conversationId, online }) =>
        setConversations((list) =>
          list.map((c) => (c.id === conversationId ? { ...c, counterpart: { ...c.counterpart, online: !!online } } : c)),
        ),
      ),
    );
    return () => {
      window.clearTimeout(timer);
      offs.forEach((off) => off());
    };
  }, [subscribe, load]);

  // Deep link from a booking: /messages?booking=<id> finds or creates that booking's chat. Only the
  // latest request may update state (the URL rewrite below re-runs this effect).
  const openSeq = useRef(0);
  useEffect(() => {
    if (!bookingParam) {
      setOpening(false);
      setOpenError('');
      return;
    }
    const seq = ++openSeq.current;
    setOpening(true);
    setOpenError('');
    chatApi
      .openForBooking(bookingParam)
      .then((conversation) => {
        if (seq !== openSeq.current) return;
        replacePath(`/messages?c=${conversation.id}`);
        void load(true);
      })
      .catch((e: any) => {
        if (seq === openSeq.current) setOpenError(e.message || 'Unable to open this chat');
      })
      .finally(() => {
        if (seq === openSeq.current) setOpening(false);
      });
  }, [bookingParam, load]);

  const select = (id: string) => onNavigate(`/messages?c=${id}`);
  const showThread = Boolean(selectedId) || opening || Boolean(openError);

  return (
    <div className="bg-[var(--color-brand-soft)]/30 sm:py-6">
      <div className="max-w-6xl mx-auto sm:px-6 lg:px-8">
        <div className="bg-white sm:rounded-3xl sm:border border-[var(--color-line)] sm:shadow-xs overflow-hidden flex h-[calc(100dvh-4.25rem)] sm:h-[calc(100dvh-8.25rem)] min-h-[480px]">
          <aside className={`${showThread ? 'hidden md:flex' : 'flex'} w-full md:w-[340px] lg:w-[380px] shrink-0 flex-col border-r border-[var(--color-line)] min-h-0`}>
            <ChatInbox
              conversations={conversations}
              loading={loading}
              error={error}
              selectedId={selectedId}
              connection={connection}
              onSelect={select}
              onRetry={() => void load()}
              onBrowseBookings={() => onNavigate(user?.role === 'PROFESSIONAL' ? '/professional/dashboard' : '/dashboard')}
            />
          </aside>

          <section className={`${showThread ? 'flex' : 'hidden md:flex'} flex-1 flex-col min-w-0 min-h-0`} aria-label="Conversation">
            {opening ? (
              <div className="h-full flex items-center justify-center text-sm text-gray-500" role="status" data-testid="chat-opening">
                Opening your chat…
              </div>
            ) : openError ? (
              <div className="h-full flex items-center justify-center p-6" role="alert" data-testid="chat-open-error">
                <div className="max-w-sm text-center">
                  <AlertCircle className="w-10 h-10 text-amber-500 mx-auto mb-3" />
                  <p className="font-bold text-sm text-gray-900">Chat isn't available for this booking yet</p>
                  <p className="text-xs text-gray-500 mt-1">{openError}</p>
                  <button
                    type="button"
                    onClick={() => replacePath('/messages')}
                    className="mt-4 inline-flex items-center gap-1.5 px-4 py-2 text-xs font-bold text-white bg-[var(--color-brand)] hover:bg-[var(--color-brand-hover)] rounded-xl cursor-pointer"
                  >
                    <ArrowLeft className="w-3.5 h-3.5" /> Back to messages
                  </button>
                </div>
              </div>
            ) : selectedId ? (
              <ChatWindow key={selectedId} conversationId={selectedId} onBack={() => onNavigate('/messages')} />
            ) : (
              <div className="h-full flex items-center justify-center p-6 text-center bg-[var(--color-page)]">
                <div>
                  <div className="w-14 h-14 mx-auto rounded-2xl bg-[var(--color-brand-light)] text-[var(--color-brand)] flex items-center justify-center mb-3">
                    <MessageSquare className="w-7 h-7" />
                  </div>
                  <p className="font-bold text-sm text-[var(--color-ink)] font-['Outfit']">Select a conversation</p>
                  <p className="text-xs text-gray-500 mt-1">Choose a booking on the left to read or send messages.</p>
                </div>
              </div>
            )}
          </section>
        </div>
      </div>
    </div>
  );
};
