import { describe, it, expect, vi, beforeEach } from 'vitest';
import { renderHook, act } from '@testing-library/react';
import { useSearchParcels } from '../../features/search/hooks/useSearchParcels';
import * as landParcelApi from '../../api/landParcelApi';
import { ApiError } from '../../api/ApiError';
import { parcelSource, toParcelFeature } from '../../features/map/layers/parcelLayer';
import type { ParcelFeatureCollection } from '../../types/api';

describe('useSearchParcels', () => {
  beforeEach(() => {
    vi.restoreAllMocks();
    parcelSource.clear();
  });

  it('replaces whatever was on the map with exactly the search results', async () => {
    // Simulates a parcel already visible on the map (e.g. just registered)
    // that does NOT intersect the upcoming search circle.
    parcelSource.addFeature(
      toParcelFeature(
        [[[-43.2, -22.91], [-43.19, -22.91], [-43.19, -22.9], [-43.2, -22.9], [-43.2, -22.91]]],
        { id: 'stale-parcel', title: 'Stale', status: 'AVAILABLE' },
      ),
    );

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

    // The real, user-visible outcome, per spec: only the intersecting parcel
    // remains — the stale one is gone.
    const features = parcelSource.getFeatures();
    expect(features).toHaveLength(1);
    expect(features[0].getId()).toBe('p-1');
    expect(features[0].get('title')).toBe('Found Plot');
    expect(parcelSource.getFeatureById('stale-parcel')).toBeNull();
  });

  it('displays real validation error message on search failure and leaves the map untouched', async () => {
    parcelSource.addFeature(toParcelFeature([[[0, 0], [0, 1], [1, 1], [1, 0], [0, 0]]], {
      id: 'unrelated',
      title: 'Unrelated',
      status: 'AVAILABLE',
    }));

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
    // A failed search must not wipe out what was already on the map.
    expect(parcelSource.getFeatureById('unrelated')).not.toBeNull();
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
