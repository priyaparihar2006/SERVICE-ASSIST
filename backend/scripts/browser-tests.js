import 'dotenv/config';
import bcrypt from 'bcrypt';
import { randomUUID } from 'node:crypto';
import { spawn } from 'node:child_process';
import { once } from 'node:events';
import { fileURLToPath } from 'node:url';
import { db } from '../src/config/db.js';

const url = new URL(process.env.DATABASE_URL);
if (process.env.NODE_ENV === 'production' || !['localhost', '127.0.0.1'].includes(url.hostname)) {
  throw new Error('Browser fixtures are restricted to a local development database');
}
const password = randomUUID() + randomUUID();
let admin;
try {
  admin = await db.user.create({
    data: {
      name: 'Browser test administrator',
      email: `browser-admin-${randomUUID()}@demo.service-assist.test`,
      passwordHash: await bcrypt.hash(password, 12),
      role: 'ADMIN',
      isDemo: true,
    },
  });
  const frontend = fileURLToPath(new URL('../../frontend/', import.meta.url));
  const cli = fileURLToPath(
    new URL('../../frontend/node_modules/@playwright/test/cli.js', import.meta.url),
  );
  const child = spawn(process.execPath, [cli, 'test', ...process.argv.slice(2)], {
    cwd: frontend,
    windowsHide: true,
    stdio: 'inherit',
    env: { ...process.env, E2E_ADMIN_EMAIL: admin.email, E2E_ADMIN_PASSWORD: password },
  });
  const [code] = await once(child, 'exit');
  process.exitCode = code === 0 ? 0 : 1;
} finally {
  if (admin) await db.user.delete({ where: { id: admin.id } });
  await db.$disconnect();
}
