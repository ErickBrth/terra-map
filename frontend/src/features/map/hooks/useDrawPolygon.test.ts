import { describe, it, expect, vi } from 'vitest';
import { renderHook } from '@testing-library/react';
import Map from 'ol/Map';
import View from 'ol/View';
import { useDrawPolygon } from './useDrawPolygon';

describe('useDrawPolygon', () => {
  it('attaches drawing interaction and layer when active and cleans up when inactive', () => {
    const map = new Map({ view: new View() });
    const onComplete = vi.fn();

    const { rerender, unmount } = renderHook(
      ({ active }) => useDrawPolygon(map, active, onComplete),
      { initialProps: { active: true } },
    );

    expect(map.getInteractions().getLength()).toBeGreaterThan(0);

    rerender({ active: false });
    unmount();
  });
});
