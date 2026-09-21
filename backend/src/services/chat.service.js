import { randomUUID } from 'node:crypto';
import { Prisma } from '@prisma/client';
import { db } from '../config/db.js';
import { env } from '../config/env.js';
import { HttpError, ensure } from '../utils/errors.js';
import { hub } from '../realtime/hub.js';
import {
  MESSAGE_ENCRYPTION_VERSION,
  createConversationKey,
  decryptMessage,
  encryptMessage,
  unwrapConversationKey,
} from './chatCrypto.js';
import { containsContactInfo } from './contactGuard.js';

// Business rule: a booking's chat accepts new messages only while a professional is assigned and the
// job is neither finished nor cancelled. Finished/cancelled bookings stay readable, read-only.
export const CHAT_OPEN_STATUSES = ['ASSIGNED', 'CONFIRMED', 'ON_THE_WAY', 'ARRIVED', 'IN_PROGRESS'];
const PARTY_ROLES = ['CUSTOMER', 'PROFESSIONAL'];
const PREVIEW_LENGTH = 80;

// Only these user columns are ever read for chat. Phone numbers and e-mail addresses are never selected.
const partySelect = { id: true, name: true, profileImage: true };
const conversationSelect = {
  id: true,
  status: true,
  wrappedKey: true,
  keyVersion: true,
  lastMessageAt: true,
  createdAt: true,
  booking: {
    select: {
      id: true,
      status: true,
      bookingDate: true,
      bookingTime: true,
      service: { select: { name: true, category: { select: { name: true } } } },
    },
  },
  participants: {
    select: {
      userId: true,
      role: true,
      leftAt: true,
      lastReadAt: true,
      user: { select: partySelect },
    },
  },
};
const messageSelect = {
  id: true,
  conversationId: true,
  senderId: true,
  clientMessageId: true,
  encryptedContent: true,
  encryptionVersion: true,
  messageType: true,
  createdAt: true,
  deletedAt: true,
  receipts: { select: { userId: true, deliveredAt: true, readAt: true } },
};

const logDecryptFailure = (conversationId) =>
  console.error(JSON.stringify({ event: 'chat_decrypt_failed', conversationId }));

function requireParty(user) {
  ensure(PARTY_ROLES.includes(user.role), 403, 'Chat is available to customers and professionals');
}

/**
 * Loads a conversation only if `user` is a current member. A non-member gets the same 404 as a
 * conversation that does not exist, so identifiers cannot be probed.
 */
async function loadMembership(user, conversationId) {
  requireParty(user);
  const conversation = await db.conversation.findFirst({
    where: { id: conversationId, participants: { some: { userId: user.id, leftAt: null } } },
    select: conversationSelect,
  });
  ensure(conversation, 404, 'Conversation not found');
  const me = conversation.participants.find((p) => p.userId === user.id);
  const other = conversation.participants.find((p) => p.userId !== user.id);
  return { conversation, me, other };
}

async function blockState(userId, otherId) {
  const rows = await db.blockedUser.findMany({
    where: {
      OR: [
        { blockerId: userId, blockedId: otherId },
        { blockerId: otherId, blockedId: userId },
      ],
    },
    select: { blockerId: true },
  });
  return {
    blockedByMe: rows.some((r) => r.blockerId === userId),
    blockedByThem: rows.some((r) => r.blockerId === otherId),
  };
}

function sendState(conversation, other, blocks) {
  if (conversation.status !== 'ACTIVE' || other.leftAt) return 'CONVERSATION_CLOSED';
  if (!CHAT_OPEN_STATUSES.includes(conversation.booking.status)) return 'BOOKING_CLOSED';
  if (blocks.blockedByMe) return 'BLOCKED_BY_YOU';
  if (blocks.blockedByThem) return 'UNAVAILABLE'; // deliberately does not reveal a block
  return null;
}

const preview = (text) =>
  text.length > PREVIEW_LENGTH ? `${text.slice(0, PREVIEW_LENGTH - 1)}…` : text;

