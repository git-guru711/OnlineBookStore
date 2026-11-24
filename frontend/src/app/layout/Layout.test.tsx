import { screen, within } from '@testing-library/react';
import { Layout } from '@/app/layout/Layout';
import { saveSession } from '@/features/auth/utils/session';
import { aUser } from '@/testing/data-generators';
import { renderWithProviders } from '@/testing/test-utils';

const logInAsDemoUser = () =>
  saveSession({ email: 'demo@example.com', password: 'demo1234', user: aUser({ fullName: 'Demo User' }) });

describe('Layout header', () => {
  it('offers "Log in" and "Register" links to guests', () => {
    renderWithProviders(<Layout />);
    const header = screen.getByRole('banner');

    expect(within(header).getByRole('link', { name: 'Log in' })).toHaveAttribute('href', '/login');
    expect(within(header).getByRole('link', { name: 'Register' })).toHaveAttribute('href', '/register');
    expect(within(header).queryByRole('button', { name: /account/i })).not.toBeInTheDocument();
  });

  it('shows logged-in users an account button with their name instead (it fits on a phone: one button, not three)', () => {
    logInAsDemoUser();

    renderWithProviders(<Layout />);
    const header = screen.getByRole('banner');

    const account = within(header).getByRole('button', { name: 'Account: Demo User' });
    expect(account).toHaveTextContent('Demo User');
    expect(account).toHaveAttribute('aria-haspopup', 'true');
    expect(within(header).queryByRole('link', { name: 'Log in' })).not.toBeInTheDocument();
  });

  it('the account menu holds "My orders" and "Log out"', async () => {
    logInAsDemoUser();
    const { user } = renderWithProviders(<Layout />);

    await user.click(within(screen.getByRole('banner')).getByRole('button', { name: 'Account: Demo User' }));

    const menu = screen.getByRole('menu');
    expect(within(menu).getByRole('menuitem', { name: 'My orders' })).toHaveAttribute('href', '/orders');
    expect(within(menu).getByRole('menuitem', { name: 'Log out' })).toBeInTheDocument();
  });

  it('keeps the cart link in the header itself, for logged-in users only', async () => {
    logInAsDemoUser();
    const { user } = renderWithProviders(<Layout />);
    const header = screen.getByRole('banner');

    expect(await within(header).findByRole('link', { name: 'Cart, 0 items' })).toHaveAttribute('href', '/cart');

    await user.click(within(header).getByRole('button', { name: 'Account: Demo User' }));
    await user.click(screen.getByRole('menuitem', { name: 'Log out' }));

    expect(within(header).queryByRole('link', { name: /cart/i })).not.toBeInTheDocument();
  });

  it('switches back to the guest links after "Log out", and the menu is gone', async () => {
    logInAsDemoUser();
    const { user } = renderWithProviders(<Layout />);
    const header = screen.getByRole('banner');

    await user.click(within(header).getByRole('button', { name: 'Account: Demo User' }));
    await user.click(screen.getByRole('menuitem', { name: 'Log out' }));

    expect(within(header).getByRole('link', { name: 'Log in' })).toBeInTheDocument();
    expect(within(header).queryByText('Demo User')).not.toBeInTheDocument();
    expect(screen.queryByRole('menu')).not.toBeInTheDocument();
  });

  it('ends every page with a footer', () => {
    renderWithProviders(<Layout />);

    expect(within(screen.getByRole('contentinfo')).getByText(/Bookstore/)).toBeInTheDocument();
  });
});
