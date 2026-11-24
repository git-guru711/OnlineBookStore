import { useMutation, useQueryClient } from '@tanstack/react-query';
import { placeOrder } from '@/features/cart/api/checkout';
import { CART_KEY } from '@/features/cart/hooks/useCart';

/** After an order, cached cart and stock are outdated: reload everything. Not awaited, so the confirmation shows at once. */
export function usePlaceOrder() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: placeOrder,
    onSuccess: () => {
      void queryClient.invalidateQueries();
    },
    // refused: show the server's current cart
    onError: () => queryClient.invalidateQueries({ queryKey: CART_KEY }),
  });
}
