import VectorLayer from 'ol/layer/Vector';
import VectorSource from 'ol/source/Vector';
import { Style, Fill, Stroke } from 'ol/style';

export const PARCEL_LAYER_NAME = 'parcels';

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

export const parcelSource = new VectorSource();

export function createParcelLayer(): VectorLayer<VectorSource> {
  const layer = new VectorLayer({
    source: parcelSource,
    zIndex: 10,
    style: (feature) => {
      const status = feature.get('status') as string | undefined;
      return (status && STATUS_STYLES[status]) || DEFAULT_STYLE;
    },
  });
  layer.set('name', PARCEL_LAYER_NAME);
  return layer;
}
