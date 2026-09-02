import { getPointResolution } from 'ol/proj';
import type Circle from 'ol/geom/Circle';

/**
 * ol's Circle#getRadius() returns Web Mercator *projected* units, which are
 * inflated by 1/cos(latitude) relative to true meters on the ground. At
 * -23.5° (São Paulo) the error is already ~9%: a "1000 m" circle on screen
 * would really measure ~918 m. getPointResolution converts projected units
 * to true meters at the circle's specific location.
 *
 * Without this correction, the circle drawn on screen would not match the
 * radius sent to the API, and the search results would look wrong to anyone
 * comparing the visual circle to what came back.
 */
export function trueRadiusInMetres(circle: Circle): number {
  const center = circle.getCenter();
  const metresPerUnit = getPointResolution('EPSG:3857', 1, center);
  return Math.round(circle.getRadius() * metresPerUnit);
}
