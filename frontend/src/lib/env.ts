/** From Vite's .env files. Only VITE_ variables reach the browser, so never put a secret in one. */

export function apiBaseUrl(): string {
  const value = import.meta.env.VITE_API_BASE_URL;
  if (!value) {
    throw new Error(
      'VITE_API_BASE_URL is not set: add it to the .env file for this mode (see frontend/.env.development).',
    );
  }
  return value.replace(/\/+$/, '');
}
