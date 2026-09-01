import { useState, type FormEvent } from 'react';
import type { GeoJsonPolygon, RegisterParcelRequest } from '../../types/api';

interface ParcelFormProps {
  boundary: GeoJsonPolygon;
  submitting: boolean;
  errorMessage: string | null;
  onSubmit: (payload: RegisterParcelRequest) => void;
  onCancel: () => void;
}

/**
 * Controlled form for the "register parcel" flow. Appears after the user
 * finishes drawing a polygon; cancelling discards the drawn boundary.
 */
export function ParcelForm({ boundary, submitting, errorMessage, onSubmit, onCancel }: ParcelFormProps) {
  const [title, setTitle] = useState('');
  const [description, setDescription] = useState('');
  const [totalPrice, setTotalPrice] = useState('');
  const [contactName, setContactName] = useState('');
  const [contactEmail, setContactEmail] = useState('');
  const [contactPhone, setContactPhone] = useState('');

  function handleSubmit(event: FormEvent) {
    event.preventDefault();
    onSubmit({
      title,
      description: description || undefined,
      totalPrice: Number(totalPrice),
      currency: 'BRL',
      contact: {
        name: contactName,
        email: contactEmail,
        phone: contactPhone || undefined,
      },
      boundary,
    });
  }

  return (
    <form className="parcel-form" onSubmit={handleSubmit}>
      <h2>New land parcel</h2>

      <label>
        Title
        <input value={title} onChange={(e) => setTitle(e.target.value)} required maxLength={120} />
      </label>

      <label>
        Description
        <textarea value={description} onChange={(e) => setDescription(e.target.value)} maxLength={2000} />
      </label>

      <label>
        Total price (BRL)
        <input
          type="number"
          min="0.01"
          step="0.01"
          value={totalPrice}
          onChange={(e) => setTotalPrice(e.target.value)}
          required
        />
      </label>

      <fieldset>
        <legend>Contact</legend>
        <label>
          Name
          <input value={contactName} onChange={(e) => setContactName(e.target.value)} required />
        </label>
        <label>
          Email
          <input type="email" value={contactEmail} onChange={(e) => setContactEmail(e.target.value)} required />
        </label>
        <label>
          Phone (optional)
          <input value={contactPhone} onChange={(e) => setContactPhone(e.target.value)} />
        </label>
      </fieldset>

      {errorMessage && <p className="form-error" role="alert">{errorMessage}</p>}

      <div className="form-actions">
        <button type="button" onClick={onCancel} disabled={submitting}>
          Cancel
        </button>
        <button type="submit" disabled={submitting}>
          {submitting ? 'Saving...' : 'Save parcel'}
        </button>
      </div>
    </form>
  );
}
