const TYPE_PREFIX = 'https://bookstore.example/problems/';

export interface ProblemDetail {
  type?: string;
  title?: string;
  status?: number;
  detail?: string;
  errors?: unknown[];
}

export class ApiError extends Error {
  readonly status: number;
  readonly type: string;
  readonly title: string;
  readonly errors: unknown[];
  readonly fieldErrors: Record<string, string>;

  constructor(status: number, type: string, title: string, message: string, errors: unknown[] = []) {
    super(message);
    this.name = 'ApiError';
    this.status = status;
    this.type = type;
    this.title = title;
    this.errors = errors;
    this.fieldErrors = toFieldErrors(errors);
  }

  static fromProblem(status: number, problem: ProblemDetail): ApiError {
    const type = problem.type?.startsWith(TYPE_PREFIX) ? problem.type.slice(TYPE_PREFIX.length) : 'unknown';
    return new ApiError(
      status,
      type,
      problem.title ?? 'Error',
      problem.detail ?? problem.title ?? GENERIC_MESSAGE,
      problem.errors ?? [],
    );
  }

  static generic(status: number): ApiError {
    return new ApiError(status, 'unknown', 'Error', GENERIC_MESSAGE);
  }

  static network(): ApiError {
    return new ApiError(0, 'network-error', 'Network error', 'Unable to reach the server.');
  }
}

const GENERIC_MESSAGE = 'Something went wrong. Please try again.';

function toFieldErrors(errors: unknown[]): Record<string, string> {
  const result: Record<string, string> = {};
  for (const error of errors) {
    if (isFieldError(error)) {
      result[error.field] = error.message;
    }
  }
  return result;
}

function isFieldError(value: unknown): value is { field: string; message: string } {
  return (
    typeof value === 'object' &&
    value !== null &&
    typeof (value as { field?: unknown }).field === 'string' &&
    typeof (value as { message?: unknown }).message === 'string'
  );
}
