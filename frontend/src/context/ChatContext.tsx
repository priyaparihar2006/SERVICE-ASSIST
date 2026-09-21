import React, { createContext, useCallback, useContext, useEffect, useRef, useState } from 'react';
import type { Socket } from 'socket.io-client';
import { useAuth } from './AuthContext';
import { chatApi } from '../services/chat';
import { createChatSocket } from '../services/chatSocket';

export type ChatConnection = 'disconnected' | 'connecting' | 'connected' | 'reconnecting';
type Handler = (payload: any) => void;

// Server events fanned out to whichever screen is open, plus a synthetic 'connected' event fired on
// every (re)connect so screens can re-sync anything they missed while offline.
const SERVER_EVENTS = ['message:new', 'message:status', 'message:deleted', 'typing', 'presence', 'conversation:updated'];

interface ChatContextType {
  connection: ChatConnection;
  unreadTotal: number;
  refreshUnread: () => void;
  subscribe: (event: string, handler: Handler) => () => void;
  emit: (event: string, payload: unknown) => void;
}

const ChatContext = createContext<ChatContextType | undefined>(undefined);

export const ChatProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const { user } = useAuth();
  const enabled = !!user && user.role !== 'ADMIN';
  const [connection, setConnection] = useState<ChatConnection>('disconnected');
  const [unreadTotal, setUnreadTotal] = useState(0);
  const socketRef = useRef<Socket | null>(null);
  const listeners = useRef(new Map<string, Set<Handler>>());
  const unreadInFlight = useRef(false);

  const dispatch = useCallback((event: string, payload?: unknown) => {
    listeners.current.get(event)?.forEach((handler) => handler(payload));
  }, []);

  const refreshUnread = useCallback(() => {
    if (!enabled || unreadInFlight.current) return;
    unreadInFlight.current = true;
    chatApi
      .unread()
      .then(setUnreadTotal)
      .catch(() => {})
      .finally(() => {
        unreadInFlight.current = false;
      });
  }, [enabled]);

  useEffect(() => {
    if (!enabled) {
      setConnection('disconnected');
      setUnreadTotal(0);
      return;
    }
    const socket = createChatSocket();
    socketRef.current = socket;
    setConnection('connecting');
    socket.on('connect', () => {
      setConnection('connected');
      refreshUnread();
      dispatch('connected');
    });
    socket.on('disconnect', () => setConnection(socket.active ? 'reconnecting' : 'disconnected'));
    socket.on('connect_error', (error) => {
      // A rejected session will not recover by retrying; other failures keep trying with back-off.
      if (error.message === 'Unauthorized') socket.disconnect();
      setConnection(socket.active ? 'reconnecting' : 'disconnected');
    });
    for (const event of SERVER_EVENTS)
      socket.on(event, (payload) => {
        dispatch(event, payload);
        if (event === 'message:new' || event === 'conversation:updated' || event === 'message:deleted')
          refreshUnread();
      });
    refreshUnread();
    return () => {
      // Signing out (or switching account) tears the connection down and forgets all chat state.
      socket.removeAllListeners();
      socket.disconnect();
      socketRef.current = null;
      setConnection('disconnected');
      setUnreadTotal(0);
    };
  }, [enabled, user?.id, dispatch, refreshUnread]);

  const subscribe = useCallback((event: string, handler: Handler) => {
    const set = listeners.current.get(event) ?? new Set<Handler>();
    set.add(handler);
    listeners.current.set(event, set);
    return () => {
      set.delete(handler);
    };
  }, []);

  const emit = useCallback((event: string, payload: unknown) => {
    const socket = socketRef.current;
    if (socket?.connected) socket.emit(event, payload);
  }, []);

  return (
    <ChatContext.Provider value={{ connection, unreadTotal, refreshUnread, subscribe, emit }}>
      {children}
    </ChatContext.Provider>
  );
};

export const useChat = () => {
  const context = useContext(ChatContext);
  if (!context) throw new Error('ChatProvider is required');
  return context;
};
