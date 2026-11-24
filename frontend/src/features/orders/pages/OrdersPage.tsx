import Button from '@mui/material/Button';
import CircularProgress from '@mui/material/CircularProgress';
import Link from '@mui/material/Link';
import Pagination from '@mui/material/Pagination';
import Paper from '@mui/material/Paper';
import Table from '@mui/material/Table';
import TableBody from '@mui/material/TableBody';
import TableCell from '@mui/material/TableCell';
import TableContainer from '@mui/material/TableContainer';
import TableHead from '@mui/material/TableHead';
import TableRow from '@mui/material/TableRow';
import Typography from '@mui/material/Typography';
import { Link as RouterLink, useSearchParams } from 'react-router-dom';
import ReceiptLongOutlinedIcon from '@mui/icons-material/ReceiptLongOutlined';
import { EmptyState } from '@/components/EmptyState';
import { ErrorMessage } from '@/components/ErrorMessage';
import { useOrders } from '@/features/orders/hooks/useOrders';
import { formatDate } from '@/utils/formatDate';
import { formatPrice } from '@/utils/formatPrice';
import { pageFromAddress } from '@/utils/pageFromAddress';
import { HOME_PATH } from '@/lib/paths';

const PAGE_SIZE = 10;

export function OrdersPage() {
  const [searchParams, setSearchParams] = useSearchParams();
  const page = pageFromAddress(searchParams.get('page'));
  const orders = useOrders({ page: page - 1, size: PAGE_SIZE }); // the API counts pages from 0

  return (
    <>
      <Typography variant="h4" component="h1" gutterBottom>
        My orders
      </Typography>

      {orders.isPending && <CircularProgress aria-label="Loading your orders" />}

      {orders.isError && (
        <ErrorMessage
          title="Could not load your orders"
          message={orders.error.message}
          onRetry={() => orders.refetch()}
        />
      )}

      {orders.data?.content.length === 0 && (
        <EmptyState
          icon={<ReceiptLongOutlinedIcon />}
          title="You have no orders yet"
          description="Your orders will be listed here once you have placed one."
          action={
            <Button component={RouterLink} to={HOME_PATH} variant="contained">
              Browse books
            </Button>
          }
        />
      )}

      {orders.data && orders.data.content.length > 0 && (
        <TableContainer component={Paper} variant="outlined">
          <Table aria-label="Your orders">
            <TableHead>
              <TableRow>
                <TableCell>Order</TableCell>
                <TableCell>Date</TableCell>
                <TableCell align="right">Books</TableCell>
                <TableCell align="right">Total</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {orders.data.content.map((order) => (
                <TableRow key={order.id}>
                  <TableCell>
                    <Link component={RouterLink} to={`/orders/${order.id}`}>
                      Order {order.id}
                    </Link>
                  </TableCell>
                  <TableCell>{formatDate(order.createdAt)}</TableCell>
                  <TableCell align="right">{order.totalQuantity}</TableCell>
                  <TableCell align="right">{formatPrice(order.totalAmount)}</TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </TableContainer>
      )}

      {orders.data && orders.data.totalPages > 1 && (
        <Pagination
          aria-label="Order pages"
          count={orders.data.totalPages}
          page={page}
          onChange={(_event, chosen) => setSearchParams({ page: String(chosen) })}
          color="primary"
          sx={{ mt: 4, display: 'flex', justifyContent: 'center' }}
        />
      )}
    </>
  );
}
