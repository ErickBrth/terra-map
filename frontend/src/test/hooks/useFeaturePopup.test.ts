import { describe, it, expect } from 'vitest';
import { renderHook } from '@testing-library/react';
import Map from 'ol/Map';
import View from 'ol/View';
import { useFeaturePopup } from '../../features/map/hooks/useFeaturePopup';

describe('useFeaturePopup', () => {
  it('initializes overlay when map and popupRef are provided and active is true', () => {
    const map = new Map({ view: new View() });
    const popupEl = document.createElement('div');
    const popupRef = { current: popupEl };

    const { result, unmount } = renderHook(() => useFeaturePopup(map, popupRef, true));

    expect(result.current.selected).toBeNull();
    expect(typeof result.current.close).toBe('function');

    unmount();
  });

  it('resets selection when active becomes false', () => {
    const map = new Map({ view: new View() });
    const popupEl = document.createElement('div');
    const popupRef = { current: popupEl };

    const { result, rerender } = renderHook(
      ({ active }) => useFeaturePopup(map, popupRef, active),
      { initialProps: { active: true } },
    );

    rerender({ active: false });
    expect(result.current.selected).toBeNull();
  });
});
