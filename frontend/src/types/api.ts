export interface GeoJsonPolygon {
  type: 'Polygon';
  coordinates: number[][][];
}

export interface GeoJsonPoint {
  type: 'Point';
  coordinates: [number, number];
}

export interface ContactInfo {
  name: string;
  email: string;
  phone?: string;
}

export interface RegisterParcelRequest {
  title: string;
  description?: string;
  totalPrice: number;
  currency: string;
  contact: ContactInfo;
  boundary: GeoJsonPolygon;
}

export interface LandParcelResponse {
  id: string;
  title: string;
  description?: string;
  totalPrice: number;
  currency: string;
  contact: ContactInfo;
  status: 'AVAILABLE' | 'RESERVED' | 'SOLD';
  boundary: GeoJsonPolygon;
}

export interface ParcelFeatureProperties {
  title: string;
  totalPrice: number;
  currency: string;
  areaInSquareMeters?: number;
  status: string;
  contact: ContactInfo;
}

export interface ParcelFeature {
  type: 'Feature';
  id: string;
  geometry: GeoJsonPolygon;
  properties: ParcelFeatureProperties;
}

export interface ParcelFeatureCollection {
  type: 'FeatureCollection';
  features: ParcelFeature[];
}

/** RFC 7807 Problem Details — the shape every error response from the API follows. */
export interface ApiProblemDetail {
  type?: string;
  title?: string;
  status: number;
  detail?: string;
  instance?: string;
  conflictingParcelIds?: string[];
  errors?: Record<string, string>;
}
