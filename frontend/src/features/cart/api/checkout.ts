import type { PlacedOrder } from '@/features/cart/types';
import { http } from '@/lib/api-client';

/** Orders what is in the cart. A repeated idempotencyKey returns the first order (200) instead of ordering twice. */
export function placeOrder(idempotencyKey: string): Promise<PlacedOrder> {
  return http.post<PlacedOrder>('/orders', undefined, { headers: { 'Idempotency-Key': idempotencyKey } });
}
