import { useEffect, useRef } from 'react';
import type Map from 'ol/Map';
import type { EventsKey } from 'ol/events';
import { unByKey } from 'ol/Observable';
import VectorSource from 'ol/source/Vector';
import VectorLayer from 'ol/layer/Vector';
import Draw from 'ol/interaction/Draw';
import type Circle from 'ol/geom/Circle';
import { toLonLat } from 'ol/proj';
import { Style, Stroke, Fill } from 'ol/style';
import { trueRadiusInMetres } from '../../../shared/geo/measure';

const searchAreaStyle = new Style({
  fill: new Fill({ color: 'rgba(163, 42, 219, 0.10)' }),
  stroke: new Stroke({ color: '#a32adb', width: 2 }),
});

export interface SearchAreaPayload {
  center: { type: 'Point'; coordinates: [number, number] };
  radiusInMeters: number;
}

/**
 * Circle-drawing interaction for the "search parcels" flow. Mirrors geojson.io's
 * behaviour: click to set the center, drag to set the radius, with the radius
 * shown live (in meters) via onRadiusChange while dragging.
 */
export function useDrawCircle(
  map: Map | null,
  active: boolean,
  onRadiusChange: (metres: number | null) => void,
  onComplete: (area: SearchAreaPayload) => void,
) {
  const onRadiusChangeRef = useRef(onRadiusChange);
  onRadiusChangeRef.current = onRadiusChange;
  const onCompleteRef = useRef(onComplete);
  onCompleteRef.current = onComplete;

  useEffect(() => {
    if (!map || !active) return;

    const source = new VectorSource();
    const layer = new VectorLayer({ source, style: searchAreaStyle, zIndex: 15 });
    const draw = new Draw({ source, type: 'Circle', style: searchAreaStyle });

    let geometryListener: EventsKey | undefined;

    draw.on('drawstart', (event) => {
      source.clear(); // only one search area sketch at a time
      const geometry = event.feature.getGeometry() as Circle;
      // Fires on every mouse-move while dragging -> the "live" part of the live radius.
      geometryListener = geometry.on('change', () => {
        onRadiusChangeRef.current(trueRadiusInMetres(geometry));
      });
    });

    draw.on('drawend', (event) => {
      if (geometryListener) unByKey(geometryListener);
      const geometry = event.feature.getGeometry() as Circle;
      const [lon, lat] = toLonLat(geometry.getCenter());
      onCompleteRef.current({
        center: { type: 'Point', coordinates: [lon, lat] },
        radiusInMeters: trueRadiusInMetres(geometry),
      });
      source.clear();
    });

    map.addLayer(layer);
    map.addInteraction(draw);

    return () => {
      if (geometryListener) unByKey(geometryListener);
      map.removeInteraction(draw);
      map.removeLayer(layer);
      source.clear();
      onRadiusChangeRef.current(null);
    };
  }, [map, active]);
}
