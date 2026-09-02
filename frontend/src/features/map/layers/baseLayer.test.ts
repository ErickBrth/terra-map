import { describe, it, expect } from 'vitest';
import { createBaseLayer } from './baseLayer';

describe('baseLayer', () => {
  it('creates an OSM TileLayer properly', () => {
    const layer = createBaseLayer();
    expect(layer).toBeDefined();
    expect(layer.getSource()).toBeDefined();
  });
});
