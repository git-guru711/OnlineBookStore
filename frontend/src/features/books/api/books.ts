import type { Book } from '@/features/books/types';
import { http } from '@/lib/api-client';
import type { PageResponse } from '@/types/api';

export interface GetBooksParams {
  page?: number;
  size?: number;
  /** "field,direction", e.g. "price,desc". The backend allows title, author, price. */
  sort?: string;
  q?: string;
}

export function getBooks({ page = 0, size = 20, sort = 'title,asc', q }: GetBooksParams = {}): Promise<
  PageResponse<Book>
> {
  const query = new URLSearchParams({ page: String(page), size: String(size), sort });
  if (q?.trim()) {
    query.set('q', q.trim());
  }
  return http.get<PageResponse<Book>>(`/books?${query}`);
}
