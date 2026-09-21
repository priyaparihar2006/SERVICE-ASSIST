import bcrypt from 'bcrypt';
import { db } from '../config/db.js';
import { cookieOptions } from '../config/env.js';
import * as auth from '../services/auth.service.js';
import { publicUser } from '../utils/serializers.js';
import { ensure } from '../utils/errors.js';

export const register = async (req, res) =>
  res
    .status(201)
    .json(
      await auth.createSession(
        await auth.register(
          req.validated,
          req.validated.role === 'PROFESSIONAL' || req.path === '/professionals/register',
        ),
        res,
      ),
    );
export const login = async (req, res) =>
  res.json(await auth.createSession(await auth.login(req.validated), res));
export const me = async (req, res) =>
  res.json({
    user: publicUser(
      await db.user.findUnique({
        where: { id: req.user.id },
        include: { addresses: { orderBy: { createdAt: 'asc' } } },
      }),
    ),
  });
export const logout = async (req, res) => {
  await db.session.deleteMany({ where: { id: req.sessionId } });
  res.clearCookie('session', cookieOptions).json({ success: true });
};
export const profile = async (req, res) =>
  res.json({
    user: publicUser(
      await db.user.update({
        where: { id: req.user.id },
        data: req.validated,
        include: { addresses: true },
      }),
    ),
  });
export const changePassword = async (req, res) => {
  ensure(
    await bcrypt.compare(req.validated.currentPassword, req.user.passwordHash),
    400,
    'Current password is incorrect',
  );
  await db.$transaction([
    db.user.update({
      where: { id: req.user.id },
      data: { passwordHash: await bcrypt.hash(req.validated.password, 12) },
    }),
    db.session.deleteMany({ where: { userId: req.user.id } }),
  ]);
  res.clearCookie('session', cookieOptions).json({ success: true });
};
