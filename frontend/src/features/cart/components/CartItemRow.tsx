import { useState } from 'react';
import IconButton from '@mui/material/IconButton';
import TableCell from '@mui/material/TableCell';
import TableRow from '@mui/material/TableRow';
import Typography from '@mui/material/Typography';
import DeleteIcon from '@mui/icons-material/DeleteOutlined';
import { useChangeQuantity, useRemoveFromCart } from '@/features/cart/hooks/useCart';
import { cartErrorMessage } from '@/features/cart/utils/cartErrorMessage';
import { ErrorSnackbar } from '@/features/cart/components/ErrorSnackbar';
import { QuantityInput } from '@/features/cart/components/QuantityInput';
import type { CartItem } from '@/features/cart/types';
import { formatPrice } from '@/utils/formatPrice';

/** Stock can drop below the cart quantity (nothing is reserved); this says what to do. Null when fine. */
function stockWarning(item: CartItem): string | null {
  if (!item.exceedsAvailable) {
    return null;
  }
  return item.availableQuantity === 0
    ? 'Sold out, please remove it from your cart.'
    : `Only ${item.availableQuantity} left, please reduce the quantity.`;
}

interface CartItemRowProps {
  item: CartItem;
}

export function CartItemRow({ item }: CartItemRowProps) {
  const changeQuantity = useChangeQuantity();
  const removeFromCart = useRemoveFromCart();
  const warning = stockWarning(item);
  const [error, setError] = useState<string | null>(null);
  const showError = (failure: Error) => setError(cartErrorMessage(failure));

  return (
    <TableRow>
      <TableCell>
        <Typography sx={{ fontWeight: 500 }}>{item.title}</Typography>
        <Typography variant="body2" color="text.secondary">
          {item.author}
        </Typography>
        {/* the Price column is hidden on phones, so the unit price shows here */}
        <Typography variant="body2" color="text.secondary" sx={{ display: { xs: 'block', sm: 'none' } }}>
          {formatPrice(item.unitPrice)} each
        </Typography>
        {warning && (
          <Typography variant="body2" color="error" sx={{ mt: 0.5 }}>
            {warning}
          </Typography>
        )}
      </TableCell>
      <TableCell align="right" sx={{ display: { xs: 'none', sm: 'table-cell' } }}>
        {formatPrice(item.unitPrice)}
      </TableCell>
      <TableCell align="center">
        <QuantityInput
          title={item.title}
          value={item.quantity}
          available={item.availableQuantity}
          onChange={(quantity) => changeQuantity.mutate({ bookId: item.bookId, quantity }, { onError: showError })}
        />
      </TableCell>
      <TableCell align="right">{formatPrice(item.lineTotal)}</TableCell>
      <TableCell align="right">
        <IconButton
          aria-label={`Remove ${item.title}`}
          onClick={() => removeFromCart.mutate(item.bookId, { onError: showError })}
        >
          <DeleteIcon />
        </IconButton>
        <ErrorSnackbar message={error} onClose={() => setError(null)} />
      </TableCell>
    </TableRow>
  );
}
