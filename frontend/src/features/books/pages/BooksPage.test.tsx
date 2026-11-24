import { screen, within } from '@testing-library/react';
import { http, HttpResponse } from 'msw';
import { useLocation } from 'react-router-dom';
import { BooksPage } from '@/features/books/pages/BooksPage';
import { aBook, aPage } from '@/testing/data-generators';
import { API, server } from '@/testing/mocks/server';
import { renderWithProviders } from '@/testing/test-utils';

function CurrentAddress() {
  const location = useLocation();
  return <p>Address: {location.pathname + location.search}</p>;
}

/** 3 pages, one book each; records every requested URL. */
function threePageCatalog() {
  const requested: URL[] = [];
  server.use(
    http.get(`${API}/books`, ({ request }) => {
      const url = new URL(request.url);
      requested.push(url);
      const page = Number(url.searchParams.get('page'));
      return HttpResponse.json(
        aPage([aBook({ id: page + 1, title: `Book on page ${page + 1}` })], {
          page,
          size: 12,
          totalElements: 30,
          totalPages: 3,
        }),
      );
    }),
  );
  return requested;
}

describe('BooksPage', () => {
  it('shows a loading indicator, then one card per book under a "Books" heading', async () => {
    server.use(
      http.get(`${API}/books`, () =>
        HttpResponse.json(
          aPage([aBook({ id: 1, title: 'Clean Code' }), aBook({ id: 5, title: 'The Pragmatic Programmer' })]),
        ),
      ),
    );

    renderWithProviders(<BooksPage />);

    expect(screen.getByRole('progressbar')).toBeInTheDocument();
    expect(await screen.findByRole('article', { name: 'Clean Code' })).toBeInTheDocument();
    expect(screen.getByRole('article', { name: 'The Pragmatic Programmer' })).toBeInTheDocument();
    expect(screen.getByRole('heading', { level: 1, name: 'Books' })).toBeInTheDocument();
    expect(screen.queryByRole('progressbar')).not.toBeInTheDocument();
  });

  it('puts the action from renderBookAction on each card, made for that book', async () => {
    server.use(
      http.get(`${API}/books`, () =>
        HttpResponse.json(aPage([aBook({ id: 1, title: 'Clean Code' }), aBook({ id: 2, title: 'Refactoring' })])),
      ),
    );

    renderWithProviders(<BooksPage renderBookAction={(book) => <button>Buy {book.title}</button>} />);

    const refactoring = await screen.findByRole('article', { name: 'Refactoring' });
    expect(within(refactoring).getByRole('button', { name: 'Buy Refactoring' })).toBeInTheDocument();
    expect(
      within(screen.getByRole('article', { name: 'Clean Code' })).getByRole('button', { name: 'Buy Clean Code' }),
    ).toBeInTheDocument();
  });

  it('says "No books available" when the catalog is empty', async () => {
    server.use(http.get(`${API}/books`, () => HttpResponse.json(aPage([]))));

    renderWithProviders(<BooksPage />);

    expect(await screen.findByText('No books available')).toBeInTheDocument();
    expect(screen.queryByRole('article')).not.toBeInTheDocument();
  });

  it('shows the error with a "Try again" button that reloads the books', async () => {
    // later handlers win: the first request gets the 500 once, the retry gets the success
    server.use(http.get(`${API}/books`, () => HttpResponse.json(aPage([aBook({ title: 'Clean Code' })]))));
    server.use(
      http.get(
        `${API}/books`,
        () =>
          HttpResponse.json(
            {
              type: 'https://bookstore.example/problems/internal-error',
              title: 'Internal server error',
              status: 500,
              detail: 'An unexpected error occurred',
            },
            { status: 500 },
          ),
        { once: true },
      ),
    );

    const { user } = renderWithProviders(<BooksPage />);

    const alert = await screen.findByRole('alert');
    expect(alert).toHaveTextContent('Could not load the books');
    expect(alert).toHaveTextContent('An unexpected error occurred');

    await user.click(within(alert).getByRole('button', { name: 'Try again' }));

    expect(await screen.findByRole('article', { name: 'Clean Code' })).toBeInTheDocument();
    expect(screen.queryByRole('alert')).not.toBeInTheDocument();
  });

  describe('pagination', () => {
    it('shows page controls under the books when the catalog has more than one page', async () => {
      server.use(
        http.get(`${API}/books`, () =>
          HttpResponse.json(
            aPage([aBook({ title: 'Clean Code' })], { page: 0, size: 12, totalElements: 30, totalPages: 3 }),
          ),
        ),
      );

      renderWithProviders(<BooksPage />);

      const pages = await screen.findByRole('navigation', { name: 'Book pages' });
      expect(within(pages).getByRole('button', { name: 'page 1' })).toHaveAttribute('aria-current', 'page');
      expect(within(pages).getByRole('button', { name: 'Go to page 2' })).toBeInTheDocument();
      expect(within(pages).getByRole('button', { name: 'Go to page 3' })).toBeInTheDocument();
    });

    it('shows no page controls when everything fits on one page', async () => {
      server.use(http.get(`${API}/books`, () => HttpResponse.json(aPage([aBook({ title: 'Clean Code' })]))));

      renderWithProviders(<BooksPage />);

      expect(await screen.findByRole('article', { name: 'Clean Code' })).toBeInTheDocument();
      expect(screen.queryByRole('navigation', { name: 'Book pages' })).not.toBeInTheDocument();
    });

    it('asks the backend for 12 books per page, starting with the first page', async () => {
      const requested = threePageCatalog();

      renderWithProviders(<BooksPage />);

      expect(await screen.findByRole('article', { name: 'Book on page 1' })).toBeInTheDocument();
      expect(requested[0]?.searchParams.get('page')).toBe('0');
      expect(requested[0]?.searchParams.get('size')).toBe('12');
    });

    it('loads the chosen page and puts it in the address when a page is clicked', async () => {
      const requested = threePageCatalog();
      const { user } = renderWithProviders(
        <>
          <BooksPage />
          <CurrentAddress />
        </>,
        { route: '/home' },
      );
      const pages = await screen.findByRole('navigation', { name: 'Book pages' });

      await user.click(within(pages).getByRole('button', { name: 'Go to page 2' }));

      expect(await screen.findByRole('article', { name: 'Book on page 2' })).toBeInTheDocument();
      expect(screen.queryByRole('article', { name: 'Book on page 1' })).not.toBeInTheDocument();
      expect(requested.at(-1)?.searchParams.get('page')).toBe('1'); // the backend counts from 0
      expect(screen.getByText('Address: /home?page=2')).toBeInTheDocument();
      expect(within(pages).getByRole('button', { name: 'page 2' })).toHaveAttribute('aria-current', 'page');
    });

    it('opens the page in the address directly (bookmark, refresh, shared link)', async () => {
      const requested = threePageCatalog();

      renderWithProviders(<BooksPage />, { route: '/home?page=2' });

      expect(await screen.findByRole('article', { name: 'Book on page 2' })).toBeInTheDocument();
      expect(requested[0]?.searchParams.get('page')).toBe('1');
      const pages = screen.getByRole('navigation', { name: 'Book pages' });
      expect(within(pages).getByRole('button', { name: 'page 2' })).toHaveAttribute('aria-current', 'page');
    });

    it.each(['abc', '0', '-3', '2.5'])('treats ?page=%s as the first page', async (value) => {
      const requested = threePageCatalog();

      renderWithProviders(<BooksPage />, { route: `/home?page=${value}` });

      expect(await screen.findByRole('article', { name: 'Book on page 1' })).toBeInTheDocument();
      expect(requested[0]?.searchParams.get('page')).toBe('0');
    });
  });

  describe('search', () => {
    function searchableCatalog() {
      const all = [
        aBook({ id: 1, title: 'Clean Code', author: 'Robert C. Martin' }),
        aBook({ id: 2, title: 'Refactoring', author: 'Martin Fowler' }),
        aBook({ id: 3, title: 'Domain-Driven Design', author: 'Eric Evans' }),
      ];
      const requested: URL[] = [];
      server.use(
        http.get(`${API}/books`, ({ request }) => {
          const url = new URL(request.url);
          requested.push(url);
          const q = url.searchParams.get('q')?.toLowerCase();
          const matches = q ? all.filter((b) => `${b.title} ${b.author}`.toLowerCase().includes(q)) : all;
          return HttpResponse.json(aPage(matches));
        }),
      );
      return requested;
    }

    it('searches by title or author: the text goes to the backend and into the address', async () => {
      const requested = searchableCatalog();
      const { user } = renderWithProviders(
        <>
          <BooksPage />
          <CurrentAddress />
        </>,
        { route: '/home' },
      );

      await user.type(await screen.findByRole('searchbox', { name: 'Search books' }), 'fowler{Enter}');

      expect(await screen.findByText('1 book found for “fowler”')).toBeInTheDocument();
      expect(screen.getByRole('article', { name: 'Refactoring' })).toBeInTheDocument();
      expect(screen.queryByRole('article', { name: 'Clean Code' })).not.toBeInTheDocument();
      expect(screen.getByText('Address: /home?q=fowler')).toBeInTheDocument();
      expect(requested.at(-1)?.searchParams.get('q')).toBe('fowler');
    });

    it('starts a new search at the first page', async () => {
      const requested = searchableCatalog();
      const { user } = renderWithProviders(
        <>
          <BooksPage />
          <CurrentAddress />
        </>,
        { route: '/home?page=3' },
      );

      await user.type(await screen.findByRole('searchbox', { name: 'Search books' }), 'clean{Enter}');

      expect(await screen.findByText('Address: /home?q=clean')).toBeInTheDocument(); // no page=3 any more
      expect(requested.at(-1)?.searchParams.get('page')).toBe('0');
    });

    it('opens a search from the address (refresh, bookmark, shared link) with the box filled in', async () => {
      const requested = searchableCatalog();

      renderWithProviders(<BooksPage />, { route: '/home?q=evans' });

      expect(await screen.findByRole('article', { name: 'Domain-Driven Design' })).toBeInTheDocument();
      expect(screen.getByRole('searchbox', { name: 'Search books' })).toHaveValue('evans');
      expect(requested[0]?.searchParams.get('q')).toBe('evans');
    });

    it('says when nothing matches, and "Clear search" shows the whole catalog again', async () => {
      searchableCatalog();
      const { user } = renderWithProviders(
        <>
          <BooksPage />
          <CurrentAddress />
        </>,
        { route: '/home?q=tolkien' },
      );

      expect(await screen.findByText('No books match “tolkien”')).toBeInTheDocument();
      await user.click(screen.getByRole('button', { name: 'Clear search' }));

      expect(await screen.findByRole('article', { name: 'Clean Code' })).toBeInTheDocument();
      expect(screen.getByText('Address: /home')).toBeInTheDocument();
      expect(screen.getByRole('searchbox', { name: 'Search books' })).toHaveValue('');
    });
  });
});
