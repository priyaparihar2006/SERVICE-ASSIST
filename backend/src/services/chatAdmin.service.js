import { db } from '../config/db.js';
import { ensure } from '../utils/errors.js';
import { decryptMessage, unwrapConversationKey } from './chatCrypto.js';

// Administrator access policy (documented in backend/README.md):
//  * Admins have no access to the participant chat endpoints and cannot browse conversations.
//  * Message text is readable only while a participant's report on that conversation is OPEN.
//  * Each read needs a written justification and is recorded in the append-only ChatAccessLog.
//  * Access is limited to the 500 most recent messages and ends when the report is resolved.
export const ADMIN_MESSAGE_LIMIT = 500;

export async function listReports({ status, page, limit }) {
  const where = status ? { status } : {};
  const [rows, total] = await db.$transaction([
    db.chatReport.findMany({
      where,
      orderBy: { createdAt: 'desc' },
      skip: (page - 1) * limit,
      take: limit,
      select: {
        id: true,
        conversationId: true,
        reason: true,
        details: true,
        status: true,
        resolutionNote: true,
        createdAt: true,
        resolvedAt: true,
        reporter: { select: { name: true, role: true } },
        conversation: { select: { booking: { select: { id: true } } } },
      },
    }),
    db.chatReport.count({ where }),
  ]);
  return {
    reports: rows.map((r) => ({
      id: r.id,
      conversationId: r.conversationId,
      bookingId: r.conversation.booking.id,
      reason: r.reason,
      details: r.details,
      status: r.status,
      resolutionNote: r.resolutionNote,
      reporterName: r.reporter.name,
      reporterRole: r.reporter.role,
      createdAt: r.createdAt,
      resolvedAt: r.resolvedAt,
    })),
    total,
  };
}

export async function readReportedMessages(admin, reportId, justification) {
  const report = await db.chatReport.findUnique({
    where: { id: reportId },
    select: {
      id: true,
      status: true,
      conversationId: true,
      conversation: {
        select: {
          id: true,
          wrappedKey: true,
          keyVersion: true,
          booking: { select: { id: true } },
        },
      },
    },
  });
  ensure(report, 404, 'Report not found');
  ensure(report.status === 'OPEN', 409, 'Message access is only available while a report is open');
  const rows = await db.message.findMany({
    where: { conversationId: report.conversationId },
    orderBy: [{ createdAt: 'desc' }, { id: 'desc' }],
    take: ADMIN_MESSAGE_LIMIT,
    select: {
      id: true,
      conversationId: true,
      senderId: true,
      encryptedContent: true,
      encryptionVersion: true,
      createdAt: true,
      deletedAt: true,
      sender: { select: { role: true } },
    },
  });
  // The audit record is written first: if it cannot be stored, no content is returned.
  await db.chatAccessLog.create({
    data: {
      adminId: admin.id,
      conversationId: report.conversationId,
      reportId: report.id,
      justification,
      messageCount: rows.length,
    },
  });
  const dataKey = unwrapConversationKey(report.conversation);
  return {
    reportId: report.id,
    bookingId: report.conversation.booking.id,
    limit: ADMIN_MESSAGE_LIMIT,
    messages: rows.reverse().map((row) => {
      let content = null;
      if (!row.deletedAt && row.encryptedContent) {
        try {
          content = decryptMessage(
            dataKey,
            { conversationId: row.conversationId, messageId: row.id, senderId: row.senderId },
            row.encryptedContent,
            row.encryptionVersion,
          );
        } catch {
          content = '[This message could not be decrypted]';
        }
      }
      return {
        id: row.id,
        senderRole: row.sender.role,
        content,
        deleted: Boolean(row.deletedAt),
        createdAt: row.createdAt,
      };
    }),
  };
}

export async function resolveReport(admin, reportId, { status, resolutionNote }) {
  const report = await db.chatReport.findUnique({
    where: { id: reportId },
    select: { id: true, status: true },
  });
  ensure(report, 404, 'Report not found');
  ensure(report.status === 'OPEN', 409, 'This report has already been closed');
  return db.chatReport.update({
    where: { id: reportId },
    data: {
      status,
      openKey: null,
      resolutionNote,
      resolvedById: admin.id,
      resolvedAt: new Date(),
    },
    select: { id: true, status: true, resolutionNote: true, resolvedAt: true },
  });
}

export async function listAccessLog({ page, limit }) {
  const [rows, total] = await db.$transaction([
    db.chatAccessLog.findMany({
      orderBy: { createdAt: 'desc' },
      skip: (page - 1) * limit,
      take: limit,
      select: {
        id: true,
        reportId: true,
        conversationId: true,
        justification: true,
        messageCount: true,
        createdAt: true,
        admin: { select: { name: true } },
      },
    }),
    db.chatAccessLog.count(),
  ]);
  return {
    entries: rows.map(({ admin, ...entry }) => ({ ...entry, adminName: admin.name })),
    total,
  };
}
