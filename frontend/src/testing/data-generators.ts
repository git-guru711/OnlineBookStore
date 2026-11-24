import type { User } from '@/features/auth/types';
import type { Book } from '@/features/books/types';
import type { Cart, CartItem } from '@/features/cart/types';
import type { Order, OrderItem } from '@/features/orders/types';
import type { PageResponse } from '@/types/api';

/** Test data with sensible defaults, so each test states only what it cares about. */
export function aBook(overrides: Partial<Book> = {}): Book {
  return {
    id: 1,
    isbn: '9780132350884',
    title: 'Clean Code',
    author: 'Robert C. Martin',
    price: 32.99,
    availableQuantity: 15,
    stockStatus: 'IN_STOCK',
    ...overrides,
  };
}

export function aPage<T>(content: T[], overrides: Partial<PageResponse<T>> = {}): PageResponse<T> {
  return {
    content,
    page: 0,
    size: 20,
    totalElements: content.length,
    totalPages: content.length === 0 ? 0 : 1,
    ...overrides,
  };
}

export function aUser(overrides: Partial<User> = {}): User {
  return {
    id: 1,
    email: 'demo@example.com',
    fullName: 'Demo User',
    role: 'USER',
    createdAt: '2026-11-20T10:15:30Z',
    ...overrides,
  };
}

export function aCartItem(overrides: Partial<CartItem> = {}): CartItem {
  return {
    bookId: 1,
    title: 'Clean Code',
    author: 'Robert C. Martin',
    unitPrice: 32.99,
    quantity: 1,
    lineTotal: 32.99,
    availableQuantity: 15,
    stockStatus: 'IN_STOCK',
    exceedsAvailable: false,
    ...overrides,
  };
}

export function aCart(items: CartItem[] = [], overrides: Partial<Cart> = {}): Cart {
  return {
    items,
    totalQuantity: items.reduce((sum, item) => sum + item.quantity, 0),
    subtotal: Math.round(items.reduce((sum, item) => sum + item.lineTotal, 0) * 100) / 100,
    checkoutAllowed: items.length > 0 && items.every((item) => !item.exceedsAvailable),
    ...overrides,
  };
}

export function anOrderItem(overrides: Partial<OrderItem> = {}): OrderItem {
  return { bookId: 1, title: 'Clean Code', unitPrice: 32.99, quantity: 1, lineTotal: 32.99, ...overrides };
}

export function anOrder(items: OrderItem[] = [anOrderItem()], overrides: Partial<Order> = {}): Order {
  return {
    id: 42,
    status: 'PLACED',
    items,
    totalQuantity: items.reduce((sum, item) => sum + item.quantity, 0),
    totalAmount: Math.round(items.reduce((sum, item) => sum + item.lineTotal, 0) * 100) / 100,
    createdAt: '2026-11-23T10:20:00Z',
    ...overrides,
  };
}
