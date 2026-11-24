import { ApiError, type ProblemDetail } from '@/lib/api-error';
import { apiBaseUrl } from '@/lib/env';

export { ApiError } from '@/lib/api-error';

type Method = 'GET' | 'POST' | 'PATCH' | 'DELETE';
type UnauthorizedListener = () => void;

let authorization: string | null = null;
const unauthorizedListeners = new Set<UnauthorizedListener>();

export function setCredentials(email: string, password: string): void {
  authorization = basicAuthorization(email, password);
}

export function clearCredentials(): void {
  authorization = null;
}

/** Called on a 401 for the stored credentials, so the app can log out. Returns an unsubscribe function. */
export function onUnauthorized(listener: UnauthorizedListener): () => void {
  unauthorizedListeners.add(listener);
  return () => unauthorizedListeners.delete(listener);
}

function basicAuthorization(email: string, password: string): string {
  // btoa() only accepts Latin-1, so encode as UTF-8 bytes first (non-ASCII passwords must work).
  const bytes = new TextEncoder().encode(`${email}:${password}`);
  return `Basic ${btoa(String.fromCharCode(...bytes))}`;
}

export interface RequestOptions {
  /** For this request only, e.g. to check a login. */
  credentials?: { email: string; password: string };
  headers?: Record<string, string>;
}

async function request<T>(method: Method, path: string, body?: unknown, options: RequestOptions = {}): Promise<T> {
  const headers: Record<string, string> = { Accept: 'application/json' };
  if (body !== undefined) {
    headers['Content-Type'] = 'application/json';
  }
  const usesStoredCredentials = !options.credentials && authorization !== null;
  const sentAuthorization = options.credentials
    ? basicAuthorization(options.credentials.email, options.credentials.password)
    : authorization;
  if (sentAuthorization) {
    headers.Authorization = sentAuthorization;
  }
  Object.assign(headers, options.headers);

  let response: Response;
  try {
    response = await fetch(`${apiBaseUrl()}${path}`, {
      method,
      headers,
      body: body === undefined ? undefined : JSON.stringify(body),
    });
  } catch {
    throw ApiError.network();
  }

  if (!response.ok) {
    // only a 401 on the stored credentials ends the session, not a failed login check or a guest request
    if (response.status === 401 && usesStoredCredentials) {
      unauthorizedListeners.forEach((listener) => listener());
    }
    throw await toApiError(response);
  }
  if (response.status === 204) {
    return undefined as T;
  }
  return (await response.json()) as T;
}

async function toApiError(response: Response): Promise<ApiError> {
  try {
    const problem = (await response.json()) as ProblemDetail;
    return ApiError.fromProblem(response.status, problem);
  } catch {
    return ApiError.generic(response.status);
  }
}

export const http = {
  get: <T>(path: string, options?: RequestOptions) => request<T>('GET', path, undefined, options),
  post: <T>(path: string, body?: unknown, options?: RequestOptions) => request<T>('POST', path, body, options),
  patch: <T>(path: string, body: unknown) => request<T>('PATCH', path, body),
  delete: <T = void>(path: string) => request<T>('DELETE', path),
};
