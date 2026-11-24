import type { ReactNode } from 'react';
import { act, renderHook } from '@testing-library/react';
import { QueryClientProvider } from '@tanstack/react-query';
import { http as mswHttp, HttpResponse } from 'msw';
import { AuthProvider } from '@/features/auth/context/AuthProvider';
import { useAuth } from '@/features/auth/hooks/useAuth';
import { ApiError, clearCredentials, http } from '@/lib/api-client';
import { aPage, aUser } from '@/testing/data-generators';
import { API, server } from '@/testing/mocks/server';
import { createTestQueryClient } from '@/testing/test-utils';

const basic = (email: string, password: string) => `Basic ${btoa(`${email}:${password}`)}`;

/** Fake backend: only demo@example.com / demo1234 is a valid login. */
function acceptOnlyTheDemoUser() {
  server.use(
    mswHttp.get(`${API}/auth/user`, ({ request }) =>
      request.headers.get('Authorization') === basic('demo@example.com', 'demo1234')
        ? HttpResponse.json(aUser({ fullName: 'Demo User' }))
        : HttpResponse.json(
            { type: 'https://bookstore.example/problems/unauthorized', title: 'Unauthorized', status: 401 },
            { status: 401 },
          ),
    ),
  );
}

function captureNextAuthorization() {
  const seen: (string | null)[] = [];
  server.use(
    mswHttp.get(`${API}/books`, ({ request }) => {
      seen.push(request.headers.get('Authorization'));
      return HttpResponse.json(aPage([]));
    }),
  );
  return seen;
}

function renderAuth() {
  const queryClient = createTestQueryClient();
  const wrapper = ({ children }: { children: ReactNode }) => (
    <QueryClientProvider client={queryClient}>
      <AuthProvider>{children}</AuthProvider>
    </QueryClientProvider>
  );
  return { ...renderHook(() => useAuth(), { wrapper }), queryClient };
}

describe('AuthContext', () => {
  it('starts with nobody logged in', () => {
    const { result } = renderAuth();

    expect(result.current.user).toBeNull();
  });

  it('logs in with valid credentials: keeps the user and sends the credentials on later requests', async () => {
    acceptOnlyTheDemoUser();
    const seen = captureNextAuthorization();
    const { result } = renderAuth();

    await act(() => result.current.login('demo@example.com', 'demo1234'));

    expect(result.current.user).toMatchObject({ fullName: 'Demo User' });
    await http.get('/books');
    expect(seen).toEqual([basic('demo@example.com', 'demo1234')]);
  });

  it('rejects wrong credentials with a 401 ApiError and stays logged out', async () => {
    acceptOnlyTheDemoUser();
    const seen = captureNextAuthorization();
    const { result } = renderAuth();

    // act() returns a thenable without .catch, so the error is caught inside it
    let error: unknown;
    await act(async () => {
      try {
        await result.current.login('demo@example.com', 'wrong-password1');
      } catch (caught) {
        error = caught;
      }
    });

    expect(error).toBeInstanceOf(ApiError);
    expect(error).toMatchObject({ status: 401 });
    expect(result.current.user).toBeNull();
    await http.get('/books');
    expect(seen).toEqual([null]);
  });

  it('logs out: forgets the user, stops sending credentials and clears cached server data', async () => {
    acceptOnlyTheDemoUser();
    const seen = captureNextAuthorization();
    const { result, queryClient } = renderAuth();
    await act(() => result.current.login('demo@example.com', 'demo1234'));
    queryClient.setQueryData(['cart'], { items: ['something from this user'] });

    act(() => result.current.logout());

    expect(result.current.user).toBeNull();
    expect(queryClient.getQueryData(['cart'])).toBeUndefined();
    await http.get('/books');
    expect(seen).toEqual([null]);
  });

  describe('after a page reload (a fresh AuthProvider)', () => {
    it('restores the logged-in user and keeps sending their credentials', async () => {
      acceptOnlyTheDemoUser();
      const firstPageLoad = renderAuth();
      await act(() => firstPageLoad.result.current.login('demo@example.com', 'demo1234'));
      firstPageLoad.unmount();
      clearCredentials(); // a reload also wipes the in-memory credentials
      const seen = captureNextAuthorization();

      const { result } = renderAuth();

      expect(result.current.user).toMatchObject({ fullName: 'Demo User' });
      await http.get('/books');
      expect(seen).toEqual([basic('demo@example.com', 'demo1234')]);
    });

    it('stays logged out after logging out', async () => {
      acceptOnlyTheDemoUser();
      const firstPageLoad = renderAuth();
      await act(() => firstPageLoad.result.current.login('demo@example.com', 'demo1234'));
      act(() => firstPageLoad.result.current.logout());
      firstPageLoad.unmount();

      const { result } = renderAuth();

      expect(result.current.user).toBeNull();
    });

    it('ignores a damaged saved session and starts logged out', () => {
      sessionStorage.setItem('bookstore.session', '{not valid json');

      const { result } = renderAuth();

      expect(result.current.user).toBeNull();
    });
  });

  it('logs out automatically when a request with the saved credentials gets 401 (e.g. password changed)', async () => {
    acceptOnlyTheDemoUser();
    server.use(
      mswHttp.get(`${API}/cart`, () =>
        HttpResponse.json(
          { type: 'https://bookstore.example/problems/unauthorized', title: 'Unauthorized', status: 401 },
          { status: 401 },
        ),
      ),
    );
    const { result, unmount } = renderAuth();
    await act(() => result.current.login('demo@example.com', 'demo1234'));

    await act(async () => {
      await http.get('/cart').catch(() => undefined);
    });

    expect(result.current.user).toBeNull();
    unmount();
    expect(renderAuth().result.current.user).toBeNull(); // the saved session was removed too
  });
});
