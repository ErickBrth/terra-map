import { RadiusBadge } from '../search/RadiusBadge';

interface MapToolbarProps {
  mode: 'idle' | 'draw-parcel' | 'draw-circle';
  radiusInMetres: number | null;
  resultCount: number | null;
  onStartRegister: () => void;
  onStartSearch: () => void;
  onCancel: () => void;
}

export function MapToolbar({
  mode,
  radiusInMetres,
  resultCount,
  onStartRegister,
  onStartSearch,
  onCancel,
}: MapToolbarProps) {
  if (mode === 'draw-parcel') {
    return (
      <div className="map-toolbar">
        <span>Click on the map to draw the parcel boundary.</span>
        <button onClick={onCancel}>Cancel</button>
      </div>
    );
  }

  if (mode === 'draw-circle') {
    return (
      <div className="map-toolbar">
        <span>Click and drag to draw the search area.</span>
        <RadiusBadge radiusInMetres={radiusInMetres} />
        <button onClick={onCancel}>Cancel</button>
      </div>
    );
  }

  return (
    <div className="map-toolbar">
      <button onClick={onStartRegister}>Register a parcel</button>
      <button onClick={onStartSearch}>Search parcels</button>
      {resultCount !== null && (
        <span className="result-count">
          {resultCount} {resultCount === 1 ? 'parcel' : 'parcels'} found
        </span>
      )}
    </div>
  );
}
