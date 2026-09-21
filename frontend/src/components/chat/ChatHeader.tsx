import React, { useEffect, useRef, useState } from 'react';
import { ArrowLeft, Ban, EllipsisVertical, Flag, Lock, ShieldCheck, X } from 'lucide-react';
import { ChatConversation, ChatReportReason } from '../../types';
import { Avatar } from './Avatar';

interface ChatHeaderProps {
  conversation: ChatConversation;
  typing: boolean;
  onBack?: () => void;
  onSetBlocked: (blocked: boolean) => Promise<void>;
  onReport: (reason: ChatReportReason, details: string) => Promise<void>;
}

const REPORT_REASONS: { value: ChatReportReason; label: string }[] = [
  { value: 'HARASSMENT', label: 'Harassment or abusive language' },
  { value: 'OFF_PLATFORM_CONTACT', label: 'Asked me to share contact details or move off the app' },
  { value: 'SPAM', label: 'Spam or unwanted promotion' },
  { value: 'SAFETY', label: 'I have a safety concern' },
  { value: 'OTHER', label: 'Something else' },
];

export const ChatHeader: React.FC<ChatHeaderProps> = ({ conversation, typing, onBack, onSetBlocked, onReport }) => {
  const { counterpart, booking } = conversation;
  const [privacyOpen, setPrivacyOpen] = useState(false);
  const [menuOpen, setMenuOpen] = useState(false);
  const [reporting, setReporting] = useState(false);
  const [reason, setReason] = useState<ChatReportReason>('HARASSMENT');
  const [details, setDetails] = useState('');
  const [busy, setBusy] = useState(false);
  const [notice, setNotice] = useState('');
  const [reportError, setReportError] = useState('');
  const closeRef = useRef<HTMLButtonElement>(null);

  useEffect(() => {
    if (!privacyOpen && !menuOpen) return;
    const onKey = (e: KeyboardEvent) => {
      if (e.key === 'Escape') {
        setPrivacyOpen(false);
        setMenuOpen(false);
      }
    };
    document.addEventListener('keydown', onKey);
    return () => document.removeEventListener('keydown', onKey);
  }, [privacyOpen, menuOpen]);

  useEffect(() => {
    if (reporting) closeRef.current?.focus();
  }, [reporting]);

  const statusText = typing
    ? 'typing…'
    : conversation.status === 'CLOSED'
      ? 'Conversation ended'
      : counterpart.online
        ? 'Online'
        : 'Offline';
  const who = counterpart.role === 'PROFESSIONAL' ? 'the professional' : 'the customer';

  const submitReport = async (e: React.FormEvent) => {
    e.preventDefault();
    setBusy(true);
    setReportError('');
    try {
      await onReport(reason, details);
      setReporting(false);
      setDetails('');
      setNotice('Thanks — your report was sent to our safety team.');
      window.setTimeout(() => setNotice(''), 6000);
    } catch (err: any) {
      setReportError(err.message || 'Unable to send your report');
    } finally {
      setBusy(false);
    }
  };

  return (
    <header className="relative border-b border-[var(--color-line)] bg-white px-3 sm:px-5 py-3">
      <div className="flex items-center gap-3">
        {onBack && (
          <button
            type="button"
            onClick={onBack}
            aria-label="Back to conversations"
            className="md:hidden -ml-1 p-2 rounded-xl text-gray-600 hover:bg-gray-100 cursor-pointer"
          >
            <ArrowLeft className="w-5 h-5" />
          </button>
        )}
        <Avatar name={counterpart.displayName} src={counterpart.avatar} online={counterpart.online && conversation.status === 'ACTIVE'} />
        <div className="min-w-0 flex-1">
          <h2 className="font-bold text-sm sm:text-base text-[var(--color-ink)] truncate font-['Outfit']" data-testid="chat-title">
            {counterpart.displayName}
          </h2>
          <p className="text-[11px] sm:text-xs text-gray-500 truncate">
            <span className="font-semibold text-[var(--color-brand-hover)]">{booking.categoryName}</span>
            <span aria-hidden="true"> · </span>
            <span title={`Booking ${booking.reference}`}>Booking {booking.reference}</span>
          </p>
          <p className={`text-[11px] ${typing || counterpart.online ? 'text-[var(--color-brand-hover)] font-medium' : 'text-gray-400'}`} aria-live="polite" data-testid="chat-presence">
            {statusText}
          </p>
        </div>

        <button
          type="button"
          onClick={() => {
            setPrivacyOpen((v) => !v);
            setMenuOpen(false);
          }}
          aria-expanded={privacyOpen}
          aria-controls="chat-privacy-panel"
          className="flex items-center gap-1.5 px-2.5 py-1.5 rounded-xl bg-[var(--color-brand-light)] text-[var(--color-brand-hover)] text-[11px] font-bold hover:bg-[#d7f1e4] cursor-pointer"
        >
          <Lock className="w-3.5 h-3.5" />
          <span className="hidden sm:inline">Private chat</span>
          <span className="sm:hidden sr-only">Privacy information</span>
        </button>

        <div className="relative">
          <button
            type="button"
            onClick={() => {
              setMenuOpen((v) => !v);
              setPrivacyOpen(false);
            }}
            aria-label="Conversation options"
            aria-haspopup="menu"
            aria-expanded={menuOpen}
            className="p-2 rounded-xl text-gray-500 hover:bg-gray-100 cursor-pointer"
          >
            <EllipsisVertical className="w-5 h-5" />
          </button>
          {menuOpen && (
            <>
              <div className="fixed inset-0 z-10" onClick={() => setMenuOpen(false)} aria-hidden="true" />
              <div role="menu" className="absolute right-0 mt-1 w-56 z-20 bg-white border border-[var(--color-line)] rounded-2xl shadow-xl p-1.5 text-xs">
                <button
                  role="menuitem"
                  type="button"
                  onClick={async () => {
                    setMenuOpen(false);
                    try {
                      await onSetBlocked(!conversation.blockedByMe);
                    } catch (e: any) {
                      alert(e.message);
                    }
                  }}
                  className="w-full flex items-center gap-2 px-3 py-2.5 rounded-xl hover:bg-[var(--color-brand-soft)] text-left font-medium cursor-pointer"
                >
                  <Ban className="w-4 h-4 text-gray-500" />
                  {conversation.blockedByMe ? `Unblock ${counterpart.displayName.split(' ')[0]}` : `Block ${counterpart.displayName.split(' ')[0]}`}
                </button>
                <button
                  role="menuitem"
                  type="button"
                  onClick={() => {
                    setMenuOpen(false);
                    setReporting(true);
                  }}
                  className="w-full flex items-center gap-2 px-3 py-2.5 rounded-xl hover:bg-red-50 text-left font-medium text-red-600 cursor-pointer"
                >
                  <Flag className="w-4 h-4" />
                  Report conversation
                </button>
              </div>
            </>
          )}
        </div>
      </div>

      {privacyOpen && (
        <div
          id="chat-privacy-panel"
          className="mt-3 rounded-2xl border border-[var(--color-line)] bg-[var(--color-brand-soft)] p-3.5 text-xs text-gray-700 space-y-2"
        >
          <div className="flex items-start justify-between gap-2">
            <p className="font-bold text-[var(--color-brand-dark)] flex items-center gap-1.5">
              <ShieldCheck className="w-4 h-4 text-[var(--color-brand)]" /> How this chat protects you
            </p>
            <button type="button" onClick={() => setPrivacyOpen(false)} aria-label="Close privacy information" className="text-gray-400 hover:text-gray-600 cursor-pointer">
              <X className="w-4 h-4" />
            </button>
          </div>
          <ul className="list-disc pl-5 space-y-1 leading-relaxed">
            <li>Your phone number and e-mail are never shown to {who}, and theirs are never shown to you.</li>
            <li>Messages are encrypted in transit (HTTPS) and encrypted again when stored.</li>
            <li>This is not end-to-end encrypted: Service Assist can open a conversation only when a report is filed, for safety review, and each review is logged.</li>
            <li>Only you and {who} on this booking can read it. Sharing phone numbers or e-mail here is blocked.</li>
          </ul>
        </div>
      )}

      {notice && (
        <p role="status" className="mt-3 rounded-xl bg-[var(--color-brand-light)] text-[var(--color-brand-dark)] text-xs px-3 py-2">
          {notice}
        </p>
      )}

      {reporting && (
        <div className="fixed inset-0 z-50 flex items-end sm:items-center justify-center p-0 sm:p-4" role="dialog" aria-modal="true" aria-labelledby="report-title">
          <div className="absolute inset-0 bg-black/40" onClick={() => !busy && setReporting(false)} />
          <form onSubmit={submitReport} className="relative w-full sm:max-w-md bg-white rounded-t-3xl sm:rounded-3xl p-5 shadow-2xl space-y-4">
            <div className="flex items-start justify-between">
              <div>
                <h3 id="report-title" className="font-bold text-base text-[var(--color-ink)] font-['Outfit']">Report this conversation</h3>
                <p className="text-xs text-gray-500 mt-0.5">Our safety team can review the messages once you submit this.</p>
              </div>
              <button ref={closeRef} type="button" onClick={() => setReporting(false)} aria-label="Close" className="p-1 text-gray-400 hover:text-gray-600 cursor-pointer">
                <X className="w-5 h-5" />
              </button>
            </div>
            <fieldset className="space-y-2">
              <legend className="sr-only">Reason</legend>
              {REPORT_REASONS.map((r) => (
                <label key={r.value} className={`flex items-start gap-2.5 p-2.5 rounded-xl border text-xs cursor-pointer ${reason === r.value ? 'border-[var(--color-brand)] bg-[var(--color-brand-soft)]' : 'border-gray-200'}`}>
                  <input type="radio" name="reason" value={r.value} checked={reason === r.value} onChange={() => setReason(r.value)} className="mt-0.5 accent-[#009051]" />
                  <span>{r.label}</span>
                </label>
              ))}
            </fieldset>
            <label className="block text-xs font-semibold text-gray-700">
              Details (optional)
              <textarea
                value={details}
                onChange={(e) => setDetails(e.target.value)}
                maxLength={1000}
                rows={3}
                className="mt-1 w-full p-3 bg-gray-50 border border-gray-200 rounded-xl text-base sm:text-xs font-normal focus:outline-none focus:ring-1 focus:ring-[var(--color-brand)]"
              />
            </label>
            {reportError && <p role="alert" className="text-xs text-red-600">{reportError}</p>}
            <div className="flex justify-end gap-2">
              <button type="button" onClick={() => setReporting(false)} className="px-4 py-2.5 text-xs font-semibold text-gray-600 rounded-xl hover:bg-gray-100 cursor-pointer">Cancel</button>
              <button type="submit" disabled={busy} className="px-5 py-2.5 text-xs font-bold text-white bg-[var(--color-brand)] hover:bg-[var(--color-brand-hover)] disabled:opacity-60 rounded-xl cursor-pointer">
                {busy ? 'Sending…' : 'Submit report'}
              </button>
            </div>
          </form>
        </div>
      )}
    </header>
  );
};
