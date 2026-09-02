import { useEffect, useRef, useMemo } from 'react';
import { useOpenLayersMap } from './hooks/useOpenLayersMap';
import { useDrawPolygon } from './hooks/useDrawPolygon';
import { useDrawCircle } from './hooks/useDrawCircle';
import { useFeaturePopup } from './hooks/useFeaturePopup';
import { createParcelLayer } from './layers/parcelLayer';
import { MapToolbar } from './MapToolbar';
import { ParcelForm } from '../parcel/ParcelForm';
import { ParcelPopup } from '../parcel/ParcelPopup';
import { useRegisterParcel } from '../parcel/hooks/useRegisterParcel';
import { useSearchParcels } from '../search/hooks/useSearchParcels';
import { useUpdateParcelStatus } from '../parcel/hooks/useUpdateParcelStatus';

export function MapCanvas() {
  const containerRef = useRef<HTMLDivElement>(null);
  const popupRef = useRef<HTMLDivElement>(null);
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
  // Clicking to open a popup only makes sense while not mid-drawing.
  const { selected, close } = useFeaturePopup(map, popupRef, mode === 'idle');
  const statusUpdate = useUpdateParcelStatus(() => close());

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

      {/* Overlay target must always be mounted (even hidden) so ol.Overlay has
          a stable DOM element to attach to before the first feature is clicked. */}
      <div ref={popupRef}>
        <ParcelPopup
          parcel={selected}
          onClose={close}
          onReserve={statusUpdate.reserve}
          onMarkSold={statusUpdate.markSold}
          busy={statusUpdate.pending}
          actionError={statusUpdate.errorMessage}
        />
      </div>

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
