import { useEffect, useRef, useMemo } from 'react';
import { useOpenLayersMap } from './hooks/useOpenLayersMap';
import { useDrawPolygon } from './hooks/useDrawPolygon';
import { useDrawCircle } from './hooks/useDrawCircle';
import { createParcelLayer } from './layers/parcelLayer';
import { MapToolbar } from './MapToolbar';
import { ParcelForm } from '../parcel/ParcelForm';
import { useRegisterParcel } from '../parcel/hooks/useRegisterParcel';
import { useSearchParcels } from '../search/hooks/useSearchParcels';

export function MapCanvas() {
  const containerRef = useRef<HTMLDivElement>(null);
  const { map, isReady } = useOpenLayersMap(containerRef);

  const register = useRegisterParcel();
  const search = useSearchParcels();

  // Only one drawing tool is ever active at a time. Both flows are simple
  // enough (idle/drawing/...) that deriving a single "mode" from the two
  // hooks is clearer than introducing a third piece of shared state.
  const mode = useMemo<'idle' | 'draw-parcel' | 'draw-circle'>(() => {
    if (register.step === 'drawing') return 'draw-parcel';
    if (search.step === 'drawing') return 'draw-circle';
    return 'idle';
  }, [register.step, search.step]);

  useEffect(() => {
    if (!map) return;
    const layer = createParcelLayer();
    map.addLayer(layer);
    return () => {
      map.removeLayer(layer);
    };
  }, [map]);

  useDrawPolygon(map, mode === 'draw-parcel', register.onPolygonDrawn);
  useDrawCircle(map, mode === 'draw-circle', search.onRadiusChange, search.onCircleDrawn);

  function handleStartRegister() {
    register.startDrawing();
  }

  function handleStartSearch() {
    search.startSearching();
  }

  function handleCancel() {
    if (mode === 'draw-parcel') register.cancel();
    if (mode === 'draw-circle') search.cancel();
  }

  return (
    <div className="map-container" ref={containerRef}>
      {!isReady && <p className="map-loading">Loading map...</p>}

      <MapToolbar
        mode={mode}
        radiusInMetres={search.radiusInMetres}
        resultCount={search.resultCount}
        onStartRegister={handleStartRegister}
        onStartSearch={handleStartSearch}
        onCancel={handleCancel}
      />

      {search.errorMessage && (
        <div className="search-error" role="alert">
          {search.errorMessage}
        </div>
      )}

      {(register.step === 'form-open' || register.step === 'submitting') && register.pendingBoundary && (
        <div className="parcel-form-overlay">
          <ParcelForm
            boundary={register.pendingBoundary}
            submitting={register.step === 'submitting'}
            errorMessage={register.errorMessage}
            onSubmit={register.submit}
            onCancel={register.cancel}
          />
        </div>
      )}
    </div>
  );
}
