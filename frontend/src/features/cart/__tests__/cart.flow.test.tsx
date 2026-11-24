import { screen, within } from '@testing-library/react';
import { http, HttpResponse } from 'msw';
import { aBook, aPage } from '@/testing/data-generators';
import { API, server } from '@/testing/mocks/server';
import { FLOW_TEST_TIMEOUT, renderApp, startLoggedIn } from '@/testing/test-utils';

const DEMO_AUTH = `Basic ${btoa('demo@example.com:demo1234')}`;

const cleanCode = aBook({ id: 1, title: 'Clean Code', price: 32.99, availableQuantity: 15 });
const designPatterns = aBook({ id: 2, title: 'Design Patterns', price: 44.5, availableQuantity: 8 });

/** In-memory cart backend: needs the demo credentials and answers every change with the whole cart. */
function fakeCartBackend() {
  const quantities = new Map<number, number>(); // bookId -> quantity, in the order added

  const cartResponse = () => {
    const items = [...quantities].map(([bookId, quantity]) => {
      const book = [cleanCode, designPatterns].find((b) => b.id === bookId)!;
      return {
        bookId,
        title: book.title,
        author: book.author,
        unitPrice: book.price,
        quantity,
        lineTotal: Math.round(book.price * quantity * 100) / 100,
        availableQuantity: book.availableQuantity,
        stockStatus: book.stockStatus,
        exceedsAvailable: quantity > book.availableQuantity,
      };
    });
    return {
      items,
      totalQuantity: items.reduce((sum, item) => sum + item.quantity, 0),
      subtotal: Math.round(items.reduce((sum, item) => sum + item.lineTotal, 0) * 100) / 100,
      checkoutAllowed: items.length > 0 && items.every((item) => !item.exceedsAvailable),
    };
  };
  const unauthorized = () => HttpResponse.json({ status: 401, title: 'Unauthorized' }, { status: 401 });
  const loggedIn = (request: Request) => request.headers.get('Authorization') === DEMO_AUTH;

  server.use(
    http.get(`${API}/books`, () => HttpResponse.json(aPage([cleanCode, designPatterns]))),
    http.get(`${API}/cart`, ({ request }) => (loggedIn(request) ? HttpResponse.json(cartResponse()) : unauthorized())),
    http.post(`${API}/cart/items`, async ({ request }) => {
      if (!loggedIn(request)) return unauthorized();
      const { bookId, quantity } = (await request.json()) as { bookId: number; quantity: number };
      const isNew = !quantities.has(bookId);
      quantities.set(bookId, (quantities.get(bookId) ?? 0) + quantity);
      return HttpResponse.json(cartResponse(), { status: isNew ? 201 : 200 });
    }),
    http.patch(`${API}/cart/items/:bookId`, async ({ request, params }) => {
      if (!loggedIn(request)) return unauthorized();
      const { quantity } = (await request.json()) as { quantity: number };
      quantities.set(Number(params.bookId), quantity);
      return HttpResponse.json(cartResponse());
    }),
    http.delete(`${API}/cart/items/:bookId`, ({ request, params }) => {
      if (!loggedIn(request)) return unauthorized();
      quantities.delete(Number(params.bookId));
      return new HttpResponse(null, { status: 204 });
    }),
  );
}

describe('Shopping cart (flow)', { timeout: FLOW_TEST_TIMEOUT }, () => {
  it('adds two books, sees the count in the header, then changes a quantity and removes a book in the cart', async () => {
    fakeCartBackend();
    startLoggedIn();
    const { user } = renderApp({ route: '/home' });
    const header = screen.getByRole('banner');

    await user.click(
      within(await screen.findByRole('article', { name: 'Clean Code' })).getByRole('button', { name: 'Add to cart' }),
    );
    await user.click(
      within(screen.getByRole('article', { name: 'Design Patterns' })).getByRole('button', { name: 'Add to cart' }),
    );
    expect(await within(header).findByRole('link', { name: 'Cart, 2 items' })).toBeInTheDocument();

    await user.click(within(header).getByRole('link', { name: 'Cart, 2 items' }));
    expect(await screen.findByRole('heading', { level: 1, name: 'Your cart' })).toBeInTheDocument();
    expect(await screen.findByRole('row', { name: /Clean Code/ })).toBeInTheDocument();
    expect(screen.getByRole('row', { name: /Design Patterns/ })).toBeInTheDocument();

    await user.click(screen.getByRole('button', { name: 'Increase quantity of Clean Code' }));
    await user.click(screen.getByRole('button', { name: 'Remove Design Patterns' }));

    expect(await within(header).findByRole('link', { name: 'Cart, 2 items' })).toBeInTheDocument(); // 2 × Clean Code
    expect(
      await within(screen.getByRole('complementary', { name: 'Cart summary' })).findByText('€65.98'),
    ).toBeInTheDocument(); // 2 × €32.99
    expect(screen.queryByRole('row', { name: /Design Patterns/ })).not.toBeInTheDocument();
  });
});
