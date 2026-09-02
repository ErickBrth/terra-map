import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { request } from './httpClient';
import { ApiError } from './ApiError';

describe('httpClient', () => {
  const originalFetch = global.fetch;

  beforeEach(() => {
    vi.restoreAllMocks();
  });

  afterEach(() => {
    global.fetch = originalFetch;
  });

  it('makes successful GET request and parses JSON', async () => {
    const mockData = { id: '123', title: 'Test Parcel' };
    global.fetch = vi.fn().mockResolvedValue({
      ok: true,
      status: 200,
      json: async () => mockData,
    });

    const result = await request<{ id: string; title: string }>('/parcels/123');

    expect(result).toEqual(mockData);
    expect(global.fetch).toHaveBeenCalledWith(
      '/api/v1/parcels/123',
      expect.objectContaining({
        headers: { 'Content-Type': 'application/json' },
      }),
    );
  });

  it('handles 204 No Content response properly', async () => {
    global.fetch = vi.fn().mockResolvedValue({
      ok: true,
      status: 204,
    });

    const result = await request<void>('/parcels/123', { method: 'DELETE' });

    expect(result).toBeUndefined();
  });

  it('throws ApiError on non-2xx response', async () => {
    const errorBody = {
      status: 404,
      title: 'Not Found',
      detail: 'Parcel not found',
    };

    global.fetch = vi.fn().mockResolvedValue({
      ok: false,
      status: 404,
      json: async () => errorBody,
    });

    await expect(request('/parcels/missing')).rejects.toThrow(ApiError);
  });
});
