import 'dotenv/config';
import bcrypt from 'bcrypt';
import { z } from 'zod';
import { db } from '../src/config/db.js';
import { password } from '../src/middleware/validate.js';
try {
  const email = z.email().parse(process.env.ADMIN_EMAIL).toLowerCase();
  const secret = password.parse(process.env.ADMIN_PASSWORD);
  await db.user.create({
    data: {
      name: 'Platform administrator',
      email,
      passwordHash: await bcrypt.hash(secret, 12),
      role: 'ADMIN',
    },
  });
  console.log('Administrator created. Remove ADMIN_PASSWORD from the environment.');
} finally {
  await db.$disconnect();
}