function conversationView(userId, conversation, { blocks, unreadCount = 0, lastMessage = null }) {
  const { me, other } = {
    me: conversation.participants.find((p) => p.userId === userId),
    other: conversation.participants.find((p) => p.userId !== userId),
  };
  const sendBlockedReason = sendState(conversation, other, blocks);
  const { booking } = conversation;
  return {
    id: conversation.id,
    status: conversation.status,
    createdAt: conversation.createdAt,
    lastMessageAt: conversation.lastMessageAt,
    booking: {
      id: booking.id,
      reference: booking.id,
      status: booking.status,
      serviceName: booking.service.name,
      categoryName: booking.service.category.name,
      scheduledDate: booking.bookingDate.toISOString().slice(0, 10),
      scheduledTimeSlot: booking.bookingTime,
    },
    // Display-only identity: no user id, phone number or e-mail address leaves the server.
    counterpart: {
      displayName: other.user.name,
      avatar: other.user.profileImage,
      role: other.role,
      online: conversation.status === 'ACTIVE' && !other.leftAt && hub.isOnline(other.userId),
    },
    myRole: me.role,
    canSend: sendBlockedReason === null,
    sendBlockedReason,
    blockedByMe: blocks.blockedByMe,
    unreadCount,
    lastMessage,
  };
}

function messageView(userId, row, text) {
  const mine = row.senderId === userId;
  const receipt = mine ? row.receipts.find((r) => r.userId !== userId) : undefined;
  return {
    id: row.id,
    conversationId: row.conversationId,
    isMine: mine,
    type: row.messageType,
    content: row.deletedAt ? null : text,
    deleted: Boolean(row.deletedAt),
    createdAt: row.createdAt,
    clientMessageId: mine ? row.clientMessageId : undefined,
    status: mine
      ? receipt?.readAt
        ? 'read'
        : receipt?.deliveredAt
          ? 'delivered'
          : 'sent'
      : undefined,
    deliveredAt: mine ? (receipt?.deliveredAt ?? null) : undefined,
    readAt: mine ? (receipt?.readAt ?? null) : undefined,
  };
}

function decryptRow(dataKey, row) {
  if (row.deletedAt || !row.encryptedContent) return null;
  return decryptMessage(
    dataKey,
    { conversationId: row.conversationId, messageId: row.id, senderId: row.senderId },
    row.encryptedContent,
    row.encryptionVersion,
  );
}

const emitConversationUpdated = (userIds, conversationId, reason) =>
  userIds.forEach((id) => hub.emitToUser(id, 'conversation:updated', { conversationId, reason }));

async function unreadCounts(userId, conversationIds) {
  if (!conversationIds.length) return new Map();
  const rows = await db.$queryRaw`
    SELECT m."conversationId" AS "conversationId", COUNT(*)::int AS unread
    FROM "MessageReceipt" r
    JOIN "Message" m ON m.id = r."messageId"
    WHERE r."userId" = ${userId} AND r."readAt" IS NULL AND m."deletedAt" IS NULL
      AND m."conversationId" IN (${Prisma.join(conversationIds)})
    GROUP BY m."conversationId"`;
  return new Map(rows.map((r) => [r.conversationId, r.unread]));
}

export async function totalUnread(user) {
  requireParty(user);
  const [row] = await db.$queryRaw`
    SELECT COUNT(*)::int AS unread
    FROM "MessageReceipt" r
    JOIN "Message" m ON m.id = r."messageId"
    JOIN "ConversationParticipant" p ON p."conversationId" = m."conversationId" AND p."userId" = r."userId"
    WHERE r."userId" = ${user.id} AND r."readAt" IS NULL AND m."deletedAt" IS NULL AND p."leftAt" IS NULL`;
  return row.unread;
}

