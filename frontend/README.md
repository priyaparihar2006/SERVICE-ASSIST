# Service Assist frontend

The existing React 19, TypeScript/TSX, Vite, Tailwind and Motion UI is preserved. Routing uses the existing history-based App component. Backend responsibilities have moved entirely to `../backend`.

## Development

```powershell
npm ci
npm run dev
```

Open http://localhost:5173. The Vite development proxy forwards `/api` to http://127.0.0.1:5000. Run the backend separately. No frontend environment file is needed with the proxy.

To use a different API, copy `.env.example` to `.env` and set `VITE_API_URL` to its complete API base, for example `https://api.example.com/api`. Never put DATABASE_URL, JWT_SECRET or AI keys in a VITE variable. Restart Vite after changing environment variables.

`src/services/api.js` is the only network boundary. It attaches session cookies, normalizes API URLs and raises server validation/network errors. `getAll` walks paginated collections where the existing dashboard/catalog expects a full collection. Authentication is restored from `/auth/me`; no token or authenticated user is trusted from localStorage. Favorites remain per-user local preferences, and cart data stays local until checkout.

The frontend now uses real loading/error/empty states, persisted addresses/profile, actual booking history and role-gated dashboards. Administration and professional settings extend the existing dashboards without replacing their original layouts. Categories come from the backend, including service counts; the old static category list is no longer used for filtering.

The dev server uses `strictPort`: if port 5173 is already taken (for example a second `npm run dev`), Vite stops with an error instead of silently picking another port, because the API only trusts the origins listed in its `FRONTEND_ORIGINS`. Close the other server, or add the new origin there. See the backend guide's "Origin is not allowed" note.

## Checks

```powershell
npm run lint
npm run build
npm run test:browser
```

Browser checks require both servers and the seeded development database to be running. Playwright uses Microsoft Edge already installed on this workstation. For another machine, choose an installed channel in `playwright.config.js`, or install Playwright Chromium and remove the channel setting. `E2E_URL` can select a different local test deployment. Use a disposable development database: the browser booking test registers a new test customer and saves a booking. Failure traces/screenshots are ignored by Git.

For the full suite including admin login, run `npm run test:browser` from `backend/`. Its fixture runner supplies temporary admin credentials to the test process and removes that account afterward. Running directly from this folder skips the administrator test unless its test credentials are supplied. No test credentials are included in the browser application bundle.

## Production hosting

### Vercel

Use Node.js 22.x (declared in both package manifests). From the repository root,
run `npm run install:frontend` followed by `npm run build` to verify the deployment.
Commit both package manifests and lockfiles together. The install includes dev
and optional dependencies required by Vite and Tailwind's native build tools.

The repository includes Vercel configuration for either the repository root or
`frontend` as the project's Root Directory. Both explicitly select Vite, install
only frontend dependencies, publish the static build, and rewrite browser routes
to `index.html`. API and asset requests are excluded from the SPA fallback.

In Vercel Settings, use the repository root (leave Root Directory empty) or
`frontend`, never `backend`, for the website project. Remove conflicting dashboard
build overrides and deploy the commit containing the configuration. A homepage
showing `FUNCTION_INVOCATION_FAILED` indicates a failing function handled the
request; this frontend is static and does not need a function to serve its HTML.

If deployment reports `Missing script: "install:frontend"` together with Node
`>=22`, check the selected Root Directory and deployment commit. In this checkout,
`>=22` belongs to the backend package; the root and frontend packages use `22.x`.
For a frontend-only Vercel project, set Root Directory to `frontend`, remove any
Install Command override of `npm run install:frontend`, and use the commands from
`frontend/vercel.json`: `npm ci --include=dev --include=optional`, `npm run build`,
and output directory `dist`. Redeploy the commit containing these files.

Set `VITE_API_URL=https://YOUR-API-HOST/api` in the Vercel production environment
before building. Deploy the Express/PostgreSQL backend separately using the
[backend guide](../backend/README.md), and include the exact frontend origin
(for example `https://service-assist-steel.vercel.app`) in its `FRONTEND_ORIGINS`.
Use a backend host that supports the persistent Socket.IO server. These Vercel
configs deploy the website only; login, catalog, bookings and chat still require
a working API. Local `backend/.env` values are not uploaded to Vercel automatically.

After deployment, check `/`, refresh `/dashboard`, and confirm `/favicon.svg`
loads. Check the deployed API's `/api/health` independently.

### General hosting

1. Set VITE_API_URL before `npm run build`, or host the API at the same site's `/api` path.
2. Publish only `dist/` to your static host. Do not publish the backend environment or source secrets.
3. Rewrite frontend routes such as `/services/ac-jet-service` and `/dashboard` to `index.html` so refresh works.
4. Proxy `/api` to the independently deployed Express API, or configure its exact FRONTEND_ORIGINS for the frontend origin.
5. Chat uses a WebSocket at `<API base>/socket.io` (default `/api/socket.io`). Forward WebSocket upgrades for that path, serve over HTTPS so it upgrades to WSS, and, if you set a Content-Security-Policy, allow the API origin in `connect-src` (both `https:` and `wss:`). The socket authenticates with the same HttpOnly cookie; nothing chat-related is stored in the browser.
6. Use HTTPS. Prefer a same-site API for reliable HttpOnly cookie authentication. Test registration, logout, deep-link refresh and booking persistence on the deployed origin.

Chat screens: `src/pages/MessagesPage.tsx`, `src/components/chat/`, `src/hooks/useConversation.ts`, `src/context/ChatContext.tsx`, `src/services/chat*.ts`. In development Vite proxies `/api` including WebSocket upgrades; set `API_PROXY_TARGET` to point the proxy at an API that is not on port 5000.

Online payment buttons are disabled until a provider is configured; checkout uses cash after service. Booking confirmation only appears after a successful database-backed API response. Failed network requests never create a local fake booking or authenticated account.
