import { describe, it, expect, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { ParcelForm } from './ParcelForm';
import type { GeoJsonPolygon } from '../../types/api';

describe('ParcelForm', () => {
  const mockBoundary: GeoJsonPolygon = {
    type: 'Polygon',
    coordinates: [[[-46.63, -23.55], [-46.62, -23.55], [-46.62, -23.56], [-46.63, -23.56], [-46.63, -23.55]]],
  };

  it('renders all fields and submits valid form data', async () => {
    const user = userEvent.setup();
    const onSubmit = vi.fn();
    const onCancel = vi.fn();

    render(
      <ParcelForm
        boundary={mockBoundary}
        submitting={false}
        errorMessage={null}
        onSubmit={onSubmit}
        onCancel={onCancel}
      />,
    );

    await user.type(screen.getByLabelText(/title/i), 'Prime Urban Lot');
    await user.type(screen.getByLabelText(/description/i), 'Great lot for development');
    await user.type(screen.getByLabelText(/total price/i), '350000');
    await user.type(screen.getByLabelText(/^name/i), 'Carlos Silva');
    await user.type(screen.getByLabelText(/email/i), 'carlos@example.com');
    await user.type(screen.getByLabelText(/phone/i), '+55 11 99999-9999');

    await user.click(screen.getByRole('button', { name: /save parcel/i }));

    expect(onSubmit).toHaveBeenCalledTimes(1);
    expect(onSubmit).toHaveBeenCalledWith({
      title: 'Prime Urban Lot',
      description: 'Great lot for development',
      totalPrice: 350000,
      currency: 'BRL',
      contact: {
        name: 'Carlos Silva',
        email: 'carlos@example.com',
        phone: '+55 11 99999-9999',
      },
      boundary: mockBoundary,
    });
  });

  it('displays error message when provided', () => {
    render(
      <ParcelForm
        boundary={mockBoundary}
        submitting={false}
        errorMessage="This boundary overlaps an existing parcel."
        onSubmit={vi.fn()}
        onCancel={vi.fn()}
      />,
    );

    const errorEl = screen.getByRole('alert');
    expect(errorEl).toHaveTextContent('This boundary overlaps an existing parcel.');
  });

  it('disables buttons and shows Saving text while submitting', () => {
    render(
      <ParcelForm
        boundary={mockBoundary}
        submitting={true}
        errorMessage={null}
        onSubmit={vi.fn()}
        onCancel={vi.fn()}
      />,
    );

    const saveBtn = screen.getByRole('button', { name: /saving/i });
    const cancelBtn = screen.getByRole('button', { name: /cancel/i });

    expect(saveBtn).toBeDisabled();
    expect(cancelBtn).toBeDisabled();
  });
});
