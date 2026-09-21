import { app } from './app.js';
import { env } from './config/env.js';
import { db } from './config/db.js';
await db.$connect();
const server = app.listen(env.PORT, () =>
  console.log(`Service Assist API listening on port ${env.PORT}`),
);
for (const signal of ['SIGINT', 'SIGTERM'])
  process.on(signal, () => {
    server.close(async () => {
      await db.$disconnect();
      process.exit(0);
    });
    setTimeout(() => process.exit(1), 10000).unref();
  });
