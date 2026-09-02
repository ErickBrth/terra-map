import { describe, it, expect, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { ParcelPopup } from '../../features/parcel/ParcelPopup';
import type { ParcelProperties } from '../../features/map/hooks/useFeaturePopup';

describe('ParcelPopup', () => {
  const sampleParcel: ParcelProperties = {
    title: 'Sunny Meadows',
    totalPrice: 180000,
    currency: 'BRL',
    status: 'AVAILABLE',
    description: 'Beautiful land with mountain views',
    contact: {
      name: 'Maria Santos',
      email: 'maria@example.com',
      phone: '+55 11 98888-7777',
    },
  };

  it('renders nothing visible when parcel is null', () => {
    const { container } = render(<ParcelPopup parcel={null} onClose={vi.fn()} />);
    const popup = container.querySelector('.parcel-popup');
    expect(popup).toHaveAttribute('hidden');
  });

  it('renders parcel details, formatted price, status, description, and contact info', async () => {
    const user = userEvent.setup();
    const onClose = vi.fn();

    render(<ParcelPopup parcel={sampleParcel} onClose={onClose} />);

    expect(screen.getByText('Sunny Meadows')).toBeInTheDocument();
    expect(screen.getByText(/180,000/)).toBeInTheDocument();
    expect(screen.getByText('AVAILABLE')).toBeInTheDocument();
    expect(screen.getByText('Beautiful land with mountain views')).toBeInTheDocument();
    expect(screen.getByText('Maria Santos')).toBeInTheDocument();
    expect(screen.getByText('maria@example.com')).toBeInTheDocument();
    expect(screen.getByText('+55 11 98888-7777')).toBeInTheDocument();

    const closeBtn = screen.getByRole('button', { name: /close/i });
    await user.click(closeBtn);
    expect(onClose).toHaveBeenCalledTimes(1);
  });
});
