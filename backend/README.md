# Service Assist API

This is a JavaScript Express API backed by PostgreSQL and Prisma. It runs independently of the existing React/Vite frontend. No database code or secrets belong in `frontend/`.

## Local setup

1. Install Node.js 22 or newer and PostgreSQL. Create an empty database named `service_assist` and a database user with access to it.
2. From this folder, run `npm ci`.
3. On a fresh checkout, copy `.env.example` to `.env` (keep an existing configured `.env`). Set `DATABASE_URL` to your database connection string. Generate a JWT secret using `node -e "console.log(require('crypto').randomBytes(48).toString('hex'))"` and put the result in `JWT_SECRET`.
4. Run `npm run db:generate`, then `npm run db:migrate`. Migrations create tables, foreign keys, indexes, checks and relationship validation triggers.
5. For a **development database only**, set `SEED_DEMO=true` and choose a `DEMO_PASSWORD` of 12–72 UTF-8 bytes. Run `npm run db:seed`. Repeated runs insert missing records without overwriting existing data. Seeded professionals use `professional1@demo.service-assist.test` through `professional5@demo.service-assist.test` and the configured password. Their names are marked Demo, their users have `isDemo=true`, and their ratings start at zero.
6. Run `npm run dev`. The API listens on port 5000. `GET /api/health` checks database connectivity.
7. Register customers through the UI. To create an administrator, set `ADMIN_EMAIL` and `ADMIN_PASSWORD` in the backend environment, run `npm run admin:create`, then remove those two values. Public registration never permits administrators and the CLI never promotes an existing account.

On Windows, stop a running API before regenerating Prisma Client. A running Node process holds Prisma's native DLL open; this can cause an EPERM rename error until that API process exits.

The development database created during implementation is an isolated PostgreSQL cluster in ignored `.pgdata/`, bound to `127.0.0.1:55432`. It does not alter the installed PostgreSQL service. Its local-only trust authentication is for this workstation, **not a deployment configuration**. `.env` contains generated local secrets and must remain ignored.

For a new PostgreSQL installation, open pgAdmin, connect to your local server using the administrator password chosen during installation, select the `postgres` database, and open Tools → Query Tool. Run these statements separately, replacing the example password before use:

```sql
CREATE USER service_assist WITH PASSWORD 'replace-with-a-strong-local-password';
CREATE DATABASE service_assist OWNER service_assist;
CREATE DATABASE service_assist_test OWNER service_assist;
```

Use `postgresql://service_assist:YOUR_URL_ENCODED_PASSWORD@localhost:5432/service_assist?schema=public` for DATABASE_URL. URL-encode special characters in the password, or use the exact connection URL supplied by your database host. For integration tests use the same pattern with `service_assist_test` as TEST_DATABASE_URL. Keep both URLs in the backend environment only. Run migrations once against each database; the normal API should point to `service_assist`, and the tests select TEST_DATABASE_URL themselves.

To restart that local cluster on this Windows workstation:

```powershell
& 'C:\Program Files\PostgreSQL\18\bin\pg_ctl.exe' -D "$PWD\.pgdata" -l "$PWD\.pglog" -o '-p 55432 -h 127.0.0.1' start
```

Stop it using the same `pg_ctl.exe -D "$PWD\.pgdata" stop -m fast`. These commands assume your terminal is in `backend/`.

## Architecture

`config/` validates environment settings and owns the Prisma client. `routes/` binds REST endpoints, role checks and request schemas. `controllers/` implements authentication request/response handling. `services/` owns authentication, catalog queries and transactional booking rules. `middleware/` handles identity, permissions and input validation. `utils/` provides explicit public serializers and errors. `app.js` configures HTTP middleware, CSRF/origin checks, CORS, rate limiting and errors; `server.js` owns startup and graceful shutdown.

The schema contains User, Session, Professional, Availability, Category, Service, ServiceVariant, Address, Booking, BookingItem, BookingHistory, Payment, Review, Notification, Offer and SupportTicket. Bookings reference their customer, address, primary service and optional professional. Items retain purchased names/prices and address snapshots preserve booking history. Reviews must reference a completed booking belonging to the same customer and professional. Payments are unique per booking and their amount must match the booking total.

