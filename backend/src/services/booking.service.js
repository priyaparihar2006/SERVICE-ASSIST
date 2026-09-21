import { randomInt } from 'node:crypto';
import { db } from '../config/db.js';
import { HttpError, ensure } from '../utils/errors.js';
import { bookingInclude } from '../utils/serializers.js';
import { announceClosed, closeConversationsForBooking } from './chat.service.js';
import { hub } from '../realtime/hub.js';

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

// Dispatch candidates use the same city, service, availability and overlap rules as assignment.
// The transaction below checks them again under row locks before saving an assignment.
export async function eligibleProfessionalsForBooking(bookingId) {
  const booking = await db.booking.findUnique({
    where: { id: bookingId },
    select: {
      id: true, status: true, bookingDate: true, startAt: true, endAt: true,
      address: { select: { city: true } },
      items: { select: { serviceId: true } },
    },
  });
  ensure(booking, 404, 'Booking not found');
  if (booking.status !== 'PENDING') return [];
  const local = new Date(booking.startAt.getTime() + 330 * 60000);
  const startMinute = local.getUTCHours() * 60 + local.getUTCMinutes();
  const endMinute = startMinute + (booking.endAt - booking.startAt) / 60000;
  const candidates = await db.professional.findMany({
    where: {
      verificationStatus: 'VERIFIED',
      isAvailableToday: true,
      serviceArea: { has: booking.address.city },
      AND: booking.items.map(({ serviceId }) => ({ services: { some: { id: serviceId } } })),
      availability: { some: {
        dayOfWeek: local.getUTCDay(),
        startMinute: { lte: startMinute },
        endMinute: { gte: endMinute },
      } },
      bookings: { none: {
        status: { notIn: ['CANCELLED', 'COMPLETED'] },
        startAt: { lt: booking.endAt },
        endAt: { gt: booking.startAt },
      } },
    },
    select: {
      id: true, businessName: true, user: { select: { name: true } },
      _count: { select: { bookings: { where: {
        bookingDate: booking.bookingDate,
        status: { notIn: ['CANCELLED', 'COMPLETED'] },
      } } } },
    },
  });
  return candidates.sort((a, b) => a._count.bookings - b._count.bookings || a.id.localeCompare(b.id));
}

async function assertEligibleForBooking(tx, booking, professionalId) {
  await tx.$queryRaw`SELECT id FROM "Professional" WHERE id = ${professionalId} FOR UPDATE`;
  const pro = await tx.professional.findUnique({
    where: { id: professionalId },
    include: { services: true, availability: true },
  });
  const addr = await tx.address.findUnique({ where: { id: booking.addressId } });
  ensure(
    pro && addr && pro.verificationStatus === 'VERIFIED' && pro.isAvailableToday &&
      pro.serviceArea.includes(addr.city) &&
      booking.items.every((i) => pro.services.some((s) => s.id === i.serviceId)),
    409,
    'Professional is not available or qualified for these services',
  );
  const local = new Date(booking.startAt.getTime() + 330 * 60000);
  const startMinute = local.getUTCHours() * 60 + local.getUTCMinutes();
  const endMinute = startMinute + (booking.endAt - booking.startAt) / 60000;
  ensure(
    pro.availability.some((a) =>
      a.dayOfWeek === local.getUTCDay() && a.startMinute <= startMinute && a.endMinute >= endMinute,
    ),
    409,
    'Outside professional availability',
  );
  ensure(
    !(await tx.booking.findFirst({
      where: {
        id: { not: booking.id }, professionalId: pro.id,
        status: { notIn: ['CANCELLED', 'COMPLETED'] },
        startAt: { lt: booking.endAt }, endAt: { gt: booking.startAt },
      },
    })),
    409,
    'Professional already has a booking at this time',
  );
}
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
const rupees = (n) => new Intl.NumberFormat('en-IN', { maximumFractionDigits: 2 }).format(n);

/**
 * The only place a coupon discount is calculated, for both the cart preview and booking creation.
 * `lines` come from database prices ({ categoryId, paise }); nothing the browser sends about amounts
 * or discounts is trusted. `customerId` enables the per-customer usage limit.
 */
export async function discountFor(tx, code, lines, customerId) {
  const subtotal = lines.reduce((sum, l) => sum + l.paise, 0) / 100;
  if (!code) return { discount: 0, coupon: null, subtotal, eligibleSubtotal: subtotal };
  const coupon = await tx.offer.findUnique({ where: { code: code.trim().toUpperCase() } });
  ensure(coupon && coupon.isActive, 400, 'This coupon code is not valid');
  ensure(coupon.expiry > new Date(), 400, 'This coupon has expired');
  const restricted = coupon.categoryIds.length > 0;
  const eligibleSubtotal =
    lines
      .filter((l) => !restricted || coupon.categoryIds.includes(l.categoryId))
      .reduce((sum, l) => sum + l.paise, 0) / 100;
  if (eligibleSubtotal <= 0) {
    const names = (
      await tx.category.findMany({
        where: { id: { in: coupon.categoryIds } },
        select: { name: true },
      })
    ).map((c) => c.name);
    throw new HttpError(
      400,
      `${coupon.code} applies only to ${names.join(', ') || 'selected'} services, and your cart has none of them`,
    );
  }
  const minimum = Number(coupon.minBookingAmount);
  ensure(
    eligibleSubtotal >= minimum,
    400,
    `${coupon.code} needs ${restricted ? 'eligible services worth' : 'a booking of'} at least \u20B9${rupees(minimum)} (yours is \u20B9${rupees(eligibleSubtotal)})`,
  );
  if (customerId && coupon.maxUsesPerCustomer != null) {
    const used = await tx.booking.count({
      where: {
        customerId,
        couponCode: { equals: coupon.code, mode: 'insensitive' },
        status: { not: 'CANCELLED' },
      },
    });
    ensure(used < coupon.maxUsesPerCustomer, 400, `You have already used ${coupon.code}`);
  }
  const raw =
    coupon.discountType === 'FLAT'
      ? Number(coupon.value)
      : Math.round((eligibleSubtotal * Number(coupon.value)) / 100);
  return {
    coupon,
    subtotal,
    eligibleSubtotal,
    discount: Math.min(
      eligibleSubtotal,
      raw,
      coupon.maxDiscount == null ? eligibleSubtotal : Number(coupon.maxDiscount),
    ),
  };
}

