import { BrowserRouter } from 'react-router-dom';
import { AppProviders } from '@/app/provider';
import { AppRoutes } from '@/app/router';
import { createQueryClient } from '@/lib/query-client';

const queryClient = createQueryClient();

export function App() {
  return (
    <AppProviders queryClient={queryClient}>
      <BrowserRouter>
        <AppRoutes />
      </BrowserRouter>
    </AppProviders>
  );
}
