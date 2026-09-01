import { request } from './httpClient';
import type {
  LandParcelResponse,
  ParcelFeatureCollection,
  RegisterParcelRequest,
} from '../types/api';

export function registerParcel(payload: RegisterParcelRequest): Promise<LandParcelResponse> {
  return request<LandParcelResponse>('/parcels', {
    method: 'POST',
    body: JSON.stringify(payload),
  });
}

export function getParcel(id: string): Promise<LandParcelResponse> {
  return request<LandParcelResponse>(`/parcels/${id}`);
}

export interface SearchParcelsPayload {
  center: { type: 'Point'; coordinates: [number, number] };
  radiusInMeters: number;
}

export function searchParcels(payload: SearchParcelsPayload): Promise<ParcelFeatureCollection> {
  return request<ParcelFeatureCollection>('/parcels/search', {
    method: 'POST',
    body: JSON.stringify(payload),
  });
}
