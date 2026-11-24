import type { Order, OrderSummary } from '@/features/orders/types';
import { http } from '@/lib/api-client';
import type { PageResponse } from '@/types/api';

/** 404 also for another user's order (no hint which). */
export function getOrder(id: number): Promise<Order> {
  return http.get<Order>(`/orders/${id}`);
}

export interface ListOrdersParams {
  page?: number;
  size?: number;
}

export function listOrders({ page = 0, size = 10 }: ListOrdersParams = {}): Promise<PageResponse<OrderSummary>> {
  const query = new URLSearchParams({ page: String(page), size: String(size) });
  return http.get<PageResponse<OrderSummary>>(`/orders?${query}`);
}
