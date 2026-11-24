import AppBar from '@mui/material/AppBar';
import Box from '@mui/material/Box';
import Button from '@mui/material/Button';
import Container from '@mui/material/Container';
import Toolbar from '@mui/material/Toolbar';
import Typography from '@mui/material/Typography';
import MenuBookIcon from '@mui/icons-material/MenuBook';
import { keyframes } from '@emotion/react';
import { Link as RouterLink, Outlet, useLocation } from 'react-router-dom';
import { AccountMenu } from '@/app/layout/AccountMenu';
import { Footer } from '@/app/layout/Footer';
import { useAuth } from '@/features/auth';
import { CartBadge } from '@/features/cart';
import { HOME_PATH } from '@/lib/paths';

const pageEnter = keyframes`
  from { opacity: 0; transform: translateY(6px); }
  to { opacity: 1; transform: none; }
`;

export function Layout() {
  const { user, logout } = useAuth();
  const { pathname } = useLocation();

  return (
    // full height, so the footer stays at the bottom
    <Box sx={{ minHeight: '100vh', display: 'flex', flexDirection: 'column' }}>
      <AppBar position="sticky">
        <Container maxWidth="lg" disableGutters>
          <Toolbar sx={{ gap: 1 }}>
            <Box
              component={RouterLink}
              to={HOME_PATH}
              sx={{ display: 'flex', alignItems: 'center', gap: 1, color: 'inherit', textDecoration: 'none' }}
            >
              <MenuBookIcon />
              <Typography variant="h6" component="span" sx={{ fontWeight: 700 }}>
                Bookstore
              </Typography>
            </Box>

            <Box sx={{ flexGrow: 1 }} />

            {user ? (
              <>
                <CartBadge />
                <AccountMenu user={user} onLogout={logout} />
              </>
            ) : (
              <>
                <Button color="inherit" component={RouterLink} to="/login">
                  Log in
                </Button>
                <Button variant="outlined" color="inherit" component={RouterLink} to="/register">
                  Register
                </Button>
              </>
            )}
          </Toolbar>
        </Container>
      </AppBar>
      <Container component="main" maxWidth="lg" sx={{ py: 4, flexGrow: 1 }}>
        {/* keyed by path: the animation runs per page, not when only ?page= changes */}
        <Box
          key={pathname}
          sx={{
            animation: `${pageEnter} 220ms ease-out`,
            '@media (prefers-reduced-motion: reduce)': { animation: 'none' },
          }}
        >
          <Outlet />
        </Box>
      </Container>
      <Footer />
    </Box>
  );
}
