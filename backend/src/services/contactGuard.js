// Best-effort detection of phone numbers and e-mail addresses typed into a chat message.
// It only keeps honest users on the platform; it cannot stop deliberate obfuscation
// (spelled-out digits, images, etc.), so it is a policy aid and not a security boundary.
const EMAIL =
  /[\p{L}\p{N}._%+-]+\s?(?:@|\(at\)|\[at\])\s?[\p{L}\p{N}-]+(?:\s?(?:\.|\(dot\)|\[dot\])\s?[\p{L}\p{N}-]+)+/iu;
// A run of at least ten digits, allowing a leading +/( and single separators between digits.
const DIGIT_RUN = /[+(]?\d(?:[\s.\-()]{0,2}\d){9,}/g;

export function containsContactInfo(text) {
  if (EMAIL.test(text)) return true;
  for (const run of text.match(DIGIT_RUN) || []) {
    const digits = run.replace(/\D/g, '').length;
    if (digits >= 10 && digits <= 15) return true;
  }
  return false;
}
