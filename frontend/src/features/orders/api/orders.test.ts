import { http, HttpResponse } from 'msw';
import { getOrder, listOrders } from '@/features/orders/api/orders';
import { ApiError } from '@/lib/api-client';
import { anOrder, anOrderItem, aPage } from '@/testing/data-generators';
import { API, server } from '@/testing/mocks/server';

describe('getOrder', () => {
  it('loads one order with GET /orders/{id}', async () => {
    server.use(
      http.get(`${API}/orders/42`, () =>
        HttpResponse.json(anOrder([anOrderItem({ title: 'Clean Code', quantity: 2, lineTotal: 65.98 })], { id: 42 })),
      ),
    );

    const order = await getOrder(42);

    expect(order.id).toBe(42);
    expect(order.items.map((item) => item.title)).toEqual(['Clean Code']);
    expect(order.totalAmount).toBe(65.98);
  });

  it("rejects with a 404 ApiError for an unknown order (or another user's)", async () => {
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

    await expect(getOrder(99)).rejects.toMatchObject({ status: 404 });
    await expect(getOrder(99)).rejects.toBeInstanceOf(ApiError);
  });
});

describe('listOrders', () => {
  function captureRequestedUrl() {
    const requested: URL[] = [];
    server.use(
      http.get(`${API}/orders`, ({ request }) => {
        requested.push(new URL(request.url));
        return HttpResponse.json(
          aPage([{ id: 2, status: 'PLACED', totalQuantity: 1, totalAmount: 39.95, createdAt: '2026-11-22T15:30:00Z' }]),
        );
      }),
    );
    return requested;
  }

  it("requests one page of the user's orders and returns it (the server sorts newest first)", async () => {
    const requested = captureRequestedUrl();

    const page = await listOrders({ page: 1, size: 5 });

    expect(requested[0]?.searchParams.get('page')).toBe('1');
    expect(requested[0]?.searchParams.get('size')).toBe('5');
    expect(page.content.map((order) => order.id)).toEqual([2]);
  });

  it('asks for the first page of 10 orders by default', async () => {
    const requested = captureRequestedUrl();

    await listOrders();

    expect(requested[0]?.searchParams.get('page')).toBe('0');
    expect(requested[0]?.searchParams.get('size')).toBe('10');
  });
});
