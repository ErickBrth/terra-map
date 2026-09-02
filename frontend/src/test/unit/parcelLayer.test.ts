import { describe, it, expect } from 'vitest';
import Feature from 'ol/Feature';
import Polygon from 'ol/geom/Polygon';
import { createParcelLayer, parcelSource, PARCEL_LAYER_NAME } from '../../features/map/layers/parcelLayer';

describe('parcelLayer', () => {
  it('creates a vector layer with parcelSource, name parcels, and status styling', () => {
    const layer = createParcelLayer();

    expect(layer).toBeDefined();
    expect(layer.getSource()).toBe(parcelSource);
    expect(layer.get('name')).toBe(PARCEL_LAYER_NAME);

    const feature = new Feature({
      geometry: new Polygon([[[-46.63, -23.55], [-46.62, -23.55], [-46.62, -23.56], [-46.63, -23.56], [-46.63, -23.55]]]),
    });
    feature.set('status', 'AVAILABLE');

    const styleFn = layer.getStyleFunction();
    expect(styleFn).toBeDefined();

    if (styleFn) {
      const style = styleFn(feature, 1);
      expect(style).toBeDefined();
    }
  });
});
