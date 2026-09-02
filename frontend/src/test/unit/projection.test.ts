import { describe, it, expect } from 'vitest';
import Polygon from 'ol/geom/Polygon';
import { fromLonLat } from 'ol/proj';
import { toGeoJsonPolygon, fromGeoJsonPolygonCoordinates } from '../../shared/geo/projection';

describe('projection utils', () => {
  it('converts an EPSG:3857 Polygon to EPSG:4326 GeoJSON with 6 decimal places', () => {
    // Coordinate in Sao Paulo: [-46.633, -23.55]
    const coords4326 = [
      [-46.635, -23.555],
      [-46.625, -23.555],
      [-46.625, -23.545],
      [-46.635, -23.545],
      [-46.635, -23.555],
    ];

    const coords3857 = [coords4326.map(([lon, lat]) => fromLonLat([lon, lat]))];
    const olPolygon = new Polygon(coords3857);

    const geoJson = toGeoJsonPolygon(olPolygon);

    expect(geoJson.type).toBe('Polygon');
    expect(geoJson.coordinates).toHaveLength(1);
    expect(geoJson.coordinates[0]).toHaveLength(5);

    // Verify coordinates match expected 6 decimal precision
    expect(geoJson.coordinates[0][0][0]).toBeCloseTo(-46.635, 4);
    expect(geoJson.coordinates[0][0][1]).toBeCloseTo(-23.555, 4);
  });

  it('converts GeoJSON coordinates (EPSG:4326) back to OpenLayers map coordinates (EPSG:3857)', () => {
    const geoJsonCoords = [
      [
        [-46.635, -23.555],
        [-46.625, -23.555],
        [-46.625, -23.545],
        [-46.635, -23.545],
        [-46.635, -23.555],
      ],
    ];

    const mapCoords = fromGeoJsonPolygonCoordinates(geoJsonCoords);

    expect(mapCoords).toHaveLength(1);
    expect(mapCoords[0]).toHaveLength(5);
    // EPSG:3857 coordinates are in meters (large numbers)
    expect(Math.abs(mapCoords[0][0][0])).toBeGreaterThan(1000000);
    expect(Math.abs(mapCoords[0][0][1])).toBeGreaterThan(1000000);
  });
});
