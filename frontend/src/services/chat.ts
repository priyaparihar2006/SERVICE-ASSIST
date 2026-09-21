import { api } from './api';
import { ChatConversation, ChatMessage, ChatReportReason } from '../types';

// REST client for private chat. Nothing here is persisted in the browser: no localStorage,
// sessionStorage or IndexedDB, so signing out leaves no message data behind.
export interface MessagePage {
  messages: ChatMessage[];
  hasMore: boolean;
  nextBefore: string | null;
}

const json = (body: unknown) => JSON.stringify(body);

export const chatApi = {
  async list(): Promise<ChatConversation[]> {
    return (await api('/conversations?limit=100')).conversations;
  },
  async unread(): Promise<number> {
    return (await api('/conversations/unread')).unread;
  },
  async openForBooking(bookingId: string): Promise<ChatConversation> {
    return (await api('/conversations', { method: 'POST', body: json({ bookingId }) })).conversation;
  },
  async get(id: string): Promise<ChatConversation> {
    return (await api(`/conversations/${id}`)).conversation;
  },
  messages(id: string, before?: string): Promise<MessagePage> {
    const query = new URLSearchParams({ limit: '40' });
    if (before) query.set('before', before);
    return api(`/conversations/${id}/messages?${query}`);
  },
  async send(id: string, content: string, clientMessageId: string): Promise<ChatMessage> {
    return (
      await api(`/conversations/${id}/messages`, {
        method: 'POST',
        body: json({ content, clientMessageId }),
      })
    ).message;
  },
  markRead(id: string) {
    return api(`/conversations/${id}/read`, { method: 'PUT', body: '{}' });
  },
  deleteMessage(messageId: string) {
    return api(`/messages/${messageId}`, { method: 'DELETE', body: '{}' });
  },
  async setBlocked(id: string, blocked: boolean): Promise<ChatConversation> {
    return (await api(`/conversations/${id}/block`, { method: blocked ? 'PUT' : 'DELETE', body: '{}' }))
      .conversation;
  },
  report(id: string, reason: ChatReportReason, details: string) {
    return api(`/conversations/${id}/report`, {
      method: 'POST',
      body: json({ reason, details: details.trim() || undefined }),
    });
  },
};
