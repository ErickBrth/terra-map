import { describe, it, expect, vi, beforeEach } from 'vitest';
import { renderHook, act } from '@testing-library/react';
import { useSearchParcels } from '../../features/search/hooks/useSearchParcels';
import * as landParcelApi from '../../api/landParcelApi';
import { ApiError } from '../../api/ApiError';
import type { ParcelFeatureCollection } from '../../types/api';

describe('useSearchParcels', () => {
  beforeEach(() => {
    vi.restoreAllMocks();
  });

  it('handles search lifecycle and updates result count', async () => {
    const mockSearchResults: ParcelFeatureCollection = {
      type: 'FeatureCollection',
      features: [
        {
          type: 'Feature',
          id: 'p-1',
          geometry: {
            type: 'Polygon',
            coordinates: [[[-46.63, -23.55], [-46.62, -23.55], [-46.62, -23.56], [-46.63, -23.56], [-46.63, -23.55]]],
          },
          properties: {
            title: 'Found Plot',
            totalPrice: 100000,
            currency: 'BRL',
            status: 'AVAILABLE',
            contact: { name: 'Dan', email: 'dan@example.com' },
          },
        },
      ],
    };

    vi.spyOn(landParcelApi, 'searchParcels').mockResolvedValue(mockSearchResults);

    const { result } = renderHook(() => useSearchParcels());

    expect(result.current.step).toBe('idle');

    act(() => {
      result.current.startSearching();
    });
    expect(result.current.step).toBe('drawing');

    act(() => {
      result.current.onRadiusChange(800);
    });
    expect(result.current.radiusInMetres).toBe(800);

    await act(async () => {
      await result.current.onCircleDrawn({
        center: { type: 'Point', coordinates: [-46.63, -23.55] },
        radiusInMeters: 800,
      });
    });

    expect(result.current.step).toBe('idle');
    expect(result.current.resultCount).toBe(1);
    expect(result.current.radiusInMetres).toBeNull();
  });

  it('displays real validation error message on search failure', async () => {
    const errorResponse = new Response(
      JSON.stringify({
        status: 400,
        errors: { radiusInMeters: 'Radius cannot exceed 50,000 metres' },
      }),
      { status: 400, headers: { 'Content-Type': 'application/json' } },
    );
    const apiError = await ApiError.fromResponse(errorResponse);

    vi.spyOn(landParcelApi, 'searchParcels').mockRejectedValue(apiError);

    const { result } = renderHook(() => useSearchParcels());

    await act(async () => {
      await result.current.onCircleDrawn({
        center: { type: 'Point', coordinates: [-46.63, -23.55] },
        radiusInMeters: 60000,
      });
    });

    expect(result.current.errorMessage).toBe('Radius cannot exceed 50,000 metres');
  });

  it('handles cancellation', () => {
    const { result } = renderHook(() => useSearchParcels());

    act(() => {
      result.current.startSearching();
      result.current.onRadiusChange(500);
    });
    expect(result.current.step).toBe('drawing');

    act(() => {
      result.current.cancel();
    });
    expect(result.current.step).toBe('idle');
    expect(result.current.radiusInMetres).toBeNull();
  });
});
