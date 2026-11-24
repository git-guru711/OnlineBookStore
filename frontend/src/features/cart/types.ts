import type { StockStatus } from '@/types/api';

export interface CartItem {
  bookId: number;
  title: string;
  author: string;
  unitPrice: number;
  quantity: number;
  lineTotal: number;
  availableQuantity: number;
  stockStatus: StockStatus;
  /** More copies than are left: checkout is blocked until reduced. */
  exceedsAvailable: boolean;
}

export interface Cart {
  items: CartItem[];
  totalQuantity: number;
  subtotal: number;
  checkoutAllowed: boolean;
}

/** Only the new order's id; the full order belongs to the orders feature. */
export interface PlacedOrder {
  id: number;
}
