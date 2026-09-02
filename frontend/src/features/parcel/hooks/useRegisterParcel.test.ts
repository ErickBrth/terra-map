import { describe, it, expect, vi, beforeEach } from 'vitest';
import { renderHook, act } from '@testing-library/react';
import { useRegisterParcel } from './useRegisterParcel';
import * as landParcelApi from '../../../api/landParcelApi';
import { ApiError } from '../../../api/ApiError';
import type { GeoJsonPolygon, LandParcelResponse } from '../../../types/api';

describe('useRegisterParcel', () => {
  const mockBoundary: GeoJsonPolygon = {
    type: 'Polygon',
    coordinates: [[[-46.63, -23.55], [-46.62, -23.55], [-46.62, -23.56], [-46.63, -23.56], [-46.63, -23.55]]],
  };

  beforeEach(() => {
    vi.restoreAllMocks();
  });

  it('manages lifecycle from idle -> drawing -> form-open -> submit -> idle', async () => {
    const mockCreated: LandParcelResponse = {
      id: 'parcel-123',
      title: 'Green Valley',
      totalPrice: 120000,
      currency: 'BRL',
      status: 'AVAILABLE',
      contact: { name: 'Alice', email: 'alice@example.com' },
      boundary: mockBoundary,
    };

    vi.spyOn(landParcelApi, 'registerParcel').mockResolvedValue(mockCreated);

    const { result } = renderHook(() => useRegisterParcel());

    expect(result.current.step).toBe('idle');

    act(() => {
      result.current.startDrawing();
    });
    expect(result.current.step).toBe('drawing');

    act(() => {
      result.current.onPolygonDrawn(mockBoundary);
    });
    expect(result.current.step).toBe('form-open');
    expect(result.current.pendingBoundary).toEqual(mockBoundary);

    await act(async () => {
      await result.current.submit({
        title: 'Green Valley',
        totalPrice: 120000,
        currency: 'BRL',
        contact: { name: 'Alice', email: 'alice@example.com' },
        boundary: mockBoundary,
      });
    });

    expect(result.current.step).toBe('idle');
    expect(result.current.pendingBoundary).toBeNull();
  });

  it('handles 409 Conflict overlap error', async () => {
    const conflictResponse = new Response(
      JSON.stringify({
        status: 409,
        detail: 'This boundary overlaps an existing parcel.',
      }),
      { status: 409, headers: { 'Content-Type': 'application/json' } },
    );
    const apiError = await ApiError.fromResponse(conflictResponse);

    vi.spyOn(landParcelApi, 'registerParcel').mockRejectedValue(apiError);

    const { result } = renderHook(() => useRegisterParcel());

    act(() => {
      result.current.onPolygonDrawn(mockBoundary);
    });

    await act(async () => {
      await result.current.submit({
        title: 'Overlapping Plot',
        totalPrice: 50000,
        currency: 'BRL',
        contact: { name: 'Bob', email: 'bob@example.com' },
        boundary: mockBoundary,
      });
    });

    expect(result.current.step).toBe('form-open');
    expect(result.current.errorMessage).toContain('overlaps an existing parcel');
  });

  it('handles cancellation', () => {
    const { result } = renderHook(() => useRegisterParcel());

    act(() => {
      result.current.startDrawing();
    });
    expect(result.current.step).toBe('drawing');

    act(() => {
      result.current.cancel();
    });
    expect(result.current.step).toBe('idle');
    expect(result.current.pendingBoundary).toBeNull();
  });
});
