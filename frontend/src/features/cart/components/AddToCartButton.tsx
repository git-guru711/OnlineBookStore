import { useState } from 'react';
import Button from '@mui/material/Button';
import Portal from '@mui/material/Portal';
import Snackbar from '@mui/material/Snackbar';
import { useLocation, useNavigate } from 'react-router-dom';
import { useAuth } from '@/features/auth';
import { useAddToCart } from '@/features/cart/hooks/useCart';
import { cartErrorMessage } from '@/features/cart/utils/cartErrorMessage';
import { ErrorSnackbar } from '@/features/cart/components/ErrorSnackbar';

interface AddToCartButtonProps {
  bookId: number;
  soldOut: boolean;
}

export function AddToCartButton({ bookId, soldOut }: AddToCartButtonProps) {
  const { user } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const addToCart = useAddToCart();
  const [added, setAdded] = useState(false);
  const [error, setError] = useState<string | null>(null);

  function handleClick() {
    if (!user) {
      // log in first, then come back to this exact page
      navigate(`/login?redirect=${encodeURIComponent(location.pathname + location.search)}`);
      return;
    }
    setError(null);
    addToCart.mutate(
      { bookId },
      {
        onSuccess: () => setAdded(true),
        onError: (failure) => setError(cartErrorMessage(failure)),
      },
    );
  }

  return (
    <>
      <Button variant="contained" disabled={soldOut} onClick={handleClick}>
        Add to cart
      </Button>
      {/* Portal: the card's hover transform would otherwise pin the fixed snackbar to the card */}
      <Portal>
        <Snackbar open={added} autoHideDuration={3000} onClose={() => setAdded(false)} message="Added to cart" />
      </Portal>
      <ErrorSnackbar message={error} onClose={() => setError(null)} />
    </>
  );
}
