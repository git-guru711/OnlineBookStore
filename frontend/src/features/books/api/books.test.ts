import { http, HttpResponse } from 'msw';
import { getBooks } from '@/features/books/api/books';
import { aBook, aPage } from '@/testing/data-generators';
import { API, server } from '@/testing/mocks/server';

describe('getBooks', () => {
  function captureRequestedUrl() {
    const requested: URL[] = [];
    server.use(
      http.get(`${API}/books`, ({ request }) => {
        requested.push(new URL(request.url));
        return HttpResponse.json(aPage([aBook({ title: 'Clean Code' })]));
      }),
    );
    return requested;
  }

  it('requests the catalog with the given paging and sorting, and returns the page', async () => {
    const requested = captureRequestedUrl();

    const page = await getBooks({ page: 1, size: 5, sort: 'price,desc' });

    expect(requested).toHaveLength(1);
    expect(requested[0]?.searchParams.get('page')).toBe('1');
    expect(requested[0]?.searchParams.get('size')).toBe('5');
    expect(requested[0]?.searchParams.get('sort')).toBe('price,desc');
    expect(page.content.map((book) => book.title)).toEqual(['Clean Code']);
  });

  it('uses the first page of 20 books sorted by title when called without parameters', async () => {
    const requested = captureRequestedUrl();

    await getBooks();

    expect(requested[0]?.searchParams.get('page')).toBe('0');
    expect(requested[0]?.searchParams.get('size')).toBe('20');
    expect(requested[0]?.searchParams.get('sort')).toBe('title,asc');
  });

  it('sends the search text as q', async () => {
    const requested = captureRequestedUrl();

    await getBooks({ q: 'clean code' });

    expect(requested[0]?.searchParams.get('q')).toBe('clean code');
  });

  it('leaves q out when there is no search text', async () => {
    const requested = captureRequestedUrl();

    await getBooks({ q: '' });

    expect(requested[0]?.searchParams.has('q')).toBe(false);
  });
});
