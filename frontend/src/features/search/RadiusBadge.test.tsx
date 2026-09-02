import { describe, it, expect } from 'vitest';
import { render, screen } from '@testing-library/react';
import { RadiusBadge } from './RadiusBadge';

describe('RadiusBadge', () => {
  it('renders nothing when radius is null', () => {
    const { container } = render(<RadiusBadge radiusInMetres={null} />);
    expect(container.firstChild).toBeNull();
  });

  it('formats meters when radius is under 1000m', () => {
    render(<RadiusBadge radiusInMetres={450} />);
    expect(screen.getByText('Radius: 450 m')).toBeInTheDocument();
  });

  it('formats kilometers when radius is 1000m or above', () => {
    render(<RadiusBadge radiusInMetres={2500} />);
    expect(screen.getByText('Radius: 2.50 km')).toBeInTheDocument();
  });

  it('indicates limit warning when radius exceeds 50km', () => {
    render(<RadiusBadge radiusInMetres={52000} />);
    const badge = screen.getByRole('status');
    expect(badge).toHaveClass('radius-badge--exceeded');
    expect(badge).toHaveTextContent('Radius: 52.00 km (max 50 km)');
  });
});
