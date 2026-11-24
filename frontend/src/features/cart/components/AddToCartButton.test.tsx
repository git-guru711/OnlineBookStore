import { screen, within } from '@testing-library/react';
import { http, HttpResponse } from 'msw';
import { useLocation } from 'react-router-dom';
import { AddToCartButton } from '@/features/cart/components/AddToCartButton';
import { aCart, aCartItem } from '@/testing/data-generators';
import { API, server } from '@/testing/mocks/server';
import { renderWithProviders, startLoggedIn } from '@/testing/test-utils';

function CurrentAddress() {
  const location = useLocation();
  return <p>Address: {location.pathname + location.search}</p>;
}

describe('AddToCartButton', () => {
  it('is enabled for a book in stock', () => {
    renderWithProviders(<AddToCartButton bookId={1} soldOut={false} />);

    expect(screen.getByRole('button', { name: 'Add to cart' })).toBeEnabled();
  });

  it('is disabled for a sold-out book', () => {
    renderWithProviders(<AddToCartButton bookId={1} soldOut />);

    expect(screen.getByRole('button', { name: 'Add to cart' })).toBeDisabled();
  });

  it('sends a guest to the login page, to come back to the same page afterwards', async () => {
    const { user } = renderWithProviders(
      <>
        <AddToCartButton bookId={1} soldOut={false} />
        <CurrentAddress />
      </>,
      { route: '/home?page=2' },
    );

    await user.click(screen.getByRole('button', { name: 'Add to cart' }));

    expect(screen.getByText(`Address: /login?redirect=${encodeURIComponent('/home?page=2')}`)).toBeInTheDocument();
  });

  it('shows its messages outside the book card, so the card cannot move them (a hover transform used to)', async () => {
    server.use(
      http.post(`${API}/cart/items`, () => HttpResponse.json(aCart([aCartItem({ bookId: 7 })]), { status: 201 })),
    );
    startLoggedIn();
    const { user } = renderWithProviders(
      <article aria-label="Book card">
        <AddToCartButton bookId={7} soldOut={false} />
      </article>,
    );

    await user.click(screen.getByRole('button', { name: 'Add to cart' }));

    expect(await screen.findByText('Added to cart')).toBeInTheDocument();
    expect(
      within(screen.getByRole('article', { name: 'Book card' })).queryByText('Added to cart'),
    ).not.toBeInTheDocument();
  });

  it('adds one copy of the book for a logged-in user and confirms it', async () => {
    let received: unknown;
    server.use(
      http.post(`${API}/cart/items`, async ({ request }) => {
        received = await request.json();
        return HttpResponse.json(aCart([aCartItem({ bookId: 7 })]), { status: 201 });
      }),
    );
    startLoggedIn();
    const { user } = renderWithProviders(<AddToCartButton bookId={7} soldOut={false} />);

    await user.click(screen.getByRole('button', { name: 'Add to cart' }));

    expect(await screen.findByText('Added to cart')).toBeInTheDocument();
    expect(received).toEqual({ bookId: 7, quantity: 1 });
  });

  describe('when the server refuses', () => {
    /** The backend's 409 for a stock shortage: a generic detail plus one entry per short book. */
    function refuseWithShortage(shortage: { bookId: number; title: string; requested: number; available: number }) {
      server.use(
        http.post(`${API}/cart/items`, () =>
          HttpResponse.json(
            {
              type: 'https://bookstore.example/problems/insufficient-stock',
              title: 'Insufficient stock',
              status: 409,
              detail: 'Some items are not available in the requested quantity',
              errors: [shortage],
            },
            { status: 409 },
          ),
        ),
      );
    }

    beforeEach(() => startLoggedIn());

    it('says how many copies are left when the cart would need more (e.g. the last copy is already in it)', async () => {
      refuseWithShortage({ bookId: 4, title: 'Domain-Driven Design', requested: 2, available: 1 });
      const { user } = renderWithProviders(<AddToCartButton bookId={4} soldOut={false} />);

      await user.click(screen.getByRole('button', { name: 'Add to cart' }));

      expect(await screen.findByRole('alert')).toHaveTextContent(
        'Only 1 copy of Domain-Driven Design left, and your cart would need 2.',
      );
      expect(screen.queryByText('Added to cart')).not.toBeInTheDocument();
    });

    it('says the book is sold out when the last copy went since the page was loaded', async () => {
      refuseWithShortage({ bookId: 4, title: 'Domain-Driven Design', requested: 1, available: 0 });
      const { user } = renderWithProviders(<AddToCartButton bookId={4} soldOut={false} />);

      await user.click(screen.getByRole('button', { name: 'Add to cart' }));

      expect(await screen.findByRole('alert')).toHaveTextContent('Domain-Driven Design is sold out.');
    });

    it("shows the server's message for any other failure", async () => {
      server.use(
        http.post(`${API}/cart/items`, () =>
          HttpResponse.json(
            {
              type: 'https://bookstore.example/problems/concurrent-modification',
              title: 'Concurrent modification',
              status: 409,
              detail: 'The cart was changed at the same time. Please try again.',
            },
            { status: 409 },
          ),
        ),
      );
      const { user } = renderWithProviders(<AddToCartButton bookId={4} soldOut={false} />);

      await user.click(screen.getByRole('button', { name: 'Add to cart' }));

      expect(await screen.findByRole('alert')).toHaveTextContent(
        'The cart was changed at the same time. Please try again.',
      );
    });
  });
});
