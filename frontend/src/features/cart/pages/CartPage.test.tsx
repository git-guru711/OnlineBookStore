import { screen, waitFor, within } from '@testing-library/react';
import { http, HttpResponse } from 'msw';
import { useLocation } from 'react-router-dom';
import { CartPage } from '@/features/cart/pages/CartPage';
import { aCart, aCartItem } from '@/testing/data-generators';
import { API, server } from '@/testing/mocks/server';
import { renderWithProviders, startLoggedIn } from '@/testing/test-utils';

const cleanCode = aCartItem({
  bookId: 1,
  title: 'Clean Code',
  author: 'Robert C. Martin',
  unitPrice: 32.99,
  quantity: 2,
  lineTotal: 65.98,
});
const designPatterns = aCartItem({
  bookId: 2,
  title: 'Design Patterns',
  author: 'Erich Gamma et al.',
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

describe('CartPage', () => {
  beforeEach(() => startLoggedIn());

  it('shows one row per book with title, author, unit price, quantity and line total', async () => {
    serveCart(aCart([cleanCode, designPatterns]));

    renderWithProviders(<CartPage />);

    expect(screen.getByRole('heading', { level: 1, name: 'Your cart' })).toBeInTheDocument();
    const row = await screen.findByRole('row', { name: /Clean Code/ });
    expect(within(row).getByText('Robert C. Martin')).toBeInTheDocument();
    expect(within(row).getByText('€32.99')).toBeInTheDocument();
    expect(within(row).getByRole('textbox', { name: 'Quantity of Clean Code' })).toHaveValue('2');
    expect(within(row).getByText('€65.98')).toBeInTheDocument();
    expect(screen.getByRole('row', { name: /Design Patterns/ })).toBeInTheDocument();
  });

  it('sums up the cart in a summary panel next to the books: subtotal from the server, number of books, Checkout', async () => {
    serveCart(aCart([cleanCode, designPatterns])); // 65.98 + 44.50; 2 + 1 books

    renderWithProviders(<CartPage />);

    const summary = await screen.findByRole('complementary', { name: 'Cart summary' });
    expect(within(summary).getByText('Subtotal (3 books)')).toBeInTheDocument();
    expect(within(summary).getByText('€110.48')).toBeInTheDocument();
    expect(within(summary).getByRole('button', { name: 'Checkout' })).toBeInTheDocument();
    expect(screen.queryByRole('row', { name: /Subtotal/ })).not.toBeInTheDocument();
  });

  it('shows a loading indicator until the cart arrives', async () => {
    serveCart(aCart([cleanCode]));

    renderWithProviders(<CartPage />);

    expect(screen.getByRole('progressbar', { name: 'Loading your cart' })).toBeInTheDocument();
    expect(await screen.findByRole('row', { name: /Clean Code/ })).toBeInTheDocument();
    expect(screen.queryByRole('progressbar')).not.toBeInTheDocument();
  });

  it('shows the error with a "Try again" button that loads the cart again', async () => {
    let attempts = 0;
    server.use(
      http.get(`${API}/cart`, () => {
        attempts += 1;
        return attempts === 1
          ? HttpResponse.json(
              { status: 500, title: 'Internal Server Error', detail: 'Something went wrong.' },
              { status: 500 },
            )
          : HttpResponse.json(aCart([cleanCode]));
      }),
    );
    const { user } = renderWithProviders(<CartPage />);

    expect(await screen.findByText('Could not load your cart')).toBeInTheDocument();
    await user.click(screen.getByRole('button', { name: 'Try again' }));

    expect(await screen.findByRole('row', { name: /Clean Code/ })).toBeInTheDocument();
  });

  it('says the cart is empty and links back to the books instead of showing an empty table', async () => {
    serveCart(aCart([]));

    renderWithProviders(<CartPage />);

    expect(await screen.findByText('Your cart is empty')).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'Browse books' })).toHaveAttribute('href', '/home');
    expect(screen.queryByRole('table')).not.toBeInTheDocument();
  });

  describe('stock problems and checkout', () => {
    it('offers "Checkout" when every book can be bought', async () => {
      serveCart(aCart([cleanCode, designPatterns]));

      renderWithProviders(<CartPage />);

      expect(await screen.findByRole('button', { name: 'Checkout' })).toBeEnabled();
      expect(screen.queryByText(/please reduce|please remove/i)).not.toBeInTheDocument();
    });

    it('"Checkout" leads to the checkout page', async () => {
      serveCart(aCart([cleanCode]));
      const { user } = renderWithProviders(
        <>
          <CartPage />
          <CurrentAddress />
        </>,
        { route: '/cart' },
      );

      await user.click(await screen.findByRole('button', { name: 'Checkout' }));

      expect(screen.getByText('Address: /checkout')).toBeInTheDocument();
    });

    it('warns on a book with fewer copies left than in the cart, and blocks checkout until it is fixed', async () => {
      // stock went down since the books were added
      const tooMany = {
        ...designPatterns,
        quantity: 3,
        lineTotal: 133.5,
        availableQuantity: 1,
        exceedsAvailable: true,
      };
      serveCart(aCart([cleanCode, tooMany])); // aCart works out checkoutAllowed: false

      renderWithProviders(<CartPage />);

      const row = await screen.findByRole('row', { name: /Design Patterns/ });
      expect(within(row).getByText('Only 1 left, please reduce the quantity.')).toBeInTheDocument();
      expect(within(screen.getByRole('row', { name: /Clean Code/ })).queryByText(/please/)).not.toBeInTheDocument();
      expect(screen.getByRole('button', { name: 'Checkout' })).toBeDisabled();
    });

    it('asks to remove a book that sold out since it was added', async () => {
      const soldOut = {
        ...designPatterns,
        availableQuantity: 0,
        stockStatus: 'OUT_OF_STOCK' as const,
        exceedsAvailable: true,
      };
      serveCart(aCart([cleanCode, soldOut]));

      renderWithProviders(<CartPage />);

      const row = await screen.findByRole('row', { name: /Design Patterns/ });
      expect(within(row).getByText('Sold out, please remove it from your cart.')).toBeInTheDocument();
      expect(screen.getByRole('button', { name: 'Checkout' })).toBeDisabled();
    });
  });

  describe('changing the cart', () => {
    it('sends a new quantity and shows the totals the server answers with', async () => {
      let patched: unknown;
      serveCart(aCart([cleanCode, designPatterns]));
      server.use(
        http.patch(`${API}/cart/items/1`, async ({ request }) => {
          patched = await request.json();
          return HttpResponse.json(aCart([{ ...cleanCode, quantity: 3, lineTotal: 98.97 }, designPatterns]));
        }),
      );
      const { user } = renderWithProviders(<CartPage />);
      const row = await screen.findByRole('row', { name: /Clean Code/ });

      await user.click(within(row).getByRole('button', { name: 'Increase quantity of Clean Code' }));

      expect(await within(row).findByText('€98.97')).toBeInTheDocument();
      expect(within(row).getByRole('textbox', { name: 'Quantity of Clean Code' })).toHaveValue('3');
      expect(
        within(screen.getByRole('complementary', { name: 'Cart summary' })).getByText('€143.47'),
      ).toBeInTheDocument();
      expect(patched).toEqual({ quantity: 3 });
    });

    it('sends a typed quantity when the field is left', async () => {
      let patched: unknown;
      serveCart(aCart([cleanCode]));
      server.use(
        http.patch(`${API}/cart/items/1`, async ({ request }) => {
          patched = await request.json();
          return HttpResponse.json(aCart([{ ...cleanCode, quantity: 5, lineTotal: 164.95 }]));
        }),
      );
      const { user } = renderWithProviders(<CartPage />);
      const field = await screen.findByRole('textbox', { name: 'Quantity of Clean Code' });

      await user.clear(field);
      await user.type(field, '5');
      await user.tab();

      expect(await screen.findByText('€164.95', { selector: 'tbody td' })).toBeInTheDocument();
      expect(patched).toEqual({ quantity: 5 });
    });

    it('removes a book: sends DELETE and the row disappears', async () => {
      let cart = aCart([cleanCode, designPatterns]);
      let deleted = false;
      server.use(
        http.get(`${API}/cart`, () => HttpResponse.json(cart)),
        http.delete(`${API}/cart/items/2`, () => {
          deleted = true;
          cart = aCart([cleanCode]);
          return new HttpResponse(null, { status: 204 });
        }),
      );
      const { user } = renderWithProviders(<CartPage />);
      const row = await screen.findByRole('row', { name: /Design Patterns/ });

      await user.click(within(row).getByRole('button', { name: 'Remove Design Patterns' }));

      // the row was found before the click, so this cannot pass without a real removal
      await waitFor(() => expect(screen.queryByRole('row', { name: /Design Patterns/ })).not.toBeInTheDocument());
      expect(deleted).toBe(true);
      expect(screen.getByRole('row', { name: /Clean Code/ })).toBeInTheDocument();
    });
  });

  describe('when a change fails', () => {
    it('explains a stock shortage and loads the cart again, so the page shows the current stock', async () => {
      // 2 × Clean Code in the cart; meanwhile others bought copies: only 2 are left
      let cart = aCart([cleanCode]);
      let loads = 0;
      server.use(
        http.get(`${API}/cart`, () => {
          loads += 1;
          return HttpResponse.json(cart);
        }),
        http.patch(`${API}/cart/items/1`, () => {
          cart = aCart([{ ...cleanCode, availableQuantity: 2 }]); // the server's current state
          return HttpResponse.json(
            {
              type: 'https://bookstore.example/problems/insufficient-stock',
              title: 'Insufficient stock',
              status: 409,
              detail: 'Some items are not available in the requested quantity',
              errors: [{ bookId: 1, title: 'Clean Code', requested: 3, available: 2 }],
            },
            { status: 409 },
          );
        }),
      );
      const { user } = renderWithProviders(<CartPage />);
      const row = await screen.findByRole('row', { name: /Clean Code/ });

      await user.click(within(row).getByRole('button', { name: 'Increase quantity of Clean Code' }));

      expect(await screen.findByRole('alert')).toHaveTextContent(
        'Only 2 copies of Clean Code left, and your cart would need 3.',
      );
      // reloaded: "+" is disabled because 2 is all that is left
      await waitFor(() =>
        expect(within(row).getByRole('button', { name: 'Increase quantity of Clean Code' })).toBeDisabled(),
      );
      expect(within(row).getByRole('textbox', { name: 'Quantity of Clean Code' })).toHaveValue('2');
      expect(loads).toBe(2);
    });
  });
});
