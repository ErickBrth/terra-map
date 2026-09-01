import { useEffect, useRef, useState, type RefObject } from 'react';
import Map from 'ol/Map';
import View from 'ol/View';
import { defaults as defaultControls } from 'ol/control/defaults';
import { fromLonLat } from 'ol/proj';
import { createBaseLayer } from '../layers/baseLayer';

// Sao Paulo, used only as a sensible default starting view.
const INITIAL_CENTER: [number, number] = [-46.633, -23.55];
const INITIAL_ZOOM = 13;

/**
 * Creates the ol.Map instance a single time and tears it down on unmount.
 *
 * The map MUST live in a ref, never in React state: OpenLayers manages its
 * own imperative rendering loop, and re-creating the Map on every render
 * causes flicker, event listener leaks, and (in React 18 StrictMode, which
 * mounts effects twice in development) duplicate maps racing for the same
 * DOM node.
 */
export function useOpenLayersMap(containerRef: RefObject<HTMLDivElement | null>) {
  const mapRef = useRef<Map | null>(null);
  const [isReady, setIsReady] = useState(false);

  useEffect(() => {
    if (!containerRef.current || mapRef.current) return;

    const map = new Map({
      target: containerRef.current,
      layers: [createBaseLayer()],
      view: new View({
        center: fromLonLat(INITIAL_CENTER),
        zoom: INITIAL_ZOOM,
        maxZoom: 19,
      }),
      controls: defaultControls({ attribution: true }),
    });

    mapRef.current = map;
    setIsReady(true);

    return () => {
      map.setTarget(undefined); // detaches DOM listeners
      map.dispose(); // releases canvas/WebGL resources
      mapRef.current = null;
      setIsReady(false);
    };
  }, [containerRef]);

  return { map: mapRef.current, isReady };
}
