import { useCallback, useEffect, useState, type ReactNode } from 'react';
import { useQueryClient } from '@tanstack/react-query';
import { fetchMe } from '@/features/auth/api/auth';
import { AuthContext } from '@/features/auth/context/auth-context';
import type { User } from '@/features/auth/types';
import { clearSession, loadSession, saveSession } from '@/features/auth/utils/session';
import { clearCredentials, onUnauthorized, setCredentials } from '@/lib/api-client';

export function AuthProvider({ children }: { children: ReactNode }) {
  // restore a saved session (also after a page refresh)
  const [user, setUser] = useState<User | null>(() => {
    const saved = loadSession();
    if (saved) {
      setCredentials(saved.email, saved.password);
    }
    return saved?.user ?? null;
  });
  const queryClient = useQueryClient();

  async function login(email: string, password: string) {
    const me = await fetchMe(email, password); // a 401 ApiError for wrong credentials; nothing is stored
    setCredentials(email, password);
    saveSession({ email, password, user: me });
    setUser(me);
  }

  const logout = useCallback(() => {
    clearCredentials();
    clearSession();
    setUser(null);
    queryClient.clear(); // the next person on this browser must not see the previous user's data
  }, [queryClient]);

  // a 401 on the saved credentials (e.g. password changed) logs out
  useEffect(() => onUnauthorized(logout), [logout]);

  return <AuthContext.Provider value={{ user, login, logout }}>{children}</AuthContext.Provider>;
}
