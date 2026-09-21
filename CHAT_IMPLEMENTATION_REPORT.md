# Private chat: implementation report

Customers and their assigned professionals can now message each other about a booking, in real time, without either side seeing the other's phone number or e-mail address. Frontend and backend stay separate (`frontend/`, `backend/`); the existing UI was extended, not rebuilt.

> **Security claim, stated plainly:** the chat is encrypted in transit (TLS) and encrypted at rest (AES-256-GCM). It is **not end-to-end encrypted**: the server can decrypt messages. It should not be described to users as E2EE.

## 1. Chat architecture

- **REST for actions, WebSocket for pushes.** Messages are sent with `POST /api/conversations/:id/messages`, so they share the API's CSRF/Origin checks, validation, idempotency and rate limits. Socket.IO (WebSocket transport only) delivers `message:new`, delivery/read status, typing, presence and conversation changes.
- **One conversation per booking and assigned professional.** Access is derived from the booking relationship and stored as `ConversationParticipant` rows. If the professional is unassigned/rejected, the thread closes in the same DB transaction and that professional loses all access; a newly assigned professional gets a fresh thread with no history.
- **Identity is never client-supplied.** REST uses the existing HttpOnly cookie session; the WebSocket handshake verifies the same cookie (JWT + DB session) and an allowed `Origin`. Every socket event is schema-validated and re-authorised against the database.
- **Frontend:** `ChatProvider` owns the socket (created after login, destroyed on logout), `useConversation` handles optimistic sends, retries, receipts, typing and resync, and `MessagesPage` (`/messages`) shows inbox + thread (two panes on desktop, one at a time on mobile). Entry points: "Chat with Professional" (customer bookings), "Chat with Customer" (professional jobs), and a Messages icon with unread badge in the navbar. No chat data is stored in browser storage.

## 2-4. Encryption architecture, E2EE status, limitations

**Decision: Option B (encryption in transit + at rest), not Option A.** True E2EE needs a way to authenticate public keys independently of the server that ships the JavaScript, multi-device key sync and lost-key recovery, and it would make the required admin safety review and "report conversation" impossible. Doing it half-way would be worse than not claiming it.

```text
master key (CHAT_ENCRYPTION_KEYS: versioned, env/secret manager)
  -> wraps a random 256-bit data key per conversation (Conversation.wrappedKey)
       -> AES-256-GCM per message (fresh 96-bit IV), AAD = conversationId|messageId|senderId
```

**Genuinely end-to-end encrypted? No.**

Provides: message text unreadable to anyone with only the database/backups/replicas; tamper detection and binding of every message to its conversation/message/sender; erasure of ciphertext on delete; master-key rotation without re-encrypting messages; TLS/WSS when deployed behind HTTPS.

Does not provide: protection from anyone who has both the DB and the master key or can run the API process; forward secrecy (a conversation key is static); metadata protection (who/when/size); retention control (backups may retain deleted ciphertext; no retention job exists); protection against a compromised client device.

## 5. Database schema changes (migration `20260921120000_private_chat`)

New models: `Conversation` (booking, status, `activeKey` uniqueness trick, wrapped key + key version, `lastMessageAt`), `ConversationParticipant` (role, `joinedAt`, `leftAt`, `lastReadAt`), `Message` (ciphertext `Bytes?`, `encryptionVersion`, `messageType`, `clientMessageId`, `deletedAt`), `MessageReceipt` (`deliveredAt`, `readAt`), `BlockedUser`, `ChatReport`, `ChatAccessLog`. Enums: `ConversationStatus`, `MessageType`, `ChatReportReason`, `ChatReportStatus`. Relations added to `User` and `Booking`. Foreign keys everywhere (Restrict, or Cascade for owned children); indexes for conversation/message/receipt/participant lookups; the unique constraint `(conversationId, senderId, clientMessageId)` makes retries idempotent.

Integrity in SQL: a conversation is ACTIVE exactly when `activeKey = bookingId` (one active thread per booking); ciphertext exists iff the message is not deleted and is at least 29 bytes; participants are customers/professionals; no self-blocks; a report is OPEN iff `openKey` is set (one open report per reporter per thread); `ChatAccessLog` is append-only via trigger. No plaintext message, password or contact detail is stored by this feature.

## 6-7. Files

