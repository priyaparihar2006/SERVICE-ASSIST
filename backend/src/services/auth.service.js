import bcrypt from 'bcrypt';
import jwt from 'jsonwebtoken';
import { db } from '../config/db.js';
import { env, cookieOptions } from '../config/env.js';
import { ensure } from '../utils/errors.js';
import { publicUser } from '../utils/serializers.js';

export async function register(data, professional = false) {
  const passwordHash = await bcrypt.hash(data.password, 12);
  return db.user.create({
    data: {
      name: data.name,
      email: data.email.toLowerCase(),
      phone: data.phone || '',
      passwordHash,
      role: professional ? 'PROFESSIONAL' : 'CUSTOMER',
      ...(professional
        ? {
            professional: {
              create: { businessName: data.businessName || data.name, serviceArea: [] },
            },
          }
        : {}),
    },
    include: { addresses: true },
  });
}
export async function login(data) {
  const user = await db.user.findUnique({
    where: { email: data.email.toLowerCase() },
    include: { addresses: true },
  });
  // Always perform a password comparison, including for an unknown account.
  const hash = user?.passwordHash || '$2b$12$LQv3c1yqBWVHxkd0LHAkCOYz6Ttx.OA.rN5gXduTKnSscl.bwBSu2';
  const valid = await bcrypt.compare(data.password, hash);
  ensure(user && valid, 401, 'Email or password is incorrect');
  return user;
}
export async function createSession(user, res) {
  const session = await db.session.create({
    data: { userId: user.id, expiresAt: new Date(Date.now() + 86400000) },
  });
  const token = jwt.sign({}, env.JWT_SECRET, {
    algorithm: 'HS256',
    subject: user.id,
    jwtid: session.id,
    issuer: 'service-assist',
    audience: 'service-assist-web',
    expiresIn: '1d',
  });
  res.cookie('session', token, { ...cookieOptions, maxAge: 86400000 });
  return { user: publicUser(user) };
}
