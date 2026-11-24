import type { User } from '@/features/auth/types';
import { http } from '@/lib/api-client';

export interface RegisterInput {
  email: string;
  password: string;
  fullName: string;
}

export function register(input: RegisterInput): Promise<User> {
  return http.post<User>('/auth/register', input);
}

/** Checks these credentials (not the stored ones): 200 with the user, or a 401 ApiError. */
export function fetchMe(email: string, password: string): Promise<User> {
  return http.get<User>('/auth/user', { credentials: { email, password } });
}
