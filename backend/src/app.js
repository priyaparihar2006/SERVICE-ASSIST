import express from 'express';
import cors from 'cors';
import helmet from 'helmet';
import cookieParser from 'cookie-parser';
import rateLimit from 'express-rate-limit';
import { ZodError } from 'zod';
import { env, origins, isAllowedOrigin } from './config/env.js';
import { db } from './config/db.js';
import { HttpError, endpoint, ensure } from './utils/errors.js';
import auth from './routes/auth.routes.js';
import catalog from './routes/catalog.routes.js';
import bookings from './routes/booking.routes.js';
import professionals from './routes/professional.routes.js';
import account from './routes/account.routes.js';
import admin from './routes/admin.routes.js';
import ai from './routes/ai.routes.js';
import chat from './routes/chat.routes.js';

export const app = express();
app.disable('x-powered-by');
app.set('trust proxy', env.TRUST_PROXY || 1);
app.use(
  helmet({
    crossOriginResourcePolicy: { policy: 'cross-origin' },
  }),
);

app.use(
  cors({
    origin: (origin, callback) => {
      if (!origin || isAllowedOrigin(origin)) {
        callback(null, true);
      } else {
        callback(null, false);
      }
    },
    credentials: true,
    methods: ['GET', 'POST', 'PUT', 'PATCH', 'DELETE', 'OPTIONS'],
    allowedHeaders: ['Content-Type', 'Authorization', 'Idempotency-Key', 'X-Requested-With'],
  }),
);

app.use(express.json({ limit: '100kb' }));
app.use(cookieParser());

app.use(
  '/api',
  rateLimit({ windowMs: 60000, limit: 300, standardHeaders: 'draft-8', legacyHeaders: false }),
);

// Health check endpoints
const healthHandler = endpoint(async (req, res) => {
  try {
    await db.$queryRaw`SELECT 1`;
    res.json({ status: 'ok', service: 'Service Assist API', database: 'connected' });
  } catch (err) {
    res.status(503).json({
      status: 'error',
      service: 'Service Assist API',
      database: 'disconnected',
      message: 'Database connection pending or unavailable',
    });
  }
});

app.get('/', healthHandler);
app.get('/health', healthHandler);
app.get('/api/health', healthHandler);

// Origin + JSON validation protects cookie-authenticated mutations from CSRF.
app.use('/api', (req, res, next) => {
  try {
    if (!['GET', 'HEAD', 'OPTIONS'].includes(req.method)) {
      const origin = req.get('Origin');
      if (origin && !isAllowedOrigin(origin)) {
        if (env.NODE_ENV !== 'development') throw new HttpError(403, 'Origin is not allowed');
        console.warn(
          JSON.stringify({ event: 'origin_rejected', origin, path: req.path, allowed: origins }),
        );
        throw new HttpError(
          403,
          `Origin is not allowed: the API only trusts ${origins.join(', ')} but this page is ${origin}. Open the app at a trusted URL or add this exact origin to FRONTEND_ORIGINS.`,
        );
      }
      ensure(
        req.get('Origin') || !req.cookies?.session,
        403,
        'Origin is required for cookie authentication',
      );
      const contentLength = req.get('content-length');
      if (contentLength && contentLength !== '0') {
        ensure(req.is('application/json'), 415, 'Use application/json');
      }
    }
    next();
  } catch (e) {
    next(e);
  }
});

app.use('/api', auth, catalog, bookings, professionals, account, chat, admin, ai);

app.use((req, res) => res.status(404).json({ error: 'Endpoint not found' }));

app.use((err, req, res, next) => {
  if (err instanceof ZodError)
    return res.status(400).json({
      error: 'Invalid request',
      details: err.issues.map((i) => ({ path: i.path.join('.'), message: i.message })),
    });
  if (err.code === 'P2002') return res.status(409).json({ error: 'This record already exists' });
  if (err.code === 'P2025') return res.status(404).json({ error: 'Record not found' });
  if (err.code === 'P2003') return res.status(400).json({ error: 'Invalid related record' });
  if (err.code === 'P2034')
    return res.status(409).json({ error: 'The record changed. Please retry.' });
  if (err.code === 'P1001' || err.code === 'P1002')
    return res
      .status(503)
      .json({ error: 'Database connection failed. Please check DATABASE_URL.' });

  const status = err.status || 500;
  if (status >= 500)
    console.error(
      JSON.stringify({
        event: 'request_failed',
        method: req.method,
        path: req.path,
        code: err.code || err.name,
        message: err.message,
      }),
    );
  res.status(status).json({
    error: status >= 500 ? 'Service temporarily unavailable. Please try again.' : err.message,
    ...(status < 500 && err.publicCode ? { code: err.publicCode } : {}),
  });
});
