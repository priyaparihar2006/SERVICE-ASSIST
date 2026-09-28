import { io, Socket } from 'socket.io-client';
import { API_URL } from './api';

/**
 * Opens the authenticated chat WebSocket. Authentication is the HttpOnly session cookie the browser
 * already holds (the API only accepts that), so no token or user id is ever passed from JavaScript.
 * WebSocket-only, with automatic reconnection and back-off.
 */
export function createChatSocket(): Socket {
  const base = new URL(API_URL, window.location.origin);
  const path = `${base.pathname.replace(/\/$/, '')}/socket.io`;
  return io(base.origin, {
    path,
    transports: ['websocket', 'polling'],
    withCredentials: true,
    reconnection: true,
    reconnectionDelay: 1000,
    reconnectionDelayMax: 10000,
    randomizationFactor: 0.5,
  });
}
