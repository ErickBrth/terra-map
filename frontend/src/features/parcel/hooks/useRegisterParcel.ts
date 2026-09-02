import { useState, useCallback } from 'react';
import Feature from 'ol/Feature';
import Polygon from 'ol/geom/Polygon';
import { registerParcel } from '../../../api/landParcelApi';
import { ApiError } from '../../../api/ApiError';
import { fromGeoJsonPolygonCoordinates } from '../../../shared/geo/projection';
import { parcelSource } from '../../map/layers/parcelLayer';
import type { GeoJsonPolygon, RegisterParcelRequest } from '../../../types/api';

export type RegisterParcelStep = 'idle' | 'drawing' | 'form-open' | 'submitting';

export function useRegisterParcel() {
  const [step, setStep] = useState<RegisterParcelStep>('idle');
  const [pendingBoundary, setPendingBoundary] = useState<GeoJsonPolygon | null>(null);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  const startDrawing = useCallback(() => {
    setErrorMessage(null);
    setStep('drawing');
  }, []);

  const onPolygonDrawn = useCallback((boundary: GeoJsonPolygon) => {
    setPendingBoundary(boundary);
    setStep('form-open');
  }, []);

  const cancel = useCallback(() => {
    setPendingBoundary(null);
    setErrorMessage(null);
    setStep('idle');
  }, []);

  const submit = useCallback(async (payload: RegisterParcelRequest) => {
    setStep('submitting');
    setErrorMessage(null);

    try {
      const created = await registerParcel(payload);

      // Render the newly created parcel immediately, without waiting for a
      // fresh search - it's the parcel the user just drew, we already have it.
      const ringsInMapProjection = fromGeoJsonPolygonCoordinates(created.boundary.coordinates);
      const feature = new Feature({ geometry: new Polygon(ringsInMapProjection) });
      feature.setId(created.id);
      feature.set('title', created.title);
      feature.set('status', created.status);
      feature.set('totalPrice', created.totalPrice);
      feature.set('currency', created.currency);
      feature.set('description', created.description);
      feature.set('contact', created.contact);
      parcelSource.addFeature(feature);

      setPendingBoundary(null);
      setStep('idle');
    } catch (error) {
      if (error instanceof ApiError) {
        if (error.status === 409) {
          setErrorMessage(error.problem.detail ?? 'This boundary overlaps an existing parcel. Draw a different area.');
        } else if (error.status === 422) {
          setErrorMessage(error.problem.detail ?? 'Invalid boundary geometry.');
        } else {
          setErrorMessage(error.userMessage);
        }
      } else if (error instanceof Error) {
        setErrorMessage(error.message);
      } else {
        setErrorMessage('Something went wrong saving the parcel. Please try again.');
      }
      setStep('form-open'); // let the user retry without redrawing the polygon
    }
  }, []);

  return { step, pendingBoundary, errorMessage, startDrawing, onPolygonDrawn, cancel, submit };
}
