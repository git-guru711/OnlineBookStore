import { Navigate, useLocation, useRoutes, type RouteObject } from 'react-router-dom';
import { Layout } from '@/app/layout/Layout';
import { NotFoundPage } from '@/app/pages/NotFoundPage';
import { LoginPage, ProtectedRoute, RegisterPage } from '@/features/auth';
import { BooksPage } from '@/features/books';
import { AddToCartButton, CartPage, CheckoutPage } from '@/features/cart';
import { OrderPage, OrdersPage } from '@/features/orders';
import { HOME_PATH } from '@/lib/paths';

/** "/" → "/home", keeping the query; `replace` keeps "/" out of the history. */
function RedirectToHome() {
  const { search } = useLocation();
  return <Navigate to={{ pathname: HOME_PATH, search }} replace />;
}

/** One route table for the app (BrowserRouter) and the tests (MemoryRouter). */
const routes: RouteObject[] = [
  {
    path: '/',
    element: <Layout />,
    children: [
      { index: true, element: <RedirectToHome /> },
      {
        path: HOME_PATH.slice(1), // "home": child paths are relative to "/"
        // the app combines features: catalog cards get the cart's button
        element: (
          <BooksPage
            renderBookAction={(book) => (
              <AddToCartButton bookId={book.id} soldOut={book.stockStatus === 'OUT_OF_STOCK'} />
            )}
          />
        ),
      },
      {
        path: 'cart',
        element: (
          <ProtectedRoute>
            <CartPage />
          </ProtectedRoute>
        ),
      },
      {
        path: 'checkout',
        element: (
          <ProtectedRoute>
            <CheckoutPage />
          </ProtectedRoute>
        ),
      },
      {
        path: 'orders',
        element: (
          <ProtectedRoute>
            <OrdersPage />
          </ProtectedRoute>
        ),
      },
      {
        path: 'orders/:id',
        element: (
          <ProtectedRoute>
            <OrderPage />
          </ProtectedRoute>
        ),
      },
      { path: 'login', element: <LoginPage /> },
      { path: 'register', element: <RegisterPage /> },
      { path: '*', element: <NotFoundPage /> },
    ],
  },
];

export function AppRoutes() {
  return useRoutes(routes);
}
