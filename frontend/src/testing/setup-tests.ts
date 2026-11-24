import '@testing-library/jest-dom/vitest';
import { cleanup } from '@testing-library/react';
import { clearCredentials } from '@/lib/api-client';
import { server } from '@/testing/mocks/server';

// Any request without a matching handler fails the test, so the API contract stays explicit.
beforeAll(() => server.listen({ onUnhandledRequest: 'error' }));
afterEach(() => {
  server.resetHandlers();
  cleanup();
  // A login in one test must never leak into the next one.
  clearCredentials();
  sessionStorage.clear();
});
afterAll(() => server.close());
