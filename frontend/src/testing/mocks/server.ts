import { setupServer } from 'msw/node';
import { handlers } from '@/testing/mocks/handlers';

export const server = setupServer(...handlers);

// Kept in its own module so handler files can use it without a circular import through this file.
export { API } from '@/testing/mocks/api-url';
