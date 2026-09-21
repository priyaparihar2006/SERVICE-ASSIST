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
| `FRONTEND_ORIGINS` | Comma-separated exact allowed frontend origins; default http://localhost:5173 |
| `COOKIE_SAME_SITE` | lax by default; none requires HTTPS and may still be blocked by third-party-cookie policies |
| `TRUST_PROXY` | Exact number of trusted reverse-proxy hops; default 0 |
| `SEED_DEMO`, `DEMO_PASSWORD` | Explicit non-production demo seed opt-in and password |
| `ADMIN_EMAIL`, `ADMIN_PASSWORD` | One-time administrator creation inputs |
| `TEST_DATABASE_URL` | Dedicated test database URL whose database name ends in `_test` |
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
| GET `/offers`, `/coupons`; POST `/coupons/validate` | Active vouchers / eligibility preview; checkout revalidates |
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

## Testing

Create a separate database whose name ends in `_test`, migrate it using `DATABASE_URL` temporarily pointed at that database, then restore the normal development URL. Set `TEST_DATABASE_URL` to the test URL and run `npm test`. The suite **truncates the dedicated test database**, seeds fixtures, and exercises actual Express/Prisma/PostgreSQL behavior. It refuses to run against a database without the `_test` suffix. Tests do not mock database persistence or authentication.

With both applications running, `npm run test:browser` from this folder runs the frontend Playwright suite with a temporary database-backed admin account, removed afterward. The runner refuses remote databases and production mode. Browser tests register demo-domain customer/professional accounts and save a booking in the connected development database. Install frontend dependencies separately first. The API integration suite also starts and stops a separate API process on port 5099 to verify that sessions and bookings survive server restarts.

## Deployment

1. Provision a private PostgreSQL database with a strong password, TLS, backups and a restricted application user. Keep demo data out of production.
2. Deploy `backend/` as a Node service with `npm ci`, `npm run db:generate`, then `npm run db:migrate` as a release step. Use `npm start` as the start command.
3. Set production environment values in your host's secret store, including NODE_ENV=production, DATABASE_URL, JWT_SECRET, PORT and exact FRONTEND_ORIGINS. Set TRUST_PROXY to match your host's proxy topology.
4. Prefer frontend and API on the same site through an HTTPS reverse proxy with `/api` forwarded to the API. Otherwise set the frontend's VITE_API_URL at build time; use correct cookie and CORS settings. Browser third-party-cookie restrictions can prevent unrelated-site cookie authentication.
5. Create the first administrator using the one-time CLI. Populate real categories/services and verify real professionals through admin APIs/UI. Do not run the demo seed in production.
6. Build and deploy the frontend separately. Configure its host to rewrite non-asset routes to `index.html`. Do not route `/api` to the SPA.
7. Verify health, registration/login/logout, a service in each active category, one real test booking, ownership restrictions, assignment and cash reconciliation. Monitor errors, database capacity and backups. Use a shared rate-limit store or enforce limits at the gateway before scaling to multiple API instances.

Cash payment is implemented. Online card/UPI payment, refunds, tax invoicing and professional payouts need a selected payment provider and business configuration before launch. No request from a browser can mark an online payment as paid. Taxes are currently zero rather than assuming the original demo's blanket 5% GST. HomeAI needs real provider credentials and a supported configured model. Password reset/email verification, email/SMS delivery and production observability are not included in this scope.
