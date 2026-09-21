import { io, Socket } from 'socket.io-client';

const API_URL = (((import.meta as any).env?.VITE_API_URL as string | undefined) || '/api').replace(/\/$/, '');

/**
 * Opens the authenticated chat WebSocket. Authentication is the HttpOnly session cookie the browser
 * already holds (the API only accepts that), so no token or user id is ever passed from JavaScript.
 * WebSocket-only, with automatic reconnection and back-off.
 */
export function createChatSocket(): Socket {
  const base = new URL(API_URL, window.location.origin);
  return io(base.origin, {
    path: `${base.pathname.replace(/\/$/, '')}/socket.io`,
    transports: ['websocket'],
    withCredentials: true,
    reconnection: true,
    reconnectionDelay: 1000,
    reconnectionDelayMax: 10000,
    randomizationFactor: 0.5,
  });
}
