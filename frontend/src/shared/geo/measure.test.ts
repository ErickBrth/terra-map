import { describe, it, expect } from 'vitest';
import Circle from 'ol/geom/Circle';
import { fromLonLat } from 'ol/proj';
import { trueRadiusInMetres } from './measure';

describe('measure utils', () => {
  it('computes true ground radius by correcting Web Mercator distortion', () => {
    // Center at Sao Paulo (-23.55 lat), where Mercator stretches distances by ~1.09
    const center = fromLonLat([-46.633, -23.55]);
    const nominalRadius3857 = 1000; // 1000 Web Mercator units
    const circle = new Circle(center, nominalRadius3857);

    const trueRadius = trueRadiusInMetres(circle);

    // Because cos(-23.55 deg) ~ 0.9167, 1000 mercator units should equal ~917 true meters
    expect(trueRadius).toBeLessThan(1000);
    expect(trueRadius).toBeGreaterThan(900);
    expect(trueRadius).toBe(917);
  });

  it('computes approximately 1:1 at the equator (0 lat)', () => {
    const equatorCenter = fromLonLat([0, 0]);
    const circle = new Circle(equatorCenter, 1000);

    const trueRadius = trueRadiusInMetres(circle);

    // At the equator, metersPerUnit is exactly 1
    expect(trueRadius).toBe(1000);
  });
});
