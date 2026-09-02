import { describe, it, expect, vi, beforeEach } from 'vitest';
import { registerParcel, getParcel, searchParcels } from './landParcelApi';
import * as httpClient from './httpClient';
import type { RegisterParcelRequest, LandParcelResponse, ParcelFeatureCollection } from '../types/api';

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
      boundary: { type: 'Polygon', coordinates: [[[-46.63, -23.55], [-46.62, -23.55], [-46.62, -23.56], [-46.63, -23.56], [-46.63, -23.55]]] },
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
    const mockResponse: LandParcelResponse = {
      id: 'abc-123',
      title: 'Hilltop',
      totalPrice: 150000,
      currency: 'BRL',
      status: 'AVAILABLE',
      contact: { name: 'John', email: 'john@example.com' },
      boundary: { type: 'Polygon', coordinates: [] },
    };

    const requestSpy = vi.spyOn(httpClient, 'request').mockResolvedValue(mockResponse);

    const result = await getParcel('abc-123');

    expect(requestSpy).toHaveBeenCalledWith('/parcels/abc-123');
    expect(result).toEqual(mockResponse);
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
});
