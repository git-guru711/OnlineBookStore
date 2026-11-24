import { http as mswHttp, HttpResponse } from 'msw';
import { API, server } from '@/testing/mocks/server';
import { ApiError, clearCredentials, http, onUnauthorized, setCredentials } from '@/lib/api-client';

describe('http client', () => {
  afterEach(() => clearCredentials());

  it('GET returns the parsed JSON body', async () => {
    server.use(mswHttp.get(`${API}/books/1`, () => HttpResponse.json({ id: 1, title: 'Clean Code' })));

    await expect(http.get('/books/1')).resolves.toEqual({ id: 1, title: 'Clean Code' });
  });

  it('POST sends a JSON body and returns the parsed response', async () => {
    let received: unknown;
    let contentType: string | null = null;
    server.use(
      mswHttp.post(`${API}/cart/items`, async ({ request }) => {
        contentType = request.headers.get('Content-Type');
        received = await request.json();
        return HttpResponse.json({ ok: true }, { status: 201 });
      }),
    );

    const result = await http.post('/cart/items', { bookId: 1, quantity: 2 });

    expect(result).toEqual({ ok: true });
    expect(received).toEqual({ bookId: 1, quantity: 2 });
    expect(contentType).toBe('application/json');
  });

  it('PATCH sends a JSON body', async () => {
    let received: unknown;
    server.use(
      mswHttp.patch(`${API}/cart/items/1`, async ({ request }) => {
        received = await request.json();
        return HttpResponse.json({ ok: true });
      }),
    );

    await http.patch('/cart/items/1', { quantity: 3 });

    expect(received).toEqual({ quantity: 3 });
  });

  it('returns undefined for 204 No Content', async () => {
    server.use(mswHttp.delete(`${API}/cart`, () => new HttpResponse(null, { status: 204 })));

    await expect(http.delete('/cart')).resolves.toBeUndefined();
  });

  it('throws an ApiError built from a ProblemDetail response', async () => {
    server.use(
      mswHttp.post(`${API}/cart/items`, () =>
        HttpResponse.json(
          {
            type: 'https://bookstore.example/problems/validation-error',
            title: 'Validation failed',
            status: 400,
            detail: 'One or more fields are invalid',
            errors: [{ field: 'quantity', message: 'must be between 1 and 99' }],
          },
          { status: 400, headers: { 'Content-Type': 'application/problem+json' } },
        ),
      ),
    );

    const error = await http.post('/cart/items', { quantity: 0 }).catch((e: unknown) => e);

    expect(error).toBeInstanceOf(ApiError);
    expect(error).toMatchObject({
      status: 400,
      type: 'validation-error',
      title: 'Validation failed',
      message: 'One or more fields are invalid',
      fieldErrors: { quantity: 'must be between 1 and 99' },
    });
  });

  it('keeps non-field error details (e.g. insufficient stock lines)', async () => {
    const errors = [{ bookId: 4, title: 'Domain-Driven Design', requested: 2, available: 1 }];
    server.use(
      mswHttp.post(`${API}/orders`, () =>
        HttpResponse.json(
          {
            type: 'https://bookstore.example/problems/insufficient-stock',
            title: 'Insufficient stock',
            status: 409,
            detail: 'Some items are no longer available',
            errors,
          },
          { status: 409 },
        ),
      ),
    );

    const error = await http.post('/orders').catch((e: unknown) => e);

    expect(error).toMatchObject({ status: 409, type: 'insufficient-stock', errors, fieldErrors: {} });
  });

  it('throws a generic ApiError when the error body is not a ProblemDetail', async () => {
    server.use(mswHttp.get(`${API}/books`, () => new HttpResponse('<html>Bad gateway</html>', { status: 502 })));

    const error = await http.get('/books').catch((e: unknown) => e);

    expect(error).toBeInstanceOf(ApiError);
    expect(error).toMatchObject({ status: 502, type: 'unknown', message: 'Something went wrong. Please try again.' });
  });

  it('throws an ApiError with status 0 when the server cannot be reached', async () => {
    server.use(mswHttp.get(`${API}/books`, () => HttpResponse.error()));

    const error = await http.get('/books').catch((e: unknown) => e);

    expect(error).toBeInstanceOf(ApiError);
    expect(error).toMatchObject({ status: 0, type: 'network-error', message: 'Unable to reach the server.' });
  });

  describe('credentials', () => {
    function captureAuthorization() {
      const seen: (string | null)[] = [];
      server.use(
        mswHttp.get(`${API}/auth/user`, ({ request }) => {
          seen.push(request.headers.get('Authorization'));
          return HttpResponse.json({ ok: true });
        }),
      );
      return seen;
    }

    it('sends Basic credentials on every request once they are set', async () => {
      const seen = captureAuthorization();
      setCredentials('jane@example.com', 'secret123');

      await http.get('/auth/user');
      await http.get('/auth/user');

      expect(seen).toEqual([
        'Basic amFuZUBleGFtcGxlLmNvbTpzZWNyZXQxMjM=',
        'Basic amFuZUBleGFtcGxlLmNvbTpzZWNyZXQxMjM=',
      ]);
    });

    it('encodes non-ASCII passwords as UTF-8', async () => {
      const seen = captureAuthorization();
      setCredentials('jane@example.com', 'pässwörd1');

      await http.get('/auth/user');

      expect(seen).toEqual(['Basic amFuZUBleGFtcGxlLmNvbTpww6Rzc3fDtnJkMQ==']);
    });

    it('sends no Authorization header after the credentials are cleared', async () => {
      const seen = captureAuthorization();
      setCredentials('jane@example.com', 'secret123');
      clearCredentials();

      await http.get('/auth/user');

      expect(seen).toEqual([null]);
    });
  });

  describe('401 listener', () => {
    beforeEach(() => {
      server.use(
        mswHttp.get(`${API}/cart`, () =>
          HttpResponse.json(
            { type: 'https://bookstore.example/problems/unauthorized', title: 'Unauthorized', status: 401 },
            { status: 401 },
          ),
        ),
      );
    });

    it('notifies listeners when a request sent with stored credentials gets 401', async () => {
      const listener = vi.fn();
      const unsubscribe = onUnauthorized(listener);
      setCredentials('jane@example.com', 'old-password1');

      await expect(http.get('/cart')).rejects.toMatchObject({ status: 401, type: 'unauthorized' });

      expect(listener).toHaveBeenCalledTimes(1);
      unsubscribe();
    });

    it('stops notifying after unsubscribe', async () => {
      const listener = vi.fn();
      onUnauthorized(listener)();
      setCredentials('jane@example.com', 'old-password1');

      await expect(http.get('/cart')).rejects.toBeInstanceOf(ApiError);

      expect(listener).not.toHaveBeenCalled();
    });

    it('does not notify for a 401 on a request without stored credentials', async () => {
      const listener = vi.fn();
      const unsubscribe = onUnauthorized(listener);

      await expect(http.get('/cart')).rejects.toBeInstanceOf(ApiError);

      expect(listener).not.toHaveBeenCalled();
      unsubscribe();
    });
  });
});
