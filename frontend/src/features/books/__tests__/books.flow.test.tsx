import { screen, within } from '@testing-library/react';
import { http, HttpResponse } from 'msw';
import { aBook, aPage } from '@/testing/data-generators';
import { API, server } from '@/testing/mocks/server';
import { FLOW_TEST_TIMEOUT, renderApp } from '@/testing/test-utils';

describe('Book list (flow)', { timeout: FLOW_TEST_TIMEOUT }, () => {
  it('shows each book with title, author and price; sold-out books cannot be added to the cart', async () => {
    server.use(
      http.get(`${API}/books`, () =>
        HttpResponse.json(
          aPage([
            aBook({ id: 1, title: 'Clean Code', author: 'Robert C. Martin', price: 32.99 }),
            aBook({
              id: 3,
              title: 'Refactoring',
              author: 'Martin Fowler',
              price: 42,
              availableQuantity: 0,
              stockStatus: 'OUT_OF_STOCK',
            }),
          ]),
        ),
      ),
    );

    renderApp({ route: '/home' });

    const cleanCode = await screen.findByRole('article', { name: 'Clean Code' });
    expect(within(cleanCode).getByText('Robert C. Martin')).toBeInTheDocument();
    expect(within(cleanCode).getByText('€32.99')).toBeInTheDocument();
    expect(within(cleanCode).getByRole('button', { name: 'Add to cart' })).toBeEnabled();

    const refactoring = screen.getByRole('article', { name: 'Refactoring' });
    expect(within(refactoring).getByText('€42.00')).toBeInTheDocument();
    expect(within(refactoring).getByText('Out of stock')).toBeInTheDocument();
    expect(within(refactoring).getByRole('button', { name: 'Add to cart' })).toBeDisabled();
  });
});
