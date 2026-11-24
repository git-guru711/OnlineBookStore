import { screen, within } from '@testing-library/react';
import { http, HttpResponse } from 'msw';
import { Route, Routes } from 'react-router-dom';
import { OrderPage } from '@/features/orders/pages/OrderPage';
import { anOrder, anOrderItem } from '@/testing/data-generators';
import { API, server } from '@/testing/mocks/server';
import { renderWithProviders, startLoggedIn } from '@/testing/test-utils';

function renderAt(route: string, routeState?: unknown) {
  return renderWithProviders(
    <Routes>
      <Route path="/orders/:id" element={<OrderPage />} />
    </Routes>,
    { route, routeState },
  );
}

describe('OrderPage', () => {
  beforeEach(() => startLoggedIn());

  it('right after checkout: thanks the user and shows the order number, the ordered books with copied prices, and the total', async () => {
    server.use(
      http.get(`${API}/orders/42`, () =>
        HttpResponse.json(
          anOrder(
            [
              anOrderItem({ bookId: 1, title: 'Clean Code', unitPrice: 32.99, quantity: 2, lineTotal: 65.98 }),
              anOrderItem({ bookId: 2, title: 'Design Patterns', unitPrice: 44.5, quantity: 1, lineTotal: 44.5 }),
            ],
            { id: 42 },
          ),
        ),
      ),
    );

    renderAt('/orders/42', { justPlaced: true }); // what checkout passes along

    expect(await screen.findByRole('heading', { level: 1, name: 'Thank you for your order' })).toBeInTheDocument();
    expect(screen.getByText('Order number 42')).toBeInTheDocument();
    const row = screen.getByRole('row', { name: /Clean Code/ });
    expect(within(row).getByText('€32.99')).toBeInTheDocument();
    expect(within(row).getByText('2')).toBeInTheDocument();
    expect(within(row).getByText('€65.98')).toBeInTheDocument();
    expect(within(screen.getByRole('row', { name: /Total/ })).getByText('€110.48')).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'Continue shopping' })).toHaveAttribute('href', '/home');
  });

  it('opened later (e.g. from "My orders"): a neutral title with the order number and date, no thank-you', async () => {
    server.use(
      http.get(`${API}/orders/42`, () =>
        HttpResponse.json(anOrder(undefined, { id: 42, createdAt: '2026-11-20T12:00:00Z' })),
      ),
    );

    renderAt('/orders/42');

    expect(await screen.findByRole('heading', { level: 1, name: 'Order 42' })).toBeInTheDocument();
    expect(screen.getByText('Placed on 20 Nov 2026')).toBeInTheDocument();
    expect(screen.queryByText('Thank you for your order')).not.toBeInTheDocument();
  });

  it('shows a loading indicator until the order arrives', async () => {
    server.use(http.get(`${API}/orders/42`, () => HttpResponse.json(anOrder(undefined, { id: 42 }))));

    renderAt('/orders/42');

    expect(screen.getByRole('progressbar', { name: 'Loading your order' })).toBeInTheDocument();
    expect(await screen.findByRole('heading', { level: 1, name: 'Order 42' })).toBeInTheDocument();
  });

  it('says "Order not found" for an unknown order, or one that belongs to someone else (404)', async () => {
    server.use(
      http.get(`${API}/orders/99`, () =>
        HttpResponse.json(
          {
            type: 'https://bookstore.example/problems/not-found',
            title: 'Not Found',
            status: 404,
            detail: 'Order 99 not found',
          },
          { status: 404 },
        ),
      ),
    );

    renderAt('/orders/99');

    expect(await screen.findByRole('heading', { level: 1, name: 'Order not found' })).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'Continue shopping' })).toHaveAttribute('href', '/home');
  });

  it('says "Order not found" for an address that is not an order number, without asking the server', () => {
    // no handler for /orders/abc: a request would fail the test
    renderAt('/orders/abc');

    expect(screen.getByRole('heading', { level: 1, name: 'Order not found' })).toBeInTheDocument();
  });
});
