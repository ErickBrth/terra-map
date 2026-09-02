import { useEffect, useState, type RefObject } from 'react';
import type Map from 'ol/Map';
import type MapBrowserEvent from 'ol/MapBrowserEvent';
import Overlay from 'ol/Overlay';
import { PARCEL_LAYER_NAME } from '../layers/parcelLayer';

export interface ParcelProperties {
  id: string;
  title: string;
  status: string;
  totalPrice?: number;
  currency?: string;
  description?: string;
  contact?: { name: string; email: string; phone?: string };
}

/**
 * Shows a popup anchored to whichever parcel the user clicks. Only listens
 * on the parcel layer (via layerFilter) so clicking the base map or the
 * in-progress sketch layer doesn't trigger it.
 */
export function useFeaturePopup(
  map: Map | null,
  popupRef: RefObject<HTMLDivElement | null>,
  active: boolean,
) {
  const [selected, setSelected] = useState<ParcelProperties | null>(null);

  useEffect(() => {
    if (!map || !popupRef.current || !active) {
      setSelected(null);
      return;
    }

    const overlay = new Overlay({
      element: popupRef.current,
      autoPan: { animation: { duration: 200 } },
      positioning: 'bottom-center',
      offset: [0, -12],
    });
    map.addOverlay(overlay);

    const onClick = (event: MapBrowserEvent) => {
      const feature = map.forEachFeatureAtPixel(event.pixel, (f) => f, {
        layerFilter: (layer) => layer.get('name') === PARCEL_LAYER_NAME,
        hitTolerance: 4, // matters on touch screens: hitting a thin polygon border is hard otherwise
      });

      if (!feature) {
        setSelected(null);
        overlay.setPosition(undefined);
        return;
      }

      setSelected({ ...feature.getProperties(), id: String(feature.getId()) } as ParcelProperties);
      overlay.setPosition(event.coordinate);
    };

    const onPointerMove = (event: MapBrowserEvent) => {
      const target = map.getTargetElement();
      if (target) {
        target.style.cursor = map.hasFeatureAtPixel(event.pixel, {
          layerFilter: (layer) => layer.get('name') === PARCEL_LAYER_NAME,
        })
          ? 'pointer'
          : '';
      }
    };

    map.on('singleclick', onClick);
    map.on('pointermove', onPointerMove);

    return () => {
      map.un('singleclick', onClick);
      map.un('pointermove', onPointerMove);
      map.removeOverlay(overlay);
    };
  }, [map, popupRef, active]);

  return { selected, close: () => setSelected(null) };
}
