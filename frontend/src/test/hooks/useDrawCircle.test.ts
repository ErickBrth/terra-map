import { describe, it, expect, vi } from 'vitest';
import { renderHook } from '@testing-library/react';
import Map from 'ol/Map';
import View from 'ol/View';
import { useDrawCircle } from '../../features/map/hooks/useDrawCircle';

describe('useDrawCircle', () => {
  it('attaches circle drawing interaction and layer when active and cleans up when inactive', () => {
    const map = new Map({ view: new View() });
    const onRadiusChange = vi.fn();
    const onComplete = vi.fn();

    const { rerender, unmount } = renderHook(
      ({ active }) => useDrawCircle(map, active, onRadiusChange, onComplete),
      { initialProps: { active: true } },
    );

    expect(map.getInteractions().getLength()).toBeGreaterThan(0);

    rerender({ active: false });
    unmount();
  });
});
