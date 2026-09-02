import { describe, it, expect, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MapToolbar } from './MapToolbar';

describe('MapToolbar', () => {
  it('renders register and search buttons in idle mode', async () => {
    const user = userEvent.setup();
    const onStartRegister = vi.fn();
    const onStartSearch = vi.fn();
    const onCancel = vi.fn();

    render(
      <MapToolbar
        mode="idle"
        radiusInMetres={null}
        resultCount={null}
        onStartRegister={onStartRegister}
        onStartSearch={onStartSearch}
        onCancel={onCancel}
      />,
    );

    const registerBtn = screen.getByRole('button', { name: /register a parcel/i });
    const searchBtn = screen.getByRole('button', { name: /search parcels/i });

    expect(registerBtn).toBeInTheDocument();
    expect(searchBtn).toBeInTheDocument();

    await user.click(registerBtn);
    expect(onStartRegister).toHaveBeenCalledTimes(1);

    await user.click(searchBtn);
    expect(onStartSearch).toHaveBeenCalledTimes(1);
  });

  it('displays result count when search results are present', () => {
    render(
      <MapToolbar
        mode="idle"
        radiusInMetres={null}
        resultCount={5}
        onStartRegister={vi.fn()}
        onStartSearch={vi.fn()}
        onCancel={vi.fn()}
      />,
    );

    expect(screen.getByText('5 parcels found')).toBeInTheDocument();
  });

  it('renders draw-parcel mode instructions and cancel button', async () => {
    const user = userEvent.setup();
    const onCancel = vi.fn();

    render(
      <MapToolbar
        mode="draw-parcel"
        radiusInMetres={null}
        resultCount={null}
        onStartRegister={vi.fn()}
        onStartSearch={vi.fn()}
        onCancel={onCancel}
      />,
    );

    expect(screen.getByText(/click on the map to draw/i)).toBeInTheDocument();
    const cancelBtn = screen.getByRole('button', { name: /cancel/i });
    await user.click(cancelBtn);
    expect(onCancel).toHaveBeenCalledTimes(1);
  });

  it('renders draw-circle mode with radius badge and cancel button', async () => {
    const user = userEvent.setup();
    const onCancel = vi.fn();

    render(
      <MapToolbar
        mode="draw-circle"
        radiusInMetres={1200}
        resultCount={null}
        onStartRegister={vi.fn()}
        onStartSearch={vi.fn()}
        onCancel={onCancel}
      />,
    );

    expect(screen.getByText(/click and drag to draw the search area/i)).toBeInTheDocument();
    expect(screen.getByText('Radius: 1.20 km')).toBeInTheDocument();
    const cancelBtn = screen.getByRole('button', { name: /cancel/i });
    await user.click(cancelBtn);
    expect(onCancel).toHaveBeenCalledTimes(1);
  });
});