export async function listConversations(user, { page, limit }) {
  requireParty(user);
  const where = { participants: { some: { userId: user.id, leftAt: null } } };
  const [rows, total] = await db.$transaction([
    db.conversation.findMany({
      where,
      select: conversationSelect,
      orderBy: [{ lastMessageAt: { sort: 'desc', nulls: 'last' } }, { createdAt: 'desc' }],
      skip: (page - 1) * limit,
      take: limit,
    }),
    db.conversation.count({ where }),
  ]);
  const ids = rows.map((c) => c.id);
  const [unread, latest, blocks] = await Promise.all([
    unreadCounts(user.id, ids),
    ids.length
      ? db.$queryRaw`
          SELECT DISTINCT ON ("conversationId") id
          FROM "Message"
          WHERE "conversationId" IN (${Prisma.join(ids)})
          ORDER BY "conversationId", "createdAt" DESC, id DESC`
      : [],
    db.blockedUser.findMany({
      where: { OR: [{ blockerId: user.id }, { blockedId: user.id }] },
      select: { blockerId: true, blockedId: true },
    }),
  ]);
  const lastRows = latest.length
    ? await db.message.findMany({
        where: { id: { in: latest.map((r) => r.id) } },
        select: messageSelect,
      })
    : [];
  const lastByConversation = new Map(lastRows.map((m) => [m.conversationId, m]));
  const conversations = rows.map((conversation) => {
    const other = conversation.participants.find((p) => p.userId !== user.id);
    const last = lastByConversation.get(conversation.id);
    let lastMessage = null;
    if (last) {
      let text = null;
      try {
        text = decryptRow(unwrapConversationKey(conversation), last);
      } catch {
        logDecryptFailure(conversation.id);
      }
      lastMessage = {
        preview: text === null ? null : preview(text),
        deleted: Boolean(last.deletedAt),
        isMine: last.senderId === user.id,
        createdAt: last.createdAt,
      };
    }
    return conversationView(user.id, conversation, {
      blocks: {
        blockedByMe: blocks.some((b) => b.blockerId === user.id && b.blockedId === other.userId),
        blockedByThem: blocks.some((b) => b.blockerId === other.userId && b.blockedId === user.id),
      },
      unreadCount: unread.get(conversation.id) || 0,
      lastMessage,
    });
  });
  return { conversations, total };
}

export async function getConversation(user, conversationId) {
  const { conversation, other } = await loadMembership(user, conversationId);
  const [blocks, unread] = await Promise.all([
    blockState(user.id, other.userId),
    unreadCounts(user.id, [conversation.id]),
  ]);
  return conversationView(user.id, conversation, {
    blocks,
    unreadCount: unread.get(conversation.id) || 0,
  });
}

/** Opens (or creates) the chat for a booking. Both parties must belong to that booking. */
export async function openConversationForBooking(user, bookingId) {
  requireParty(user);
  const booking = await db.booking.findFirst({
    where: {
      id: bookingId,
      ...(user.role === 'CUSTOMER'
        ? { customerId: user.id }
        : { professional: { userId: user.id } }),
    },
    select: {
      id: true,
      customerId: true,
      status: true,
      professional: { select: { userId: true } },
    },
  });
  ensure(booking, 404, 'Booking not found');
  ensure(booking.professional, 409, 'A professional has not been assigned to this booking yet');
  const existing = await db.conversation.findUnique({
    where: { activeKey: booking.id },
    select: { id: true },
  });
  if (existing) return { conversation: await getConversation(user, existing.id), created: false };
  ensure(
    CHAT_OPEN_STATUSES.includes(booking.status),
    409,
    'Chat is available once a professional is assigned and until the job is finished',
  );
  const id = randomUUID();
  const { wrappedKey, keyVersion } = createConversationKey(id);
  try {
    await db.conversation.create({
      data: {
        id,
        bookingId: booking.id,
        activeKey: booking.id,
        wrappedKey,
        keyVersion,
        participants: {
          create: [
            { userId: booking.customerId, role: 'CUSTOMER' },
            { userId: booking.professional.userId, role: 'PROFESSIONAL' },
          ],
        },
      },
    });
  } catch (error) {
    // A concurrent request created the active conversation first; return that one.
    if (error.code !== 'P2002') throw error;
    const raced = await db.conversation.findUnique({
      where: { activeKey: booking.id },
      select: { id: true },
    });
    ensure(raced, 409, 'Please retry');
    return { conversation: await getConversation(user, raced.id), created: false };
  }
  emitConversationUpdated(
    [booking.customerId, booking.professional.userId].filter((u) => u !== user.id),
    id,
    'created',
  );
  return { conversation: await getConversation(user, id), created: true };
}

