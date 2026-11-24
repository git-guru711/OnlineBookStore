import { useId, type ReactNode } from 'react';
import Card from '@mui/material/Card';
import CardActions from '@mui/material/CardActions';
import CardContent from '@mui/material/CardContent';
import Chip from '@mui/material/Chip';
import Typography from '@mui/material/Typography';
import { BookCover } from '@/features/books/components/BookCover';
import type { Book } from '@/features/books/types';
import { formatPrice } from '@/utils/formatPrice';

const LOW_STOCK_THRESHOLD = 3;

interface BookCardProps {
  book: Book;
  /** Passed in by the app, so the books feature does not depend on the cart feature. */
  action?: ReactNode;
}

export function BookCard({ book, action }: BookCardProps) {
  const titleId = useId();
  const soldOut = book.stockStatus === 'OUT_OF_STOCK';
  const lowStock = !soldOut && book.availableQuantity <= LOW_STOCK_THRESHOLD;

  return (
    // full height with the action at the bottom, so the cards in a row line up
    <Card
      component="article"
      aria-labelledby={titleId}
      variant="outlined"
      sx={{ height: '100%', display: 'flex', flexDirection: 'column' }}
    >
      <BookCover isbn={book.isbn} title={book.title} />
      <CardContent sx={{ flexGrow: 1 }}>
        <Typography id={titleId} variant="h6" component="h2">
          {book.title}
        </Typography>
        <Typography color="text.secondary">{book.author}</Typography>
        {/* body font for prices: Playfair's old-style digits look uneven */}
        <Typography
          component="p"
          sx={{ mt: 1, fontSize: '1.2rem', fontWeight: 700, fontVariantNumeric: 'tabular-nums' }}
        >
          {formatPrice(book.price)}
        </Typography>
        {soldOut && <Chip label="Out of stock" color="error" size="small" sx={{ mt: 1 }} />}
        {lowStock && <Chip label={`Only ${book.availableQuantity} left`} color="warning" size="small" sx={{ mt: 1 }} />}
      </CardContent>
      {action && <CardActions sx={{ px: 2, pb: 2 }}>{action}</CardActions>}
    </Card>
  );
}
