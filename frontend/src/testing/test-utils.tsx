import type { ReactElement, ReactNode } from 'react';
import { render, renderHook } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { QueryClient } from '@tanstack/react-query';
import { MemoryRouter } from 'react-router-dom';
import { AppProviders } from '@/app/provider';
import { AppRoutes } from '@/app/router';
import { saveSession } from '@/features/auth/utils/session';
import type { User } from '@/features/auth/types';
import { aUser } from '@/testing/data-generators';

/** Flow tests click through whole journeys, so they get more than the 5 s default on a slow machine. */
export const FLOW_TEST_TIMEOUT = 15_000;

/** A fresh client per test: no cache shared between tests, and failures show at once (no retries). */
export function createTestQueryClient() {
  return new QueryClient({ defaultOptions: { queries: { retry: false }, mutations: { retry: false } } });
}

interface RenderOptions {
  route?: string;
  routeState?: unknown;
}

export function renderWithProviders(ui: ReactElement, { route = '/', routeState }: RenderOptions = {}) {
  const queryClient = createTestQueryClient();
  const user = userEvent.setup();
  const result = render(
    <AppProviders queryClient={queryClient}>
      <MemoryRouter
        initialEntries={[
          {
            pathname: new URL(route, 'http://test').pathname,
            search: new URL(route, 'http://test').search,
            state: routeState,
          },
        ]}
      >
        {ui}
      </MemoryRouter>
    </AppProviders>,
  );
  return { ...result, user, queryClient };
}

export function renderApp(options: RenderOptions = {}) {
  return renderWithProviders(<AppRoutes />, options);
}

export function renderHookWithProviders<Result>(hook: () => Result) {
  const queryClient = createTestQueryClient();
  const wrapper = ({ children }: { children: ReactNode }) => (
    <AppProviders queryClient={queryClient}>{children}</AppProviders>
  );
  return { ...renderHook(hook, { wrapper }), queryClient };
}

/** Logged in from the first render, as after a page refresh. */
export function startLoggedIn(user: User = aUser(), password = 'demo1234') {
  saveSession({ email: user.email, password, user });
}