export async function getMessages(user, conversationId, { before, limit }) {
  const { conversation, other } = await loadMembership(user, conversationId);
  let cursor;
  if (before) {
    cursor = await db.message.findFirst({
      where: { id: before, conversationId },
      select: { createdAt: true, id: true },
    });
    ensure(cursor, 400, 'Invalid message cursor');
  }
  const rows = await db.message.findMany({
    where: {
      conversationId,
      ...(cursor
        ? {
            OR: [
              { createdAt: { lt: cursor.createdAt } },
              { createdAt: cursor.createdAt, id: { lt: cursor.id } },
            ],
          }
        : {}),
    },
    orderBy: [{ createdAt: 'desc' }, { id: 'desc' }],
    take: limit + 1,
    select: messageSelect,
  });
  const hasMore = rows.length > limit;
  const page = rows.slice(0, limit).reverse();
  const dataKey = unwrapConversationKey(conversation);
  const messages = page.map((row) => {
    let text = null;
    try {
      text = decryptRow(dataKey, row);
    } catch {
      logDecryptFailure(conversationId);
      text = '[This message could not be decrypted]';
    }
    return messageView(user.id, row, text);
  });
  // Fetching history is proof of delivery for the messages this user received.
  const undelivered = page
    .filter((m) => m.senderId !== user.id)
    .filter((m) => m.receipts.some((r) => r.userId === user.id && !r.deliveredAt))
    .map((m) => m.id);
  if (undelivered.length) await markDelivered(user.id, conversationId, other.userId, undelivered);
  return { messages, hasMore, nextBefore: hasMore ? page[0].id : null };
}

async function markDelivered(userId, conversationId, senderId, messageIds) {
  const now = new Date();
  const pending = await db.messageReceipt.findMany({
    where: {
      userId,
      deliveredAt: null,
      messageId: { in: messageIds },
      message: { conversationId, senderId: { not: userId } },
    },
    select: { messageId: true },
  });
  if (!pending.length) return [];
  const ids = pending.map((r) => r.messageId);
  await db.messageReceipt.updateMany({
    where: { userId, deliveredAt: null, messageId: { in: ids } },
    data: { deliveredAt: now },
  });
  hub.emitToUser(senderId, 'message:status', {
    conversationId,
    messageIds: ids,
    status: 'delivered',
    at: now,
  });
  return ids;
}

/** Called for a recipient's delivery acknowledgement over the WebSocket. */
export async function acknowledgeDelivery(user, conversationId, messageIds) {
  const { other } = await loadMembership(user, conversationId);
  return markDelivered(user.id, conversationId, other.userId, messageIds);
}

