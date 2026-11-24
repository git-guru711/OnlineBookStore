import { screen } from '@testing-library/react';
import { useTheme } from '@mui/material/styles';
import { useQueryClient } from '@tanstack/react-query';
import { useLocation } from 'react-router-dom';
import { theme as appTheme } from '@/app/theme';
import { createTestQueryClient, renderWithProviders } from '@/testing/test-utils';

function ProbeComponent() {
  const location = useLocation();
  const queryClient = useQueryClient();
  const theme = useTheme();
  return (
    <ul>
      <li>path: {location.pathname + location.search}</li>
      <li>retries: {String(queryClient.getDefaultOptions().queries?.retry)}</li>
      <li>primary: {theme.palette.primary.main}</li>
    </ul>
  );
}

describe('renderWithProviders', () => {
  it('gives a single component the router (at the chosen URL), a no-retry QueryClient and the app theme', () => {
    renderWithProviders(<ProbeComponent />, { route: '/cart?x=1' });

    expect(screen.getByText('path: /cart?x=1')).toBeInTheDocument();
    expect(screen.getByText('retries: false')).toBeInTheDocument();
    expect(screen.getByText(`primary: ${appTheme.palette.primary.main}`)).toBeInTheDocument();
  });

  it('uses a fresh QueryClient for every render, so no cache leaks between tests', () => {
    const first = renderWithProviders(<ProbeComponent />);
    const second = renderWithProviders(<ProbeComponent />);

    expect(first.queryClient).not.toBe(second.queryClient);
    expect(createTestQueryClient()).not.toBe(first.queryClient);
  });
});
