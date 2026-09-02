import { useEffect, useRef } from 'react';
import type Map from 'ol/Map';
import VectorSource from 'ol/source/Vector';
import VectorLayer from 'ol/layer/Vector';
import Draw from 'ol/interaction/Draw';
import Snap from 'ol/interaction/Snap';
import type Polygon from 'ol/geom/Polygon';
import { Style, Fill, Stroke } from 'ol/style';
import { toGeoJsonPolygon } from '../../../shared/geo/projection';
import { parcelSource } from '../layers/parcelLayer';
import type { GeoJsonPolygon } from '../../../types/api';

const sketchStyle = new Style({
  fill: new Fill({ color: 'rgba(31, 111, 235, 0.15)' }),
  stroke: new Stroke({ color: '#1f6feb', width: 2, lineDash: [6, 6] }),
});

/**
 * Draw-a-polygon interaction for the "register a parcel" flow.
 * Active only while `active` is true; the sketch layer and the interactions
 * are created and torn down together so nothing leaks between toggles.
 */
export function useDrawPolygon(
  map: Map | null,
  active: boolean,
  onComplete: (boundary: GeoJsonPolygon) => void,
) {
  // Keeps the callback fresh without re-creating the interaction on every render.
  const onCompleteRef = useRef(onComplete);
  onCompleteRef.current = onComplete;

  useEffect(() => {
    if (!map || !active) return;

    const source = new VectorSource();
    const layer = new VectorLayer({ source, style: sketchStyle, zIndex: 20 });
    const draw = new Draw({ source, type: 'Polygon', style: sketchStyle });
    // Snaps new vertices onto existing parcel edges/corners, so a neighbour
    // boundary lines up exactly instead of overlapping by a few centimetres —
    // which the database would otherwise reject even though adjacency is allowed.
    const snap = new Snap({ source: parcelSource });

    draw.on('drawend', (event) => {
      const geometry = event.feature.getGeometry() as Polygon;
      onCompleteRef.current(toGeoJsonPolygon(geometry));
      source.clear(); // the parcel is now owned by the form/API flow, not the sketch layer
    });

    map.addLayer(layer);
    map.addInteraction(draw);
    // Snap must be added after Draw so it takes priority when both could react to a pointer event.
    map.addInteraction(snap);

    return () => {
      map.removeInteraction(snap);
      map.removeInteraction(draw);
      map.removeLayer(layer);
      source.clear();
    };
  }, [map, active]);
}
