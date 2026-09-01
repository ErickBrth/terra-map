import VectorLayer from 'ol/layer/Vector';
import VectorSource from 'ol/source/Vector';
import { Style, Fill, Stroke } from 'ol/style';

const STATUS_STYLES: Record<string, Style> = {
  AVAILABLE: new Style({
    fill: new Fill({ color: 'rgba(46, 160, 67, 0.25)' }),
    stroke: new Stroke({ color: '#2ea043', width: 2 }),
  }),
  RESERVED: new Style({
    fill: new Fill({ color: 'rgba(219, 171, 9, 0.25)' }),
    stroke: new Stroke({ color: '#dbab09', width: 2 }),
  }),
  SOLD: new Style({
    fill: new Fill({ color: 'rgba(139, 148, 158, 0.25)' }),
    stroke: new Stroke({ color: '#8b949e', width: 2 }),
  }),
};

const DEFAULT_STYLE = STATUS_STYLES.AVAILABLE;

/**
 * Shared vector source for all registered parcels.
 * Lives at module level so that useRegisterParcel can add a newly saved parcel
 * directly to the map without a full search round-trip.
 *
 * Design note: because this source is module-level (a singleton), two MapCanvas
 * instances (which should never exist at the same time in this app) would share
 * the same source. That is intentional for performance, but worth knowing.
 */
export const parcelSource = new VectorSource();

export function createParcelLayer(): VectorLayer<VectorSource> {
  return new VectorLayer({
    source: parcelSource,
    zIndex: 10,
    style: (feature) => {
      const status = feature.get('status') as string | undefined;
      return (status && STATUS_STYLES[status]) || DEFAULT_STYLE;
    },
  });
}