export async function sendMessage(user, conversationId, { content, clientMessageId }) {
  const { conversation, me, other } = await loadMembership(user, conversationId);
  const blocks = await blockState(user.id, other.userId);
  const blockedReason = sendState(conversation, other, blocks);
  if (blockedReason) throw new HttpError(403, sendBlockedMessage(blockedReason));
  if (env.CHAT_BLOCK_CONTACT_INFO && containsContactInfo(content))
    throw Object.assign(
      new HttpError(
        422,
        'For your privacy, phone numbers and e-mail addresses cannot be shared in chat. Keep the conversation here.',
      ),
      { publicCode: 'CONTACT_INFO_NOT_ALLOWED' },
    );
  const duplicate = await db.message.findUnique({
    where: {
      conversationId_senderId_clientMessageId: {
        conversationId,
        senderId: user.id,
        clientMessageId,
      },
    },
    select: messageSelect,
  });
  if (duplicate) return messageView(user.id, duplicate, content);
  const messageId = randomUUID();
  const encryptedContent = encryptMessage(
    unwrapConversationKey(conversation),
    { conversationId, messageId, senderId: user.id },
    content,
  );
  let row;
  try {
    row = await db.$transaction(async (tx) => {
      const created = await tx.message.create({
        data: {
          id: messageId,
          conversationId,
          senderId: user.id, // always the authenticated user, never a client-supplied value
          clientMessageId,
          encryptedContent,
          encryptionVersion: MESSAGE_ENCRYPTION_VERSION,
          receipts: { create: { userId: other.userId } },
        },
        select: messageSelect,
      });
      await tx.conversation.update({
        where: { id: conversationId },
        data: { lastMessageAt: created.createdAt },
      });
      return created;
    });
  } catch (error) {
    if (error.code !== 'P2002') throw error;
    // A retry raced its own first attempt: return the stored message.
    const stored = await db.message.findUnique({
      where: {
        conversationId_senderId_clientMessageId: {
          conversationId,
          senderId: user.id,
          clientMessageId,
        },
      },
      select: messageSelect,
    });
    ensure(stored, 409, 'Please retry');
    return messageView(user.id, stored, content);
  }
  hub.emitToUser(other.userId, 'message:new', {
    conversationId,
    message: messageView(other.userId, row, content),
  });
  hub.emitToUser(user.id, 'message:new', {
    conversationId,
    message: messageView(user.id, row, content),
  });
  if (!hub.isOnline(other.userId)) await notifyOffline(other.userId, conversation.booking.id);
  return messageView(user.id, row, content);
}

function sendBlockedMessage(reason) {
  return (
    {
      CONVERSATION_CLOSED: 'This conversation has ended because the booking was reassigned',
      BOOKING_CLOSED: 'This booking is finished, so the chat is now read-only',
      BLOCKED_BY_YOU: 'You have blocked this person. Unblock them to send messages',
      UNAVAILABLE: 'Messages cannot be sent in this conversation',
    }[reason] || 'Messages cannot be sent in this conversation'
  );
}

// One unread reminder per booking; it never contains any message text.
async function notifyOffline(userId, bookingId) {
  try {
    const message = `You have a new message about booking ${bookingId}`;
    const pending = await db.notification.findFirst({
      where: { userId, type: 'CHAT', message, isRead: false },
      select: { id: true },
    });
    if (!pending)
      await db.notification.create({
        data: { userId, type: 'CHAT', title: 'New message', message },
      });
  } catch {
    console.error(JSON.stringify({ event: 'chat_notification_failed' }));
  }
}

export async function markRead(user, conversationId) {
  const { other } = await loadMembership(user, conversationId);
  const now = new Date();
  const unread = await db.messageReceipt.findMany({
    where: { userId: user.id, readAt: null, message: { conversationId } },
    select: { messageId: true },
  });
  const ids = unread.map((r) => r.messageId);
  await db.$transaction([
    db.messageReceipt.updateMany({
      where: { userId: user.id, deliveredAt: null, messageId: { in: ids } },
      data: { deliveredAt: now },
    }),
    db.messageReceipt.updateMany({
      where: { userId: user.id, readAt: null, messageId: { in: ids } },
      data: { readAt: now },
    }),
    db.conversationParticipant.update({
      where: { conversationId_userId: { conversationId, userId: user.id } },
      data: { lastReadAt: now },
    }),
  ]);
  if (ids.length) {
    hub.emitToUser(other.userId, 'message:status', {
      conversationId,
      messageIds: ids,
      status: 'read',
      at: now,
    });
  }
  emitConversationUpdated([user.id], conversationId, 'read');
  return { read: ids.length };
}

