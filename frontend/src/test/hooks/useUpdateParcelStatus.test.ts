import { describe, it, expect, vi, beforeEach } from 'vitest';
import { renderHook, act } from '@testing-library/react';
import { useUpdateParcelStatus } from '../../features/parcel/hooks/useUpdateParcelStatus';
import * as landParcelApi from '../../api/landParcelApi';
import { ApiError } from '../../api/ApiError';
import { parcelSource, toParcelFeature } from '../../features/map/layers/parcelLayer';

describe('useUpdateParcelStatus', () => {
  beforeEach(() => {
    vi.restoreAllMocks();
    parcelSource.clear();
    parcelSource.addFeature(
      toParcelFeature([[[-46.63, -23.55], [-46.62, -23.55], [-46.62, -23.56], [-46.63, -23.56], [-46.63, -23.55]]], {
        id: 'parcel-1',
        title: 'Green Valley',
        status: 'AVAILABLE',
      }),
    );
  });

  it('reserving a parcel updates its status on the real map feature and calls onDone', async () => {
    vi.spyOn(landParcelApi, 'reserveParcel').mockResolvedValue({
      id: 'parcel-1',
      title: 'Green Valley',
      totalPrice: 1,
      currency: 'BRL',
      status: 'RESERVED',
      contact: { name: 'A', email: 'a@example.com' },
      boundary: { type: 'Polygon', coordinates: [] },
    });

    const onDone = vi.fn();
    const { result } = renderHook(() => useUpdateParcelStatus(onDone));

    await act(async () => {
      await result.current.reserve('parcel-1');
    });

    expect(parcelSource.getFeatureById('parcel-1')?.get('status')).toBe('RESERVED');
    expect(onDone).toHaveBeenCalledWith('RESERVED');
    expect(result.current.pending).toBe(false);
    expect(result.current.errorMessage).toBeNull();
  });

  it('a failed transition leaves the map feature untouched and surfaces the error', async () => {
    const conflictResponse = new Response(
      JSON.stringify({ status: 409, detail: 'Only AVAILABLE parcels can be reserved' }),
      { status: 409, headers: { 'Content-Type': 'application/json' } },
    );
    const apiError = await ApiError.fromResponse(conflictResponse);
    vi.spyOn(landParcelApi, 'reserveParcel').mockRejectedValue(apiError);

    const onDone = vi.fn();
    const { result } = renderHook(() => useUpdateParcelStatus(onDone));

    await act(async () => {
      await result.current.reserve('parcel-1');
    });

    expect(parcelSource.getFeatureById('parcel-1')?.get('status')).toBe('AVAILABLE');
    expect(onDone).not.toHaveBeenCalled();
    expect(result.current.errorMessage).toContain('Only AVAILABLE parcels can be reserved');
  });

  it('marking a parcel sold updates its status on the real map feature', async () => {
    vi.spyOn(landParcelApi, 'markParcelSold').mockResolvedValue({
      id: 'parcel-1',
      title: 'Green Valley',
      totalPrice: 1,
      currency: 'BRL',
      status: 'SOLD',
      contact: { name: 'A', email: 'a@example.com' },
      boundary: { type: 'Polygon', coordinates: [] },
    });

    const { result } = renderHook(() => useUpdateParcelStatus(vi.fn()));

    await act(async () => {
      await result.current.markSold('parcel-1');
    });

    expect(parcelSource.getFeatureById('parcel-1')?.get('status')).toBe('SOLD');
  });
});
