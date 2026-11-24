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
import { Link as RouterLink, useLocation, useParams } from 'react-router-dom';
import CheckCircleIcon from '@mui/icons-material/CheckCircle';
import { CheckoutSteps } from '@/components/CheckoutSteps';
import { ErrorMessage } from '@/components/ErrorMessage';
import { useOrder } from '@/features/orders/hooks/useOrder';
import { ApiError } from '@/lib/api-client';
import { formatDate } from '@/utils/formatDate';
import { formatPrice } from '@/utils/formatPrice';
import { HOME_PATH } from '@/lib/paths';

function orderIdFrom(value: string | undefined): number | null {
  const id = Number(value);
  return Number.isInteger(id) && id >= 1 ? id : null;
}

const continueShopping = (
  <Button component={RouterLink} to={HOME_PATH} variant="contained" sx={{ mt: 3 }}>
    Continue shopping
  </Button>
);

export interface OrderPageState {
  justPlaced?: boolean;
}

/** Thanks the user right after checkout; opened later, it has a neutral title. Prices are the copied ones. */
export function OrderPage() {
  const id = orderIdFrom(useParams().id);
  const order = useOrder(id);
  const justPlaced = (useLocation().state as OrderPageState | null)?.justPlaced === true;

  const notFound = id === null || (order.error instanceof ApiError && order.error.status === 404);
  if (notFound) {
    return (
      <>
        <Typography variant="h4" component="h1" gutterBottom>
          Order not found
        </Typography>
        <Typography color="text.secondary">There is no order with this number in your account.</Typography>
        {continueShopping}
      </>
    );
  }

  if (order.isPending) {
    return <CircularProgress aria-label="Loading your order" />;
  }

  if (order.isError) {
    return (
      <ErrorMessage title="Could not load your order" message={order.error.message} onRetry={() => order.refetch()} />
    );
  }

  return (
    <>
      {justPlaced && (
        <>
          <CheckoutSteps active={3} />
          <CheckCircleIcon aria-hidden color="success" sx={{ fontSize: 56, mb: 1 }} />
        </>
      )}
      <Typography variant="h4" component="h1" gutterBottom>
        {justPlaced ? 'Thank you for your order' : `Order ${order.data.id}`}
      </Typography>
      <Typography color="text.secondary" sx={{ mb: 3 }}>
        {justPlaced ? `Order number ${order.data.id}` : `Placed on ${formatDate(order.data.createdAt)}`}
      </Typography>

      <TableContainer component={Paper} variant="outlined">
        <Table aria-label="Ordered books">
          <TableHead>
            <TableRow>
              <TableCell>Book</TableCell>
              <TableCell align="right">Price</TableCell>
              <TableCell align="right">Quantity</TableCell>
              <TableCell align="right">Line total</TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {order.data.items.map((item) => (
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
                <Typography sx={{ fontWeight: 600 }}>{formatPrice(order.data.totalAmount)}</Typography>
              </TableCell>
            </TableRow>
          </TableFooter>
        </Table>
      </TableContainer>

      {continueShopping}
    </>
  );
}
