import { expect, test, type APIRequestContext } from '@playwright/test';

/**
 * The whole purchase in a real browser against the real backend. The backend's data lives as long as it runs,
 * so the test registers its own user and picks a book that still has stock.
 */

interface Book {
  id: number;
  title: string;
  price: number;
  availableQuantity: number;
}

const euro = new Intl.NumberFormat('en-IE', { style: 'currency', currency: 'EUR' }); // same format as the app

/** The book with the most copies left, so repeated runs keep working. */
async function aBookWithStock(request: APIRequestContext): Promise<Book> {
  const response = await request.get('/api/v1/books?size=100');
  expect(response.ok(), 'the backend answers on /api/v1/books').toBeTruthy();
  const books: Book[] = (await response.json()).content;
  const book = [...books].sort((a, b) => b.availableQuantity - a.availableQuantity)[0];
  expect(book?.availableQuantity ?? 0, 'a book with at least 2 copies left').toBeGreaterThanOrEqual(2);
  return book!;
}

async function stockOf(request: APIRequestContext, bookId: number): Promise<number> {
  const response = await request.get(`/api/v1/books/${bookId}`);
  return (await response.json()).availableQuantity;
}

test('a new customer registers, finds a book, changes the quantity in the cart and checks out', async ({
  page,
  request,
}) => {
  const book = await aBookWithStock(request);
  const stockBefore = book.availableQuantity;
  const customer = { name: 'Erin Shopper', email: `e2e.${Date.now()}@example.com`, password: 'secret123' };
  const header = page.getByRole('banner');

  // The site root opens the home page
  await page.goto('/');
  await expect(page).toHaveURL(/\/home$/);
  await expect(page.getByRole('heading', { level: 1, name: 'Books' })).toBeVisible();

  // Register: logged in straight away, back on the home page
  await header.getByRole('link', { name: 'Register' }).click();
  await page.getByLabel('Full name').fill(customer.name);
  await page.getByLabel('Email').fill(customer.email);
  await page.getByLabel('Password').fill(customer.password);
  await page.getByRole('button', { name: 'Create account' }).click();
  await expect(header.getByRole('button', { name: `Account: ${customer.name}` })).toBeVisible();
  await expect(page).toHaveURL(/\/home$/);

  // Search for the book and add it to the cart
  await page.getByRole('searchbox', { name: 'Search books' }).fill(book.title);
  await page.getByRole('searchbox', { name: 'Search books' }).press('Enter');
  await expect(page).toHaveURL(/\/home\?q=/);
  const card = page.getByRole('article', { name: book.title, exact: true });
  await card.getByRole('button', { name: 'Add to cart' }).click();
  await expect(page.getByText('Added to cart')).toBeVisible();
  await expect(header.getByRole('link', { name: 'Cart, 1 item' })).toBeVisible();

  // In the cart: one more copy; the summary shows twice the price
  await header.getByRole('link', { name: 'Cart, 1 item' }).click();
  await expect(page.getByRole('heading', { level: 1, name: 'Your cart' })).toBeVisible();
  await page.getByRole('button', { name: `Increase quantity of ${book.title}` }).click();
  await expect(header.getByRole('link', { name: 'Cart, 2 items' })).toBeVisible();
  const total = euro.format(book.price * 2);
  await expect(page.getByRole('complementary', { name: 'Cart summary' }).getByText(total)).toBeVisible();

  // Checkout: review, then place the order
  await page.getByRole('button', { name: 'Checkout' }).click();
  await expect(page.getByRole('heading', { level: 1, name: 'Checkout' })).toBeVisible();
  await expect(page.getByRole('row', { name: new RegExp(`Total.*${escapeRegExp(total)}`) })).toBeVisible();
  await page.getByRole('button', { name: 'Place order' }).click();

  // Confirmation: thanks, order number, the book; the cart is empty again
  await expect(page.getByRole('heading', { level: 1, name: 'Thank you for your order' })).toBeVisible();
  const orderNumber = (await page.getByText(/^Order number \d+$/).textContent())!.replace('Order number ', '');
  await expect(page).toHaveURL(new RegExp(`/orders/${orderNumber}$`));
  await expect(page.getByRole('row', { name: new RegExp(escapeRegExp(book.title)) })).toBeVisible();
  await expect(header.getByRole('link', { name: 'Cart, 0 items' })).toBeVisible();

  // "My orders" lists it
  await header.getByRole('button', { name: `Account: ${customer.name}` }).click();
  await page.getByRole('menuitem', { name: 'My orders' }).click();
  await expect(page.getByRole('heading', { level: 1, name: 'My orders' })).toBeVisible();
  await expect(page.getByRole('link', { name: new RegExp(`Order ${orderNumber}\\b`) })).toBeVisible();

  // The real stock went down by exactly the two copies bought
  expect(await stockOf(request, book.id)).toBe(stockBefore - 2);
});

function escapeRegExp(text: string): string {
  return text.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
}
