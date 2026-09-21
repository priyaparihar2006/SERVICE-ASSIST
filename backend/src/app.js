import express from 'express';
import cors from 'cors';
import helmet from 'helmet';
import cookieParser from 'cookie-parser';
import rateLimit from 'express-rate-limit';
import { ZodError } from 'zod';
import { env, origins } from './config/env.js';
import { db } from './config/db.js';
import { endpoint, ensure } from './utils/errors.js';
import auth from './routes/auth.routes.js';
import catalog from './routes/catalog.routes.js';
import bookings from './routes/booking.routes.js';
import professionals from './routes/professional.routes.js';
import account from './routes/account.routes.js';
import admin from './routes/admin.routes.js';
import ai from './routes/ai.routes.js';

export const app = express();
app.disable('x-powered-by');
app.set('trust proxy', env.TRUST_PROXY);
app.use(helmet());
app.use(cors({ origin: origins, credentials: true }));
app.use(express.json({ limit: '100kb' }));
app.use(cookieParser());
app.use(
  '/api',
  rateLimit({ windowMs: 60000, limit: 300, standardHeaders: 'draft-8', legacyHeaders: false }),
);
// Origin + JSON validation protects cookie-authenticated mutations from CSRF.
app.use('/api', (req, res, next) => {
  try {
    if (!['GET', 'HEAD', 'OPTIONS'].includes(req.method)) {
      ensure(
        !req.get('Origin') || origins.includes(req.get('Origin')),
        403,
        'Origin is not allowed',
      );
      ensure(
        req.get('Origin') || !req.cookies.session,
        403,
        'Origin is required for cookie authentication',
      );
      ensure(req.is('application/json'), 415, 'Use application/json');
    }
    next();
  } catch (e) {
    next(e);
  }
});
app.get(
  '/api/health',
  endpoint(async (req, res) => {
    await db.$queryRaw`SELECT 1`;
    res.json({ status: 'ok', service: 'Service Assist API' });
  }),
);
app.use('/api', auth, catalog, bookings, professionals, account, admin, ai);
app.use((req, res) => res.status(404).json({ error: 'Endpoint not found' }));
app.use((err, req, res, next) => {
  if (err instanceof ZodError)
    return res
      .status(400)
      .json({
        error: 'Invalid request',
        details: err.issues.map((i) => ({ path: i.path.join('.'), message: i.message })),
      });
  if (err.code === 'P2002') return res.status(409).json({ error: 'This record already exists' });
  if (err.code === 'P2025') return res.status(404).json({ error: 'Record not found' });
  if (err.code === 'P2003') return res.status(400).json({ error: 'Invalid related record' });
  if (err.code === 'P2034')
    return res.status(409).json({ error: 'The record changed. Please retry.' });
  const status = err.status || 500;
  if (status >= 500)
    console.error(
      JSON.stringify({
        event: 'request_failed',
        method: req.method,
        path: req.path,
        code: err.code || err.name,
      }),
    );
  res
    .status(status)
    .json({
      error: status >= 500 ? 'Service temporarily unavailable. Please try again.' : err.message,
    });
});
