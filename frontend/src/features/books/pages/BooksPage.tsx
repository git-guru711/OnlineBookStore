import type { ReactNode } from 'react';
import Box from '@mui/material/Box';
import Button from '@mui/material/Button';
import Pagination from '@mui/material/Pagination';
import Typography from '@mui/material/Typography';
import { useSearchParams } from 'react-router-dom';
import MenuBookOutlinedIcon from '@mui/icons-material/MenuBookOutlined';
import SearchOffIcon from '@mui/icons-material/SearchOff';
import { EmptyState } from '@/components/EmptyState';
import { ErrorMessage } from '@/components/ErrorMessage';
import { useBooks } from '@/features/books/hooks/useBooks';
import { BookCard } from '@/features/books/components/BookCard';
import { BookCardSkeleton } from '@/features/books/components/BookCardSkeleton';
import { BookSearch } from '@/features/books/components/BookSearch';
import { HeroBanner } from '@/features/books/components/HeroBanner';
import type { Book } from '@/features/books/types';
import { pageFromAddress } from '@/utils/pageFromAddress';

/** 12 fits full rows in a 1-, 2-, 3- or 4-column grid. */
const PAGE_SIZE = 12;

const gridSx = { display: 'grid', gap: 2, gridTemplateColumns: 'repeat(auto-fill, minmax(240px, 1fr))' } as const;

interface BooksPageProps {
  renderBookAction?: (book: Book) => ReactNode;
}

export function BooksPage({ renderBookAction }: BooksPageProps = {}) {
  const [searchParams, setSearchParams] = useSearchParams();
  const page = pageFromAddress(searchParams.get('page'));
  const query = searchParams.get('q')?.trim() ?? '';
  const books = useBooks({ page: page - 1, size: PAGE_SIZE, q: query }); // the API counts pages from 0

  function search(text: string) {
    setSearchParams(text ? { q: text } : {});
  }

  function goToPage(chosen: number) {
    setSearchParams(query ? { q: query, page: String(chosen) } : { page: String(chosen) });
  }

  const clearSearch = (
    <Button variant="outlined" onClick={() => search('')}>
      Clear search
    </Button>
  );

  return (
    <>
      {page === 1 && !query && <HeroBanner />}
      <Typography variant="h4" component="h1" gutterBottom>
        Books
      </Typography>

      <Box sx={{ mb: 3, maxWidth: 560 }}>
        <BookSearch key={query} value={query} onSearch={search} />
      </Box>

      {query && books.data && books.data.totalElements > 0 && (
        <Box sx={{ mb: 2, display: 'flex', alignItems: 'center', gap: 2, flexWrap: 'wrap' }}>
          <Typography color="text.secondary">
            {books.data.totalElements} {books.data.totalElements === 1 ? 'book' : 'books'} found for “{query}”
          </Typography>
          {clearSearch}
        </Box>
      )}

      {books.isPending && (
        // card-shaped placeholders, announced as one loading indicator
        <Box role="progressbar" aria-label="Loading books" sx={gridSx}>
          {Array.from({ length: 6 }, (_, index) => (
            <BookCardSkeleton key={index} />
          ))}
        </Box>
      )}

      {books.isError && (
        <ErrorMessage title="Could not load the books" message={books.error.message} onRetry={() => books.refetch()} />
      )}

      {books.data?.content.length === 0 &&
        (query ? (
          <EmptyState
            icon={<SearchOffIcon />}
            title={`No books match “${query}”`}
            description="Try another title or author, or check the spelling."
            action={clearSearch}
          />
        ) : (
          <EmptyState
            icon={<MenuBookOutlinedIcon />}
            title="No books available"
            description="Please check back soon."
          />
        ))}

      {books.data && books.data.content.length > 0 && (
        <Box sx={gridSx}>
          {books.data.content.map((book) => (
            <BookCard key={book.id} book={book} action={renderBookAction?.(book)} />
          ))}
        </Box>
      )}

      {books.data && books.data.totalPages > 1 && (
        <Pagination
          aria-label="Book pages"
          count={books.data.totalPages}
          page={page}
          onChange={(_event, chosen) => goToPage(chosen)}
          color="primary"
          sx={{ mt: 4, display: 'flex', justifyContent: 'center' }}
        />
      )}
    </>
  );
}
