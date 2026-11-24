import { http, HttpResponse } from 'msw';
import { placeOrder } from '@/features/cart/api/checkout';
import { anOrder } from '@/testing/data-generators';
import { API, server } from '@/testing/mocks/server';

describe('placeOrder', () => {
  it('posts to /orders without a body (the server orders what is in the cart) and returns the new order id', async () => {
    const received: { method: string; body: string; key: string | null }[] = [];
    server.use(
      http.post(`${API}/orders`, async ({ request }) => {
        received.push({
          method: request.method,
          body: await request.text(),
          key: request.headers.get('Idempotency-Key'),
        });
        return HttpResponse.json(anOrder(undefined, { id: 42 }), { status: 201 });
      }),
    );

    const order = await placeOrder('3f2a9c1e-7b4d-4e8a-9c21-5d6e7f8a9b0c');

    expect(received).toEqual([{ method: 'POST', body: '', key: '3f2a9c1e-7b4d-4e8a-9c21-5d6e7f8a9b0c' }]);
    expect(order.id).toBe(42);
  });
});
