export type StockStatus = 'IN_STOCK' | 'OUT_OF_STOCK';

export interface PageResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}
