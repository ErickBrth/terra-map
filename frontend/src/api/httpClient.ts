import { ApiError } from './ApiError';

const BASE_URL = import.meta.env.VITE_API_BASE_URL ?? '/api/v1';
const TIMEOUT_MS = 15_000;

/**
 * All API calls go through this single function. Components and hooks never
 * call fetch() directly — that keeps timeout handling and error parsing in
 * one place instead of duplicated at every call site.
 */
export async function request<T>(path: string, init?: RequestInit): Promise<T> {
  const controller = new AbortController();
  const timeout = setTimeout(() => controller.abort(), TIMEOUT_MS);

  try {
    const response = await fetch(`${BASE_URL}${path}`, {
      ...init,
      signal: controller.signal,
      headers: { 'Content-Type': 'application/json', ...init?.headers },
    });

    if (!response.ok) {
      throw await ApiError.fromResponse(response);
    }

    return response.status === 204 ? (undefined as T) : ((await response.json()) as T);
  } finally {
    clearTimeout(timeout);
  }
}
