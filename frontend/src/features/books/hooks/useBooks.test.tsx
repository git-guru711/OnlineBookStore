import { waitFor } from '@testing-library/react';
import { http, HttpResponse } from 'msw';
import { useBooks } from '@/features/books/hooks/useBooks';
import { ApiError } from '@/lib/api-client';
import { aBook, aPage } from '@/testing/data-generators';
import { API, server } from '@/testing/mocks/server';
import { renderHookWithProviders } from '@/testing/test-utils';

describe('useBooks', () => {
  it('is loading at first, then returns the page of books', async () => {
    server.use(http.get(`${API}/books`, () => HttpResponse.json(aPage([aBook({ title: 'Clean Code' })]))));

    const { result } = renderHookWithProviders(() => useBooks());

    expect(result.current.isPending).toBe(true);
    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(result.current.data?.content.map((book) => book.title)).toEqual(['Clean Code']);
  });

  it('exposes the failure as an ApiError', async () => {
    server.use(
      http.get(`${API}/books`, () =>
        HttpResponse.json(
          { type: 'https://bookstore.example/problems/internal-error', title: 'Internal server error', status: 500 },
          { status: 500 },
        ),
      ),
    );

    const { result } = renderHookWithProviders(() => useBooks());

    await waitFor(() => expect(result.current.isError).toBe(true));
    expect(result.current.error).toBeInstanceOf(ApiError);
    expect(result.current.error).toMatchObject({ status: 500, type: 'internal-error' });
  });
});
