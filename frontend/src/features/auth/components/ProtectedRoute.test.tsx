import { screen } from '@testing-library/react';
import { Route, Routes, useSearchParams } from 'react-router-dom';
import { ProtectedRoute } from '@/features/auth/components/ProtectedRoute';
import { saveSession } from '@/features/auth/utils/session';
import { aUser } from '@/testing/data-generators';
import { renderWithProviders } from '@/testing/test-utils';

function LoginStandIn() {
  const [searchParams] = useSearchParams();
  return <p>Login page, redirect={searchParams.get('redirect')}</p>;
}

function renderProtectedAt(route: string) {
  return renderWithProviders(
    <Routes>
      <Route path="/login" element={<LoginStandIn />} />
      <Route
        path="/cart"
        element={
          <ProtectedRoute>
            <p>Cart page</p>
          </ProtectedRoute>
        }
      />
      <Route
        path="/orders/:id"
        element={
          <ProtectedRoute>
            <p>Order page</p>
          </ProtectedRoute>
        }
      />
    </Routes>,
    { route },
  );
}

describe('ProtectedRoute', () => {
  it('sends a guest to the login page, remembering the page they wanted', () => {
    renderProtectedAt('/cart');

    expect(screen.getByText('Login page, redirect=/cart')).toBeInTheDocument();
    expect(screen.queryByText('Cart page')).not.toBeInTheDocument();
  });

  it('remembers the full address, including the query string', () => {
    renderProtectedAt('/orders/42?highlight=new');

    expect(screen.getByText('Login page, redirect=/orders/42?highlight=new')).toBeInTheDocument();
  });

  it('shows the page to a logged-in user', () => {
    // a saved session, as after a page refresh
    saveSession({ email: 'demo@example.com', password: 'demo1234', user: aUser({ fullName: 'Demo User' }) });

    renderProtectedAt('/cart');

    expect(screen.getByText('Cart page')).toBeInTheDocument();
  });
});
