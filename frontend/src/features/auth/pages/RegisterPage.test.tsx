import { screen, waitFor } from '@testing-library/react';
import { http, HttpResponse } from 'msw';
import { Route, Routes } from 'react-router-dom';
import { RegisterPage } from '@/features/auth/pages/RegisterPage';
import { aUser } from '@/testing/data-generators';
import { API, server } from '@/testing/mocks/server';
import { renderWithProviders } from '@/testing/test-utils';

/** Counts registrations, to prove invalid input never reaches the backend. */
function countRegisterRequests() {
  const calls: unknown[] = [];
  server.use(
    http.post(`${API}/auth/register`, async ({ request }) => {
      calls.push(await request.json());
      return HttpResponse.json(aUser(), { status: 201 });
    }),
  );
  return calls;
}

describe('RegisterPage', () => {
  it('shows a "Create an account" heading with labelled full name, email and password fields', () => {
    renderWithProviders(<RegisterPage />, { route: '/register' });

    expect(screen.getByRole('heading', { level: 1, name: 'Create an account' })).toBeInTheDocument();
    expect(screen.getByLabelText('Full name')).toBeInTheDocument();
    expect(screen.getByLabelText('Email')).toHaveAttribute('type', 'email');
    expect(screen.getByLabelText('Password')).toHaveAttribute('type', 'password');
  });

  it('shows a message under each empty field and sends no request', async () => {
    const calls = countRegisterRequests();
    const { user } = renderWithProviders(<RegisterPage />, { route: '/register' });

    await user.click(screen.getByRole('button', { name: 'Create account' }));

    expect(screen.getByLabelText('Full name')).toHaveAccessibleDescription('Enter your full name');
    expect(screen.getByLabelText('Email')).toHaveAccessibleDescription('Enter your email');
    expect(screen.getByLabelText('Password')).toHaveAccessibleDescription('Enter a password');
    expect(calls).toHaveLength(0);
  });

  it('rejects an invalid email', async () => {
    const calls = countRegisterRequests();
    const { user } = renderWithProviders(<RegisterPage />, { route: '/register' });

    await user.type(screen.getByLabelText('Full name'), 'Sam Reader');
    await user.type(screen.getByLabelText('Email'), 'sam-at-example.com');
    await user.type(screen.getByLabelText('Password'), 'secret123');
    await user.click(screen.getByRole('button', { name: 'Create account' }));

    expect(screen.getByLabelText('Email')).toHaveAccessibleDescription('Enter a valid email address');
    expect(calls).toHaveLength(0);
  });

  it.each([
    ['abc1234', 'Use at least 8 characters'],
    ['onlyletters', 'Use at least one letter and one digit'],
    ['12345678', 'Use at least one letter and one digit'],
    [`a1${'x'.repeat(71)}`, 'Use at most 72 characters'],
  ])('rejects the password "%s" with "%s"', async (password, message) => {
    const calls = countRegisterRequests();
    const { user } = renderWithProviders(<RegisterPage />, { route: '/register' });

    await user.type(screen.getByLabelText('Full name'), 'Sam Reader');
    await user.type(screen.getByLabelText('Email'), 'sam@example.com');
    await user.type(screen.getByLabelText('Password'), password);
    await user.click(screen.getByRole('button', { name: 'Create account' }));

    expect(screen.getByLabelText('Password')).toHaveAccessibleDescription(message);
    expect(calls).toHaveLength(0);
  });

  it('rejects a full name longer than 100 characters', async () => {
    const calls = countRegisterRequests();
    const { user } = renderWithProviders(<RegisterPage />, { route: '/register' });

    await user.type(screen.getByLabelText('Full name'), 'N'.repeat(101));
    await user.type(screen.getByLabelText('Email'), 'sam@example.com');
    await user.type(screen.getByLabelText('Password'), 'secret123');
    await user.click(screen.getByRole('button', { name: 'Create account' }));

    expect(screen.getByLabelText('Full name')).toHaveAccessibleDescription('Use at most 100 characters');
    expect(calls).toHaveLength(0);
  });

  describe('server answers', () => {
    async function submitValidForm(user: ReturnType<typeof renderWithProviders>['user'], email = 'sam@example.com') {
      await user.type(screen.getByLabelText('Full name'), 'Sam Reader');
      await user.type(screen.getByLabelText('Email'), email);
      await user.type(screen.getByLabelText('Password'), 'secret123');
      await user.click(screen.getByRole('button', { name: 'Create account' }));
    }

    it('shows the field errors from a 400 response next to the matching fields', async () => {
      server.use(
        http.post(`${API}/auth/register`, () =>
          HttpResponse.json(
            {
              type: 'https://bookstore.example/problems/validation-error',
              title: 'Validation failed',
              status: 400,
              detail: 'One or more fields are invalid',
              errors: [{ field: 'email', message: 'must be a well-formed email address' }],
            },
            { status: 400 },
          ),
        ),
      );
      const { user } = renderWithProviders(<RegisterPage />, { route: '/register' });

      await submitValidForm(user, 'sam@example'); // passes our simple check, the backend is stricter

      await waitFor(() =>
        expect(screen.getByLabelText('Email')).toHaveAccessibleDescription('must be a well-formed email address'),
      );
    });

    it('says "This email is already registered" for a 409', async () => {
      server.use(
        http.post(`${API}/auth/register`, () =>
          HttpResponse.json(
            {
              type: 'https://bookstore.example/problems/email-already-registered',
              title: 'Email already registered',
              status: 409,
              detail: 'Email sam@example.com is already registered',
            },
            { status: 409 },
          ),
        ),
      );
      const { user } = renderWithProviders(<RegisterPage />, { route: '/register' });

      await submitValidForm(user);

      expect(await screen.findByRole('alert')).toHaveTextContent('This email is already registered');
    });
  });

  describe('after a successful registration', () => {
    const basic = (email: string, password: string) => `Basic ${btoa(`${email}:${password}`)}`;

    /** Registration succeeds; /auth/user accepts only the new account's credentials. */
    function acceptTheNewAccount() {
      const loginChecks: (string | null)[] = [];
      server.use(
        http.post(`${API}/auth/register`, () =>
          HttpResponse.json(aUser({ id: 7, email: 'sam@example.com', fullName: 'Sam Reader' }), { status: 201 }),
        ),
        http.get(`${API}/auth/user`, ({ request }) => {
          loginChecks.push(request.headers.get('Authorization'));
          return request.headers.get('Authorization') === basic('sam@example.com', 'secret123')
            ? HttpResponse.json(aUser({ id: 7, email: 'sam@example.com', fullName: 'Sam Reader' }))
            : HttpResponse.json({ status: 401 }, { status: 401 });
        }),
      );
      return loginChecks;
    }

    function renderRegisterAt(route: string) {
      return renderWithProviders(
        <Routes>
          <Route path="/register" element={<RegisterPage />} />
          <Route path="/home" element={<p>Home page</p>} />
          <Route path="/cart" element={<p>Cart page</p>} />
        </Routes>,
        { route },
      );
    }

    async function fillAndSubmit(user: ReturnType<typeof renderRegisterAt>['user']) {
      await user.type(screen.getByLabelText('Full name'), 'Sam Reader');
      await user.type(screen.getByLabelText('Email'), 'sam@example.com');
      await user.type(screen.getByLabelText('Password'), 'secret123');
      await user.click(screen.getByRole('button', { name: 'Create account' }));
    }

    it('logs in with the new credentials and goes to the home page', async () => {
      const loginChecks = acceptTheNewAccount();
      const { user } = renderRegisterAt('/register');

      await fillAndSubmit(user);

      expect(await screen.findByText('Home page')).toBeInTheDocument();
      expect(loginChecks).toEqual([basic('sam@example.com', 'secret123')]);
    });

    it('goes back to the page in ?redirect, but never to another website', async () => {
      acceptTheNewAccount();
      const { user } = renderRegisterAt('/register?redirect=/cart');

      await fillAndSubmit(user);

      expect(await screen.findByText('Cart page')).toBeInTheDocument();
    });
  });
});
