import { fireEvent, screen, within } from '@testing-library/react';
import { BookCard } from '@/features/books/components/BookCard';
import { aBook } from '@/testing/data-generators';
import { renderWithProviders } from '@/testing/test-utils';

describe('BookCard', () => {
  it('shows the title as a heading, the author and the formatted price, in an article named after the book', () => {
    renderWithProviders(<BookCard book={aBook({ title: 'Clean Code', author: 'Robert C. Martin', price: 32.99 })} />);

    const card = screen.getByRole('article', { name: 'Clean Code' });
    expect(within(card).getByRole('heading', { name: 'Clean Code' })).toBeInTheDocument();
    expect(within(card).getByText('Robert C. Martin')).toBeInTheDocument();
    expect(within(card).getByText('€32.99')).toBeInTheDocument();
  });

  it('shows the action it is given at the bottom of the card', () => {
    renderWithProviders(<BookCard book={aBook({ title: 'Clean Code' })} action={<button>Do something</button>} />);

    const card = screen.getByRole('article', { name: 'Clean Code' });
    expect(within(card).getByRole('button', { name: 'Do something' })).toBeInTheDocument();
  });

  it('shows no buttons when it is given no action', () => {
    renderWithProviders(<BookCard book={aBook()} />);

    expect(screen.queryByRole('button')).not.toBeInTheDocument();
  });

  describe('stock status', () => {
    it('shows "Out of stock" for a sold-out book', () => {
      renderWithProviders(<BookCard book={aBook({ availableQuantity: 0, stockStatus: 'OUT_OF_STOCK' })} />);

      expect(screen.getByText('Out of stock')).toBeInTheDocument();
    });

    it.each([1, 2, 3])('shows "Only %i left" when that few copies remain', (available) => {
      renderWithProviders(<BookCard book={aBook({ availableQuantity: available, stockStatus: 'IN_STOCK' })} />);

      expect(screen.getByText(`Only ${available} left`)).toBeInTheDocument();
    });

    it('shows no stock note when plenty of copies remain', () => {
      renderWithProviders(<BookCard book={aBook({ availableQuantity: 4, stockStatus: 'IN_STOCK' })} />);

      expect(screen.queryByText(/out of stock/i)).not.toBeInTheDocument();
      expect(screen.queryByText(/only \d+ left/i)).not.toBeInTheDocument();
    });
  });

  describe('cover', () => {
    it("shows the book's cover, found by its ISBN (Open Library covers service)", () => {
      renderWithProviders(<BookCard book={aBook({ title: 'Clean Code', isbn: '9780132350884' })} />);

      const cover = screen.getByRole('img', { name: 'Cover of Clean Code' });
      expect(cover).toHaveAttribute('src', 'https://covers.openlibrary.org/b/isbn/9780132350884-M.jpg?default=false');
      expect(cover).toHaveAttribute('loading', 'lazy'); // only covers that scroll into view are downloaded
    });

    it('shows a drawn placeholder instead when there is no cover (the service answers 404)', () => {
      const { container } = renderWithProviders(<BookCard book={aBook({ title: 'Clean Code' })} />);

      fireEvent.error(screen.getByRole('img', { name: 'Cover of Clean Code' }));

      expect(container.querySelector('img')).toBeNull(); // no broken-image icon
      expect(screen.getByRole('img', { name: 'Cover of Clean Code' })).toBeInTheDocument();
    });
  });
});
