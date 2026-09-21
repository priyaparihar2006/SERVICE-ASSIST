import React, { Fragment, useLayoutEffect, useRef, useState } from 'react';
import { AlertCircle, Loader2, RefreshCw, ShieldCheck, WifiOff } from 'lucide-react';
import { ChatMessage } from '../../types';
import { useConversation } from '../../hooks/useConversation';
import { ChatHeader } from './ChatHeader';
import { MessageBubble } from './MessageBubble';
import { ComposerHandle, MessageComposer } from './MessageComposer';
import { blockedReasonText, dayLabel } from './chatUtils';

interface ChatWindowProps {
  conversationId: string;
  onBack?: () => void;
}

export const ChatWindow: React.FC<ChatWindowProps> = ({ conversationId, onBack }) => {
  const chat = useConversation(conversationId);
  const { conversation, messages, loading, error, hasMore, loadingOlder, theyAreTyping, connection } = chat;
  const [draft, setDraft] = useState('');
  const scroller = useRef<HTMLDivElement>(null);
  const composer = useRef<ComposerHandle>(null);
  const stickToBottom = useRef(true);
  const restoreFromBottom = useRef<number | null>(null);

  // Start every conversation at the newest message, and keep drafts from leaking between threads.
  useLayoutEffect(() => {
    stickToBottom.current = true;
    setDraft('');
  }, [conversationId]);

  useLayoutEffect(() => {
    const el = scroller.current;
    if (!el) return;
    if (restoreFromBottom.current !== null) {
      el.scrollTop = el.scrollHeight - restoreFromBottom.current;
      restoreFromBottom.current = null;
      return;
    }
    if (stickToBottom.current || messages[messages.length - 1]?.isMine) el.scrollTop = el.scrollHeight;
  }, [messages, theyAreTyping, loading]);

  const onScroll = () => {
    const el = scroller.current;
    if (el) stickToBottom.current = el.scrollHeight - el.scrollTop - el.clientHeight < 120;
  };

  const loadEarlier = () => {
    const el = scroller.current;
    if (el) restoreFromBottom.current = el.scrollHeight - el.scrollTop;
    void chat.loadOlder();
  };

  const submit = () => {
    const text = draft.trim();
    if (!text) return;
    setDraft('');
    stickToBottom.current = true;
    void chat.send(text);
  };

  const editFailed = (m: ChatMessage) => {
    setDraft(m.content ?? '');
    chat.discard(m.clientMessageId!);
    composer.current?.focus();
  };

  if (error && !conversation) {
    return (
      <div className="h-full flex items-center justify-center p-6" role="alert" data-testid="chat-error">
        <div className="max-w-sm text-center">
          <AlertCircle className="w-10 h-10 text-red-400 mx-auto mb-3" />
          <p className="font-bold text-sm text-gray-900">We couldn't open this conversation</p>
          <p className="text-xs text-gray-500 mt-1">{error}</p>
          <div className="mt-4 flex justify-center gap-2">
            {onBack && (
              <button type="button" onClick={onBack} className="md:hidden px-4 py-2 text-xs font-semibold text-gray-600 rounded-xl hover:bg-gray-100 cursor-pointer">
                Back
              </button>
            )}
            <button type="button" onClick={() => void chat.reload()} className="inline-flex items-center gap-1.5 px-4 py-2 text-xs font-bold text-white bg-[var(--color-brand)] hover:bg-[var(--color-brand-hover)] rounded-xl cursor-pointer">
              <RefreshCw className="w-3.5 h-3.5" /> Try again
            </button>
          </div>
        </div>
      </div>
    );
  }

  if (!conversation) {
    return (
      <div className="h-full flex flex-col" aria-busy="true" data-testid="chat-loading">
        <div className="h-[73px] border-b border-[var(--color-line)] px-5 flex items-center gap-3 animate-pulse">
          <span className="w-11 h-11 rounded-2xl bg-gray-100" />
          <span className="space-y-2 flex-1">
            <span className="block h-3 w-40 rounded bg-gray-100" />
            <span className="block h-2.5 w-56 rounded bg-gray-100" />
          </span>
        </div>
        <div className="flex-1 p-5 space-y-3 animate-pulse">
          <span className="block h-10 w-2/5 rounded-2xl bg-gray-100" />
          <span className="block h-10 w-1/3 rounded-2xl bg-gray-100 ml-auto" />
          <span className="block h-10 w-1/2 rounded-2xl bg-gray-100" />
        </div>
        <span className="sr-only" role="status">Loading conversation</span>
      </div>
    );
  }

  const canSend = conversation.canSend;
  let previousDay = '';
  return (
    <div className="h-full flex flex-col min-h-0 bg-[var(--color-page)]" data-testid="chat-window">
      <ChatHeader
        conversation={conversation}
        typing={theyAreTyping}
        onBack={onBack}
        onSetBlocked={chat.setBlocked}
        onReport={chat.report}
      />

      {connection !== 'connected' && (
        <div role="status" className="px-4 py-1.5 bg-amber-50 border-b border-amber-100 text-[11px] text-amber-800 flex items-center gap-1.5">
          <WifiOff className="w-3.5 h-3.5" />
          {connection === 'connecting' ? 'Connecting to live updates…' : 'Live updates paused — reconnecting. You can still send messages.'}
        </div>
      )}

      <div
        ref={scroller}
        onScroll={onScroll}
        role="log"
        aria-live="polite"
        aria-label={`Conversation with ${conversation.counterpart.displayName}`}
        className="flex-1 overflow-y-auto px-3 sm:px-5 py-4 space-y-2"
        data-testid="chat-messages"
      >
        {hasMore && (
          <div className="flex justify-center pb-2">
            <button
              type="button"
              onClick={loadEarlier}
              disabled={loadingOlder}
              className="inline-flex items-center gap-1.5 px-3.5 py-1.5 text-[11px] font-semibold text-[var(--color-brand-hover)] bg-white border border-[var(--color-line)] rounded-full hover:bg-[var(--color-brand-soft)] disabled:opacity-60 cursor-pointer"
            >
              {loadingOlder && <Loader2 className="w-3 h-3 animate-spin" />} Load earlier messages
            </button>
          </div>
        )}

        {error && <p role="alert" className="text-center text-xs text-red-600">{error}</p>}

        {messages.length === 0 && !loading && (
          <div className="py-12 text-center max-w-xs mx-auto" data-testid="chat-empty-thread">
            <div className="w-12 h-12 mx-auto rounded-2xl bg-[var(--color-brand-light)] text-[var(--color-brand)] flex items-center justify-center mb-3">
              <ShieldCheck className="w-6 h-6" />
            </div>
            <p className="font-bold text-sm text-[var(--color-ink)] font-['Outfit']">Start the conversation</p>
            <p className="text-xs text-gray-500 mt-1 leading-relaxed">
              Share timing, access instructions or questions about this booking. Your phone number stays hidden.
            </p>
          </div>
        )}

        {messages.map((m) => {
          const day = dayLabel(m.createdAt);
          const showDay = day !== previousDay;
          previousDay = day;
          return (
            <Fragment key={m.clientMessageId && m.isMine ? m.clientMessageId : m.id}>
              {showDay && (
                <div className="flex justify-center py-1">
                  <span className="text-[10px] font-semibold text-gray-500 bg-white border border-[var(--color-line)] rounded-full px-3 py-0.5">{day}</span>
                </div>
              )}
              <MessageBubble
                message={m}
                canDelete
                onRetry={(msg) => void chat.send(msg.content ?? '', msg.clientMessageId)}
                onEdit={editFailed}
                onDiscard={(msg) => chat.discard(msg.clientMessageId!)}
                onDelete={(msg) => chat.remove(msg.id)}
              />
            </Fragment>
          );
        })}

        {theyAreTyping && (
          <div className="flex justify-start" data-testid="chat-typing" role="status" aria-label={`${conversation.counterpart.displayName} is typing`}>
            <div className="bg-white border border-[var(--color-line)] rounded-2xl rounded-bl-md px-4 py-3 flex items-center gap-1">
              {[0, 150, 300].map((delay) => (
                <span key={delay} className="w-1.5 h-1.5 rounded-full bg-gray-400 animate-bounce" style={{ animationDelay: `${delay}ms` }} />
              ))}
            </div>
          </div>
        )}
      </div>

      <MessageComposer
        ref={composer}
        value={draft}
        onChange={setDraft}
        onSubmit={submit}
        onTyping={chat.notifyTyping}
        disabled={!canSend}
        disabledText={blockedReasonText(conversation.sendBlockedReason)}
      />
    </div>
  );
};
