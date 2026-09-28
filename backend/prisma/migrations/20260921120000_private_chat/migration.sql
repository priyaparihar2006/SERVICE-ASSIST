-- CreateEnum
CREATE TYPE "ConversationStatus" AS ENUM ('ACTIVE', 'CLOSED');

-- CreateEnum
CREATE TYPE "MessageType" AS ENUM ('TEXT');

-- CreateEnum
CREATE TYPE "ChatReportReason" AS ENUM ('HARASSMENT', 'SPAM', 'OFF_PLATFORM_CONTACT', 'SAFETY', 'OTHER');

-- CreateEnum
CREATE TYPE "ChatReportStatus" AS ENUM ('OPEN', 'RESOLVED', 'DISMISSED');

-- CreateTable
CREATE TABLE "Conversation" (
    "id" TEXT NOT NULL,
    "bookingId" TEXT NOT NULL,
    "status" "ConversationStatus" NOT NULL DEFAULT 'ACTIVE',
    "activeKey" TEXT,
    "wrappedKey" BYTEA NOT NULL,
    "keyVersion" INTEGER NOT NULL,
    "lastMessageAt" TIMESTAMP(3),
    "createdAt" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updatedAt" TIMESTAMP(3) NOT NULL,

    CONSTRAINT "Conversation_pkey" PRIMARY KEY ("id")
);

-- CreateTable
CREATE TABLE "ConversationParticipant" (
    "id" TEXT NOT NULL,
    "conversationId" TEXT NOT NULL,
    "userId" TEXT NOT NULL,
    "role" "Role" NOT NULL,
    "joinedAt" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "leftAt" TIMESTAMP(3),
    "lastReadAt" TIMESTAMP(3),

    CONSTRAINT "ConversationParticipant_pkey" PRIMARY KEY ("id")
);

-- CreateTable
CREATE TABLE "Message" (
    "id" TEXT NOT NULL,
    "conversationId" TEXT NOT NULL,
    "senderId" TEXT NOT NULL,
    "clientMessageId" TEXT,
    "encryptedContent" BYTEA,
    "encryptionVersion" INTEGER NOT NULL DEFAULT 1,
    "messageType" "MessageType" NOT NULL DEFAULT 'TEXT',
    "createdAt" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updatedAt" TIMESTAMP(3) NOT NULL,
    "deletedAt" TIMESTAMP(3),

    CONSTRAINT "Message_pkey" PRIMARY KEY ("id")
);

-- CreateTable
CREATE TABLE "MessageReceipt" (
    "id" TEXT NOT NULL,
    "messageId" TEXT NOT NULL,
    "userId" TEXT NOT NULL,
    "deliveredAt" TIMESTAMP(3),
    "readAt" TIMESTAMP(3),

    CONSTRAINT "MessageReceipt_pkey" PRIMARY KEY ("id")
);

-- CreateTable
CREATE TABLE "BlockedUser" (
    "id" TEXT NOT NULL,
    "blockerId" TEXT NOT NULL,
    "blockedId" TEXT NOT NULL,
    "createdAt" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT "BlockedUser_pkey" PRIMARY KEY ("id")
);

-- CreateTable
CREATE TABLE "ChatReport" (
    "id" TEXT NOT NULL,
    "conversationId" TEXT NOT NULL,
    "reporterId" TEXT NOT NULL,
    "reason" "ChatReportReason" NOT NULL,
    "details" TEXT NOT NULL DEFAULT '',
    "status" "ChatReportStatus" NOT NULL DEFAULT 'OPEN',
    "openKey" TEXT,
    "resolvedById" TEXT,
    "resolutionNote" TEXT NOT NULL DEFAULT '',
    "createdAt" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "resolvedAt" TIMESTAMP(3),

    CONSTRAINT "ChatReport_pkey" PRIMARY KEY ("id")
);

-- CreateTable
CREATE TABLE "ChatAccessLog" (
    "id" TEXT NOT NULL,
    "adminId" TEXT NOT NULL,
    "conversationId" TEXT NOT NULL,
    "reportId" TEXT NOT NULL,
    "justification" TEXT NOT NULL,
    "messageCount" INTEGER NOT NULL,
    "createdAt" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT "ChatAccessLog_pkey" PRIMARY KEY ("id")
);

-- CreateIndex
CREATE UNIQUE INDEX "Conversation_activeKey_key" ON "Conversation"("activeKey");

-- CreateIndex
CREATE INDEX "Conversation_bookingId_idx" ON "Conversation"("bookingId");

-- CreateIndex
CREATE INDEX "Conversation_lastMessageAt_idx" ON "Conversation"("lastMessageAt");

-- CreateIndex
CREATE INDEX "ConversationParticipant_userId_leftAt_idx" ON "ConversationParticipant"("userId", "leftAt");

-- CreateIndex
CREATE UNIQUE INDEX "ConversationParticipant_conversationId_userId_key" ON "ConversationParticipant"("conversationId", "userId");

-- CreateIndex
CREATE INDEX "Message_conversationId_createdAt_id_idx" ON "Message"("conversationId", "createdAt", "id");

-- CreateIndex
CREATE UNIQUE INDEX "Message_conversationId_senderId_clientMessageId_key" ON "Message"("conversationId", "senderId", "clientMessageId");

-- CreateIndex
CREATE INDEX "MessageReceipt_userId_readAt_idx" ON "MessageReceipt"("userId", "readAt");

-- CreateIndex
CREATE UNIQUE INDEX "MessageReceipt_messageId_userId_key" ON "MessageReceipt"("messageId", "userId");

-- CreateIndex
CREATE INDEX "BlockedUser_blockedId_idx" ON "BlockedUser"("blockedId");

