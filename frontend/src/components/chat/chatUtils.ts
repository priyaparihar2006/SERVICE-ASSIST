import { ChatSendBlockedReason } from '../../types';

const sameDay = (a: Date, b: Date) => a.toDateString() === b.toDateString();

export const formatClock = (iso: string) =>
  new Date(iso).toLocaleTimeString('en-IN', { hour: 'numeric', minute: '2-digit' });

export function dayLabel(iso: string): string {
  const date = new Date(iso);
  const now = new Date();
  if (sameDay(date, now)) return 'Today';
  const yesterday = new Date(now);
  yesterday.setDate(now.getDate() - 1);
  if (sameDay(date, yesterday)) return 'Yesterday';
  return date.toLocaleDateString('en-IN', {
    weekday: 'short',
    day: 'numeric',
    month: 'short',
    year: date.getFullYear() === now.getFullYear() ? undefined : 'numeric',
  });
}

export function listTime(iso: string | null): string {
  if (!iso) return '';
  const date = new Date(iso);
  const now = new Date();
  if (sameDay(date, now)) return formatClock(iso);
  const yesterday = new Date(now);
  yesterday.setDate(now.getDate() - 1);
  if (sameDay(date, yesterday)) return 'Yesterday';
  return date.toLocaleDateString('en-IN', { day: 'numeric', month: 'short' });
}

export const initials = (name: string) =>
  name
    .split(/\s+/)
    .filter(Boolean)
    .slice(0, 2)
    .map((part) => part[0]?.toUpperCase())
    .join('') || '?';

export function blockedReasonText(reason: ChatSendBlockedReason | null): string {
  switch (reason) {
    case 'CONVERSATION_CLOSED':
      return 'This conversation has ended because the booking was reassigned.';
    case 'BOOKING_CLOSED':
      return 'This booking is finished, so the chat is now read-only.';
    case 'BLOCKED_BY_YOU':
      return 'You blocked this person. Unblock them to send messages.';
    case 'UNAVAILABLE':
      return 'Messages cannot be sent in this conversation.';
    default:
      return '';
  }
}