Money uses PostgreSQL Decimal columns; booking totals are calculated from database package prices, never client totals. Date/time selection is interpreted in Asia/Kolkata. New bookings are pending until an admin assigns a verified professional with matching services, service area and working hours. Row locks serialize competing assignments and duplicate submissions.

## Environment

| Variable | Meaning |
|---|---|
| `DATABASE_URL` | Backend-only PostgreSQL URL, including credentials and required provider TLS options |
| `JWT_SECRET` | Random secret, at least 32 characters; never a frontend variable |
| `PORT` | API port; default 5000 |
| `NODE_ENV` | development, test or production |
| `FRONTEND_ORIGINS` | Comma-separated exact browser origins allowed to call the API (also gates the chat WebSocket). Validated at startup: no `*`, path or query; a trailing slash is normalised. Optional in development, where it defaults to `http://localhost:5173,http://127.0.0.1:5173` (a browser treats `localhost` and `127.0.0.1` as different origins); **required in production**, where the API refuses to start without it |
| `COOKIE_SAME_SITE` | lax by default; none requires HTTPS and may still be blocked by third-party-cookie policies |
| `TRUST_PROXY` | Exact number of trusted reverse-proxy hops; default 0 |
| `SEED_DEMO`, `DEMO_PASSWORD` | Explicit non-production demo seed opt-in and password |
| `ADMIN_EMAIL`, `ADMIN_PASSWORD` | One-time administrator creation inputs |
| `TEST_DATABASE_URL` | Dedicated test database URL whose database name ends in `_test` |
| `CHAT_ENCRYPTION_KEYS`, `CHAT_ACTIVE_KEY_VERSION`, `CHAT_BLOCK_CONTACT_INFO` | Private-chat encryption keys and contact-info policy; see [Private chat](#private-chat) |
| `GEMINI_API_KEY`, `GEMINI_MODEL` | Optional server-only HomeAI configuration; unconfigured requests return 503 |

Authentication uses a one-day JWT in an HttpOnly cookie and a database session. Logout and password changes invalidate sessions. Production cookies use Secure. Cookie-authenticated mutations require an allowed Origin and JSON content type. The browser API client sends credentials. Do not add wildcard CORS origins.

## API reference

All paths below start with `/api`. Successful collection responses use named arrays (`services`, `bookings`, etc.). Errors use `{ error, details? }`. Paginated catalog, professional and booking endpoints accept `page` and `limit` (maximum 100) and return pagination metadata. Other large lists accept those parameters too.

| Methods and paths | Access / purpose |
|---|---|
| GET `/health` | Database readiness |
| POST `/auth/register`, `/professionals/register` | Customer/professional registration; professional profiles await verification |
| POST `/auth/login` | Email and password |
| GET `/auth/me` | Current authenticated account and addresses |
| POST `/auth/logout` | Revoke current session |
| PUT `/auth/profile` | Update name, phone, profile image |
| POST `/auth/change-password` | Current password plus new password; revokes all sessions |
| GET `/categories`, `/categories/:slug` | Active categories and real counts |
| GET `/services` | `category` (ID/slug), `categoryId`, `search`, `location`/`city`, `sort`, pagination |
| GET `/services/:id`, `/services/slug/:slug` | Active service and packages; legacy slug in `:id` also supported |
| GET `/search?q=` | Catalog text search |
| POST `/services`, PUT/DELETE `/services/:id` | Admin create/update/soft-delete |
| PUT `/services/:id/variants/:variantId` | Admin package price/name/duration; updates starting price |
| GET/POST `/addresses` | Current user's persisted addresses |
| POST `/auth/addresses` | Compatibility alias for address creation |
| PUT `/addresses/:id/default` | Current user's default address |
| POST `/bookings` | Customer; requires UUID `Idempotency-Key` header |
| GET `/bookings`, `/bookings/:id` | Role-scoped history/detail; caller-supplied owner filters cannot widen access |
| PUT `/bookings/:id/cancel` | Cancel within allowed state/role rules |
| PUT/PATCH `/bookings/:id/status` | Role/state-controlled transitions; assignment requires professionalId, start requires otp |
| GET `/professionals`, `/professionals/:id` | Public verified profiles; no account emails or passwords |
| GET/PUT `/professionals/profile` | Own business details, services, areas and accepting-work flag |
| PUT `/professionals/availability` | Own weekly windows `{availability:[{dayOfWeek,startMinute,endMinute}]}` |
| GET `/professionals/bookings` | Own assignments |
| PUT `/professionals/bookings/:id/status` | Accept, reject and progress own assignment |
| GET `/professionals/earnings` | Actual collected payments for completed assignments, before any settlement deductions |
| GET `/offers`, `/coupons` | Active vouchers |
| POST `/coupons/validate` | `{code, items:[{serviceId, variantId?, quantity}]}` -> `{valid, coupon, subtotal, discount, taxes, total, message}`. Public (a signed-in user also gets the per-customer usage check). The **server** prices the cart from database prices and calculates the discount; amounts or discounts in the request are ignored. Rate limited to 30 attempts/minute per IP. Checkout re-validates and re-prices everything |
| GET/POST `/reviews` | Public reviews / customer's completed booking review |
| GET `/notifications`; PATCH `/notifications/:id/read` | Own notifications |
| GET/POST `/support/tickets` | Own tickets; admins can list all |
| POST `/support/ticket` | Compatibility alias for ticket creation |
| GET `/admin/metrics`, `/admin/users`, `/admin/professionals` | Admin platform data |
| GET `/admin/services` | Admin catalog including inactive services, with pagination |
| GET/POST `/admin/categories`; PUT/DELETE `/admin/categories/:id` | Category management; deletion is deactivation |
| PUT `/admin/professionals/:id/verification` | Verify, reject or return to pending |
| PUT `/admin/bookings/:id/payment` | Record collected cash on completed booking with unique transaction/receipt reference |
| PUT `/admin/support/tickets/:id` | Update support status |
| POST `/ai/chat` | Optional configured AI; explicit unavailable response without provider |

Status sequence: `PENDING → ASSIGNED → CONFIRMED → ON_THE_WAY → ARRIVED → IN_PROGRESS → COMPLETED`. Assigned professionals can reject back to PENDING. Customers can cancel only pending, assigned or confirmed bookings. Administrators can cancel before arrival. Terminal bookings cannot be reopened. OTP is returned only to the customer, checked on the server and limited to five failed attempts.

Example booking body:

```json
{
  "addressId": "saved-address-id",
  "items": [{ "serviceId": "srv-ac-foamjet", "variantId": "var-split-1", "quantity": 1 }],
  "bookingDate": "YYYY-MM-DD",
  "bookingTime": "10:00 AM - 11:00 AM",
  "paymentMethod": "CASH"
}
```

Reuse the same UUID idempotency key for retries of one checkout. A new checkout needs a new key. Requests allow up to 10 items and 8 hours total duration; use a future slot within 90 days. The slot identifies the start window; stored package duration determines the reserved end time. Multi-service carts require an assigned professional qualified for every item; split the cart if separate professionals are needed.

## Private chat

Customers and their assigned professionals can message each other about a booking without either side ever seeing the other's phone number or e-mail address. Code lives in `src/services/chat*.js`, `src/routes/chat.routes.js` and `src/realtime/`.

### Who can talk to whom

- A conversation belongs to **one booking and one assigned professional**. `POST /conversations {bookingId}` succeeds only for that booking's customer or its currently assigned professional, and only while a professional is assigned and the job is `ASSIGNED`, `CONFIRMED`, `ON_THE_WAY`, `ARRIVED` or `IN_PROGRESS`.
- When the job is `COMPLETED` or `CANCELLED` the thread stays readable but becomes read-only (`sendBlockedReason: BOOKING_CLOSED`).
- If the professional is rejected/unassigned (`PENDING`) the thread is closed in the same database transaction: the previous professional loses **all** access immediately (`leftAt`), a newly assigned professional gets a **fresh** thread with no history, and the customer keeps the old one read-only.
- Every read and write goes through one membership check. A non-member receives the same `404` as a non-existent id, so conversation ids cannot be probed. Ids are UUIDs, but authorisation never relies on their secrecy. The sender of a message is always the authenticated user; body fields such as `senderId` are ignored.
- Administrators **cannot** use the participant endpoints (403).

### Phone-number privacy

- No chat query selects `phone` or `email`. Conversation/message payloads contain display name, avatar, role, service category, booking reference and status only, with no user ids.
- `bookingView` (used by every booking endpoint) previously returned `customerPhone`, `userPhone`, `userEmail` and `professionalPhone` to both parties. It now returns them **only to administrators**. `/auth/me` and profile updates return the caller's own phone only. Catalog/professional endpoints never included phone numbers. Admin dashboards keep contact details for support and safety work.
- Message text that looks like a phone number (ten or more digits, allowing separators) or an e-mail address is rejected with `422 CONTACT_INFO_NOT_ALLOWED` (`CHAT_BLOCK_CONTACT_INFO=true`, the default). This is a best-effort policy aid: spelled-out digits or images can evade it.
- Nothing is written to browser storage; the socket and all chat state are torn down on logout.
- The server logs event names and ids only, never message text or contact data. A test asserts that message text and phone numbers never reach the process log.

### Encryption: what it is and is not

**This chat is NOT end-to-end encrypted.** It uses TLS in transit plus authenticated encryption at rest, and the server can decrypt messages.

Why not end-to-end: a browser app has no independent channel to authenticate users' public keys (the same server that hosts the JavaScript would also distribute the keys, so a compromised server could substitute keys), multi-device sync and lost-key recovery need a designed protocol, and the required admin safety review and "report conversation" flows cannot work if nobody but the two participants can ever read a thread. Adopting a reviewed protocol (e.g. Signal/MLS) later is possible but is a separate project.

What is implemented (Node `crypto`, no custom primitives):

```text
master key (CHAT_ENCRYPTION_KEYS, versioned, held in env / secret manager)
  -> wraps a random 256-bit data key per conversation (Conversation.wrappedKey)
       -> AES-256-GCM per message, fresh random 96-bit IV
```

- Message text is never stored in plaintext. Only `iv || tag || ciphertext` is stored (`Message.encryptedContent`, `encryptionVersion`).
- GCM additional authenticated data binds each ciphertext to its `conversationId`, `messageId` and `senderId`; the wrapped key is bound to its conversation and key version. Bit-flips, and ciphertext copied to another message/conversation/sender, fail authentication (tested).
- Deleting a message erases its ciphertext (`encryptedContent = NULL`; a database `CHECK` keeps this consistent).
- Rotation: add a new version to `CHAT_ENCRYPTION_KEYS`, set `CHAT_ACTIVE_KEY_VERSION`, run `npm run chat:rotate-keys`, then retire the old version once it reports no conversations on it. Only 60-byte wrapped keys are rewritten, never message ciphertext (tested with a process that knows only the new key).

Provided: confidentiality of message text against database/backup/replica readers who lack the master key, integrity and binding of stored messages, encryption in transit when deployed behind HTTPS/WSS.

**Not** provided: protection from anyone who can read both the database and the master key, or run the API process (operators, a compromised server); forward secrecy (a conversation key is static for that conversation); protection of metadata (who talked to whom, when, message sizes); protection after a device is compromised; retention control (backups may keep deleted ciphertext, and no automatic retention window is enforced).

### Administrator access policy

- Admins cannot browse conversations. Message text is available **only while a participant's report on that conversation is open** (`POST /admin/chat/reports/:id/messages`), returns at most the newest 500 messages, and requires a written justification (15+ characters).
- Every access is written to `ChatAccessLog` (admin, report, conversation, justification, message count, time) **before** any content is returned. A database trigger makes that table append-only; the log is readable at `GET /admin/chat/access-log`.
- Resolving/dismissing the report ends access. While a report is open, its conversation's messages cannot be deleted, so evidence is preserved. Admin-facing UI for reports is not built yet; the API is complete.

### REST endpoints (all under `/api`; customers and professionals unless noted)

| Method and path | Purpose |
|---|---|
| GET `/conversations?page&limit` | My conversations with unread counts and last-message previews |
| GET `/conversations/unread` | Total unread messages |
| POST `/conversations` `{bookingId}` | Open or create the chat for a booking (`201` new, `200` existing) |
| GET `/conversations/:id` | One conversation; includes `canSend` and `sendBlockedReason` |
| GET `/conversations/:id/messages?before&limit` | History, oldest-to-newest, cursor paged; marks fetched messages delivered |
| POST `/conversations/:id/messages` `{content, clientMessageId}` | Send text (1-2000 chars). `clientMessageId` (UUID) makes retries idempotent |
| PUT `/conversations/:id/read` | Mark everything read, notify the sender |
| DELETE `/messages/:id` | Delete own message for everyone (blocked while a report is open) |
| PUT / DELETE `/conversations/:id/block` | Block / unblock the other party (the blocked person is not told) |
| POST `/conversations/:id/report` `{reason, details?}` | Report; reasons `HARASSMENT`, `SPAM`, `OFF_PLATFORM_CONTACT`, `SAFETY`, `OTHER` |
| GET `/admin/chat/reports?status` | Admin: reports (no message text, no contact data) |
| POST `/admin/chat/reports/:id/messages` `{justification}` | Admin: read a reported thread (audited) |
| PUT `/admin/chat/reports/:id` `{status, resolutionNote}` | Admin: resolve or dismiss |
| GET `/admin/chat/access-log` | Admin: audit trail |

Responses use `Cache-Control: no-store`. Limits per authenticated account: 30 sends/minute, 240 chat requests/minute, 5 reports/hour; admin routes 60/minute; the global 300/minute per-IP limit still applies. Errors keep the `{ error }` shape; a refused send adds `code`.

### WebSocket (Socket.IO) - path `/api/socket.io`, WebSocket transport only

Authentication: the HttpOnly `session` cookie is verified (JWT plus database session) during the handshake, exactly like REST. No token or user id is accepted from JavaScript. The handshake must carry an allowed `Origin` (blocks cross-site WebSocket hijacking), is rate-limited per IP, and each user may hold at most 5 connections. Logout and password change disconnect the user's sockets immediately, and a 60-second sweep drops sockets whose session expired or was revoked. Admins cannot connect.

| Direction | Event | Payload |
|---|---|---|
| server -> client | `message:new` | `{conversationId, message}` to both participants (multi-tab safe) |
| server -> client | `message:status` | `{conversationId, messageIds, status: 'delivered'\|'read', at}` to the sender |
| server -> client | `message:deleted` | `{conversationId, messageId}` |
| server -> client | `typing` | `{conversationId, isTyping}` |
| server -> client | `presence` | `{conversationId, online}` for the counterpart in active conversations |
| server -> client | `conversation:updated` | `{conversationId, reason: 'created'\|'closed'\|'read'\|'blocked'\|'unblocked'}` |
| server -> client | `booking:updated` | `{bookingId, status}` to the booking customer and affected professional after commit |
| server -> client | `notification:new` | `{bookingId}` to the same accounts after a booking status change |
| client -> server | `typing` | `{conversationId, isTyping}`, ack `{ok}` or `{ok:false, error}` |
| client -> server | `message:delivered` | `{conversationId, messageIds[]}`, ack as above |

Every client event is schema-validated and re-authorised against the database, and each connection is limited to 40 events per 10 seconds (dropped beyond 120). Messages are **sent over REST**, so they share CSRF/Origin checks, validation, idempotency and rate limits; sockets carry pushes and lightweight signals. The client polls while the socket is down.

### Chat environment variables

| Variable | Meaning |
|---|---|
| `CHAT_ENCRYPTION_KEYS` | Required. Comma-separated `version:base64(32 random bytes)`; generate with `node -e "console.log('1:'+require('crypto').randomBytes(32).toString('base64'))"`. Load from a secret manager in production; never commit |
| `CHAT_ACTIVE_KEY_VERSION` | Required. Version used for new conversations and by key rotation; must appear in the list |
| `CHAT_BLOCK_CONTACT_INFO` | `true` (default) rejects phone numbers/e-mail addresses in messages |

Losing every copy of a master key version makes conversations wrapped under it unreadable. Back keys up separately from the database.

### Chat scaling and known gaps

- Presence, socket connection caps and the REST rate limiters are in memory. Before running more than one API instance, add a Socket.IO Redis adapter with shared presence, and a shared rate-limit store (or enforce limits at the gateway).
- Not implemented: image/file attachments (they need access-controlled object storage, type/size/malware checks and signed URLs), push/e-mail/SMS notifications (in-app notifications only, without message text), an admin UI for reports, message retention/erasure jobs.

### Coupon rules

A coupon can be limited to categories (`Offer.categoryIds`, empty = all) and to a number of uses per customer (`Offer.maxUsesPerCustomer`; cancelled bookings do not count). The discount is calculated on the **eligible** items only, checked against the coupon's minimum, and capped by `maxDiscount` and the eligible subtotal. Rejections say why (not valid, expired, minimum not met, wrong category, already used). Seeded: `WELCOME150` (flat 150, min 399, one use per customer), `CLEAN10` (10%, max 200, min 500, Home Cleaning, Bathroom Cleaning, Sofa Cleaning and Pest Control), `SALON200` (Beauty & Salon), `WEEKEND50`, `FESTIVE300`. Re-running `npm run db:seed` applies restrictions to existing coupon rows. Taxes are currently zero.

### Troubleshooting: "Origin is not allowed"

Every non-GET request under `/api` is checked against `FRONTEND_ORIGINS` (a CSRF defence for cookie sessions; this is not CORS). The browser's `Origin` must match an allowed entry exactly: scheme, host (`localhost` is not `127.0.0.1`) and port. Typical causes are a second dev server that moved to another port (Vite prints the URL it actually used), or opening `http://127.0.0.1:5173` when only `localhost` is allowed. In development the error message names the rejected origin and the allowed ones, and the API logs an `origin_rejected` line. Fix it by opening the app at an allowed URL or adding that exact origin to `FRONTEND_ORIGINS`; never use `*`.

## Testing

Create a separate database whose name ends in `_test`, migrate it using `DATABASE_URL` temporarily pointed at that database, then restore the normal development URL. Set `TEST_DATABASE_URL` to the test URL and run `npm test` (test files run one at a time because they share that database; `test/chat.test.js` covers chat privacy, access control, encryption, key rotation, WebSocket authentication, delivery/read receipts, admin audit and hostile input over real HTTP and WebSocket connections). The suite **truncates the dedicated test database**, seeds fixtures, and exercises actual Express/Prisma/PostgreSQL behavior. It refuses to run against a database without the `_test` suffix. Tests do not mock database persistence or authentication.

With both applications running, `npm run test:browser` from this folder runs the frontend Playwright suite with a temporary database-backed admin account, removed afterward. The runner refuses remote databases and production mode. Browser tests register demo-domain customer/professional accounts and save a booking in the connected development database. Install frontend dependencies separately first. The API integration suite also starts and stops a separate API process on port 5099 to verify that sessions and bookings survive server restarts.

## Deployment

### Booking dispatch and private chat workflow

1. A customer saves a booking. It is `PENDING` in PostgreSQL; the customer sees “Waiting for professional assignment”. Admin accounts receive an in-app dispatch notification.
2. On the admin dashboard, choose **Find eligible professionals** for that booking. The API checks verified status, today's availability flag, city, every requested service, the scheduled availability window, and conflicting bookings, then lists candidates by fewest active jobs that day. The admin selects one candidate. Assignment rechecks eligibility under database row locks and changes the status to `ASSIGNED`.
3. The professional sees the request on the dashboard (or `GET /api/professionals/booking-requests`) with the service, customer display name, booking ID, slot and service address. The customer and professional receive private in-app notifications and `booking:updated` Socket.IO events.
4. The assigned professional accepts via `POST /api/bookings/:id/accept` (`CONFIRMED`) or rejects via `POST /api/bookings/:id/reject` (`PENDING`). Both use the authenticated session and a locked booking row. Acceptance rechecks current verification, availability, service area, services and conflicts. Rejection clears the assignment, revokes the old professional's conversation membership, retains the customer's old chat history, and sends the request back to admin dispatch. The rejected request remains in the professional's own limited history list.
5. Chat may be opened only by the booking customer and currently assigned professional while the booking is `ASSIGNED`, `CONFIRMED`, `ON_THE_WAY`, `ARRIVED`, or `IN_PROGRESS`. Completed and cancelled conversations remain readable to their current participants, but sending is disabled. A former professional loses all conversation access after rejection or reassignment. No chat is created before assignment.

The admin-only `GET /api/admin/bookings/:id/eligible-professionals` endpoint returns display names and IDs of eligible candidates. `GET /api/professionals/rejected-bookings` returns only a professional's own rejected request reference, service and slot; it does not reveal the current assignment or customer address. `BookingAssignment` records each assignment, acceptance and rejection; `activeKey` permits only one active assignment record per booking. The existing booking and chat authorization remains authoritative.

The UI refreshes booking data on `booking:updated` and after Socket.IO reconnect, with a 30-second visible-page poll if a push is missed. `notification:new` refreshes the open notification drawer. Events contain only the booking ID and status, never customer contact data or message text. Admins do not connect to chat sockets, so the admin dispatch dashboard polls every 30 seconds and offers a manual refresh.

Before deploying, run `npm run db:migrate` once per release and `npm run db:generate` during build. Deploy the API and frontend behind HTTPS, forward `/api/socket.io` WebSocket upgrades, set exact `FRONTEND_ORIGINS`, and verify cookie `Secure`/`SameSite` behavior on the actual domains. The current Socket.IO hub and rate limits are process-local; use one API instance until a shared adapter and shared rate-limit store are configured. Browser notifications are in-app only; there is no push, SMS or e-mail dispatch. Chat uses server-side AES-256-GCM encryption at rest and **is not end-to-end encrypted**.

### Production release checklist

1. Provision a private PostgreSQL database with a strong password, TLS, backups and a restricted application user. Keep demo data out of production.
2. Deploy `backend/` as a Node service with `npm ci`, `npm run db:generate`, then `npm run db:migrate` as a release step. Use `npm start` as the start command.
3. Set production environment values in your host's secret store, including NODE_ENV=production, DATABASE_URL, JWT_SECRET, PORT and exact FRONTEND_ORIGINS. Set TRUST_PROXY to match your host's proxy topology.
4. Prefer frontend and API on the same site through an HTTPS reverse proxy with `/api` forwarded to the API. Otherwise set the frontend's VITE_API_URL at build time; use correct cookie and CORS settings. Browser third-party-cookie restrictions can prevent unrelated-site cookie authentication.
5. Create the first administrator using the one-time CLI. Populate real categories/services and verify real professionals through admin APIs/UI. Do not run the demo seed in production.
6. Build and deploy the frontend separately. Configure its host to rewrite non-asset routes to `index.html`. Do not route `/api` to the SPA.
7. Chat: store `CHAT_ENCRYPTION_KEYS` in the host's secret manager and back it up separately from the database. Terminate TLS in front of the API so chat runs over HTTPS/WSS, and make the reverse proxy forward WebSocket upgrades for `/api/socket.io` (for nginx: `proxy_http_version 1.1; proxy_set_header Upgrade $http_upgrade; proxy_set_header Connection "upgrade"; proxy_read_timeout 75s;`). Keep `FRONTEND_ORIGINS` exact: it also gates the WebSocket handshake. Make sure request-body logging (proxy, APM, WAF) is off for `/api/conversations`, or message text will be logged even though the API never logs it.
8. Verify health, registration/login/logout, a service in each active category, one real test booking, ownership restrictions, assignment and cash reconciliation. Monitor errors, database capacity and backups. Use a shared rate-limit store or enforce limits at the gateway before scaling to multiple API instances.

Cash payment is implemented. Online card/UPI payment, refunds, tax invoicing and professional payouts need a selected payment provider and business configuration before launch. No request from a browser can mark an online payment as paid. Taxes are currently zero rather than assuming the original demo's blanket 5% GST. HomeAI needs real provider credentials and a supported configured model. Password reset/email verification, email/SMS delivery and production observability are not included in this scope.
