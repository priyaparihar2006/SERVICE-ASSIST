import { randomInt } from 'node:crypto';
import { db } from '../config/db.js';
import { ensure } from '../utils/errors.js';
import { bookingInclude } from '../utils/serializers.js';
import { announceClosed, closeConversationsForBooking } from './chat.service.js';

export const transitions = {
  PENDING: ['ASSIGNED', 'CANCELLED'],
  ASSIGNED: ['CONFIRMED', 'PENDING', 'CANCELLED'],
  CONFIRMED: ['ON_THE_WAY', 'CANCELLED'],
  ON_THE_WAY: ['ARRIVED', 'CANCELLED'],
  ARRIVED: ['IN_PROGRESS'],
  IN_PROGRESS: ['COMPLETED'],
  COMPLETED: [],
  CANCELLED: [],
};
export const scope = (user) =>
  user.role === 'ADMIN'
    ? {}
    : user.role === 'PROFESSIONAL'
      ? { professionalId: user.professional?.id || 'none' }
      : { customerId: user.id };
export function slotStart(date, slot) {
  const m = /^(0?[1-9]|1[0-2]):([0-5]\d) (AM|PM) - (0?[1-9]|1[0-2]):([0-5]\d) (AM|PM)$/.exec(slot);
  ensure(m, 400, 'Select a valid time slot');
  const hour = (Number(m[1]) % 12) + (m[3] === 'PM' ? 12 : 0);
  const endHour = (Number(m[4]) % 12) + (m[6] === 'PM' ? 12 : 0);
  ensure(
    endHour * 60 + Number(m[5]) > hour * 60 + Number(m[2]),
    400,
    'Time slot must end after it starts',
  );
  ensure(hour >= 8 && hour <= 18, 400, 'Service hours are 08:00–19:00 India time');
  const start = new Date(`${date}T${String(hour).padStart(2, '0')}:${m[2]}:00+05:30`);
  ensure(
    Number.isFinite(start.getTime()) &&
      start.toLocaleDateString('en-CA', { timeZone: 'Asia/Kolkata' }) === date,
    400,
    'Invalid booking date',
  );
  ensure(
    start > new Date() && start < new Date(Date.now() + 90 * 86400000),
    400,
    'Book a future slot within 90 days',
  );
  return start;
}
export async function discountFor(tx, code, subtotal) {
  if (!code) return { discount: 0, coupon: null };
  const coupon = await tx.offer.findUnique({ where: { code: code.toUpperCase() } });
  ensure(
    coupon &&
      coupon.isActive &&
      coupon.expiry > new Date() &&
      subtotal >= Number(coupon.minBookingAmount),
    400,
    'Coupon is expired or minimum booking amount is not met',
  );
  const raw =
    coupon.discountType === 'FLAT'
      ? Number(coupon.value)
      : Math.round((subtotal * Number(coupon.value)) / 100);
  return {
    coupon,
    discount: Math.min(
      subtotal,
      raw,
      coupon.maxDiscount == null ? subtotal : Number(coupon.maxDiscount),
    ),
  };
}
export async function createBooking(user, data, requestKey) {
  return db.$transaction(async (tx) => {
    // Serialize duplicate submissions for this customer before reading their key.
    await tx.$queryRaw`SELECT id FROM "User" WHERE id = ${user.id} FOR UPDATE`;
    const prior = await tx.booking.findUnique({
      where: { customerId_requestKey: { customerId: user.id, requestKey } },
      include: bookingInclude,
    });
    if (prior) return prior;
    const address = await tx.address.findFirst({
      where: { id: data.addressId || data.address?.id, userId: user.id },
    });
    ensure(address, 400, 'Select an address belonging to your account');
    const startAt = slotStart(
      data.bookingDate || data.scheduledDate,
      data.bookingTime || data.scheduledTimeSlot,
    );
    const requested = data.items || [
      { serviceId: data.serviceId, variantId: data.variantId, quantity: 1 },
    ];
    let subtotal = 0,
      duration = 0;
    const items = [];
    for (const item of requested) {
      const service = await tx.service.findFirst({
        where: {
          id: item.serviceId || item.service?.id,
          isActive: true,
          category: { isActive: true },
        },
        include: { variants: true },
      });
      ensure(
        service && service.locations.includes(address.city),
        400,
        'A service is unavailable at this address',
      );
      const variant =
        service.variants.find((v) => v.id === (item.variantId || item.variant?.id)) ||
        (!item.variantId && !item.variant?.id ? service.variants[0] : null);
      ensure(variant, 400, 'Invalid service package');
      const quantity = item.quantity || 1;
      subtotal += Math.round(Number(variant.price) * 100) * quantity;
      duration += variant.durationMin * quantity;
      items.push({
        serviceId: service.id,
        variantId: variant.id,
        name: variant.name,
        quantity,
        unitPrice: variant.price,
      });
    }
    ensure(duration <= 480, 400, 'Please split bookings longer than eight hours');
    subtotal /= 100;
    const endAt = new Date(startAt.getTime() + duration * 60000);
    const localStart = new Date(startAt.getTime() + 330 * 60000);
    ensure(
      localStart.getUTCHours() * 60 + localStart.getUTCMinutes() + duration <= 19 * 60,
      400,
      'The service would finish after working hours; choose an earlier slot',
    );
    const { discount } = await discountFor(tx, data.couponCode, subtotal);
    const totalAmount = subtotal - discount;
    const booking = await tx.booking.create({
      data: {
        customerId: user.id,
        serviceId: items[0].serviceId,
        addressId: address.id,
        addressSnapshot: JSON.parse(JSON.stringify(address)),
        bookingDate: new Date(`${data.bookingDate || data.scheduledDate}T00:00:00Z`),
        bookingTime: data.bookingTime || data.scheduledTimeSlot,
        startAt,
        endAt,
        subtotal,
        discount,
        taxes: 0,
        totalAmount,
        notes: data.notes || data.specialInstructions || '',
        couponCode: data.couponCode,
        verificationOtp: String(randomInt(1000, 10000)),
        requestKey,
        items: { create: items },
        payment: { create: { amount: totalAmount, paymentMethod: 'CASH' } },
        history: {
          create: { status: 'PENDING', note: 'Booking saved; awaiting professional assignment' },
        },
      },
      include: bookingInclude,
    });
    await tx.notification.create({
      data: {
        userId: user.id,
        title: 'Booking received',
        message: `Your booking ${booking.id} has been saved.`,
      },
    });
    return booking;
  });
}
export async function updateStatus(user, bookingId, data) {
  let closedChats = [];
  const result = await db.$transaction(async (tx) => {
    await tx.$queryRaw`SELECT id FROM "Booking" WHERE id = ${bookingId} FOR UPDATE`;
    const b = await tx.booking.findFirst({
      where: { id: bookingId, ...scope(user) },
      include: { items: true },
    });
    ensure(b, 404, 'Booking not found');
    ensure(
      transitions[b.status].includes(data.status),
      409,
      `Cannot change ${b.status} to ${data.status}`,
    );
    if (user.role === 'CUSTOMER')
      ensure(
        data.status === 'CANCELLED' && ['PENDING', 'ASSIGNED', 'CONFIRMED'].includes(b.status),
        403,
        'This booking can no longer be cancelled by the customer',
      );
    if (user.role === 'PROFESSIONAL')
      ensure(
        ['CONFIRMED', 'PENDING', 'ON_THE_WAY', 'ARRIVED', 'IN_PROGRESS', 'COMPLETED'].includes(
          data.status,
        ),
        403,
        'Action not permitted',
      );
    let professionalId = b.professionalId;
    if (data.status === 'ASSIGNED') {
      ensure(
        user.role === 'ADMIN' && data.professionalId,
        403,
        'Only an admin may assign a professional',
      );
      await tx.$queryRaw`SELECT id FROM "Professional" WHERE id = ${data.professionalId} FOR UPDATE`;
      const pro = await tx.professional.findUnique({
        where: { id: data.professionalId },
        include: { services: true, availability: true },
      });
      const addr = await tx.address.findUnique({ where: { id: b.addressId } });
      ensure(
        pro &&
          pro.verificationStatus === 'VERIFIED' &&
          pro.isAvailableToday &&
          pro.serviceArea.includes(addr.city) &&
          b.items.every((i) => pro.services.some((s) => s.id === i.serviceId)),
        400,
        'Professional is not available or qualified for these services',
      );
      const local = new Date(b.startAt.getTime() + 330 * 60000);
      const startMinute = local.getUTCHours() * 60 + local.getUTCMinutes();
      const endMinute = startMinute + (b.endAt - b.startAt) / 60000;
      ensure(
        pro.availability.some(
          (a) =>
            a.dayOfWeek === local.getUTCDay() &&
            a.startMinute <= startMinute &&
            a.endMinute >= endMinute,
        ),
        409,
        'Outside professional availability',
      );
      ensure(
        !(await tx.booking.findFirst({
          where: {
            professionalId: pro.id,
            status: { notIn: ['CANCELLED', 'COMPLETED'] },
            startAt: { lt: b.endAt },
            endAt: { gt: b.startAt },
          },
        })),
        409,
        'Professional already has a booking at this time',
      );
      professionalId = pro.id;
    }
    if (data.status === 'PENDING') professionalId = null;
    if (data.status === 'IN_PROGRESS') {
      ensure(b.otpAttempts < 5, 429, 'Too many incorrect codes; contact support');
      if (data.otp !== b.verificationOtp) {
        await tx.booking.update({ where: { id: b.id }, data: { otpAttempts: { increment: 1 } } });
        return { otpError: true };
      }
    }
    // A different (or no) professional means the previous professional's chat access ends now.
    if (professionalId !== b.professionalId)
      closedChats = await closeConversationsForBooking(tx, b.id);
    const updated = await tx.booking.update({
      where: { id: b.id },
      data: {
        status: data.status,
        professionalId,
        history: { create: { status: data.status, note: data.note || '' } },
      },
      include: bookingInclude,
    });
    await tx.notification.create({
      data: {
        userId: b.customerId,
        title: 'Booking updated',
        message: `Booking ${b.id}: ${data.status}`,
      },
    });
    return updated;
  });
  announceClosed(closedChats);
  return result;
}
