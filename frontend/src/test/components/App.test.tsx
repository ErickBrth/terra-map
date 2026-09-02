import { describe, it, expect } from 'vitest';
import { render } from '@testing-library/react';
import App from '../../App';

describe('App', () => {
  it('renders app layout container with MapCanvas', () => {
    const { container } = render(<App />);
    expect(container.querySelector('.app-layout')).toBeInTheDocument();
    expect(container.querySelector('.map-container')).toBeInTheDocument();
  });
});
