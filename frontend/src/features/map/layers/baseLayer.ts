import TileLayer from 'ol/layer/Tile';
import OSM from 'ol/source/OSM';

/** OpenStreetMap tile layer. Kept in its own module so the base layer
 * can be swapped later (e.g. for a different tile provider) without
 * touching the map setup hook. */
export function createBaseLayer(): TileLayer {
  return new TileLayer({ source: new OSM() });
}
