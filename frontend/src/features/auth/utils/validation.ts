/** Each rule returns the message to show, or undefined when the value is fine. */

const EMAIL_PATTERN = /^[^\s@]+@[^\s@]+$/;

export function emailError(email: string): string | undefined {
  if (!email.trim()) {
    return 'Enter your email';
  }
  return EMAIL_PATTERN.test(email.trim()) ? undefined : 'Enter a valid email address';
}

export function fullNameError(fullName: string): string | undefined {
  if (!fullName.trim()) {
    return 'Enter your full name';
  }
  return fullName.trim().length > 100 ? 'Use at most 100 characters' : undefined;
}

/** New passwords: 8–72 characters (72 is the BCrypt limit), at least one letter and one digit. */
export function newPasswordError(password: string): string | undefined {
  if (!password) {
    return 'Enter a password';
  }
  if (password.length < 8) {
    return 'Use at least 8 characters';
  }
  if (password.length > 72) {
    return 'Use at most 72 characters';
  }
  return /[A-Za-z]/.test(password) && /\d/.test(password) ? undefined : 'Use at least one letter and one digit';
}

/** Removes the fields without a message, so `Object.keys(errors).length === 0` means "all valid". */
export function withoutEmpty<T extends Record<string, string | undefined>>(errors: T): Partial<T> {
  return Object.fromEntries(Object.entries(errors).filter(([, message]) => message !== undefined)) as Partial<T>;
}
