import { screen, within } from '@testing-library/react';
import { http, HttpResponse, type JsonBodyType } from 'msw';
import { aCart, aCartItem } from '@/testing/data-generators';
import { API, server } from '@/testing/mocks/server';
import { FLOW_TEST_TIMEOUT, renderApp, startLoggedIn } from '@/testing/test-utils';

const DEMO_AUTH = `Basic ${btoa('demo@example.com:demo1234')}`;

/** Like the real backend: POST /orders turns the cart into order 42 with copied prices and empties the cart. */
function fakeCheckoutBackend() {
  let cart = aCart([aCartItem({ bookId: 1, title: 'Clean Code', unitPrice: 32.99, quantity: 2, lineTotal: 65.98 })]);
  let order: JsonBodyType = null;
  const unauthorized = () => HttpResponse.json({ status: 401, title: 'Unauthorized' }, { status: 401 });
  const loggedIn = (request: Request) => request.headers.get('Authorization') === DEMO_AUTH;

  server.use(
    http.get(`${API}/cart`, ({ request }) => (loggedIn(request) ? HttpResponse.json(cart) : unauthorized())),
    http.post(`${API}/orders`, ({ request }) => {
      if (!loggedIn(request)) return unauthorized();
      order = {
        id: 42,
        status: 'PLACED',
        items: cart.items.map(({ bookId, title, unitPrice, quantity, lineTotal }) => ({
          bookId,
          title,
          unitPrice,
          quantity,
          lineTotal,
        })),
        totalQuantity: cart.totalQuantity,
        totalAmount: cart.subtotal,
        createdAt: '2026-11-23T10:20:00Z',
      };
      cart = aCart([]);
      return HttpResponse.json(order, { status: 201, headers: { Location: '/api/v1/orders/42' } });
    }),
    http.get(`${API}/orders/42`, ({ request }) => (loggedIn(request) ? HttpResponse.json(order) : unauthorized())),
  );
}

describe('Checkout (flow)', { timeout: FLOW_TEST_TIMEOUT }, () => {
  it('goes from the cart to checkout, places the order, and shows the confirmation with an empty cart', async () => {
    fakeCheckoutBackend();
    startLoggedIn();
    const { user } = renderApp({ route: '/cart' });
    const header = screen.getByRole('banner');

    await user.click(await screen.findByRole('button', { name: 'Checkout' }));
    expect(await screen.findByRole('heading', { level: 1, name: 'Checkout' })).toBeInTheDocument();
    const line = await screen.findByRole('row', { name: /Clean Code/ });
    expect(within(line).getByText('2')).toBeInTheDocument();
    expect(within(line).getByText('€65.98')).toBeInTheDocument();
    expect(screen.getByRole('row', { name: /Total.*€65\.98/ })).toBeInTheDocument();

    await user.click(screen.getByRole('button', { name: 'Place order' }));

    expect(await screen.findByRole('heading', { level: 1, name: 'Thank you for your order' })).toBeInTheDocument();
    expect(await screen.findByText('Order number 42')).toBeInTheDocument();
    expect(screen.getByRole('row', { name: /Clean Code/ })).toBeInTheDocument();
    expect(await within(header).findByRole('link', { name: 'Cart, 0 items' })).toBeInTheDocument();
  });
});
