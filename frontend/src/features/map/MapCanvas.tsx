import { useEffect, useRef } from 'react';
import { useOpenLayersMap } from './hooks/useOpenLayersMap';
import { useDrawPolygon } from './hooks/useDrawPolygon';
import { createParcelLayer } from './layers/parcelLayer';
import { MapToolbar } from './MapToolbar';
import { ParcelForm } from '../parcel/ParcelForm';
import { useRegisterParcel } from '../parcel/hooks/useRegisterParcel';

export function MapCanvas() {
  const containerRef = useRef<HTMLDivElement>(null);
  const { map, isReady } = useOpenLayersMap(containerRef);

  const { step, pendingBoundary, errorMessage, startDrawing, onPolygonDrawn, cancel, submit } =
    useRegisterParcel();

  // Registered parcels are always visible; only the *drawing* interaction
  // toggles on/off with the toolbar.
  useEffect(() => {
    if (!map) return;
    const layer = createParcelLayer();
    map.addLayer(layer);
    return () => {
      map.removeLayer(layer);
    };
  }, [map]);

  useDrawPolygon(map, step === 'drawing', onPolygonDrawn);

  return (
    <div className="map-container" ref={containerRef}>
      {!isReady && <p className="map-loading">Loading map...</p>}

      <MapToolbar
        isDrawing={step === 'drawing'}
        onStartDrawing={startDrawing}
        onCancelDrawing={cancel}
      />

      {step === 'form-open' && pendingBoundary && (
        <div className="parcel-form-overlay">
          <ParcelForm
            boundary={pendingBoundary}
            submitting={false}
            errorMessage={errorMessage}
            onSubmit={submit}
            onCancel={cancel}
          />
        </div>
      )}

      {step === 'submitting' && pendingBoundary && (
        <div className="parcel-form-overlay">
          <ParcelForm
            boundary={pendingBoundary}
            submitting={true}
            errorMessage={null}
            onSubmit={submit}
            onCancel={cancel}
          />
        </div>
      )}
    </div>
  );
}
