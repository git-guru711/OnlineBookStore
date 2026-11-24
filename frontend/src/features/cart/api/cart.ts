import type { Cart } from '@/features/cart/types';
import { http } from '@/lib/api-client';

// Every change answers with the whole cart, so the UI never calculates totals or stock warnings.

/** An empty cart when the user has none (never 404). */
export function getCart(): Promise<Cart> {
  return http.get<Cart>('/cart');
}

export interface AddItemInput {
  bookId: number;
  quantity?: number;
}

export function addItem({ bookId, quantity = 1 }: AddItemInput): Promise<Cart> {
  return http.post<Cart>('/cart/items', { bookId, quantity });
}

export interface ChangeQuantityInput {
  bookId: number;
  /** The new quantity (1–99), not a difference. */
  quantity: number;
}

export function changeQuantity({ bookId, quantity }: ChangeQuantityInput): Promise<Cart> {
  return http.patch<Cart>(`/cart/items/${bookId}`, { quantity });
}

export function removeItem(bookId: number): Promise<void> {
  return http.delete(`/cart/items/${bookId}`);
}

export function clearCart(): Promise<void> {
  return http.delete('/cart');
}
