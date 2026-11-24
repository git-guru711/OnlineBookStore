import { keepPreviousData, useQuery } from '@tanstack/react-query';
import { listOrders, type ListOrdersParams } from '@/features/orders/api/orders';

export function useOrders(params: ListOrdersParams = {}) {
  return useQuery({
    queryKey: ['orders', 'list', params],
    queryFn: () => listOrders(params),
    placeholderData: keepPreviousData,
  });
}