**Created (backend):** `src/services/chat.service.js`, `chatAdmin.service.js`, `chatCrypto.js`, `contactGuard.js`; `src/routes/chat.routes.js`; `src/realtime/socket.js`, `hub.js`; `scripts/rotate-chat-keys.js`; `prisma/migrations/20260921120000_private_chat/migration.sql`; `test/chat.test.js`.
**Created (frontend):** `src/pages/MessagesPage.tsx`; `src/components/chat/{Avatar,ChatHeader,ChatInbox,ChatWindow,MessageBubble,MessageComposer}.tsx`, `chatUtils.ts`; `src/hooks/useConversation.ts`; `src/context/ChatContext.tsx`; `src/services/chat.ts`, `chatSocket.ts`; `tests/chat.spec.js`.
**Created (root):** this report.

**Modified (backend):** `prisma/schema.prisma`; `src/app.js` (mount, error `code`); `src/server.js` (HTTP server + Socket.IO, graceful shutdown); `src/config/env.js` (chat keys, validation); `src/middleware/auth.js` (shared `authenticateToken`); `src/controllers/auth.controller.js` (revoke sockets on logout/password change); `src/services/booking.service.js` (close chat when the professional changes); `src/utils/serializers.js` (**phone/e-mail only for admins**); `scripts/browser-tests.js` (spec filter); `package.json`/lock (`socket.io`, `cookie`, dev `socket.io-client`, `--test-concurrency=1`); `.env.example`; `README.md`.
**Modified (frontend):** `src/App.tsx` (provider, `/messages` route, no footer/AI widget on chat); `src/components/common/Navbar.tsx` (Messages icon; header compaction on phones/tablets); `src/components/common/BrandLogo.tsx` (smaller wordmark on very narrow screens); `src/pages/CustomerDashboardPage.tsx`, `ProfessionalDashboardPage.tsx` (**"Call" buttons and the customer phone number removed; chat added**); `src/services/api.js` (error `status`/`code`); `src/types.ts`; `vite.config.ts` (`ws: true`, `API_PROXY_TARGET`); `package.json`/lock (`socket.io-client`); `README.md`. Root `README.md` links the docs.

## 8. API endpoints

