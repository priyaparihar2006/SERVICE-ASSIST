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

## Checks

```powershell
npm run lint
npm run build
npm run test:browser
```

Browser checks require both servers and the seeded development database to be running. Playwright uses Microsoft Edge already installed on this workstation. For another machine, choose an installed channel in `playwright.config.js`, or install Playwright Chromium and remove the channel setting. `E2E_URL` can select a different local test deployment. Use a disposable development database: the browser booking test registers a new test customer and saves a booking. Failure traces/screenshots are ignored by Git.

For the full suite including admin login, run `npm run test:browser` from `backend/`. Its fixture runner supplies temporary admin credentials to the test process and removes that account afterward. Running directly from this folder skips the administrator test unless its test credentials are supplied. No test credentials are included in the browser application bundle.

## Production hosting

1. Set VITE_API_URL before `npm run build`, or host the API at the same site's `/api` path.
2. Publish only `dist/` to your static host. Do not publish the backend environment or source secrets.
3. Rewrite frontend routes such as `/services/ac-jet-service` and `/dashboard` to `index.html` so refresh works.
4. Proxy `/api` to the independently deployed Express API, or configure its exact FRONTEND_ORIGINS for the frontend origin.
5. Use HTTPS. Prefer a same-site API for reliable HttpOnly cookie authentication. Test registration, logout, deep-link refresh and booking persistence on the deployed origin.

Online payment buttons are disabled until a provider is configured; checkout uses cash after service. Booking confirmation only appears after a successful database-backed API response. Failed network requests never create a local fake booking or authenticated account.
