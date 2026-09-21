import { Server } from 'socket.io';
import { parseCookie } from 'cookie';
import { z } from 'zod';
import { db } from '../config/db.js';
import { env, origins } from '../config/env.js';
import { authenticateToken } from '../middleware/auth.js';
import {
  acknowledgeDelivery,
  activeCounterpartId,
  presenceAudience,
} from '../services/chat.service.js';
import { hub, sessionRoom } from './hub.js';

export const SOCKET_PATH = '/api/socket.io';
const MAX_CONNECTIONS_PER_USER = 5;
const HANDSHAKES_PER_MINUTE = 60;
const EVENTS_PER_WINDOW = 40; // events accepted per 10 s per connection
const EVENTS_HARD_LIMIT = 120; // beyond this the connection is dropped
const SESSION_SWEEP_MS = 60_000;

const uuid = z.uuid();
const typingPayload = z.object({ conversationId: uuid, isTyping: z.boolean() });
const deliveredPayload = z.object({
  conversationId: uuid,
  messageIds: z.array(uuid).min(1).max(100),
});

// Client address for handshake throttling; only trusts X-Forwarded-For behind a configured proxy.
function clientIp(req) {
  if (env.TRUST_PROXY > 0) {
    const hops = String(req.headers['x-forwarded-for'] || '')
      .split(',')
      .map((v) => v.trim())
      .filter(Boolean);
    if (hops.length) return hops[Math.max(0, hops.length - env.TRUST_PROXY)];
  }
  return req.socket.remoteAddress || 'unknown';
}

function handshakeLimiter() {
  const seen = new Map();
  const sweep = setInterval(() => {
    const cutoff = Date.now() - 60_000;
    for (const [ip, times] of seen) {
      const fresh = times.filter((t) => t > cutoff);
      if (fresh.length) seen.set(ip, fresh);
      else seen.delete(ip);
    }
  }, 60_000);
  sweep.unref();
  return {
    allow(ip) {
      const now = Date.now();
      const times = (seen.get(ip) || []).filter((t) => t > now - 60_000);
      times.push(now);
      seen.set(ip, times);
      return times.length <= HANDSHAKES_PER_MINUTE;
    },
    stop: () => clearInterval(sweep),
  };
}

export function attachRealtime(httpServer) {
  const limiter = handshakeLimiter();
  const io = new Server(httpServer, {
    path: SOCKET_PATH,
    serveClient: false,
    // WebSocket only: no long-polling fallback, so there are no sticky sessions or extra HTTP surface.
    transports: ['websocket'],
    maxHttpBufferSize: 8 * 1024,
    pingInterval: 20_000,
    pingTimeout: 20_000,
    allowRequest(req, callback) {
      // Browsers do not apply CORS/SameSite protections to WebSocket upgrades, so the Origin must be
      // checked here (cross-site WebSocket hijacking). Cookie authentication requires an Origin.
      const origin = req.headers.origin;
      if (!origin || !origins.includes(origin)) return callback('Origin is not allowed', false);
      if (!limiter.allow(clientIp(req))) return callback('Too many connection attempts', false);
      callback(null, true);
    },
  });
  hub.attach(io);

  // Authenticate every connection from the HttpOnly session cookie, exactly like the REST API.
  io.use(async (socket, next) => {
    try {
      const token = parseCookie(socket.request.headers.cookie || '').session;
      const { user, sessionId, expiresAt } = await authenticateToken(token);
      if (!['CUSTOMER', 'PROFESSIONAL'].includes(user.role))
        throw new Error('Chat is not available');
      if (hub.connectionCount(user.id) >= MAX_CONNECTIONS_PER_USER)
        throw new Error('Too many open connections');
      // Identity comes from the verified session only; nothing about who the user is is client-supplied.
      socket.data = { user: { id: user.id, role: user.role }, sessionId, expiresAt };
      next();
    } catch (error) {
      next(new Error(error.status === 401 ? 'Unauthorized' : 'Connection refused'));
    }
  });

  io.on('connection', (socket) => {
    const { user, sessionId, expiresAt } = socket.data;
    socket.join([hub.userRoom(user.id), sessionRoom(sessionId)]);
    if (hub.connectionCount(user.id) === 1) announcePresence(user.id, true);

    let windowStart = Date.now();
    let events = 0;
    // Returns false when the event must be ignored (expired session or flooding).
    const admit = () => {
      if (expiresAt <= new Date()) {
        socket.disconnect(true);
        return false;
      }
      if (Date.now() - windowStart > 10_000) {
        windowStart = Date.now();
        events = 0;
      }
      events += 1;
      if (events > EVENTS_HARD_LIMIT) {
        socket.disconnect(true);
        return false;
      }
      return events <= EVENTS_PER_WINDOW;
    };
    const reply = (ack, body) => typeof ack === 'function' && ack(body);

    socket.on('typing', async (payload, ack) => {
      try {
        if (!admit()) return reply(ack, { ok: false, error: 'rate_limited' });
        const { conversationId, isTyping } = typingPayload.parse(payload);
        const counterpart = await activeCounterpartId(user, conversationId);
        if (!counterpart) return reply(ack, { ok: false, error: 'forbidden' });
        hub.emitToUser(counterpart, 'typing', { conversationId, isTyping });
        reply(ack, { ok: true });
      } catch {
        reply(ack, { ok: false, error: 'invalid' });
      }
    });

    socket.on('message:delivered', async (payload, ack) => {
      try {
        if (!admit()) return reply(ack, { ok: false, error: 'rate_limited' });
        const { conversationId, messageIds } = deliveredPayload.parse(payload);
        await acknowledgeDelivery(user, conversationId, messageIds);
        reply(ack, { ok: true });
      } catch (error) {
        reply(ack, { ok: false, error: error.status === 404 ? 'forbidden' : 'invalid' });
      }
    });

    socket.on('disconnect', () => {
      if (!hub.isOnline(user.id)) announcePresence(user.id, false);
    });
  });

  // Sessions can be revoked or expire while a socket stays open, so re-verify them periodically.
  const sweep = setInterval(async () => {
    try {
      const sessions = new Set([...io.sockets.sockets.values()].map((s) => s.data.sessionId));
      if (!sessions.size) return;
      const live = await db.session.findMany({
        where: { id: { in: [...sessions] }, expiresAt: { gt: new Date() } },
        select: { id: true },
      });
      const liveIds = new Set(live.map((s) => s.id));
      for (const id of sessions) if (!liveIds.has(id)) hub.disconnectSession(id);
    } catch {
      console.error(JSON.stringify({ event: 'socket_session_sweep_failed' }));
    }
  }, SESSION_SWEEP_MS);
  sweep.unref();

  return {
    io,
    async close() {
      clearInterval(sweep);
      limiter.stop();
      hub.detach();
      await io.close();
    },
  };
}

async function announcePresence(userId, online) {
  try {
    for (const { userId: audienceId, conversationId } of await presenceAudience(userId))
      hub.emitToUser(audienceId, 'presence', { conversationId, online });
  } catch {
    console.error(JSON.stringify({ event: 'presence_announce_failed' }));
  }
}
