import { QueryClient } from '@tanstack/react-query';
import { ApiError } from '@/lib/api-error';

const MAX_RETRIES = 3;

/** Retry only network and 5xx errors: a 4xx gives the same answer every time. */
export function shouldRetry(failureCount: number, error: Error): boolean {
  const clientError = error instanceof ApiError && error.status >= 400 && error.status < 500;
  return !clientError && failureCount < MAX_RETRIES;
}

export function createQueryClient(): QueryClient {
  return new QueryClient({ defaultOptions: { queries: { retry: shouldRetry } } });
}
