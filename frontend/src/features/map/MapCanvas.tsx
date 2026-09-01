import { useRef } from 'react';
import { useOpenLayersMap } from './hooks/useOpenLayersMap';

export function MapCanvas() {
  const containerRef = useRef<HTMLDivElement>(null);
  const { isReady } = useOpenLayersMap(containerRef);

  return (
    <div className="map-container" ref={containerRef}>
      {!isReady && <p style={{ position: 'absolute', top: 12, left: 12 }}>Loading map…</p>}
    </div>
  );
}
