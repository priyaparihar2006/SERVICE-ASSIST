import { Booking } from '../types';

/**
 * Checks if a booking's scheduled date or creation date is within the last 15 days.
 */
export function isBookingWithin15Days(booking: Booking): boolean {
  const dateStr = booking.scheduledDate || booking.createdAt || booking.date;
  if (!dateStr) return true; // keep if unknown

  try {
    // Attempt standard parse, or handle e.g. "28 Sep 2026" or "Today, 28 Sep 2026"
    const cleaned = dateStr.replace(/^(Today|Tomorrow|Yesterday),?\s*/i, '');
    const bookingTime = new Date(cleaned).getTime();

    if (isNaN(bookingTime)) {
      // If parsing fails, try YYYY-MM-DD
      return true;
    }

    const now = new Date().getTime();
    const fifteenDaysInMs = 15 * 24 * 60 * 60 * 1000;
    const diff = now - bookingTime;

    // Within last 15 days (or future scheduled bookings)
    return diff <= fifteenDaysInMs;
  } catch {
    return true;
  }
}

/**
 * Returns days elapsed and remaining retention days out of 15.
 */
export function get15DayRetentionInfo(booking: Booking) {
  const dateStr = booking.scheduledDate || booking.createdAt || booking.date;
  if (!dateStr) return { daysAgo: 0, daysRemaining: 15, isWithin15Days: true };

  try {
    const cleaned = dateStr.replace(/^(Today|Tomorrow|Yesterday),?\s*/i, '');
    const bookingTime = new Date(cleaned).getTime();

    if (isNaN(bookingTime)) {
      return { daysAgo: 0, daysRemaining: 15, isWithin15Days: true };
    }

    const now = new Date().getTime();
    const diffDays = Math.floor((now - bookingTime) / (1000 * 60 * 60 * 24));

    if (diffDays < 0) {
      // Future booking
      return { daysAgo: 0, daysRemaining: 15, isWithin15Days: true };
    }

    const daysRemaining = Math.max(0, 15 - diffDays);
    return {
      daysAgo: diffDays,
      daysRemaining,
      isWithin15Days: diffDays <= 15,
    };
  } catch {
    return { daysAgo: 0, daysRemaining: 15, isWithin15Days: true };
  }
}

/**
 * Filters bookings to only retain those within the last 15 days.
 * Older records are automatically removed.
 */
export function filterAndPurgeOlderThan15Days(bookings: Booking[]): {
  retained: Booking[];
  removedCount: number;
} {
  const retained = bookings.filter((b) => isBookingWithin15Days(b));
  const removedCount = bookings.length - retained.length;
  return { retained, removedCount };
}
