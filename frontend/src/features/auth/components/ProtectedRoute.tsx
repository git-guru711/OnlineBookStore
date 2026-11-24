import type { ReactNode } from 'react';
import { Navigate, useLocation } from 'react-router-dom';
import { useAuth } from '@/features/auth/hooks/useAuth';

/** Sends guests to /login?redirect=<this page>, so they come back after logging in. */
export function ProtectedRoute({ children }: { children: ReactNode }) {
  const { user } = useAuth();
  const location = useLocation();

  if (!user) {
    const wanted = location.pathname + location.search;
    return <Navigate to={`/login?redirect=${encodeURIComponent(wanted)}`} replace />;
  }
  return children;
}
