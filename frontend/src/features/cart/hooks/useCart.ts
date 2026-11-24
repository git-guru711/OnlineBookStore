import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useAuth } from '@/features/auth';
import { addItem, changeQuantity, getCart, removeItem } from '@/features/cart/api/cart';
import type { Cart } from '@/features/cart/types';

/** Logout clears the whole cache, so the next user never sees this cart. */
export const CART_KEY = ['cart'] as const;

export function useCart() {
  const { user } = useAuth();
  return useQuery({
    queryKey: CART_KEY,
    queryFn: getCart,
    enabled: user !== null,
  });
}

/** The server answers with the whole cart: cache it, so the badge and the cart page update at once. */
function useUpdateCartWith<Input>(change: (input: Input) => Promise<Cart>) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: change,
    onSuccess: (cart) => queryClient.setQueryData(CART_KEY, cart),
    // refused (e.g. 409): the cached cart is outdated, so reload it
    onError: () => queryClient.invalidateQueries({ queryKey: CART_KEY }),
  });
}

export function useAddToCart() {
  return useUpdateCartWith(addItem);
}

export function useChangeQuantity() {
  return useUpdateCartWith(changeQuantity);
}

/** DELETE answers 204 without a cart, so reload it instead of recalculating totals here. */
export function useRemoveFromCart() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: removeItem,
    onSuccess: () => queryClient.invalidateQueries({ queryKey: CART_KEY }),
  });
}
