import React from 'react';
import { AlertCircle, CalendarCheck, Lock, MessageSquare, RefreshCw } from 'lucide-react';
import { ChatConversation } from '../../types';
import { ChatConnection } from '../../context/ChatContext';
import { Avatar } from './Avatar';
import { listTime } from './chatUtils';

interface ChatInboxProps {
  conversations: ChatConversation[];
  loading: boolean;
  error: string;
  selectedId: string | null;
  connection: ChatConnection;
  onSelect: (id: string) => void;
  onRetry: () => void;
  onBrowseBookings: () => void;
}

const isFinished = (c: ChatConversation) => c.status === 'CLOSED' || ['COMPLETED', 'CANCELLED'].includes(c.booking.status);

export const ChatInbox: React.FC<ChatInboxProps> = ({
  conversations,
  loading,
  error,
  selectedId,
  connection,
  onSelect,
  onRetry,
  onBrowseBookings,
}) => (
  <div className="flex flex-col h-full min-h-0">
    <div className="px-4 sm:px-5 py-4 border-b border-[var(--color-line)] flex items-center justify-between">
      <div>
        <h1 className="font-extrabold text-lg text-[var(--color-ink)] font-['Outfit']">Messages</h1>
        <p className="text-[11px] text-gray-500 flex items-center gap-1">
          <Lock className="w-3 h-3" /> Phone numbers stay private
        </p>
      </div>
      <span
        className={`text-[10px] font-bold px-2 py-1 rounded-full ${
          connection === 'connected' ? 'bg-[var(--color-brand-light)] text-[var(--color-brand-hover)]' : 'bg-amber-50 text-amber-700'
        }`}
        role="status"
        data-testid="chat-connection"
      >
        {connection === 'connected' ? 'Live' : connection === 'connecting' ? 'Connecting…' : 'Reconnecting…'}
      </span>
    </div>

    <div className="flex-1 overflow-y-auto" aria-label="Conversations">
      {loading && conversations.length === 0 ? (
        <ul className="p-3 space-y-2" aria-busy="true" aria-label="Loading conversations">
          {[0, 1, 2, 3].map((i) => (
            <li key={i} className="flex items-center gap-3 p-3 animate-pulse">
              <span className="w-11 h-11 rounded-2xl bg-gray-100" />
              <span className="flex-1 space-y-2">
                <span className="block h-3 w-1/2 rounded bg-gray-100" />
                <span className="block h-2.5 w-3/4 rounded bg-gray-100" />
              </span>
            </li>
          ))}
        </ul>
      ) : error ? (
        <div role="alert" className="m-4 p-5 rounded-2xl border border-red-100 bg-red-50/60 text-center">
          <AlertCircle className="w-8 h-8 text-red-400 mx-auto mb-2" />
          <p className="text-sm font-semibold text-gray-800">We couldn't load your messages</p>
          <p className="text-xs text-gray-500 mt-1">{error}</p>
          <button
            type="button"
            onClick={onRetry}
            className="mt-3 inline-flex items-center gap-1.5 px-4 py-2 text-xs font-bold text-white bg-[var(--color-brand)] hover:bg-[var(--color-brand-hover)] rounded-xl cursor-pointer"
          >
            <RefreshCw className="w-3.5 h-3.5" /> Try again
          </button>
        </div>
      ) : conversations.length === 0 ? (
        <div className="p-8 text-center" data-testid="chat-empty">
          <div className="w-14 h-14 mx-auto rounded-2xl bg-[var(--color-brand-light)] text-[var(--color-brand)] flex items-center justify-center mb-3">
            <MessageSquare className="w-7 h-7" />
          </div>
          <h2 className="font-bold text-sm text-[var(--color-ink)] font-['Outfit']">No conversations yet</h2>
          <p className="text-xs text-gray-500 mt-1 leading-relaxed">
            Once a professional is assigned to your booking you can message them here — without sharing either phone number.
          </p>
          <button
            type="button"
            onClick={onBrowseBookings}
            className="mt-4 inline-flex items-center gap-1.5 px-4 py-2.5 text-xs font-bold text-white bg-[var(--color-brand)] hover:bg-[var(--color-brand-hover)] rounded-xl cursor-pointer"
          >
            <CalendarCheck className="w-4 h-4" /> View my bookings
          </button>
        </div>
      ) : (
        <ul>
          {conversations.map((c) => {
            const last = c.lastMessage;
            return (
              <li key={c.id}>
                <button
                  type="button"
                  onClick={() => onSelect(c.id)}
                  aria-current={c.id === selectedId ? 'true' : undefined}
                  data-testid="chat-list-item"
                  className={`w-full flex items-center gap-3 px-4 sm:px-5 py-3.5 text-left border-b border-[var(--color-line)]/60 transition-colors cursor-pointer ${
                    c.id === selectedId ? 'bg-[var(--color-brand-light)]/70' : 'hover:bg-[var(--color-brand-soft)]'
                  }`}
                >
                  <Avatar name={c.counterpart.displayName} src={c.counterpart.avatar} online={c.counterpart.online && c.status === 'ACTIVE'} />
                  <span className="min-w-0 flex-1">
                    <span className="flex items-baseline justify-between gap-2">
                      <span className="font-bold text-sm text-[var(--color-ink)] truncate font-['Outfit']">{c.counterpart.displayName}</span>
                      <span className="text-[10px] text-gray-400 shrink-0">{listTime(last?.createdAt ?? c.createdAt)}</span>
                    </span>
                    <span className="block text-[11px] text-[var(--color-brand-hover)] font-semibold truncate">
                      {c.booking.categoryName} · Booking {c.booking.reference.slice(-8)}
                      {isFinished(c) && <span className="ml-1.5 text-gray-400 font-medium">· {c.status === 'CLOSED' ? 'Ended' : 'Finished'}</span>}
                    </span>
                    <span className="flex items-center justify-between gap-2 mt-0.5">
                      <span className={`text-xs truncate ${c.unreadCount ? 'text-gray-900 font-semibold' : 'text-gray-500'}`}>
                        {!last
                          ? 'No messages yet'
                          : last.deleted
                            ? 'Message deleted'
                            : `${last.isMine ? 'You: ' : ''}${last.preview ?? 'Encrypted message'}`}
                      </span>
                      {c.unreadCount > 0 && (
                        <span
                          aria-label={`${c.unreadCount} unread`}
                          className="min-w-[20px] h-5 px-1.5 rounded-full bg-[var(--color-brand)] text-white text-[10px] font-bold flex items-center justify-center"
                        >
                          {c.unreadCount > 99 ? '99+' : c.unreadCount}
                        </span>
                      )}
                    </span>
                  </span>
                </button>
              </li>
            );
          })}
        </ul>
      )}
    </div>
  </div>
);
