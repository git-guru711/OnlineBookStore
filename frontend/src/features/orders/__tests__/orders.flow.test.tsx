import { screen, within } from '@testing-library/react';
import { http, HttpResponse } from 'msw';
import { anOrder, anOrderItem, aPage } from '@/testing/data-generators';
import { API, server } from '@/testing/mocks/server';
import { FLOW_TEST_TIMEOUT, renderApp, startLoggedIn } from '@/testing/test-utils';

const DEMO_AUTH = `Basic ${btoa('demo@example.com:demo1234')}`;

const first = anOrder(
  [anOrderItem({ bookId: 1, title: 'Clean Code', unitPrice: 32.99, quantity: 2, lineTotal: 65.98 })],
  {
    id: 1,
    createdAt: '2026-11-20T10:00:00Z',
  },
);
const second = anOrder(
  [anOrderItem({ bookId: 7, title: 'Release It!', unitPrice: 39.95, quantity: 1, lineTotal: 39.95 })],
  {
    id: 2,
    createdAt: '2026-11-22T15:30:00Z',
  },
);

/** The demo user's two past orders: summaries newest first, details by id. */
function fakeOrdersBackend() {
  const summary = ({ id, status, totalQuantity, totalAmount, createdAt }: typeof first) => ({
    id,
    status,
    totalQuantity,
    totalAmount,
    createdAt,
  });
  const unauthorized = () => HttpResponse.json({ status: 401, title: 'Unauthorized' }, { status: 401 });
  const loggedIn = (request: Request) => request.headers.get('Authorization') === DEMO_AUTH;
  server.use(
    http.get(`${API}/orders`, ({ request }) =>
      loggedIn(request) ? HttpResponse.json(aPage([summary(second), summary(first)], { size: 10 })) : unauthorized(),
    ),
    http.get(`${API}/orders/1`, ({ request }) => (loggedIn(request) ? HttpResponse.json(first) : unauthorized())),
  );
}

describe('Order history (flow)', { timeout: FLOW_TEST_TIMEOUT }, () => {
  it('opens "My orders" from the header, lists the orders newest first, and opens an older order', async () => {
    fakeOrdersBackend();
    startLoggedIn();
    const { user } = renderApp({ route: '/home' });
    const header = screen.getByRole('banner');

    await user.click(within(header).getByRole('button', { name: 'Account: Demo User' }));
    await user.click(screen.getByRole('menuitem', { name: 'My orders' }));

    expect(await screen.findByRole('heading', { level: 1, name: 'My orders' })).toBeInTheDocument();
    const rows = await screen.findAllByRole('row', { name: /Order \d/ });
    expect(rows.map((row) => within(row).getByRole('link').textContent)).toEqual(['Order 2', 'Order 1']);

    await user.click(screen.getByRole('link', { name: 'Order 1' }));

    // an older order gets a neutral title, not "Thank you for your order"
    expect(await screen.findByRole('heading', { level: 1, name: 'Order 1' })).toBeInTheDocument();
    expect(screen.queryByText('Thank you for your order')).not.toBeInTheDocument();
    expect(screen.getByRole('row', { name: /Clean Code/ })).toBeInTheDocument();
  });
});
