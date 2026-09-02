import Feature from 'ol/Feature';
import Polygon from 'ol/geom/Polygon';
import VectorLayer from 'ol/layer/Vector';
import VectorSource from 'ol/source/Vector';
import { Style, Fill, Stroke } from 'ol/style';
import { fromGeoJsonPolygonCoordinates } from '../../../shared/geo/projection';
import type { ContactInfo } from '../../../types/api';

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

export interface ParcelFeatureProps {
  id: string;
  title: string;
  status: string;
  totalPrice?: number;
  currency?: string;
  description?: string;
  contact?: ContactInfo;
}

/**
 * Builds an ol.Feature for a parcel, ready to add to parcelSource. Both the
 * "just registered" flow and the "search results" flow need the exact same
 * shape (GeoJSON coordinates converted to map projection, same property
 * names) so the popup works identically regardless of where the parcel came
 * from — this is the single place that construction happens.
 */
export function toParcelFeature(coordinates: number[][][], props: ParcelFeatureProps): Feature {
  const ringsInMapProjection = fromGeoJsonPolygonCoordinates(coordinates);
  const feature = new Feature({ geometry: new Polygon(ringsInMapProjection) });
  feature.setId(props.id);
  feature.set('title', props.title);
  feature.set('status', props.status);
  feature.set('totalPrice', props.totalPrice);
  feature.set('currency', props.currency);
  feature.set('description', props.description);
  feature.set('contact', props.contact);
  return feature;
}

/**
 * Updates the status of an already-rendered feature in place (e.g. after
 * reserving or marking it sold). Calling `.set()` triggers the layer's style
 * function to re-run, so the parcel's colour updates on the map immediately
 * without needing a fresh search.
 */
export function updateFeatureStatus(id: string, status: string): void {
  const feature = parcelSource.getFeatureById(id);
  feature?.set('status', status);
}
