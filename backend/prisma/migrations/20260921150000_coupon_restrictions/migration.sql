-- AlterTable
ALTER TABLE "Offer" ADD COLUMN     "categoryIds" TEXT[] DEFAULT ARRAY[]::TEXT[],
ADD COLUMN     "maxUsesPerCustomer" INTEGER;


ALTER TABLE "Offer" ADD CONSTRAINT offer_max_uses_positive CHECK ("maxUsesPerCustomer" IS NULL OR "maxUsesPerCustomer" > 0);
