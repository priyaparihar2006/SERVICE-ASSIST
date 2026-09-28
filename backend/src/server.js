import { createServer } from 'node:http';
import { app } from './app.js';
import { env } from './config/env.js';
import { db } from './config/db.js';
import { attachRealtime } from './realtime/socket.js';
try {
  await db.$connect();
  console.log('Database connected successfully');
} catch (err) {
  console.warn('Database connection status:', err.message);
}
const httpServer = createServer(app);
const realtime = attachRealtime(httpServer);
httpServer.listen(env.PORT, () => console.log(`Service Assist API listening on port ${env.PORT}`));
for (const signal of ['SIGINT', 'SIGTERM'])
  process.on(signal, async () => {
    setTimeout(() => process.exit(1), 10000).unref();
    await realtime.close(); // also closes the HTTP server
    await db.$disconnect();
    process.exit(0);
  });
