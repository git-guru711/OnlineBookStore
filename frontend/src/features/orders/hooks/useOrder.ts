import { useQuery } from '@tanstack/react-query';
import { getOrder } from '@/features/orders/api/orders';

/** id is null for an invalid order number: nothing is requested. */
export function useOrder(id: number | null) {
  return useQuery({
    queryKey: ['orders', id],
    queryFn: () => getOrder(id as number),
    enabled: id !== null,
  });
}