`GET /conversations`, `GET /conversations/unread`, `POST /conversations {bookingId}`, `GET /conversations/:id`, `GET|POST /conversations/:id/messages`, `PUT /conversations/:id/read`, `DELETE /messages/:id`, `PUT|DELETE /conversations/:id/block`, `POST /conversations/:id/report`; admin: `GET /admin/chat/reports`, `POST /admin/chat/reports/:id/messages`, `PUT /admin/chat/reports/:id`, `GET /admin/chat/access-log`. Full details, limits and payloads: [backend/README.md#private-chat](backend/README.md#private-chat).

## 9. WebSocket events

Path `/api/socket.io`. Server to client: `message:new`, `message:status`, `message:deleted`, `typing`, `presence`, `conversation:updated`. Client to server: `typing`, `message:delivered` (both acknowledged). Table in the backend README.

## 10. Security measures

- **Backend-enforced privacy:** phone/e-mail removed from every booking payload for customers and professionals (the pre-existing `bookingView` leaked them to both parties); chat queries select display fields only; payloads carry no user ids; message text that looks like a phone number or e-mail is rejected (best effort).
- **Access control:** one membership check for every conversation/message operation; non-members get a `404` identical to "doesn't exist"; admins are refused on participant endpoints; sender taken from the session; UUID-validated ids; idempotent sends.
- **Admin policy:** report-gated, written justification, newest 500 messages, audited in an append-only table before content is returned, ends when the report closes; messages under an open report cannot be deleted.
- **Transport/session:** WebSocket handshake requires a valid cookie session and an allowed Origin (CSWSH), per-IP handshake limit, 5 connections per user, per-connection event limits, disconnect on logout/password change, periodic session sweep; WebSocket-only transport; `Cache-Control: no-store`; existing CSRF (Origin + JSON) applies to sends.
- **Rate limits per account:** 30 sends/min, 240 chat requests/min, 5 reports/hour, plus the global per-IP limit.
- **Input handling:** zod validation, 2000-character limit, control and bidirectional-override characters rejected, content rendered as plain text only (no HTML).
- **Logging:** message text and contact data are never logged (asserted by a test).
- **Secrets:** master keys only in env/secret manager; not in the frontend or the repo (`.env.example` holds a placeholder). `npm audit` reported 0 vulnerabilities for the new dependencies.

## 11. Testing results

| Suite | Result |
|---|---|
| Backend `npm test` (real PostgreSQL + real HTTP + real WebSockets) | **48/48 pass**: 25 pre-existing + 23 new (22 chat scenarios + parent) |
| Browser (Edge via Playwright, separate customer/professional/outsider/admin accounts) | **8/8 pass**: 5 pre-existing + 3 new (desktop flow, outsider denial, mobile 375px) |
| Frontend `tsc --noEmit` and `vite build` | pass (existing bundle-size advisory) |
| Mutation checks (temporarily broke a control, confirmed tests fail) | phone leak reverted -> failed; WebSocket Origin check removed -> failed; reassignment revocation removed -> failed |

Covered: booking APIs hide the other party's phone/e-mail (admin still sees them); conversation and message access by wrong customer, wrong professional, admin, unauthenticated and guessed ids; forged sender ignored; validation, retries and rate limits; contact-info refusal; ciphertext at rest, bit-flip and cross-row swap detection, DB constraints; delivery and read receipts, unread counts, paging; WebSocket refusal (no/forged cookie, wrong/missing Origin, admin, connection cap, floods); real-time delivery/typing/presence reaching only the right sockets; logout and password change revoking sockets; delete, block, report and the audited admin flow; reassignment; read-only after completion; key rotation with a process that knows only the new key; SQL/HTML payloads stored inertly; no plaintext or phone numbers in logs. In the browser: live delivery and read ticks between two real sessions, typing indicator, online status, contact-info error with edit, privacy panel, deletion, XSS payload shown as text, secure logout, offline send with safe retry, and no horizontal scroll or overlap on a 375px phone.

Bugs the tests found and I fixed: stuck "Opening your chat" after a deep link; the floating AI widget covering the Send button; a pre-existing navbar overflow on signed-in phones (worsened by the new icon).

## 12. Environment variables

New (backend): `CHAT_ENCRYPTION_KEYS` (required, `version:base64(32 bytes)`, comma-separated), `CHAT_ACTIVE_KEY_VERSION` (required), `CHAT_BLOCK_CONTACT_INFO` (default `true`). Dev only (frontend Vite): `API_PROXY_TARGET`. Existing `FRONTEND_ORIGINS` now also gates WebSocket handshakes.

## 13. Local setup

```powershell
cd backend
npm ci
# add to backend/.env:  CHAT_ENCRYPTION_KEYS=1:<output of the generator>   CHAT_ACTIVE_KEY_VERSION=1
node -e "console.log('1:'+require('crypto').randomBytes(32).toString('base64'))"
npm run db:generate     # stop a running API first on Windows if the engine file is locked
npm run db:migrate
npm run dev
cd ../frontend; npm ci; npm run dev
```

Then sign in as a customer with an assigned booking and click "Chat with Professional"; sign in as that professional in a second browser profile to reply. Tests: `npm test` (backend, needs the `_test` database), and `npm run test:browser -- tests/chat.spec.js` from `backend/` with both servers running. If the auth rate limiter (30/15 min per IP) trips during repeated browser runs, restart the API.

## 14. Production requirements

HTTPS everywhere with WebSocket upgrade forwarded for `/api/socket.io` and generous idle timeouts; master keys in a secret manager with a separate backup (losing a key version makes those conversations unreadable) and a rotation routine; exact `FRONTEND_ORIGINS`; database and backup encryption and access restriction; no request-body logging on chat routes in any proxy/APM/WAF; a shared Socket.IO adapter (Redis) and shared presence/rate-limit stores before running more than one API instance; frontend CSP `connect-src` for the API's `https:`/`wss:` origin.

## 15. Remaining security and product concerns

- Not end-to-end encrypted; operators with DB + key access, or the running API, can read messages. No forward secrecy or metadata protection.
- Contact-info filtering is best effort and can be evaded (spelled-out digits, images, other channels). Administrators still see phone numbers and e-mail for support.
- No message retention/erasure policy or job; backups may keep deleted ciphertext. A written retention policy and a legal/privacy review of admin access are needed.
- Attachments are not implemented (they need access-controlled storage, type/size/malware validation and signed URLs). No push/e-mail/SMS alerts, only in-app notifications without message text.
- No admin UI for chat reports (API only). No automated abuse detection beyond user reports.
- In-memory presence and rate limits are single-instance only. WebSocket-only transport needs an environment that allows WebSockets; without one the client falls back to polling.
- Narrow phones (about 360px and below) still have a few pixels of pre-existing header overflow.
- I did not run a third-party penetration test; the audit above is my own testing and code review.
- Your running dev API reloaded automatically with the new code; I did not touch your servers. Migration `20260921120000_private_chat` was applied to the dev and test databases, and `.env` gained `CHAT_ENCRYPTION_KEYS` for local use only.
