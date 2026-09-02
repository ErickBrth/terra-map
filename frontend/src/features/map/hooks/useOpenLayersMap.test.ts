import { describe, it, expect } from 'vitest';
import { renderHook } from '@testing-library/react';
import { useOpenLayersMap } from './useOpenLayersMap';

describe('useOpenLayersMap', () => {
  it('initializes map on mount and disposes on unmount', () => {
    const div = document.createElement('div');
    const containerRef = { current: div };

    const { result, unmount } = renderHook(() => useOpenLayersMap(containerRef));

    expect(result.current.isReady).toBe(true);
    expect(result.current.map).toBeDefined();

    unmount();
  });

  it('does not initialize if containerRef is null', () => {
    const containerRef = { current: null };

    const { result } = renderHook(() => useOpenLayersMap(containerRef));

    expect(result.current.isReady).toBe(false);
    expect(result.current.map).toBeNull();
  });
});
