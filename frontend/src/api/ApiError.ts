import type { ApiProblemDetail } from '../types/api';

/**
 * Wraps a non-2xx API response. Carries the parsed RFC 7807 Problem Details
 * body so callers can branch on `status` (409 = overlap, 422 = invalid geometry,
 * 400 = validation) without re-parsing the response themselves.
 */
export class ApiError extends Error {
  readonly status: number;
  readonly problem: ApiProblemDetail;

  constructor(problem: ApiProblemDetail) {
    const message = ApiError.extractMessage(problem);
    super(message);
    this.name = 'ApiError';
    this.status = problem.status;
    this.problem = problem;
  }

  private static extractMessage(problem: ApiProblemDetail): string {
    // If specific field validation errors are present, extract them directly
    if (problem.errors && Object.keys(problem.errors).length > 0) {
      return Object.values(problem.errors).join('. ');
    }
    return problem.detail ?? problem.title ?? `Request failed with status ${problem.status}`;
  }

  get userMessage(): string {
    return ApiError.extractMessage(this.problem);
  }

  static async fromResponse(response: Response): Promise<ApiError> {
    try {
      const body = (await response.json()) as ApiProblemDetail;
      return new ApiError({ ...body, status: body.status ?? response.status });
    } catch {
      // Response wasn't JSON (e.g. a network gateway error page) — fall back to a minimal shape.
      return new ApiError({ status: response.status, title: response.statusText });
    }
  }
}
