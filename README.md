# Service Assist

A home-services marketplace with a preserved React/Vite frontend and a separate JavaScript Express/PostgreSQL backend.

```text
SERVICE-ASSIST/
  frontend/                 React pages, components, state and API client
    src/services/api.js     Browser-only REST client
    tests/                  Playwright browser checks
    package.json
    .env.example
  backend/                  Express REST API; all database access lives here
    src/
      config/
      controllers/
      middleware/
      routes/
      services/
      utils/
      app.js
      server.js
    prisma/                 Schema, migrations and explicit demo seed
    scripts/                Administrator provisioning
    test/                   PostgreSQL-backed integration tests
    package.json
    .env.example
  .gitignore
  package.json              Root install/start commands; apps stay separate
  README.md
  IMPLEMENTATION_REPORT.md
```

The existing frontend was actually TypeScript/TSX, despite the supplied brief describing JavaScript. Its components, custom history-based routing and visual design are retained. All new backend code and the browser API client are JavaScript. There is no Express, Prisma or database access in the frontend.

## Start locally

Set up PostgreSQL and backend environment variables using [backend/README.md](backend/README.md). On this workstation, an isolated local cluster and ignored backend `.env` have already been created; see that guide for restarting the cluster.

From the project root, install dependencies for both apps and start them together:

```powershell
npm install
npm run dev
```

Keep this terminal open; Ctrl+C stops both apps. The root package only coordinates commands; frontend and backend retain their own dependencies and source code. PostgreSQL must be running before starting the API.

For a fresh database, run `npm --prefix backend run db:generate` and `npm --prefix backend run db:migrate` before starting.

Alternatively, start the apps separately. In one terminal, beginning at the project root:

```powershell
cd backend
npm ci
npm run db:generate
npm run db:migrate
npm run dev
```

For a development demo catalog, set `SEED_DEMO=true` and a strong `DEMO_PASSWORD` in `backend/.env`, then run `npm run db:seed`. Never seed demo accounts into production.

In another terminal, also beginning at the project root (enter `cd frontend` as one command):

```powershell
cd frontend
npm ci
npm run dev
```

Open **http://localhost:5173**. Vite forwards `/api` to the backend at port 5000. Register through Sign In / Create New Account. Professional registration creates a pending profile; administrators verify it before assignment. Create an administrator with the documented backend CLI.

## What is integrated

- Password-based registration/login, HttpOnly JWT session cookies, logout and password changes.
- Database catalog, accurate category relationships/counts, search, city filtering, sorting and service packages.
- Persisted customer profiles and addresses, server-priced bookings, history, cancellation, reviews and receipts.
- Professional profiles, service associations, weekly availability, assigned work, accept/reject, server-side OTP verification and earnings from collected payments.
- Admin metrics, category/service management, professional verification, assignment and cash-payment recording.
- Private in-app chat between a booking's customer and assigned professional (real time, read receipts, typing/online status, block/report). Phone numbers and e-mail addresses are never exposed between the two parties. Encrypted in transit and at rest; **not** end-to-end encrypted. See the [chat report](CHAT_IMPLEMENTATION_REPORT.md).
- Database offers, notifications and support tickets. Optional backend-only HomeAI provider configuration.

The old demo role switcher, password-free login, in-memory database, fabricated booking metrics and fake support-success fallback were replaced. Sofa Cleaning was empty because service records belonged to the broad cleaning category while the UI selected `cat-sofa-cleaning`; the canonical database seed and UI now agree.

## Verification and deployment

- Backend: `npm test` with a separately migrated `TEST_DATABASE_URL` whose name ends in `_test`. This resets only that test database.
- Frontend: `npm run lint`, `npm run build`.
- Browser: start both applications, then run `npm run test:browser` in `frontend/`. The configuration uses installed Microsoft Edge and creates a clearly named browser-test customer/booking in the connected development database.
- Deployment, environment reference and every API endpoint: [backend guide](backend/README.md).
- Frontend build and hosting: [frontend guide](frontend/README.md).
- Changes, schema, test evidence and remaining setup: [implementation report](IMPLEMENTATION_REPORT.md).
- Private chat architecture, encryption limits, security audit and setup: [chat report](CHAT_IMPLEMENTATION_REPORT.md) and the [Private chat](backend/README.md#private-chat) section.

Cash payment is implemented. Online payments, refunds, tax invoicing and payouts require payment-provider and business configuration. No online payment is simulated. HomeAI needs configured credentials/model. Review the remaining launch requirements in the implementation report before treating this as a production deployment.
