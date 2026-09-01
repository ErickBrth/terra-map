interface MapToolbarProps {
  isDrawing: boolean;
  onStartDrawing: () => void;
  onCancelDrawing: () => void;
}

/**
 * Toolbar overlaid on the map. In "navigate" mode it shows the "Register a parcel"
 * button; once the user clicks it, the toolbar switches to a hint + cancel button.
 */
export function MapToolbar({ isDrawing, onStartDrawing, onCancelDrawing }: MapToolbarProps) {
  return (
    <div className="map-toolbar">
      {isDrawing ? (
        <>
          <span>Click on the map to draw the parcel boundary.</span>
          <button onClick={onCancelDrawing}>Cancel</button>
        </>
      ) : (
        <button onClick={onStartDrawing}>Register a parcel</button>
      )}
    </div>
  );
}
