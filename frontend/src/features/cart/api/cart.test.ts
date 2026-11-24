import { http, HttpResponse } from 'msw';
import { addItem, changeQuantity, clearCart, getCart, removeItem } from '@/features/cart/api/cart';
import { aCart, aCartItem } from '@/testing/data-generators';
import { API, server } from '@/testing/mocks/server';

interface RecordedRequest {
  method: string;
  path: string;
  body: unknown;
}

/** Records every cart request and answers like the backend (the cart, or 204 for DELETE). */
function recordCartRequests(cart = aCart([aCartItem()])) {
  const recorded: RecordedRequest[] = [];
  const answer = async ({ request }: { request: Request }) => {
    const text = await request.text();
    recorded.push({
      method: request.method,
      path: new URL(request.url).pathname.replace('/api/v1', ''),
      body: text ? JSON.parse(text) : undefined,
    });
    return request.method === 'DELETE' ? new HttpResponse(null, { status: 204 }) : HttpResponse.json(cart);
  };
  server.use(http.all(`${API}/cart`, answer), http.all(`${API}/cart/*`, answer));
  return recorded;
}

describe('cart api', () => {
  it('getCart loads the cart with GET /cart', async () => {
    const recorded = recordCartRequests(aCart([aCartItem({ title: 'Clean Code', quantity: 2, lineTotal: 65.98 })]));

    const cart = await getCart();

    expect(recorded).toEqual([{ method: 'GET', path: '/cart', body: undefined }]);
    expect(cart.items.map((item) => item.title)).toEqual(['Clean Code']);
    expect(cart.totalQuantity).toBe(2);
  });

  it('addItem posts the book and quantity, and returns the updated cart', async () => {
    const recorded = recordCartRequests();

    const cart = await addItem({ bookId: 4, quantity: 2 });

    expect(recorded).toEqual([{ method: 'POST', path: '/cart/items', body: { bookId: 4, quantity: 2 } }]);
    expect(cart.items).toHaveLength(1);
  });

  it('addItem adds one copy when no quantity is given', async () => {
    const recorded = recordCartRequests();

    await addItem({ bookId: 4 });

    expect(recorded[0]?.body).toEqual({ bookId: 4, quantity: 1 });
  });

  it('changeQuantity patches the line for that book with the new quantity', async () => {
    const recorded = recordCartRequests();

    await changeQuantity({ bookId: 4, quantity: 3 });

    expect(recorded).toEqual([{ method: 'PATCH', path: '/cart/items/4', body: { quantity: 3 } }]);
  });

  it('removeItem deletes the line for that book', async () => {
    const recorded = recordCartRequests();

    await expect(removeItem(4)).resolves.toBeUndefined();

    expect(recorded).toEqual([{ method: 'DELETE', path: '/cart/items/4', body: undefined }]);
  });

  it('clearCart deletes the whole cart', async () => {
    const recorded = recordCartRequests();

    await expect(clearCart()).resolves.toBeUndefined();

    expect(recorded).toEqual([{ method: 'DELETE', path: '/cart', body: undefined }]);
  });
});
