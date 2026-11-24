import { createContext } from 'react';
import type { User } from '@/features/auth/types';

export interface AuthContextValue {
  user: User | null;
  login: (email: string, password: string) => Promise<void>;
  /** Also clears all cached server data. */
  logout: () => void;
}

export const AuthContext = createContext<AuthContextValue | null>(null);
