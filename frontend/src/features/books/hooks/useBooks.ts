import { keepPreviousData, useQuery } from '@tanstack/react-query';
import { getBooks, type GetBooksParams } from '@/features/books/api/books';

export function useBooks(params: GetBooksParams = {}) {
  return useQuery({
    queryKey: ['books', params],
    queryFn: () => getBooks(params),
    // keep the current page visible while the next one loads
    placeholderData: keepPreviousData,
  });
}
