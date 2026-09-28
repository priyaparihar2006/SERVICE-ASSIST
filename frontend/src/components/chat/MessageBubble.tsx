import React, { useState } from 'react';
import { AlertCircle, Check, CheckCheck, Clock, Trash2 } from 'lucide-react';
import { ChatMessage } from '../../types';
import { formatClock } from './chatUtils';

interface MessageBubbleProps {
  message: ChatMessage;
  canDelete: boolean;
  onRetry: (message: ChatMessage) => void;
  onEdit: (message: ChatMessage) => void;
  onDiscard: (message: ChatMessage) => void;
  onDelete: (message: ChatMessage) => Promise<void>;
}

const STATUS_LABEL = { sending: 'Sending', sent: 'Sent', delivered: 'Delivered', read: 'Read', failed: 'Not sent' } as const;

function StatusIcon({ status }: { status: ChatMessage['status'] }) {
  if (!status) return null;
  const common = { 'aria-label': STATUS_LABEL[status], role: 'img' } as const;
  if (status === 'sending') return <Clock {...common} className="w-3 h-3 opacity-80" />;
  if (status === 'sent') return <Check {...common} className="w-3.5 h-3.5 opacity-80" />;
  if (status === 'delivered') return <CheckCheck {...common} className="w-3.5 h-3.5 opacity-80" />;
  if (status === 'read') return <CheckCheck {...common} className="w-3.5 h-3.5 text-[#B8FFDD]" />;
  return <AlertCircle {...common} className="w-3.5 h-3.5 text-red-500" />;
}

export const MessageBubble: React.FC<MessageBubbleProps> = ({ message, canDelete, onRetry, onEdit, onDiscard, onDelete }) => {
  const [confirming, setConfirming] = useState(false);
  const [busy, setBusy] = useState(false);
  const failed = message.status === 'failed';

  if (message.deleted) {
    return (
      <div className={`flex ${message.isMine ? 'justify-end' : 'justify-start'}`}>
        <p className="max-w-[80%] rounded-2xl px-3.5 py-2 text-xs italic text-gray-400 border border-dashed border-gray-200 bg-white/60">
          {message.isMine ? 'You deleted this message' : 'This message was deleted'}
        </p>
      </div>
    );
  }

  return (
    <div className={`group flex flex-col ${message.isMine ? 'items-end' : 'items-start'}`} data-testid="chat-message">
      <div className="flex items-end gap-1.5 max-w-[86%] sm:max-w-[72%]">
        {message.isMine && canDelete && !failed && message.status !== 'sending' && !confirming && (
          <button
            type="button"
            onClick={() => setConfirming(true)}
            aria-label="Delete message"
            className="p-1 rounded-lg text-gray-300 hover:text-red-500 hover:bg-red-50 opacity-100 sm:opacity-0 sm:group-hover:opacity-100 focus-visible:opacity-100 transition-opacity cursor-pointer"
          >
            <Trash2 className="w-3.5 h-3.5" />
          </button>
        )}
        <div
          className={`rounded-2xl px-3.5 py-2 text-[15px] sm:text-sm leading-relaxed shadow-2xs ${
            message.isMine
              ? failed
                ? 'bg-white text-gray-800 border border-red-300 rounded-br-md'
                : 'bg-[var(--color-brand)] text-white rounded-br-md'
              : 'bg-white text-[var(--color-ink)] border border-[var(--color-line)] rounded-bl-md'
          }`}
        >
          {/* Always rendered as plain text: message content is never interpreted as HTML. */}
          <p className="whitespace-pre-wrap break-words [overflow-wrap:anywhere]">{message.content}</p>
          <div
            className={`mt-0.5 flex items-center justify-end gap-1 text-[10px] ${
              message.isMine && !failed ? 'text-white/80' : 'text-gray-400'
            }`}
          >
            <time dateTime={message.createdAt}>{formatClock(message.createdAt)}</time>
            {message.isMine && <StatusIcon status={message.status} />}
          </div>
        </div>
      </div>

      {confirming && (
        <div
          role="alertdialog"
          aria-label="Delete this message?"
          className="mt-1.5 flex items-center gap-2 text-xs bg-white border border-red-200 rounded-xl px-3 py-2 shadow-xs"
        >
          <span className="text-gray-700">Delete for everyone?</span>
          <button
            type="button"
            disabled={busy}
            onClick={async () => {
              setBusy(true);
              try {
                await onDelete(message);
              } catch (e: any) {
                alert(e.message);
                setBusy(false);
                setConfirming(false);
              }
            }}
            className="font-bold text-red-600 hover:underline disabled:opacity-50 cursor-pointer"
          >
            {busy ? 'Deleting…' : 'Delete'}
          </button>
          <button type="button" onClick={() => setConfirming(false)} className="text-gray-500 hover:underline cursor-pointer">
            Cancel
          </button>
        </div>
      )}

      {failed && (
        <div role="alert" className="mt-1 max-w-[86%] sm:max-w-[72%] text-right text-xs text-red-600">
          <p>{message.failure?.message || 'Message not sent'}</p>
          <div className="mt-0.5 flex justify-end gap-3 font-semibold">
            {message.failure?.retryable && (
              <button type="button" onClick={() => onRetry(message)} className="hover:underline cursor-pointer">
                Retry
              </button>
            )}
            <button type="button" onClick={() => onEdit(message)} className="hover:underline cursor-pointer">
              Edit
            </button>
            <button type="button" onClick={() => onDiscard(message)} className="text-gray-500 hover:underline cursor-pointer">
              Discard
            </button>
          </div>
        </div>
      )}
    </div>
  );
};