export async function deleteMessage(user, messageId) {
  requireParty(user);
  const message = await db.message.findFirst({
    where: {
      id: messageId,
      conversation: { participants: { some: { userId: user.id, leftAt: null } } },
    },
    select: {
      id: true,
      senderId: true,
      deletedAt: true,
      conversationId: true,
      conversation: { select: { participants: { select: { userId: true } } } },
    },
  });
  ensure(message, 404, 'Message not found');
  ensure(message.senderId === user.id, 403, 'You can only delete your own messages');
  if (message.deletedAt) return { deleted: true };
  // Evidence must not disappear while a safety report on this conversation is being reviewed.
  ensure(
    !(await db.chatReport.findFirst({
      where: { conversationId: message.conversationId, status: 'OPEN' },
      select: { id: true },
    })),
    409,
    'This conversation is under review, so messages cannot be deleted right now',
  );
  await db.message.update({
    where: { id: message.id },
    data: { deletedAt: new Date(), encryptedContent: null },
  });
  for (const { userId } of message.conversation.participants)
    hub.emitToUser(userId, 'message:deleted', {
      conversationId: message.conversationId,
      messageId: message.id,
    });
  return { deleted: true };
}

export async function setBlocked(user, conversationId, blocked) {
  const { other } = await loadMembership(user, conversationId);
  const key = { blockerId: user.id, blockedId: other.userId };
  if (blocked)
    await db.blockedUser.upsert({
      where: { blockerId_blockedId: key },
      create: key,
      update: {},
    });
  else await db.blockedUser.deleteMany({ where: key });
  emitConversationUpdated([user.id], conversationId, blocked ? 'blocked' : 'unblocked');
  return getConversation(user, conversationId);
}

export async function reportConversation(user, conversationId, { reason, details }) {
  await loadMembership(user, conversationId);
  try {
    const report = await db.chatReport.create({
      data: {
        conversationId,
        reporterId: user.id,
        reason,
        details: details || '',
        openKey: `${conversationId}:${user.id}`,
      },
      select: { id: true, status: true, reason: true, createdAt: true },
    });
    return report;
  } catch (error) {
    if (error.code === 'P2002')
      throw new HttpError(409, 'You already have an open report for this conversation');
    throw error;
  }
}

/** For typing indicators: the other member's user id if `user` may still use this ACTIVE conversation. */
export async function activeCounterpartId(user, conversationId) {
  if (!PARTY_ROLES.includes(user.role)) return null;
  const conversation = await db.conversation.findFirst({
    where: {
      id: conversationId,
      status: 'ACTIVE',
      participants: { some: { userId: user.id, leftAt: null } },
    },
    select: { participants: { select: { userId: true, leftAt: true } } },
  });
  const other = conversation?.participants.find((p) => p.userId !== user.id && !p.leftAt);
  return other?.userId ?? null;
}

/** Users to notify of presence changes: the counterparts in this user's active conversations. */
export async function presenceAudience(userId) {
  const rows = await db.conversationParticipant.findMany({
    where: { userId, leftAt: null, conversation: { status: 'ACTIVE' } },
    select: {
      conversationId: true,
      conversation: {
        select: {
          participants: {
            where: { userId: { not: userId }, leftAt: null },
            select: { userId: true },
          },
        },
      },
    },
  });
  return rows.flatMap((r) =>
    r.conversation.participants.map((p) => ({
      userId: p.userId,
      conversationId: r.conversationId,
    })),
  );
}

/**
 * Called inside the booking-update transaction whenever a booking's professional changes: the
 * previous professional immediately loses all access and the thread is closed for new messages.
 */
export async function closeConversationsForBooking(tx, bookingId) {
  const open = await tx.conversation.findMany({
    where: { bookingId, status: 'ACTIVE' },
    select: { id: true, participants: { select: { userId: true } } },
  });
  if (!open.length) return [];
  const ids = open.map((c) => c.id);
  await tx.conversation.updateMany({
    where: { id: { in: ids } },
    data: { status: 'CLOSED', activeKey: null },
  });
  await tx.conversationParticipant.updateMany({
    where: { conversationId: { in: ids }, role: 'PROFESSIONAL', leftAt: null },
    data: { leftAt: new Date() },
  });
  return open;
}

export function announceClosed(closed) {
  for (const c of closed)
    emitConversationUpdated(
      c.participants.map((p) => p.userId),
      c.id,
      'closed',
    );
}
