import jwt from 'jsonwebtoken';
import { db } from '../config/db.js';
import { env } from '../config/env.js';
import { ensure } from '../utils/errors.js';

// Shared by the REST middleware and the WebSocket handshake so both enforce identical rules.
export async function authenticateToken(token) {
  ensure(token, 401, 'Please sign in');
  let claims;
  try {
    claims = jwt.verify(token, env.JWT_SECRET, {
      algorithms: ['HS256'],
      issuer: 'service-assist',
      audience: 'service-assist-web',
    });
  } catch {
    ensure(false, 401, 'Session expired. Please sign in again');
  }
  const session = await db.session.findUnique({
    where: { id: claims.jti },
    include: { user: { include: { professional: true } } },
  });
  ensure(
    session && session.userId === claims.sub && session.expiresAt > new Date(),
    401,
    'Session expired',
  );
  return { user: session.user, sessionId: session.id, expiresAt: session.expiresAt };
}
export async function authenticate(req, res, next) {
  try {
    const { user, sessionId } = await authenticateToken(
      req.cookies.session || req.headers.authorization?.replace(/^Bearer /, ''),
    );
    req.user = user;
    req.sessionId = sessionId;
    next();
  } catch (error) {
    next(error);
  }
}
export const authorize =
  (...roles) =>
  (req, res, next) => {
    try {
      ensure(roles.includes(req.user.role), 403, 'You do not have permission');
      next();
    } catch (error) {
      next(error);
    }
  };
