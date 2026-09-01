import { transform } from 'ol/proj';
import type Polygon from 'ol/geom/Polygon';
import type { GeoJsonPolygon } from '../../types/api';

const MAP_PROJECTION = 'EPSG:3857'; // what OpenLayers draws in
const API_PROJECTION = 'EPSG:4326'; // what the backend and GeoJSON expect

/**
 * Decimal places kept for lon/lat.
 * 6 decimal places ≈ 11 cm precision — plenty for a land parcel boundary,
 * and keeps payloads small (floating-point noise beyond 6 places is irrelevant).
 */
const COORDINATE_PRECISION = 6;

function round(value: number): number {
  return Number(value.toFixed(COORDINATE_PRECISION));
}

/**
 * Converts an OpenLayers polygon (drawn in EPSG:3857, meters) into a GeoJSON
 * polygon in EPSG:4326 (degrees), ready to send to the API.
 *
 */
export function toGeoJsonPolygon(polygon: Polygon): GeoJsonPolygon {
  const rings = polygon.getCoordinates(); // array of linear rings, in map projection
  const coordinates = rings.map((ring) =>
    ring.map(([x, y]) => {
      const [lon, lat] = transform([x, y], MAP_PROJECTION, API_PROJECTION);
      return [round(lon), round(lat)];
    }),
  );

  return { type: 'Polygon', coordinates };
}

/**
 * The reverse conversion: turns GeoJSON coordinates (EPSG:4326) into
 * OpenLayers ring coordinates (EPSG:3857), used when rendering a parcel
 * fetched from the API back onto the map.
 */
export function fromGeoJsonPolygonCoordinates(coordinates: number[][][]): number[][][] {
  return coordinates.map((ring) =>
    ring.map(([lon, lat]) => transform([lon, lat], API_PROJECTION, MAP_PROJECTION)),
  );
}
