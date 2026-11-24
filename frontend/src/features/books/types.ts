import type { StockStatus } from '@/types/api';

export interface Book {
  id: number;
  isbn: string;
  title: string;
  author: string;
  price: number;
  availableQuantity: number;
  stockStatus: StockStatus;
}
