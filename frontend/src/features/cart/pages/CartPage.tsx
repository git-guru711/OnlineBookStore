import Button from '@mui/material/Button';
import Box from '@mui/material/Box';
import CircularProgress from '@mui/material/CircularProgress';
import Paper from '@mui/material/Paper';
import Table from '@mui/material/Table';
import TableBody from '@mui/material/TableBody';
import TableCell from '@mui/material/TableCell';
import TableContainer from '@mui/material/TableContainer';
import TableHead from '@mui/material/TableHead';
import TableRow from '@mui/material/TableRow';
import Typography from '@mui/material/Typography';
import { Link as RouterLink } from 'react-router-dom';
import ShoppingCartOutlinedIcon from '@mui/icons-material/ShoppingCartOutlined';
import { EmptyState } from '@/components/EmptyState';
import { ErrorMessage } from '@/components/ErrorMessage';
import { useCart } from '@/features/cart/hooks/useCart';
import { CartItemRow } from '@/features/cart/components/CartItemRow';
import { CartSummary } from '@/features/cart/components/CartSummary';
import { HOME_PATH } from '@/lib/paths';

export function CartPage() {
  const cart = useCart();

  return (
    <>
      <Typography variant="h4" component="h1" gutterBottom>
        Your cart
      </Typography>

      {cart.isPending && <CircularProgress aria-label="Loading your cart" />}

      {cart.isError && (
        <ErrorMessage title="Could not load your cart" message={cart.error.message} onRetry={() => cart.refetch()} />
      )}

      {cart.data?.items.length === 0 && (
        <EmptyState
          icon={<ShoppingCartOutlinedIcon />}
          title="Your cart is empty"
          description="Find a book you like and add it to your cart."
          action={
            <Button component={RouterLink} to={HOME_PATH} variant="contained">
              Browse books
            </Button>
          }
        />
      )}

      {cart.data && cart.data.items.length > 0 && (
        <Box sx={{ display: 'grid', gap: 3, gridTemplateColumns: { xs: '1fr', md: '1fr 320px' }, alignItems: 'start' }}>
          <TableContainer component={Paper} variant="outlined">
            <Table aria-label="Books in your cart">
              <TableHead>
                <TableRow>
                  <TableCell>Book</TableCell>
                  <TableCell align="right" sx={{ display: { xs: 'none', sm: 'table-cell' } }}>
                    Price
                  </TableCell>
                  <TableCell align="center">Quantity</TableCell>
                  <TableCell align="right">Line total</TableCell>
                  {/* the Remove buttons carry their own labels */}
                  <TableCell />
                </TableRow>
              </TableHead>
              <TableBody>
                {cart.data.items.map((item) => (
                  <CartItemRow key={item.bookId} item={item} />
                ))}
              </TableBody>
            </Table>
          </TableContainer>
          <CartSummary cart={cart.data} />
        </Box>
      )}
    </>
  );
}
