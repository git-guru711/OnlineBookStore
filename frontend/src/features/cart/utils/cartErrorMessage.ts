import { ApiError } from '@/lib/api-client';

export interface StockShortage {
  bookId: number;
  title: string;
  requested: number;
  available: number;
}

function isStockShortage(value: unknown): value is StockShortage {
  const entry = value as Partial<StockShortage> | null;
  return (
    typeof entry === 'object' &&
    entry !== null &&
    typeof entry.title === 'string' &&
    typeof entry.requested === 'number' &&
    typeof entry.available === 'number'
  );
}

export function stockShortages(error: unknown): StockShortage[] {
  return error instanceof ApiError && error.type === 'insufficient-stock' ? error.errors.filter(isStockShortage) : [];
}

/** For a stock shortage, errors[] names the book and the copies left, so the message can be specific. */
export function cartErrorMessage(error: unknown): string {
  {
    const [shortage] = stockShortages(error);
    if (shortage) {
      if (shortage.available === 0) {
        return `${shortage.title} is sold out.`;
      }
      const copies = shortage.available === 1 ? 'copy' : 'copies';
      return `Only ${shortage.available} ${copies} of ${shortage.title} left, and your cart would need ${shortage.requested}.`;
    }
  }
  return error instanceof Error ? error.message : 'Something went wrong. Please try again.';
}
