// =========================================================================
// CONTACT-INFO SCRUBBER (Shared Module)
// Blocks contact-sharing attempts: phone, email, UPI, external messaging
// =========================================================================

export type ScrubberReason = 'PHONE' | 'EMAIL' | 'EXTERNAL_APP' | 'ABUSE';

export interface ScrubberResult {
  blocked: boolean;
  reason?: ScrubberReason;
  userMessage?: string;
}

const WORD_TO_DIGIT: Record<string, string> = {
  zero: '0', one: '1', two: '2', three: '3', four: '4',
  five: '5', six: '6', seven: '7', eight: '8', nine: '9',
  shunya: '0', ek: '1', do: '2', teen: '3', chaar: '4',
  paanch: '5', chhe: '6', saat: '7', aath: '8', nau: '9'
};

const SOCIAL_PATTERNS = [
  /\b(whatsapp|wa\.me|telegram|t\.me|insta(gram)?|snapchat|fb|facebook)\b/i,
  /\b(call\s*me|my\s*number|ping\s*me|text\s*me|phone\s*pe\s*baat)\b/i
];

const EMAIL_PATTERN = /[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}/;
const UPI_PATTERN = /[a-zA-Z0-9.\-_]{2,256}@(okhdfcbank|okaxis|okicici|oksbi|paytm|apl|ybl|upi|ibl|axl)\b/i;

export function scrubMessageContent(text: string): ScrubberResult {
  if (!text || text.trim().length === 0) {
    return { blocked: false };
  }

  const normalized = text.toLowerCase();

  // 1. Check for email addresses
  if (EMAIL_PATTERN.test(text)) {
    return {
      blocked: true,
      reason: 'EMAIL',
      userMessage: "For your safety, contact details can't be shared here. Keep chatting in the app."
    };
  }

  // 2. Check for UPI IDs
  if (UPI_PATTERN.test(text)) {
    return {
      blocked: true,
      reason: 'EXTERNAL_APP',
      userMessage: "For your safety, contact details can't be shared here. Keep chatting in the app."
    };
  }

  // 3. Check for external apps and solicitations
  for (const pattern of SOCIAL_PATTERNS) {
    if (pattern.test(normalized)) {
      return {
        blocked: true,
        reason: 'EXTERNAL_APP',
        userMessage: "For your safety, contact details can't be shared here. Keep chatting in the app."
      };
    }
  }

  // 4. Check for spelled-out digits (e.g., "nine eight seven six...")
  let convertedText = normalized;
  for (const [word, digit] of Object.entries(WORD_TO_DIGIT)) {
    const wordRegex = new RegExp(`\\b${word}\\b`, 'g');
    convertedText = convertedText.replace(wordRegex, digit);
  }

  // Extract all digit runs after stripping spaces, dots, dashes, parentheses
  // Check if string contains +91 or 0091 or 10-digit Indian phone sequence
  const digitsOnly = convertedText.replace(/\D/g, '');

  // Look for +91 / 0091 followed by 10 digits starting with 6, 7, 8, 9
  if (/(?:\+?91|0091)?[6-9]\d{9}/.test(digitsOnly)) {
    // Check if it's a legitimate 10-digit mobile number
    // Avoid false positives for benign things like "order 12345" or "402, street 5"
    const phoneMatch = /(?:^|\D)(?:(?:\+?91|0091)[\s.-]?)?([6-9][\s.-]*(?:\d[\s.-]*){9})(?:\D|$)/;
    if (phoneMatch.test(convertedText) || (digitsOnly.length >= 10 && /(?:^|\D)[6-9]\d{9}(?:\D|$)/.test(digitsOnly))) {
      return {
        blocked: true,
        reason: 'PHONE',
        userMessage: "For your safety, contact details can't be shared here. Keep chatting in the app."
      };
    }
  }

  return { blocked: false };
}
