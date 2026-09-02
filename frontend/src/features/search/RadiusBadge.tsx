interface RadiusBadgeProps {
  radiusInMetres: number | null;
}

const MAX_ALLOWED_RADIUS_METRES = 50_000;

/**
 * role="status" + aria-live make a screen reader announce the radius as it
 * changes — accessibility on a map is a detail almost nobody bothers with.
 */
export function RadiusBadge({ radiusInMetres }: RadiusBadgeProps) {
  if (radiusInMetres === null) return null;

  const isExceeded = radiusInMetres > MAX_ALLOWED_RADIUS_METRES;
  const label =
    radiusInMetres >= 1000 ? `${(radiusInMetres / 1000).toFixed(2)} km` : `${radiusInMetres} m`;

  return (
    <div
      className={`radius-badge ${isExceeded ? 'radius-badge--exceeded' : ''}`}
      role="status"
      aria-live="polite"
    >
      Radius: {label} {isExceeded ? '(max 50 km)' : ''}
    </div>
  );
}
