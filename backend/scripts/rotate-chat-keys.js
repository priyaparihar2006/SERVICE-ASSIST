// Re-wraps every conversation's data key under CHAT_ACTIVE_KEY_VERSION. Message ciphertext is not
// touched, so rotation is cheap. Run it after adding a new key version and making it active; retire
// the old version from CHAT_ENCRYPTION_KEYS only once this reports no remaining conversations on it.
import 'dotenv/config';
import { db } from '../src/config/db.js';
import { chatKeys } from '../src/config/env.js';
import { rewrapConversationKey } from '../src/services/chatCrypto.js';

const stale = await db.conversation.findMany({
  where: { keyVersion: { not: chatKeys.activeVersion } },
  select: { id: true, wrappedKey: true, keyVersion: true },
});
for (const conversation of stale)
  await db.conversation.update({
    where: { id: conversation.id, keyVersion: conversation.keyVersion },
    data: rewrapConversationKey(conversation),
  });
console.log(`Re-wrapped ${stale.length} conversation key(s) to version ${chatKeys.activeVersion}.`);
for (const { keyVersion, _count } of await db.conversation.groupBy({
  by: ['keyVersion'],
  _count: true,
}))
  console.log(`  version ${keyVersion}: ${_count} conversation(s)`);
await db.$disconnect();
