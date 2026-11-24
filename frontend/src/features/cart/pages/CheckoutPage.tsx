import { useId, useState } from 'react';
import Alert from '@mui/material/Alert';
import AlertTitle from '@mui/material/AlertTitle';
import Box from '@mui/material/Box';
import Button from '@mui/material/Button';
import CircularProgress from '@mui/material/CircularProgress';
import Paper from '@mui/material/Paper';
import Table from '@mui/material/Table';
import TableBody from '@mui/material/TableBody';
import TableCell from '@mui/material/TableCell';
import TableContainer from '@mui/material/TableContainer';
import TableFooter from '@mui/material/TableFooter';
import TableHead from '@mui/material/TableHead';
import TableRow from '@mui/material/TableRow';
import Typography from '@mui/material/Typography';
import { Link as RouterLink, useNavigate } from 'react-router-dom';
import ShoppingCartOutlinedIcon from '@mui/icons-material/ShoppingCartOutlined';
import { CheckoutSteps } from '@/components/CheckoutSteps';
import { EmptyState } from '@/components/EmptyState';
import { ErrorMessage } from '@/components/ErrorMessage';
import { useCart } from '@/features/cart/hooks/useCart';
import { usePlaceOrder } from '@/features/cart/hooks/usePlaceOrder';
import { stockShortages, type StockShortage } from '@/features/cart/utils/cartErrorMessage';
import { formatPrice } from '@/utils/formatPrice';
import { HOME_PATH } from '@/lib/paths';

function shortageText({ title, requested, available }: StockShortage): string {
  return available === 0 ? `${title}: sold out` : `${title}: ${requested} in your cart, only ${available} left`;
}

export function CheckoutPage() {
  const cart = useCart();
  const placeOrder = usePlaceOrder();
  // one key per visit, reused by every retry, so a lost response cannot create a second order
  const [idempotencyKey] = useState(() => crypto.randomUUID());
  const navigate = useNavigate();
  const refusalTitleId = useId();
  const shortages = stockShortages(placeOrder.error);
  const backToCart = (
    <Button component={RouterLink} to="/cart">
      Back to cart
    </Button>
  );

  return (
    <>
      <CheckoutSteps active={1} />
      <Typography variant="h4" component="h1" gutterBottom>
        Checkout
      </Typography>

      {placeOrder.isError && (
        <Alert severity="error" sx={{ mb: 3 }} aria-labelledby={refusalTitleId}>
          <AlertTitle id={refusalTitleId}>Your order was not placed</AlertTitle>
          {shortages.length > 0 ? (
            <>
              Nothing was ordered. These books are no longer available in the quantity in your cart:
              <ul>
                {shortages.map((shortage) => (
                  <li key={shortage.bookId}>{shortageText(shortage)}</li>
                ))}
              </ul>
            </>
          ) : (
            placeOrder.error.message
          )}
        </Alert>
      )}

      {cart.isPending && <CircularProgress aria-label="Loading your order" />}

      {cart.isError && (
        <ErrorMessage title="Could not load your cart" message={cart.error.message} onRetry={() => cart.refetch()} />
      )}

      {cart.data?.items.length === 0 && (
        <EmptyState
          icon={<ShoppingCartOutlinedIcon />}
          title="Your cart is empty"
          description="There is nothing to order yet."
          action={
            <Button component={RouterLink} to={HOME_PATH} variant="contained">
              Browse books
            </Button>
          }
        />
      )}

      {cart.data && cart.data.items.length > 0 && (
        <>
          <TableContainer component={Paper} variant="outlined">
            <Table aria-label="Order summary">
              <TableHead>
                <TableRow>
                  <TableCell>Book</TableCell>
                  <TableCell align="right">Price</TableCell>
                  <TableCell align="right">Quantity</TableCell>
                  <TableCell align="right">Line total</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {cart.data.items.map((item) => (
                  <TableRow key={item.bookId}>
                    <TableCell>{item.title}</TableCell>
                    <TableCell align="right">{formatPrice(item.unitPrice)}</TableCell>
                    <TableCell align="right">{item.quantity}</TableCell>
                    <TableCell align="right">{formatPrice(item.lineTotal)}</TableCell>
                  </TableRow>
                ))}
              </TableBody>
              <TableFooter>
                <TableRow>
                  <TableCell colSpan={3} align="right">
                    <Typography sx={{ fontWeight: 600 }}>Total</Typography>
                  </TableCell>
                  <TableCell align="right">
                    <Typography sx={{ fontWeight: 600 }}>{formatPrice(cart.data.subtotal)}</Typography>
                  </TableCell>
                </TableRow>
              </TableFooter>
            </Table>
          </TableContainer>

          {cart.data.checkoutAllowed ? (
            <Box sx={{ mt: 3, display: 'flex', justifyContent: 'space-between' }}>
              {backToCart}
              <Button
                variant="contained"
                size="large"
                // disabled while sending: a second click would order twice
                disabled={placeOrder.isPending}
                onClick={() =>
                  placeOrder.mutate(idempotencyKey, {
                    // tells the order page to say "Thank you"
                    onSuccess: (order) => navigate(`/orders/${order.id}`, { state: { justPlaced: true } }),
                  })
                }
              >
                Place order
              </Button>
            </Box>
          ) : (
            <Alert severity="warning" sx={{ mt: 3 }} action={backToCart}>
              Some books are not available in the quantity in your cart. Please fix your cart first.
            </Alert>
          )}
        </>
      )}
    </>
  );
}
