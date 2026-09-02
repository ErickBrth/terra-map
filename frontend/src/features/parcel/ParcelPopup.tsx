import type { ParcelProperties } from '../map/hooks/useFeaturePopup';

interface ParcelPopupProps {
  parcel: ParcelProperties | null;
  onClose: () => void;
  onReserve: (id: string) => void;
  onMarkSold: (id: string) => void;
  busy: boolean;
  actionError: string | null;
}

function formatPrice(totalPrice?: number, currency?: string): string | null {
  if (totalPrice === undefined || !currency) return null;
  try {
    return new Intl.NumberFormat('en-US', { style: 'currency', currency }).format(totalPrice);
  } catch {
    return `${currency} ${totalPrice.toFixed(2)}`;
  }
}

/**
 * Rendered by React inside the Overlay element, so all text here is
 * automatically escaped — never use innerHTML/dangerouslySetInnerHTML on
 * this content. The description comes from an anonymous user and is the
 * most likely XSS vector in the whole project.
 */
export function ParcelPopup({ parcel, onClose, onReserve, onMarkSold, busy, actionError }: ParcelPopupProps) {
  if (!parcel) return <div className="parcel-popup" hidden />;

  const price = formatPrice(parcel.totalPrice, parcel.currency);

  return (
    <div className="parcel-popup">
      <div className="parcel-popup-arrow" />
      <button className="parcel-popup-close" onClick={onClose} aria-label="Close">
        ×
      </button>
      <h3>{parcel.title}</h3>
      {price && <p className="parcel-popup-price">{price}</p>}
      <span className={`parcel-popup-status status-${parcel.status.toLowerCase()}`}>
        {parcel.status}
      </span>
      {parcel.description && <p className="parcel-popup-description">{parcel.description}</p>}
      {parcel.contact && (
        <div className="parcel-popup-contact">
          <strong>{parcel.contact.name}</strong>
          <span>{parcel.contact.email}</span>
          {parcel.contact.phone && <span>{parcel.contact.phone}</span>}
        </div>
      )}

      {actionError && <p className="form-error" role="alert">{actionError}</p>}

      <div className="parcel-popup-actions">
        {parcel.status === 'AVAILABLE' && (
          <button disabled={busy} onClick={() => onReserve(parcel.id)}>
            Reserve
          </button>
        )}
        {parcel.status !== 'SOLD' && (
          <button disabled={busy} onClick={() => onMarkSold(parcel.id)}>
            Mark as sold
          </button>
        )}
      </div>
    </div>
  );
}