-- CreateIndex
CREATE UNIQUE INDEX "BlockedUser_blockerId_blockedId_key" ON "BlockedUser"("blockerId", "blockedId");

-- CreateIndex
CREATE UNIQUE INDEX "ChatReport_openKey_key" ON "ChatReport"("openKey");

-- CreateIndex
CREATE INDEX "ChatReport_status_createdAt_idx" ON "ChatReport"("status", "createdAt");

-- CreateIndex
CREATE INDEX "ChatReport_conversationId_idx" ON "ChatReport"("conversationId");

-- CreateIndex
CREATE INDEX "ChatAccessLog_conversationId_idx" ON "ChatAccessLog"("conversationId");

-- CreateIndex
CREATE INDEX "ChatAccessLog_adminId_createdAt_idx" ON "ChatAccessLog"("adminId", "createdAt");

-- AddForeignKey
ALTER TABLE "Conversation" ADD CONSTRAINT "Conversation_bookingId_fkey" FOREIGN KEY ("bookingId") REFERENCES "Booking"("id") ON DELETE RESTRICT ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE "ConversationParticipant" ADD CONSTRAINT "ConversationParticipant_conversationId_fkey" FOREIGN KEY ("conversationId") REFERENCES "Conversation"("id") ON DELETE CASCADE ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE "ConversationParticipant" ADD CONSTRAINT "ConversationParticipant_userId_fkey" FOREIGN KEY ("userId") REFERENCES "User"("id") ON DELETE RESTRICT ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE "Message" ADD CONSTRAINT "Message_conversationId_fkey" FOREIGN KEY ("conversationId") REFERENCES "Conversation"("id") ON DELETE CASCADE ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE "Message" ADD CONSTRAINT "Message_senderId_fkey" FOREIGN KEY ("senderId") REFERENCES "User"("id") ON DELETE RESTRICT ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE "MessageReceipt" ADD CONSTRAINT "MessageReceipt_messageId_fkey" FOREIGN KEY ("messageId") REFERENCES "Message"("id") ON DELETE CASCADE ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE "MessageReceipt" ADD CONSTRAINT "MessageReceipt_userId_fkey" FOREIGN KEY ("userId") REFERENCES "User"("id") ON DELETE RESTRICT ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE "BlockedUser" ADD CONSTRAINT "BlockedUser_blockerId_fkey" FOREIGN KEY ("blockerId") REFERENCES "User"("id") ON DELETE CASCADE ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE "BlockedUser" ADD CONSTRAINT "BlockedUser_blockedId_fkey" FOREIGN KEY ("blockedId") REFERENCES "User"("id") ON DELETE CASCADE ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE "ChatReport" ADD CONSTRAINT "ChatReport_conversationId_fkey" FOREIGN KEY ("conversationId") REFERENCES "Conversation"("id") ON DELETE RESTRICT ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE "ChatReport" ADD CONSTRAINT "ChatReport_reporterId_fkey" FOREIGN KEY ("reporterId") REFERENCES "User"("id") ON DELETE RESTRICT ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE "ChatReport" ADD CONSTRAINT "ChatReport_resolvedById_fkey" FOREIGN KEY ("resolvedById") REFERENCES "User"("id") ON DELETE RESTRICT ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE "ChatAccessLog" ADD CONSTRAINT "ChatAccessLog_adminId_fkey" FOREIGN KEY ("adminId") REFERENCES "User"("id") ON DELETE RESTRICT ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE "ChatAccessLog" ADD CONSTRAINT "ChatAccessLog_conversationId_fkey" FOREIGN KEY ("conversationId") REFERENCES "Conversation"("id") ON DELETE RESTRICT ON UPDATE CASCADE;


-- Integrity rules that Prisma cannot express.
ALTER TABLE "Conversation"
  ADD CONSTRAINT conversation_active_key_matches_status
  CHECK (("status" = 'ACTIVE' AND "activeKey" = "bookingId") OR ("status" = 'CLOSED' AND "activeKey" IS NULL)),
  ADD CONSTRAINT conversation_key_version_positive CHECK ("keyVersion" > 0);
ALTER TABLE "ConversationParticipant"
  ADD CONSTRAINT participant_role_is_party CHECK ("role" IN ('CUSTOMER', 'PROFESSIONAL'));
-- Ciphertext exists exactly while the message is not deleted, and always carries iv (12) + tag (16).
ALTER TABLE "Message"
  ADD CONSTRAINT message_ciphertext_matches_deletion
  CHECK (("deletedAt" IS NULL AND "encryptedContent" IS NOT NULL AND octet_length("encryptedContent") > 28)
      OR ("deletedAt" IS NOT NULL AND "encryptedContent" IS NULL)),
  ADD CONSTRAINT message_encryption_version_positive CHECK ("encryptionVersion" > 0);
ALTER TABLE "BlockedUser" ADD CONSTRAINT blocked_user_not_self CHECK ("blockerId" <> "blockedId");
ALTER TABLE "ChatReport"
  ADD CONSTRAINT chat_report_open_key_matches_status
  CHECK (("status" = 'OPEN' AND "openKey" IS NOT NULL) OR ("status" <> 'OPEN' AND "openKey" IS NULL)),
  ADD CONSTRAINT chat_report_details_length CHECK (char_length("details") <= 1000);

-- The administrator access log is append-only: rows can be neither changed nor removed.
CREATE FUNCTION chat_access_log_is_append_only() RETURNS trigger AS $$
BEGIN
  RAISE EXCEPTION 'ChatAccessLog is append-only';
END;
$$ LANGUAGE plpgsql;
CREATE TRIGGER chat_access_log_append_only
  BEFORE UPDATE OR DELETE ON "ChatAccessLog"
  FOR EACH ROW EXECUTE FUNCTION chat_access_log_is_append_only();
