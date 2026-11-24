import { keyframes } from '@emotion/react';
import Badge from '@mui/material/Badge';
import IconButton from '@mui/material/IconButton';
import ShoppingCartIcon from '@mui/icons-material/ShoppingCart';
import { Link as RouterLink } from 'react-router-dom';
import { useAuth } from '@/features/auth';
import { useCart } from '@/features/cart/hooks/useCart';

const bump = keyframes`
  0% { transform: scale(1) translate(50%, -50%); }
  40% { transform: scale(1.35) translate(40%, -40%); }
  100% { transform: scale(1) translate(50%, -50%); }
`;

/** The count comes from the cached cart, so it updates after every cart change. */
export function CartBadge() {
  const { user } = useAuth();
  const cart = useCart();

  if (!user) {
    return null;
  }

  const count = cart.data?.totalQuantity ?? 0;
  return (
    <IconButton
      component={RouterLink}
      to="/cart"
      color="inherit"
      // screen readers get the count in words
      aria-label={`Cart, ${count} ${count === 1 ? 'item' : 'items'}`}
    >
      <Badge
        key={count} // a new count re-creates the badge, which replays the bump
        badgeContent={count}
        color="secondary"
        showZero
        sx={{
          '& .MuiBadge-badge': { animation: `${bump} 320ms ease-out` },
          '@media (prefers-reduced-motion: reduce)': { '& .MuiBadge-badge': { animation: 'none' } },
        }}
      >
        <ShoppingCartIcon />
      </Badge>
    </IconButton>
  );
}
