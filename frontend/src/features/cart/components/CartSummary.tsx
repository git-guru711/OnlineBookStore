import Box from '@mui/material/Box';
import Button from '@mui/material/Button';
import Divider from '@mui/material/Divider';
import Paper from '@mui/material/Paper';
import Typography from '@mui/material/Typography';
import { Link as RouterLink, useNavigate } from 'react-router-dom';
import type { Cart } from '@/features/cart/types';
import { formatPrice } from '@/utils/formatPrice';
import { HOME_PATH } from '@/lib/paths';

interface CartSummaryProps {
  cart: Cart;
}

export function CartSummary({ cart }: CartSummaryProps) {
  const navigate = useNavigate();
  const books = `${cart.totalQuantity} ${cart.totalQuantity === 1 ? 'book' : 'books'}`;

  return (
    <Paper
      component="aside"
      aria-label="Cart summary"
      variant="outlined"
      sx={{ p: 3, position: { md: 'sticky' }, top: { md: 24 } }}
    >
      <Typography variant="h6" component="h2" gutterBottom>
        Summary
      </Typography>
      <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'baseline', my: 2 }}>
        <Typography>Subtotal ({books})</Typography>
        <Typography sx={{ fontWeight: 700, fontSize: '1.25rem' }}>{formatPrice(cart.subtotal)}</Typography>
      </Box>
      <Typography variant="body2" color="text.secondary">
        Copies are reserved when you place the order, not before.
      </Typography>
      <Divider sx={{ my: 2 }} />
      <Button
        fullWidth
        variant="contained"
        size="large"
        disabled={!cart.checkoutAllowed}
        onClick={() => navigate('/checkout')}
      >
        Checkout
      </Button>
      {!cart.checkoutAllowed && (
        <Typography variant="body2" color="error" sx={{ mt: 1.5 }}>
          Please fix the books marked in red first.
        </Typography>
      )}
      <Button fullWidth component={RouterLink} to={HOME_PATH} sx={{ mt: 1 }}>
        Continue shopping
      </Button>
    </Paper>
  );
}
