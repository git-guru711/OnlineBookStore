import { screen, within } from '@testing-library/react';
import { http, HttpResponse } from 'msw';
import { aUser } from '@/testing/data-generators';
import { API, server } from '@/testing/mocks/server';
import { FLOW_TEST_TIMEOUT, renderApp } from '@/testing/test-utils';

/** The Basic header the backend expects: "Basic " + base64("email:password"). */
const basic = (email: string, password: string) => `Basic ${btoa(`${email}:${password}`)}`;

describe('Login, logout and registration (flow)', { timeout: FLOW_TEST_TIMEOUT }, () => {
  it('logs in from the header with the demo account, shows the user, and logs out again', async () => {
    server.use(
      http.get(`${API}/auth/user`, ({ request }) =>
        request.headers.get('Authorization') === basic('demo@example.com', 'demo1234')
          ? HttpResponse.json(aUser({ email: 'demo@example.com', fullName: 'Demo User' }))
          : HttpResponse.json(
              { type: 'https://bookstore.example/problems/unauthorized', status: 401 },
              { status: 401 },
            ),
      ),
    );
    const { user } = renderApp({ route: '/home' });
    const header = screen.getByRole('banner');

    await user.click(within(header).getByRole('link', { name: 'Log in' }));
    await user.type(await screen.findByLabelText('Email'), 'demo@example.com');
    await user.type(screen.getByLabelText('Password'), 'demo1234');
    await user.click(screen.getByRole('button', { name: 'Log in' }));

    expect(await screen.findByRole('heading', { level: 1, name: 'Books' })).toBeInTheDocument();
    expect(within(header).getByText('Demo User')).toBeInTheDocument();
    expect(within(header).queryByRole('link', { name: 'Log in' })).not.toBeInTheDocument();

    await user.click(within(header).getByRole('button', { name: 'Account: Demo User' }));
    await user.click(screen.getByRole('menuitem', { name: 'Log out' }));

    expect(within(header).getByRole('link', { name: 'Log in' })).toBeInTheDocument();
    expect(within(header).queryByText('Demo User')).not.toBeInTheDocument();
  });

  it('registers a new account and is logged in straight away', async () => {
    server.use(
      http.post(`${API}/auth/register`, () =>
        HttpResponse.json(aUser({ id: 7, email: 'sam@example.com', fullName: 'Sam Reader' }), { status: 201 }),
      ),
      http.get(`${API}/auth/user`, ({ request }) =>
        request.headers.get('Authorization') === basic('sam@example.com', 'secret123')
          ? HttpResponse.json(aUser({ id: 7, email: 'sam@example.com', fullName: 'Sam Reader' }))
          : HttpResponse.json({ status: 401 }, { status: 401 }),
      ),
    );
    const { user } = renderApp({ route: '/home' });
    const header = screen.getByRole('banner');

    await user.click(within(header).getByRole('link', { name: 'Register' }));
    await user.type(await screen.findByLabelText('Full name'), 'Sam Reader');
    await user.type(screen.getByLabelText('Email'), 'sam@example.com');
    await user.type(screen.getByLabelText('Password'), 'secret123');
    await user.click(screen.getByRole('button', { name: 'Create account' }));

    // the header updates before the page navigates: wait for both
    expect(await within(header).findByText('Sam Reader')).toBeInTheDocument();
    expect(await screen.findByRole('heading', { level: 1, name: 'Books' })).toBeInTheDocument();
  });
});
