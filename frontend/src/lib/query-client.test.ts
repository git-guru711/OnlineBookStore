import { ApiError } from '@/lib/api-error';
import { createQueryClient, shouldRetry } from '@/lib/query-client';

const problem = (status: number) => ApiError.fromProblem(status, { status, title: 'Error' });

describe('shouldRetry', () => {
  it.each([400, 401, 403, 404, 409])('never retries a %i: asking again gives the same answer', (status) => {
    expect(shouldRetry(0, problem(status))).toBe(false);
  });

  it('retries a server error (5xx) or a network failure, up to 3 times', () => {
    expect(shouldRetry(0, problem(500))).toBe(true);
    expect(shouldRetry(2, problem(503))).toBe(true);
    expect(shouldRetry(3, problem(500))).toBe(false); // 1 try + 3 retries, then give up
    expect(shouldRetry(0, ApiError.network())).toBe(true);
  });
});

describe('createQueryClient', () => {
  it('uses shouldRetry for queries', () => {
    expect(createQueryClient().getDefaultOptions().queries?.retry).toBe(shouldRetry);
  });
});
