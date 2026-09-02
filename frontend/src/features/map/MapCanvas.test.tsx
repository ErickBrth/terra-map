import { describe, it, expect, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MapCanvas } from './MapCanvas';

describe('MapCanvas', () => {
  it('renders map container, toolbar and buttons', async () => {
    const user = userEvent.setup();
    render(<MapCanvas />);

    const registerBtn = screen.getByRole('button', { name: /register a parcel/i });
    const searchBtn = screen.getByRole('button', { name: /search parcels/i });

    expect(registerBtn).toBeInTheDocument();
    expect(searchBtn).toBeInTheDocument();

    await user.click(registerBtn);
    expect(screen.getByText(/click on the map to draw the parcel boundary/i)).toBeInTheDocument();

    const cancelBtn = screen.getByRole('button', { name: /cancel/i });
    await user.click(cancelBtn);
    expect(screen.getByRole('button', { name: /register a parcel/i })).toBeInTheDocument();
  });
});
