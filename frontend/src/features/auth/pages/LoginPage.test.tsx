import { screen } from '@testing-library/react';
import { http, HttpResponse } from 'msw';
import { Route, Routes } from 'react-router-dom';
import { LoginPage } from '@/features/auth/pages/LoginPage';
import { aUser } from '@/testing/data-generators';
import { API, server } from '@/testing/mocks/server';
import { renderWithProviders } from '@/testing/test-utils';

/** Counts login checks, to prove invalid input never reaches the backend. */
function countLoginRequests() {
  const calls: string[] = [];
  server.use(
    http.get(`${API}/auth/user`, ({ request }) => {
      calls.push(request.url);
      return HttpResponse.json(aUser());
    }),
  );
  return calls;
}

const basic = (email: string, password: string) => `Basic ${btoa(`${email}:${password}`)}`;

/** Fake backend: only demo@example.com / demo1234 is a valid login. */
function acceptOnlyTheDemoUser() {
  server.use(
    http.get(`${API}/auth/user`, ({ request }) =>
      request.headers.get('Authorization') === basic('demo@example.com', 'demo1234')
        ? HttpResponse.json(aUser({ fullName: 'Demo User' }))
        : HttpResponse.json(
            { type: 'https://bookstore.example/problems/unauthorized', title: 'Unauthorized', status: 401 },
            { status: 401 },
          ),
    ),
  );
}

function renderLoginAt(route: string) {
  return renderWithProviders(
    <Routes>
      <Route path="/login" element={<LoginPage />} />
      <Route path="/home" element={<p>Home page</p>} />
      <Route path="/cart" element={<p>Cart page</p>} />
    </Routes>,
    { route },
  );
}

describe('LoginPage', () => {
  it('shows a "Log in" heading with labelled email and password fields', () => {
    renderWithProviders(<LoginPage />, { route: '/login' });

    expect(screen.getByRole('heading', { level: 1, name: 'Log in' })).toBeInTheDocument();
    expect(screen.getByLabelText('Email')).toHaveAttribute('type', 'email');
    expect(screen.getByLabelText('Password')).toHaveAttribute('type', 'password');
  });

  it('shows a message under each empty field and sends no request', async () => {
    const calls = countLoginRequests();
    const { user } = renderWithProviders(<LoginPage />, { route: '/login' });

    await user.click(screen.getByRole('button', { name: 'Log in' }));

    expect(screen.getByLabelText('Email')).toHaveAccessibleDescription('Enter your email');
    expect(screen.getByLabelText('Password')).toHaveAccessibleDescription('Enter your password');
    expect(calls).toHaveLength(0);
  });

  it('rejects an email without "@" before contacting the backend', async () => {
    const calls = countLoginRequests();
    const { user } = renderWithProviders(<LoginPage />, { route: '/login' });

    await user.type(screen.getByLabelText('Email'), 'not-an-email');
    await user.type(screen.getByLabelText('Password'), 'demo1234');
    await user.click(screen.getByRole('button', { name: 'Log in' }));

    expect(screen.getByLabelText('Email')).toHaveAccessibleDescription('Enter a valid email address');
    expect(calls).toHaveLength(0);
  });

  describe('submitting valid input', () => {
    async function logIn(user: ReturnType<typeof renderLoginAt>['user'], password: string) {
      await user.type(screen.getByLabelText('Email'), 'demo@example.com');
      await user.type(screen.getByLabelText('Password'), password);
      await user.click(screen.getByRole('button', { name: 'Log in' }));
    }

    it('shows "Invalid email or password" for wrong credentials and stays on the page', async () => {
      acceptOnlyTheDemoUser();
      const { user } = renderLoginAt('/login');

      await logIn(user, 'wrong-password1');

      expect(await screen.findByRole('alert')).toHaveTextContent('Invalid email or password');
      expect(screen.getByRole('heading', { name: 'Log in' })).toBeInTheDocument();
    });

    it('goes to the home page after a successful login', async () => {
      acceptOnlyTheDemoUser();
      const { user } = renderLoginAt('/login');

      await logIn(user, 'demo1234');

      expect(await screen.findByText('Home page')).toBeInTheDocument();
    });

    it('goes back to the page in ?redirect after a successful login', async () => {
      acceptOnlyTheDemoUser();
      const { user } = renderLoginAt('/login?redirect=/cart');

      await logIn(user, 'demo1234');

      expect(await screen.findByText('Cart page')).toBeInTheDocument();
    });

    it('ignores a redirect to another website and goes home instead (no open redirect)', async () => {
      acceptOnlyTheDemoUser();
      const { user } = renderLoginAt('/login?redirect=https://evil.example/phishing');

      await logIn(user, 'demo1234');

      expect(await screen.findByText('Home page')).toBeInTheDocument();
    });
  });
});
