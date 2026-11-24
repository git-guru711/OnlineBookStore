import type { RequestHandler } from 'msw';
import { booksHandlers } from '@/testing/mocks/handlers/books';
import { cartHandlers } from '@/testing/mocks/handlers/cart';

/** Default happy-path handlers; tests override them with server.use(...). */
export const handlers: RequestHandler[] = [...booksHandlers, ...cartHandlers];
