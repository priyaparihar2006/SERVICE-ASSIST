export function getCouponValidityInfo(expiryDateStr?: string) {
  if (!expiryDateStr) {
    return {
      availableUpToText: 'Offer available for a limited time',
      expiryWithinText: 'Expires within 30 days',
      daysLeft: 30,
      isExpiringSoon: false,
    };
  }

  try {
    const expiryDate = new Date(expiryDateStr);
    const now = new Date();
    // Normalize to date difference
    const diffTime = expiryDate.getTime() - now.getTime();
    const daysLeft = Math.ceil(diffTime / (1000 * 60 * 60 * 24));

    const options: Intl.DateTimeFormatOptions = { day: 'numeric', month: 'short', year: 'numeric' };
    const formattedDate = expiryDate.toLocaleDateString('en-IN', options);

    let expiryWithinText = '';
    if (daysLeft < 0) {
      expiryWithinText = 'Offer Expired';
    } else if (daysLeft === 0) {
      expiryWithinText = 'Expires today';
    } else if (daysLeft === 1) {
      expiryWithinText = 'Expires in 24 hours';
    } else {
      expiryWithinText = `Expires within ${daysLeft} days`;
    }

    return {
      availableUpToText: `Offer available up to ${formattedDate}`,
      expiryWithinText,
      daysLeft: Math.max(0, daysLeft),
      isExpiringSoon: daysLeft <= 7,
    };
  } catch {
    return {
      availableUpToText: `Offer available up to ${expiryDateStr}`,
      expiryWithinText: 'Expires within 30 days',
      daysLeft: 30,
      isExpiringSoon: false,
    };
  }
}
