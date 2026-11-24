import { screen, waitFor, within } from '@testing-library/react';
import { http, HttpResponse } from 'msw';
import { useLocation } from 'react-router-dom';
import { CheckoutPage } from '@/features/cart/pages/CheckoutPage';
import { aBook, aCart, aCartItem, anOrder, aPage } from '@/testing/data-generators';
import { API, server } from '@/testing/mocks/server';
import { renderWithProviders, startLoggedIn } from '@/testing/test-utils';

const cleanCode = aCartItem({ bookId: 1, title: 'Clean Code', unitPrice: 32.99, quantity: 2, lineTotal: 65.98 });
const designPatterns = aCartItem({
  bookId: 2,
  title: 'Design Patterns',
  unitPrice: 44.5,
  quantity: 1,
  lineTotal: 44.5,
});

function CurrentAddress() {
  const location = useLocation();
  return <p>Address: {location.pathname}</p>;
}

function serveCart(cart: ReturnType<typeof aCart>) {
  server.use(http.get(`${API}/cart`, () => HttpResponse.json(cart)));
}

describe('CheckoutPage', () => {
  beforeEach(() => startLoggedIn());

  it('shows the order summary: each book with quantity and line total, the total, and "Place order"', async () => {
    serveCart(aCart([cleanCode, designPatterns]));

    renderWithProviders(<CheckoutPage />);

    expect(screen.getByRole('heading', { level: 1, name: 'Checkout' })).toBeInTheDocument();
    const row = await screen.findByRole('row', { name: /Clean Code/ });
    expect(within(row).getByText('€32.99')).toBeInTheDocument();
    expect(within(row).getByText('2')).toBeInTheDocument();
    expect(within(row).getByText('€65.98')).toBeInTheDocument();
    expect(screen.getByRole('row', { name: /Design Patterns/ })).toBeInTheDocument();
    expect(within(screen.getByRole('row', { name: /Total/ })).getByText('€110.48')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Place order' })).toBeEnabled();
  });

  it('is read-only: quantities are changed in the cart, not here', async () => {
    serveCart(aCart([cleanCode]));

    renderWithProviders(<CheckoutPage />);

    await screen.findByRole('row', { name: /Clean Code/ });
    expect(screen.queryByRole('textbox')).not.toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'Back to cart' })).toHaveAttribute('href', '/cart');
  });

  it('shows a loading indicator until the cart arrives', async () => {
    serveCart(aCart([cleanCode]));

    renderWithProviders(<CheckoutPage />);

    expect(screen.getByRole('progressbar', { name: 'Loading your order' })).toBeInTheDocument();
    expect(await screen.findByRole('row', { name: /Clean Code/ })).toBeInTheDocument();
  });

  it('says there is nothing to order for an empty cart, without "Place order"', async () => {
    serveCart(aCart([]));

    renderWithProviders(<CheckoutPage />);

    expect(await screen.findByText('Your cart is empty')).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'Browse books' })).toHaveAttribute('href', '/home');
    expect(screen.queryByRole('button', { name: 'Place order' })).not.toBeInTheDocument();
  });

  it('sends the user back to the cart when some books are short, without "Place order"', async () => {
    const short = { ...designPatterns, quantity: 3, lineTotal: 133.5, availableQuantity: 1, exceedsAvailable: true };
    serveCart(aCart([cleanCode, short])); // checkoutAllowed: false

    renderWithProviders(<CheckoutPage />);

    expect(
      await screen.findByText('Some books are not available in the quantity in your cart. Please fix your cart first.'),
    ).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'Back to cart' })).toHaveAttribute('href', '/cart');
    expect(screen.queryByRole('button', { name: 'Place order' })).not.toBeInTheDocument();
  });

  describe('placing the order', () => {
    it('sends the order, opens its confirmation page, and marks cart and catalog as out of date', async () => {
      let cart = aCart([cleanCode]);
      let cartLoads = 0;
      let ordersPosted = 0;
      server.use(
        http.get(`${API}/cart`, () => {
          cartLoads += 1;
          return HttpResponse.json(cart);
        }),
        http.post(`${API}/orders`, () => {
          ordersPosted += 1;
          cart = aCart([]); // the server empties the cart
          return HttpResponse.json(anOrder(undefined, { id: 42 }), { status: 201 });
        }),
      );
      const { user, queryClient } = renderWithProviders(
        <>
          <CheckoutPage />
          <CurrentAddress />
        </>,
        { route: '/checkout' },
      );
      // a catalog page seen earlier: its stock is outdated once the order is placed
      queryClient.setQueryData(['books', { page: 0, size: 12 }], aPage([aBook()]));

      await user.click(await screen.findByRole('button', { name: 'Place order' }));

      expect(await screen.findByText('Address: /orders/42')).toBeInTheDocument();
      expect(ordersPosted).toBe(1);
      expect(queryClient.getQueryState(['books', { page: 0, size: 12 }])?.isInvalidated).toBe(true);
      expect(cartLoads).toBe(2); // loaded again: now empty, so the header badge shows 0
    });

    it('lists every book that is short when the server refuses, and loads the cart again', async () => {
      let cart = aCart([cleanCode, designPatterns]);
      let cartLoads = 0;
      server.use(
        http.get(`${API}/cart`, () => {
          cartLoads += 1;
          return HttpResponse.json(cart);
        }),
        http.post(`${API}/orders`, () => {
          // others bought copies meanwhile: nothing is ordered
          cart = aCart([
            { ...cleanCode, availableQuantity: 1, exceedsAvailable: true },
            { ...designPatterns, availableQuantity: 0, stockStatus: 'OUT_OF_STOCK', exceedsAvailable: true },
          ]);
          return HttpResponse.json(
            {
              type: 'https://bookstore.example/problems/insufficient-stock',
              title: 'Insufficient stock',
              status: 409,
              detail: 'Some items are not available in the requested quantity',
              errors: [
                { bookId: 1, title: 'Clean Code', requested: 2, available: 1 },
                { bookId: 2, title: 'Design Patterns', requested: 1, available: 0 },
              ],
            },
            { status: 409 },
          );
        }),
      );
      const { user } = renderWithProviders(
        <>
          <CheckoutPage />
          <CurrentAddress />
        </>,
        { route: '/checkout' },
      );

      await user.click(await screen.findByRole('button', { name: 'Place order' }));

      const alert = await screen.findByRole('alert', { name: 'Your order was not placed' });
      expect(within(alert).getByText('Clean Code: 2 in your cart, only 1 left')).toBeInTheDocument();
      expect(within(alert).getByText('Design Patterns: sold out')).toBeInTheDocument();
      expect(screen.getByText('Address: /checkout')).toBeInTheDocument();
      // reloaded: the cart can no longer be ordered, so "Place order" is gone
      await waitFor(() => expect(screen.queryByRole('button', { name: 'Place order' })).not.toBeInTheDocument());
      expect(cartLoads).toBe(2);
    });

    it("shows the server's message for any other refusal (e.g. the cart was emptied in another tab)", async () => {
      serveCart(aCart([cleanCode]));
      server.use(
        http.post(`${API}/orders`, () =>
          HttpResponse.json(
            {
              type: 'https://bookstore.example/problems/cart-empty',
              title: 'Cart is empty',
              status: 400,
              detail: 'Your cart is empty',
            },
            { status: 400 },
          ),
        ),
      );
      const { user } = renderWithProviders(<CheckoutPage />);

      await user.click(await screen.findByRole('button', { name: 'Place order' }));

      const alert = await screen.findByRole('alert', { name: 'Your order was not placed' });
      expect(alert).toHaveTextContent('Your cart is empty');
    });

    it('disables "Place order" while the order is being placed, so a double click orders only once', async () => {
      serveCart(aCart([cleanCode]));
      let ordersPosted = 0;
      let answer: () => void = () => {};
      server.use(
        http.post(`${API}/orders`, async () => {
          ordersPosted += 1;
          await new Promise<void>((resolve) => (answer = resolve)); // the server is still working...
          return HttpResponse.json(anOrder(undefined, { id: 42 }), { status: 201 });
        }),
      );
      const { user } = renderWithProviders(
        <>
          <CheckoutPage />
          <CurrentAddress />
        </>,
        { route: '/checkout' },
      );
      const button = await screen.findByRole('button', { name: 'Place order' });

      await user.dblClick(button);

      expect(button).toBeDisabled();
      answer();
      expect(await screen.findByText('Address: /orders/42')).toBeInTheDocument();
      expect(ordersPosted).toBe(1);
    });

    it('retries with the same Idempotency-Key after a failed attempt, so a lost response can never order twice', async () => {
      serveCart(aCart([cleanCode]));
      const keys: (string | null)[] = [];
      server.use(
        http.post(`${API}/orders`, ({ request }) => {
          keys.push(request.headers.get('Idempotency-Key'));
          return keys.length === 1
            ? HttpResponse.json(
                { status: 503, title: 'Service Unavailable', detail: 'Please try again.' },
                { status: 503 },
              )
            : HttpResponse.json(anOrder(undefined, { id: 42 }), { status: 201 });
        }),
      );
      const { user } = renderWithProviders(
        <>
          <CheckoutPage />
          <CurrentAddress />
        </>,
        { route: '/checkout' },
      );

      await user.click(await screen.findByRole('button', { name: 'Place order' }));
      expect(await screen.findByRole('alert', { name: 'Your order was not placed' })).toBeInTheDocument();
      await user.click(screen.getByRole('button', { name: 'Place order' }));

      expect(await screen.findByText('Address: /orders/42')).toBeInTheDocument();
      expect(keys).toHaveLength(2);
      expect(keys[0]).toMatch(/^[0-9a-f-]{36}$/); // a UUID
      expect(keys[1]).toBe(keys[0]); // the same purchase attempt
    });

    it('uses a new Idempotency-Key for a new visit to checkout (a new purchase)', async () => {
      serveCart(aCart([cleanCode]));
      const keys: (string | null)[] = [];
      server.use(
        http.post(`${API}/orders`, ({ request }) => {
          keys.push(request.headers.get('Idempotency-Key'));
          return HttpResponse.json(anOrder(undefined, { id: 40 + keys.length }), { status: 201 });
        }),
      );

      const first = renderWithProviders(<CheckoutPage />);
      await first.user.click(await screen.findByRole('button', { name: 'Place order' }));
      await waitFor(() => expect(keys).toHaveLength(1));
      first.unmount();
      const second = renderWithProviders(<CheckoutPage />);
      await second.user.click(await screen.findByRole('button', { name: 'Place order' }));
      await waitFor(() => expect(keys).toHaveLength(2));

      expect(keys[1]).not.toBe(keys[0]);
    });
  });
});
