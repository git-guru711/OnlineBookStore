import { screen } from '@testing-library/react';
import { http, HttpResponse } from 'msw';
import { CartBadge } from '@/features/cart/components/CartBadge';
import { aCart, aCartItem } from '@/testing/data-generators';
import { API, server } from '@/testing/mocks/server';
import { renderWithProviders, startLoggedIn } from '@/testing/test-utils';

function serveCart(cart: ReturnType<typeof aCart>) {
  server.use(http.get(`${API}/cart`, () => HttpResponse.json(cart)));
}

describe('CartBadge', () => {
  it('links to the cart and shows how many books are in it (all copies counted)', async () => {
    serveCart(aCart([aCartItem({ bookId: 1, quantity: 2, lineTotal: 65.98 }), aCartItem({ bookId: 2, quantity: 1 })]));
    startLoggedIn();

    renderWithProviders(<CartBadge />);

    const link = await screen.findByRole('link', { name: 'Cart, 3 items' });
    expect(link).toHaveAttribute('href', '/cart');
    expect(link).toHaveTextContent('3');
  });

  it('says "1 item" for a single book', async () => {
    serveCart(aCart([aCartItem({ quantity: 1 })]));
    startLoggedIn();

    renderWithProviders(<CartBadge />);

    expect(await screen.findByRole('link', { name: 'Cart, 1 item' })).toBeInTheDocument();
  });

  it('says "0 items" for an empty cart', async () => {
    serveCart(aCart([]));
    startLoggedIn();

    renderWithProviders(<CartBadge />);

    expect(await screen.findByRole('link', { name: 'Cart, 0 items' })).toBeInTheDocument();
  });

  it('shows nothing to a guest', () => {
    renderWithProviders(<CartBadge />);

    expect(screen.queryByRole('link')).not.toBeInTheDocument();
  });
});
