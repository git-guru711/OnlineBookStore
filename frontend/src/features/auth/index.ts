// Public API: the only module other features may import.
export { ProtectedRoute } from './components/ProtectedRoute';
export type { User } from './types';
export { AuthProvider } from './context/AuthProvider';
export { useAuth } from './hooks/useAuth';
export { LoginPage } from './pages/LoginPage';
export { RegisterPage } from './pages/RegisterPage';
