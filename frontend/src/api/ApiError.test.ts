import { describe, it, expect } from 'vitest';
import { ApiError } from './ApiError';

describe('ApiError', () => {
  it('parses RFC 7807 JSON response body with detail properly', async () => {
    const mockProblem = {
      type: 'https://terramap.dev/errors/overlapping-parcel',
      title: 'Overlapping parcel',
      status: 409,
      detail: 'The drawn boundary overlaps 1 existing parcel.',
      instance: '/api/v1/parcels',
      conflictingParcelIds: ['3f0a8e99-1234-5678-9abc-def012345678'],
    };

    const mockResponse = new Response(JSON.stringify(mockProblem), {
      status: 409,
      headers: { 'Content-Type': 'application/problem+json' },
    });

    const error = await ApiError.fromResponse(mockResponse);

    expect(error).toBeInstanceOf(ApiError);
    expect(error.status).toBe(409);
    expect(error.message).toBe('The drawn boundary overlaps 1 existing parcel.');
    expect(error.userMessage).toBe('The drawn boundary overlaps 1 existing parcel.');
    expect(error.problem.conflictingParcelIds).toEqual(['3f0a8e99-1234-5678-9abc-def012345678']);
  });

  it('extracts field validation errors when problem.errors is present', async () => {
    const mockProblem = {
      type: 'https://terramap.dev/errors/validation-failed',
      title: 'Validation Failed',
      status: 400,
      detail: 'One or more request parameters failed validation',
      errors: {
        radiusInMeters: 'Radius cannot exceed 50,000 metres',
      },
      instance: '/api/v1/parcels/search',
    };

    const mockResponse = new Response(JSON.stringify(mockProblem), {
      status: 400,
      headers: { 'Content-Type': 'application/problem+json' },
    });

    const error = await ApiError.fromResponse(mockResponse);

    expect(error).toBeInstanceOf(ApiError);
    expect(error.status).toBe(400);
    expect(error.message).toBe('Radius cannot exceed 50,000 metres');
    expect(error.userMessage).toBe('Radius cannot exceed 50,000 metres');
  });

  it('falls back gracefully when response is not JSON', async () => {
    const mockResponse = new Response('502 Bad Gateway', {
      status: 502,
      statusText: 'Bad Gateway',
      headers: { 'Content-Type': 'text/plain' },
    });

    const error = await ApiError.fromResponse(mockResponse);

    expect(error).toBeInstanceOf(ApiError);
    expect(error.status).toBe(502);
    expect(error.problem.title).toBe('Bad Gateway');
  });
});
