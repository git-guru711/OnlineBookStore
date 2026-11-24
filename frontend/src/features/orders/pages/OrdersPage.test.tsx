import { screen, within } from '@testing-library/react';
import { http, HttpResponse } from 'msw';
import { useLocation } from 'react-router-dom';
import { OrdersPage } from '@/features/orders/pages/OrdersPage';
import type { OrderSummary } from '@/features/orders/types';
import { aPage } from '@/testing/data-generators';
import { API, server } from '@/testing/mocks/server';
import { renderWithProviders, startLoggedIn } from '@/testing/test-utils';

const anOrderSummary = (overrides: Partial<OrderSummary> = {}): OrderSummary => ({
  id: 1,
  status: 'PLACED',
  totalQuantity: 2,
  totalAmount: 65.98,
  createdAt: '2026-11-20T10:00:00Z',
  ...overrides,
});

function CurrentAddress() {
  const location = useLocation();
  return <p>Address: {location.pathname + location.search}</p>;
}

describe('OrdersPage', () => {
  beforeEach(() => startLoggedIn());

  it('lists the orders in the order the server sends them (newest first): number, date, books and total', async () => {
    server.use(
      http.get(`${API}/orders`, () =>
        HttpResponse.json(
          aPage([
            anOrderSummary({ id: 2, totalQuantity: 1, totalAmount: 39.95, createdAt: '2026-11-22T12:00:00Z' }),
            anOrderSummary({ id: 1, totalQuantity: 2, totalAmount: 65.98, createdAt: '2026-11-20T12:00:00Z' }),
          ]),
        ),
      ),
    );

    renderWithProviders(<OrdersPage />);

    expect(screen.getByRole('heading', { level: 1, name: 'My orders' })).toBeInTheDocument();
    const rows = await screen.findAllByRole('row', { name: /Order \d/ });
    expect(rows).toHaveLength(2);
    expect(within(rows[0]!).getByRole('link', { name: 'Order 2' })).toHaveAttribute('href', '/orders/2');
    expect(within(rows[0]!).getByText('22 Nov 2026')).toBeInTheDocument();
    expect(within(rows[0]!).getByText('1')).toBeInTheDocument();
    expect(within(rows[0]!).getByText('€39.95')).toBeInTheDocument();
    expect(within(rows[1]!).getByRole('link', { name: 'Order 1' })).toHaveAttribute('href', '/orders/1');
  });

  it('says there are no orders yet and links to the books', async () => {
    server.use(http.get(`${API}/orders`, () => HttpResponse.json(aPage([]))));

    renderWithProviders(<OrdersPage />);

    expect(await screen.findByText('You have no orders yet')).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'Browse books' })).toHaveAttribute('href', '/home');
    expect(screen.queryByRole('table')).not.toBeInTheDocument();
  });

  it('shows a loading indicator until the orders arrive', async () => {
    server.use(http.get(`${API}/orders`, () => HttpResponse.json(aPage([anOrderSummary()]))));

    renderWithProviders(<OrdersPage />);

    expect(screen.getByRole('progressbar', { name: 'Loading your orders' })).toBeInTheDocument();
    expect(await screen.findByRole('link', { name: 'Order 1' })).toBeInTheDocument();
  });

  it('shows the error with a "Try again" button that loads the orders again', async () => {
    let attempts = 0;
    server.use(
      http.get(`${API}/orders`, () => {
        attempts += 1;
        return attempts === 1
          ? HttpResponse.json(
              { status: 500, title: 'Internal Server Error', detail: 'Something went wrong.' },
              { status: 500 },
            )
          : HttpResponse.json(aPage([anOrderSummary()]));
      }),
    );
    const { user } = renderWithProviders(<OrdersPage />);

    expect(await screen.findByText('Could not load your orders')).toBeInTheDocument();
    await user.click(screen.getByRole('button', { name: 'Try again' }));

    expect(await screen.findByRole('link', { name: 'Order 1' })).toBeInTheDocument();
  });

  it('pages through a long history; the page is kept in the address (?page=2)', async () => {
    const requestedPages: string[] = [];
    server.use(
      http.get(`${API}/orders`, ({ request }) => {
        const page = Number(new URL(request.url).searchParams.get('page'));
        requestedPages.push(String(page));
        return HttpResponse.json(
          aPage([anOrderSummary({ id: 20 - page })], { page, size: 10, totalElements: 15, totalPages: 2 }),
        );
      }),
    );
    const { user } = renderWithProviders(
      <>
        <OrdersPage />
        <CurrentAddress />
      </>,
      { route: '/orders' },
    );

    await user.click(await screen.findByRole('button', { name: 'Go to page 2' }));

    expect(await screen.findByRole('link', { name: 'Order 19' })).toBeInTheDocument();
    expect(screen.getByText('Address: /orders?page=2')).toBeInTheDocument();
    expect(requestedPages).toEqual(['0', '1']);
  });
});
