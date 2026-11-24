import { waitFor } from '@testing-library/react';
import { http, HttpResponse } from 'msw';
import { useAddToCart, useCart, useChangeQuantity, useRemoveFromCart } from '@/features/cart/hooks/useCart';
import { aCart, aCartItem } from '@/testing/data-generators';
import { API, server } from '@/testing/mocks/server';
import { renderHookWithProviders, startLoggedIn } from '@/testing/test-utils';

const cleanCode = aCartItem({ bookId: 1, title: 'Clean Code', unitPrice: 32.99, quantity: 1, lineTotal: 32.99 });
const designPatterns = aCartItem({
  bookId: 2,
  title: 'Design Patterns',
  unitPrice: 44.5,
  quantity: 1,
  lineTotal: 44.5,
});

function serveCart(cart = aCart([cleanCode])) {
  const loads = { count: 0 };
  server.use(
    http.get(`${API}/cart`, () => {
      loads.count += 1;
      return HttpResponse.json(cart);
    }),
  );
  return loads;
}

describe('useCart', () => {
  it('loads nothing for a guest: a guest has no cart', async () => {
    const loads = serveCart();

    const { result } = renderHookWithProviders(() => useCart());

    // give a request the chance to happen, then check that none did
    await new Promise((resolve) => setTimeout(resolve, 50));
    expect(loads.count).toBe(0);
    expect(result.current.data).toBeUndefined();
  });

  it("loads the logged-in user's cart", async () => {
    serveCart(aCart([cleanCode]));
    startLoggedIn();

    const { result } = renderHookWithProviders(() => useCart());

    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(result.current.data?.items.map((item) => item.title)).toEqual(['Clean Code']);
  });
});

describe('cart changes', () => {
  function renderCartWithChanges() {
    return renderHookWithProviders(() => ({
      cart: useCart(),
      add: useAddToCart(),
      change: useChangeQuantity(),
      remove: useRemoveFromCart(),
    }));
  }

  beforeEach(() => startLoggedIn());

  it('adding a book shows the cart the server answered with, without loading the cart again', async () => {
    const loads = serveCart(aCart([cleanCode]));
    server.use(
      http.post(`${API}/cart/items`, () => HttpResponse.json(aCart([cleanCode, designPatterns]), { status: 201 })),
    );
    const { result } = renderCartWithChanges();
    await waitFor(() => expect(result.current.cart.isSuccess).toBe(true));

    await result.current.add.mutateAsync({ bookId: 2 });

    await waitFor(() => expect(result.current.cart.data?.totalQuantity).toBe(2));
    expect(loads.count).toBe(1); // only the first load: the POST answer went straight into the cache
  });

  it('changing a quantity shows the cart the server answered with, without loading the cart again', async () => {
    const loads = serveCart(aCart([cleanCode]));
    server.use(
      http.patch(`${API}/cart/items/1`, () =>
        HttpResponse.json(aCart([{ ...cleanCode, quantity: 3, lineTotal: 98.97 }])),
      ),
    );
    const { result } = renderCartWithChanges();
    await waitFor(() => expect(result.current.cart.isSuccess).toBe(true));

    await result.current.change.mutateAsync({ bookId: 1, quantity: 3 });

    await waitFor(() => expect(result.current.cart.data?.subtotal).toBe(98.97));
    expect(loads.count).toBe(1);
  });

  it('removing a book loads the cart again, because DELETE answers with no content', async () => {
    let cart = aCart([cleanCode, designPatterns]);
    const loads = { count: 0 };
    server.use(
      http.get(`${API}/cart`, () => {
        loads.count += 1;
        return HttpResponse.json(cart);
      }),
      http.delete(`${API}/cart/items/2`, () => {
        cart = aCart([cleanCode]); // the server's cart after the delete
        return new HttpResponse(null, { status: 204 });
      }),
    );
    const { result } = renderCartWithChanges();
    await waitFor(() => expect(result.current.cart.data?.items).toHaveLength(2));

    await result.current.remove.mutateAsync(2);

    await waitFor(() => expect(result.current.cart.data?.items.map((item) => item.title)).toEqual(['Clean Code']));
    expect(loads.count).toBe(2);
  });
});