/** Prices cart items from the database (never from the request) for the coupon preview. */
export async function cartLines(tx, items) {
  const lines = [];
  for (const item of items) {
    const service = await tx.service.findFirst({
      where: { id: item.serviceId, isActive: true, category: { isActive: true } },
      include: { variants: true },
    });
    ensure(service, 400, 'A service in your cart is no longer available');
    const variant = item.variantId
      ? service.variants.find((v) => v.id === item.variantId)
      : service.variants[0];
    ensure(variant, 400, 'Invalid service package');
    lines.push({
      categoryId: service.categoryId,
      paise: Math.round(Number(variant.price) * 100) * item.quantity,
    });
  }
  return lines;
}

export async function createBooking(user, data, requestKey) {
  const saved = await db.$transaction(async (tx) => {
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
    const lines = [];
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
      lines.push({
        categoryId: service.categoryId,
        paise: Math.round(Number(variant.price) * 100) * quantity,
      });
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
    const { discount, coupon } = await discountFor(tx, data.couponCode, lines, user.id);
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
        couponCode: coupon?.code,
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
    const admins = await tx.user.findMany({ where: { role: 'ADMIN' }, select: { id: true } });
    if (admins.length) await tx.notification.createMany({
      data: admins.map(({ id }) => ({
        userId: id,
        title: 'Booking needs assignment',
        message: `Booking ${booking.id} is waiting for dispatch.`,
      })),
    });
    return booking;
  });
  hub.emitToUser(user.id, 'booking:updated', { bookingId: saved.id, status: saved.status });
  return saved;
}
export async function updateStatus(user, bookingId, data) {
  let closedChats = [];
  let recipients = [];
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
    if (data.status === 'CONFIRMED' || data.status === 'PENDING')
      ensure(user.role === 'PROFESSIONAL', 403, 'Only the assigned professional may respond');
    if (data.status === 'PENDING')
      ensure(b.status === 'ASSIGNED', 409, 'Only a new request can be rejected');
    let professionalId = b.professionalId;
    if (data.status === 'ASSIGNED') {
      ensure(
        user.role === 'ADMIN' && data.professionalId,
        403,
        'Only an admin may assign a professional',
      );
      await assertEligibleForBooking(tx, b, data.professionalId);
      professionalId = data.professionalId;
    }
    if (data.status === 'CONFIRMED') {
      ensure(b.startAt > new Date(), 409, 'The booking slot has passed; contact support');
      await assertEligibleForBooking(tx, b, b.professionalId);
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
    if (data.status === 'ASSIGNED') {
      await tx.bookingAssignment.create({
        data: { bookingId: b.id, professionalId, activeKey: b.id },
      });
    } else if (data.status === 'CONFIRMED') {
      await tx.bookingAssignment.updateMany({
        where: { activeKey: b.id, professionalId: b.professionalId },
        data: { acceptedAt: new Date() },
      });
    } else if (data.status === 'PENDING') {
      await tx.bookingAssignment.updateMany({
        where: { activeKey: b.id, professionalId: b.professionalId },
        data: { activeKey: null, rejectedAt: new Date() },
      });
    } else if (['CANCELLED', 'COMPLETED'].includes(data.status)) {
      await tx.bookingAssignment.updateMany({
        where: { activeKey: b.id },
        data: { activeKey: null, closedAt: new Date() },
      });
    }
    const updated = await tx.booking.update({
      where: { id: b.id },
      data: {
        status: data.status,
        professionalId,
        history: { create: { status: data.status, note: data.note || '' } },
      },
      include: bookingInclude,
    });
    const ids = new Set([b.customerId]);
    if (updated.professional?.userId) ids.add(updated.professional.userId);
    if (b.professionalId && b.professionalId !== professionalId) {
      const prior = await tx.professional.findUnique({
        where: { id: b.professionalId }, select: { userId: true },
      });
      if (prior) ids.add(prior.userId);
    }
    if (data.status === 'PENDING') {
      const admins = await tx.user.findMany({ where: { role: 'ADMIN' }, select: { id: true } });
      admins.forEach(({ id }) => ids.add(id));
    }
    recipients = [...ids];
    const title = data.status === 'ASSIGNED' ? 'Professional assigned'
      : data.status === 'CONFIRMED' ? 'Booking accepted'
      : data.status === 'PENDING' ? 'Assignment declined'
      : 'Booking updated';
    await tx.notification.createMany({
      data: recipients.map((userId) => ({
        userId, title, message: `Booking ${b.id}: ${data.status}`,
      })),
    });
    return updated;
  });
  announceClosed(closedChats);
  if (!result.otpError)
    recipients.forEach((id) => {
      hub.emitToUser(id, 'booking:updated', { bookingId: result.id, status: result.status });
      hub.emitToUser(id, 'notification:new', { bookingId: result.id });
    });
  return result;
}
