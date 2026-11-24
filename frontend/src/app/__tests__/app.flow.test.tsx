import { screen, within } from '@testing-library/react';
import { http, HttpResponse } from 'msw';
import { useLocation } from 'react-router-dom';
import { AppRoutes } from '@/app/router';
import { aBook, aPage } from '@/testing/data-generators';
import { API, server } from '@/testing/mocks/server';
import { FLOW_TEST_TIMEOUT, renderApp, renderWithProviders } from '@/testing/test-utils';

function CurrentAddress() {
  const location = useLocation();
  return <p>Address: {location.pathname + location.search}</p>;
}

function renderAppWithAddress(route: string) {
  return renderWithProviders(
    <>
      <AppRoutes />
      <CurrentAddress />
    </>,
    { route },
  );
}

describe('App shell (flow)', { timeout: FLOW_TEST_TIMEOUT }, () => {
  it('shows the site header with a home link and a main region on the home page', () => {
    renderApp({ route: '/home' });

    const banner = screen.getByRole('banner');
    expect(within(banner).getByRole('link', { name: 'Bookstore' })).toHaveAttribute('href', '/home');
    expect(screen.getByRole('main')).toBeInTheDocument();
  });

  it('redirects the site root "/" to the home page "/home"', async () => {
    renderAppWithAddress('/');

    expect(await screen.findByText('Address: /home')).toBeInTheDocument();
    expect(screen.getByRole('heading', { level: 1, name: 'Books' })).toBeInTheDocument();
  });

  it('keeps the query when redirecting, so old links like "/?page=2" still open that page', async () => {
    renderAppWithAddress('/?page=2&q=martin');

    expect(await screen.findByText('Address: /home?page=2&q=martin')).toBeInTheDocument();
  });

  it('pages through the catalog at /home?page=N', async () => {
    server.use(
      http.get(`${API}/books`, ({ request }) => {
        const page = Number(new URL(request.url).searchParams.get('page') ?? 0);
        return HttpResponse.json(
          aPage([aBook({ id: page + 1, title: `Book on page ${page + 1}` })], {
            page,
            totalPages: 3,
            totalElements: 3,
          }),
        );
      }),
    );
    const { user } = renderAppWithAddress('/');

    const pages = await screen.findByRole('navigation', { name: 'Book pages' });
    await user.click(within(pages).getByRole('button', { name: 'Go to page 2' }));

    expect(await screen.findByRole('article', { name: 'Book on page 2' })).toBeInTheDocument();
    expect(screen.getByText('Address: /home?page=2')).toBeInTheDocument();
  });

  it('shows "Page not found" for an unknown URL, still inside the layout', () => {
    renderApp({ route: '/does-not-exist' });

    expect(screen.getByRole('heading', { name: 'Page not found' })).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'Back to the bookstore' })).toHaveAttribute('href', '/home');
    expect(screen.getByRole('banner')).toBeInTheDocument();
  });

  it('navigates home without a page reload when the logo is clicked', async () => {
    const { user } = renderAppWithAddress('/does-not-exist');

    await user.click(within(screen.getByRole('banner')).getByRole('link', { name: 'Bookstore' }));

    expect(screen.queryByRole('heading', { name: 'Page not found' })).not.toBeInTheDocument();
    expect(screen.getByText('Address: /home')).toBeInTheDocument();
  });
});
