import { useState, useCallback, useRef } from 'react';
import Feature from 'ol/Feature';
import Polygon from 'ol/geom/Polygon';
import { searchParcels } from '../../../api/landParcelApi';
import { ApiError } from '../../../api/ApiError';
import { fromGeoJsonPolygonCoordinates } from '../../../shared/geo/projection';
import { parcelSource } from '../../map/layers/parcelLayer';
import type { SearchAreaPayload } from '../../map/hooks/useDrawCircle';

export type SearchParcelsStep = 'idle' | 'drawing' | 'searching';

export function useSearchParcels() {
  const [step, setStep] = useState<SearchParcelsStep>('idle');
  const [radiusInMetres, setRadiusInMetres] = useState<number | null>(null);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [resultCount, setResultCount] = useState<number | null>(null);

  // Guards against a slow earlier search overwriting a faster later one — if
  // the user draws a second circle before the first search resolves, only
  // the most recent request is allowed to update the map.
  const requestTokenRef = useRef(0);

  const startSearching = useCallback(() => {
    setErrorMessage(null);
    setResultCount(null);
    setStep('drawing');
  }, []);

  const cancel = useCallback(() => {
    setRadiusInMetres(null);
    setStep('idle');
  }, []);

  const onCircleDrawn = useCallback(async (area: SearchAreaPayload) => {
    const currentToken = ++requestTokenRef.current;
    setStep('searching');
    setErrorMessage(null);

    try {
      const result = await searchParcels(area);

      if (currentToken !== requestTokenRef.current) return; // a newer search already won

      const features = result.features.map((f) => {
        const ringsInMapProjection = fromGeoJsonPolygonCoordinates(f.geometry.coordinates);
        const feature = new Feature({ geometry: new Polygon(ringsInMapProjection) });
        feature.setId(f.id);
        feature.set('title', f.properties.title);
        feature.set('status', f.properties.status);
        feature.set('totalPrice', f.properties.totalPrice);
        feature.set('currency', f.properties.currency);
        feature.set('contact', f.properties.contact);
        return feature;
      });

      // The spec is explicit: render ONLY the parcels intersecting the circle.
      parcelSource.clear();
      parcelSource.addFeatures(features);
      setResultCount(features.length);
      setRadiusInMetres(null);
      setStep('idle');
    } catch (error) {
      if (currentToken !== requestTokenRef.current) return;
      if (error instanceof ApiError) {
        setErrorMessage(error.userMessage);
      } else if (error instanceof Error) {
        setErrorMessage(error.message);
      } else {
        setErrorMessage('Search failed. Please try drawing the circle again.');
      }
      setRadiusInMetres(null);
      setStep('idle');
    }
  }, []);

  return {
    step,
    radiusInMetres,
    errorMessage,
    resultCount,
    startSearching,
    onRadiusChange: setRadiusInMetres,
    onCircleDrawn,
    cancel,
  };
}
