ALTER TABLE "Service" ADD CONSTRAINT service_price_positive CHECK ("startingPrice" > 0), ADD CONSTRAINT service_duration_positive CHECK (duration > 0);
ALTER TABLE "ServiceVariant" ADD CONSTRAINT variant_price_positive CHECK (price > 0), ADD CONSTRAINT variant_duration_positive CHECK ("durationMin" > 0);
ALTER TABLE "BookingItem" ADD CONSTRAINT item_quantity_positive CHECK (quantity > 0 AND quantity <= 10), ADD CONSTRAINT item_price_positive CHECK ("unitPrice" > 0);
ALTER TABLE "Booking" ADD CONSTRAINT booking_amounts_valid CHECK (subtotal > 0 AND discount >= 0 AND discount <= subtotal AND taxes >= 0 AND "totalAmount" = subtotal - discount + taxes), ADD CONSTRAINT booking_time_valid CHECK ("endAt" > "startAt");
ALTER TABLE "Payment" ADD CONSTRAINT payment_amount_valid CHECK (amount >= 0), ADD CONSTRAINT payment_method_valid CHECK ("paymentMethod" IN ('CASH', 'CARD', 'UPI', 'NETBANKING', 'WALLET'));
ALTER TABLE "Review" ADD CONSTRAINT review_rating_valid CHECK (rating BETWEEN 1 AND 5);
ALTER TABLE "Availability" ADD CONSTRAINT availability_valid CHECK ("dayOfWeek" BETWEEN 0 AND 6 AND "startMinute" >= 0 AND "endMinute" <= 1440 AND "startMinute" < "endMinute");
ALTER TABLE "Offer" ADD CONSTRAINT offer_valid CHECK ("discountType" IN ('FLAT', 'PERCENTAGE') AND value > 0 AND "minBookingAmount" >= 0 AND ("discountType" <> 'PERCENTAGE' OR value <= 100));
CREATE UNIQUE INDEX address_one_default_per_user ON "Address" ("userId") WHERE "isDefault" = true;

CREATE FUNCTION validate_booking_relations() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM "Address" WHERE id = NEW."addressId" AND "userId" = NEW."customerId") THEN
    RAISE EXCEPTION 'Booking address must belong to customer' USING ERRCODE = '23514';
  END IF;
  RETURN NEW;
END $$;
CREATE TRIGGER booking_relations BEFORE INSERT OR UPDATE ON "Booking" FOR EACH ROW EXECUTE FUNCTION validate_booking_relations();

CREATE FUNCTION validate_payment_booking() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM "Booking" WHERE id = NEW."bookingId" AND "totalAmount" = NEW.amount) THEN
    RAISE EXCEPTION 'Payment must match booking total' USING ERRCODE = '23514';
  END IF;
  RETURN NEW;
END $$;
CREATE TRIGGER payment_booking BEFORE INSERT OR UPDATE ON "Payment" FOR EACH ROW EXECUTE FUNCTION validate_payment_booking();

CREATE FUNCTION validate_booking_item() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM "ServiceVariant" WHERE id = NEW."variantId" AND "serviceId" = NEW."serviceId") THEN
    RAISE EXCEPTION 'Variant must belong to item service' USING ERRCODE = '23514';
  END IF;
  RETURN NEW;
END $$;
CREATE TRIGGER booking_item_variant BEFORE INSERT OR UPDATE ON "BookingItem" FOR EACH ROW EXECUTE FUNCTION validate_booking_item();

CREATE FUNCTION validate_review_booking() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM "Booking" WHERE id = NEW."bookingId" AND "customerId" = NEW."customerId" AND "professionalId" = NEW."professionalId" AND status = 'COMPLETED') THEN
    RAISE EXCEPTION 'Review must match a completed booking' USING ERRCODE = '23514';
  END IF;
  RETURN NEW;
END $$;
CREATE TRIGGER review_booking BEFORE INSERT OR UPDATE ON "Review" FOR EACH ROW EXECUTE FUNCTION validate_review_booking();
