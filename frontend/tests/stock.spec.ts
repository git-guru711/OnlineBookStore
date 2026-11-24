import { expect, test, type APIRequestContext } from '@playwright/test';

interface Book {
  id: number;
  title: string;
  availableQuantity: number;
}

const PASSWORD = 'secret123';

function basic(email: string): Record<string, string> {
  return { Authorization: `Basic ${Buffer.from(`${email}:${PASSWORD}`).toString('base64')}` };
}

async function registerCustomer(request: APIRequestContext, name: string): Promise<string> {
  const email = `e2e.${name.toLowerCase()}.${Date.now()}@example.com`;
  const response = await request.post('/api/v1/auth/register', { data: { email, password: PASSWORD, fullName: name } });
  expect(response.status()).toBe(201);
  return email;
}

/** Another customer buys `quantity` copies through the API. */
async function buy(request: APIRequestContext, bookId: number, quantity: number): Promise<void> {
  const email = await registerCustomer(request, 'Rival');
  const added = await request.post('/api/v1/cart/items', { headers: basic(email), data: { bookId, quantity } });
  expect(added.ok()).toBeTruthy();
  expect((await request.post('/api/v1/orders', { headers: basic(email) })).status()).toBe(201);
}

async function stockOf(request: APIRequestContext, bookId: number): Promise<number> {
  return (await (await request.get(`/api/v1/books/${bookId}`)).json()).availableQuantity;
}

/** The book with the fewest copies left, reduced to exactly one, so repeated runs use up as little stock as possible. */
async function aBookWithOneCopyLeft(request: APIRequestContext): Promise<Book> {
  const books: Book[] = (await (await request.get('/api/v1/books?size=100')).json()).content;
  const book = books
    .filter((b) => b.availableQuantity > 0)
    .sort((a, b) => a.availableQuantity - b.availableQuantity)[0];
  expect(book, 'a book that is still in stock (restart the backend to reset the demo data)').toBeDefined();
  if (book!.availableQuantity > 1) {
    await buy(request, book!.id, book!.availableQuantity - 1);
  }
  expect(await stockOf(request, book!.id)).toBe(1);
  return book!;
}

test('when another customer buys the last copy, the cart blocks checkout and the catalog shows "Out of stock"', async ({
  page,
  request,
}) => {
  const book = await aBookWithOneCopyLeft(request);
  const email = await registerCustomer(request, 'Alice');
  const header = page.getByRole('banner');

  // Alice logs in and puts the last copy in her cart
  await page.goto('/login');
  await page.getByLabel('Email').fill(email);
  await page.getByLabel('Password').fill(PASSWORD);
  await page.getByRole('button', { name: 'Log in' }).click();
  await expect(header.getByRole('button', { name: 'Account: Alice' })).toBeVisible();

  await page.goto(`/home?q=${encodeURIComponent(book.title)}`);
  const card = page.getByRole('article', { name: book.title, exact: true });
  await expect(card.getByText('Only 1 left')).toBeVisible();
  await card.getByRole('button', { name: 'Add to cart' }).click();
  await expect(header.getByRole('link', { name: 'Cart, 1 item' })).toBeVisible();

  // the cart reserves nothing: another customer buys the same last copy first
  await buy(request, book.id, 1);
  expect(await stockOf(request, book.id)).toBe(0);

  // Alice's cart flags the book and blocks checkout
  await header.getByRole('link', { name: 'Cart, 1 item' }).click();
  await expect(page.getByRole('row', { name: new RegExp(escapeRegExp(book.title)) })).toContainText(
    'Sold out, please remove it from your cart.',
  );
  await expect(page.getByRole('button', { name: 'Checkout' })).toBeDisabled();
  await expect(page.getByText('Please fix the books marked in red first.')).toBeVisible();

  // the catalog shows the book as sold out
  await page.goto(`/home?q=${encodeURIComponent(book.title)}`);
  await expect(card.getByText('Out of stock')).toBeVisible();
  await expect(card.getByRole('button', { name: 'Add to cart' })).toBeDisabled();
});

function escapeRegExp(text: string): string {
  return text.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
}
