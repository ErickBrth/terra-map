import { describe, it, expect, vi, beforeEach } from 'vitest';
import { registerParcel, getParcel, searchParcels, reserveParcel, markParcelSold } from '../../api/landParcelApi';
import * as httpClient from '../../api/httpClient';
import { ApiError } from '../../api/ApiError';
import type { RegisterParcelRequest, LandParcelResponse, ParcelFeatureCollection } from '../../types/api';

const MOCK_PARCEL: LandParcelResponse = {
  id: 'abc-123',
  title: 'Hilltop',
  totalPrice: 150000,
  currency: 'BRL',
  status: 'AVAILABLE',
  contact: { name: 'John', email: 'john@example.com' },
  boundary: { type: 'Polygon', coordinates: [] },
};

describe('landParcelApi', () => {
  beforeEach(() => {
    vi.restoreAllMocks();
  });

  it('calls POST /parcels when registering a parcel', async () => {
    const payload: RegisterParcelRequest = {
      title: 'Riverside',
      totalPrice: 200000,
      currency: 'BRL',
      contact: { name: 'Jane', email: 'jane@example.com' },
      boundary: {
        type: 'Polygon',
        coordinates: [[[-46.63, -23.55], [-46.62, -23.55], [-46.62, -23.56], [-46.63, -23.56], [-46.63, -23.55]]],
      },
    };

    const mockResponse: LandParcelResponse = {
      id: '123',
      title: 'Riverside',
      totalPrice: 200000,
      currency: 'BRL',
      status: 'AVAILABLE',
      contact: { name: 'Jane', email: 'jane@example.com' },
      boundary: payload.boundary,
    };

    const requestSpy = vi.spyOn(httpClient, 'request').mockResolvedValue(mockResponse);

    const result = await registerParcel(payload);

    expect(requestSpy).toHaveBeenCalledWith('/parcels', {
      method: 'POST',
      body: JSON.stringify(payload),
    });
    expect(result).toEqual(mockResponse);
  });

  it('calls GET /parcels/:id when retrieving parcel details', async () => {
    const requestSpy = vi.spyOn(httpClient, 'request').mockResolvedValue(MOCK_PARCEL);

    const result = await getParcel('abc-123');

    expect(requestSpy).toHaveBeenCalledWith('/parcels/abc-123');
    expect(result).toEqual(MOCK_PARCEL);
  });

  it('calls POST /parcels/search when executing circular search', async () => {
    const searchPayload = {
      center: { type: 'Point' as const, coordinates: [-46.63, -23.55] as [number, number] },
      radiusInMeters: 1500,
    };

    const mockResponse: ParcelFeatureCollection = {
      type: 'FeatureCollection',
      features: [],
    };

    const requestSpy = vi.spyOn(httpClient, 'request').mockResolvedValue(mockResponse);

    const result = await searchParcels(searchPayload);

    expect(requestSpy).toHaveBeenCalledWith('/parcels/search', {
      method: 'POST',
      body: JSON.stringify(searchPayload),
    });
    expect(result).toEqual(mockResponse);
  });

  it('calls PATCH /parcels/:id/reserve and returns the updated parcel', async () => {
    const reserved = { ...MOCK_PARCEL, status: 'RESERVED' as const };
    const requestSpy = vi.spyOn(httpClient, 'request').mockResolvedValue(reserved);

    const result = await reserveParcel('abc-123');

    expect(requestSpy).toHaveBeenCalledWith('/parcels/abc-123/reserve', { method: 'PATCH' });
    expect(result.status).toBe('RESERVED');
  });

  it('calls PATCH /parcels/:id/sell and returns the updated parcel', async () => {
    const sold = { ...MOCK_PARCEL, status: 'SOLD' as const };
    const requestSpy = vi.spyOn(httpClient, 'request').mockResolvedValue(sold);

    const result = await markParcelSold('abc-123');

    expect(requestSpy).toHaveBeenCalledWith('/parcels/abc-123/sell', { method: 'PATCH' });
    expect(result.status).toBe('SOLD');
  });

  it('propagates ApiError when reserve fails with 409 Conflict', async () => {
    vi.spyOn(httpClient, 'request').mockRejectedValue(
      new ApiError({ status: 409, title: 'Conflict', detail: 'Parcel is not currently AVAILABLE' }),
    );

    await expect(reserveParcel('abc-123')).rejects.toBeInstanceOf(ApiError);
  });
});
