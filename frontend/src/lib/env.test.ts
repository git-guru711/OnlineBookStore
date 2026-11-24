import { apiBaseUrl } from '@/lib/env';

describe('apiBaseUrl', () => {
  afterEach(() => vi.unstubAllEnvs());

  it('is the backend API address declared in the .env file for the current mode (VITE_API_BASE_URL)', () => {
    vi.stubEnv('VITE_API_BASE_URL', '/api/v1');

    expect(apiBaseUrl()).toBe('/api/v1');
  });

  it('also accepts a full address, e.g. a backend on another host', () => {
    vi.stubEnv('VITE_API_BASE_URL', 'https://api.example.com/api/v1');

    expect(apiBaseUrl()).toBe('https://api.example.com/api/v1');
  });

  it('drops a trailing slash, so paths like "/books" never produce "//books"', () => {
    vi.stubEnv('VITE_API_BASE_URL', '/api/v1/');

    expect(apiBaseUrl()).toBe('/api/v1');
  });

  it('fails with a clear message when the address is not configured, instead of guessing', () => {
    vi.stubEnv('VITE_API_BASE_URL', '');

    expect(() => apiBaseUrl()).toThrow('VITE_API_BASE_URL is not set');
  });
});
