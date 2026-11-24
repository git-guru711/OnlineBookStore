import type { User } from '@/features/auth/types';

// HTTP Basic needs the credentials on every request, so they are kept in sessionStorage (per tab).

const SESSION_KEY = 'bookstore.session';

export interface SavedSession {
  email: string;
  password: string;
  user: User;
}

export function loadSession(): SavedSession | null {
  try {
    const raw = sessionStorage.getItem(SESSION_KEY);
    if (!raw) {
      return null;
    }
    const saved = JSON.parse(raw) as Partial<SavedSession>;
    if (typeof saved.email === 'string' && typeof saved.password === 'string' && saved.user) {
      return saved as SavedSession;
    }
  } catch {
    // Damaged value or storage unavailable: start as a guest.
  }
  return null;
}

export function saveSession(session: SavedSession) {
  try {
    sessionStorage.setItem(SESSION_KEY, JSON.stringify(session));
  } catch {
    // storage unavailable: the login still works until the next refresh
  }
}

export function clearSession() {
  try {
    sessionStorage.removeItem(SESSION_KEY);
  } catch {
    // Nothing to clear.
  }
}
