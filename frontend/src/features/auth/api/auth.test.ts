import { http, HttpResponse } from 'msw';
import { fetchMe, register } from '@/features/auth/api/auth';
import { ApiError, onUnauthorized } from '@/lib/api-client';
import { aUser } from '@/testing/data-generators';
import { API, server } from '@/testing/mocks/server';

const basic = (email: string, password: string) => `Basic ${btoa(`${email}:${password}`)}`;

describe('auth api', () => {
  describe('register', () => {
    it('posts the new account as JSON and returns the created user', async () => {
      let received: unknown;
      server.use(
        http.post(`${API}/auth/register`, async ({ request }) => {
          received = await request.json();
          return HttpResponse.json(aUser({ id: 7, email: 'sam@example.com', fullName: 'Sam Reader' }), { status: 201 });
        }),
      );

      const user = await register({ email: 'sam@example.com', password: 'secret123', fullName: 'Sam Reader' });

      expect(received).toEqual({ email: 'sam@example.com', password: 'secret123', fullName: 'Sam Reader' });
      expect(user).toMatchObject({ id: 7, fullName: 'Sam Reader' });
    });
  });

  describe('fetchMe', () => {
    it('checks the given credentials with Basic auth and returns the user', async () => {
      let authorization: string | null = null;
      server.use(
        http.get(`${API}/auth/user`, ({ request }) => {
          authorization = request.headers.get('Authorization');
          return HttpResponse.json(aUser({ fullName: 'Demo User' }));
        }),
      );

      const user = await fetchMe('demo@example.com', 'demo1234');

      expect(authorization).toBe(basic('demo@example.com', 'demo1234'));
      expect(user.fullName).toBe('Demo User');
    });

    it('rejects wrong credentials with a 401 ApiError, without triggering the logout listener', async () => {
      server.use(
        http.get(`${API}/auth/user`, () =>
          HttpResponse.json(
            { type: 'https://bookstore.example/problems/unauthorized', title: 'Unauthorized', status: 401 },
            { status: 401 },
          ),
        ),
      );
      const logoutListener = vi.fn();
      const unsubscribe = onUnauthorized(logoutListener);

      const error = await fetchMe('demo@example.com', 'wrong-password1').catch((e: unknown) => e);

      expect(error).toBeInstanceOf(ApiError);
      expect(error).toMatchObject({ status: 401, type: 'unauthorized' });
      expect(logoutListener).not.toHaveBeenCalled();
      unsubscribe();
    });
  });
});
