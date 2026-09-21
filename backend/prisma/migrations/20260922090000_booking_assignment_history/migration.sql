CREATE TABLE "BookingAssignment" (
  "id" TEXT NOT NULL,
  "bookingId" TEXT NOT NULL,
  "professionalId" TEXT NOT NULL,
  "activeKey" TEXT,
  "assignedAt" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "acceptedAt" TIMESTAMP(3),
  "rejectedAt" TIMESTAMP(3),
  "closedAt" TIMESTAMP(3),
  CONSTRAINT "BookingAssignment_pkey" PRIMARY KEY ("id")
);
CREATE UNIQUE INDEX "BookingAssignment_activeKey_key" ON "BookingAssignment"("activeKey");
CREATE INDEX "BookingAssignment_professionalId_assignedAt_idx" ON "BookingAssignment"("professionalId", "assignedAt");
CREATE INDEX "BookingAssignment_bookingId_assignedAt_idx" ON "BookingAssignment"("bookingId", "assignedAt");
ALTER TABLE "BookingAssignment" ADD CONSTRAINT "BookingAssignment_bookingId_fkey" FOREIGN KEY ("bookingId") REFERENCES "Booking"("id") ON DELETE RESTRICT ON UPDATE CASCADE;
ALTER TABLE "BookingAssignment" ADD CONSTRAINT "BookingAssignment_professionalId_fkey" FOREIGN KEY ("professionalId") REFERENCES "Professional"("id") ON DELETE RESTRICT ON UPDATE CASCADE;

-- Preserve current assignments when adding the audit table to an existing deployment.
INSERT INTO "BookingAssignment" ("id", "bookingId", "professionalId", "activeKey", "assignedAt", "acceptedAt", "closedAt")
SELECT md5(random()::text || clock_timestamp()::text || b."id"), b."id", b."professionalId",
  CASE WHEN b."status" IN ('ASSIGNED', 'CONFIRMED', 'ON_THE_WAY', 'ARRIVED', 'IN_PROGRESS') THEN b."id" ELSE NULL END,
  b."createdAt",
  CASE WHEN b."status" IN ('CONFIRMED', 'ON_THE_WAY', 'ARRIVED', 'IN_PROGRESS', 'COMPLETED') THEN b."updatedAt" ELSE NULL END,
  CASE WHEN b."status" IN ('COMPLETED', 'CANCELLED') THEN b."updatedAt" ELSE NULL END
FROM "Booking" b WHERE b."professionalId" IS NOT NULL;
