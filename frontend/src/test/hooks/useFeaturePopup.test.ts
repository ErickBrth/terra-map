import { describe, it, expect, vi, beforeEach } from 'vitest';
import { renderHook, act } from '@testing-library/react';
import Map from 'ol/Map';
import View from 'ol/View';
import Feature from 'ol/Feature';
import Polygon from 'ol/geom/Polygon';
import { fromLonLat } from 'ol/proj';
import { useFeaturePopup } from '../../features/map/hooks/useFeaturePopup';

// ── Overlay mock ────────────────────────────────────────────────────────────
// ol/Overlay tries to call getSize() / getBoundingClientRect() on DOM elements
// which jsdom cannot fulfil (no real layout engine). We replace it with a
// lightweight stub that tracks setPosition calls without touching the DOM.
vi.mock('ol/Overlay', () => {
  const noop = () => {};
  return {
    default: vi.fn().mockImplementation(() => ({
      // Required by Map.addOverlay/removeOverlay internals
      getId: vi.fn(() => undefined),
      getElement: vi.fn(() => document.createElement('div')),
      getMap: vi.fn(() => null),
      setMap: vi.fn(),
      // Required by OL's observable/event infrastructure
      changed: vi.fn(),
      dispatchEvent: vi.fn(),
      on: vi.fn(() => ({ type: 'change', listener: noop })),
      un: vi.fn(),
      once: vi.fn(),
      // The methods the hook actually uses
      setPosition: vi.fn(),
      getPosition: vi.fn(() => undefined),
      setOffset: vi.fn(),
      setElement: vi.fn(),
    })),
  };
});

// ── Helpers ─────────────────────────────────────────────────────────────────

/** Creates a minimal OL Map suitable for JSDOM (no canvas rendering). */
function makeMap() {
  const container = document.createElement('div');
  document.body.appendChild(container);
  return new Map({ target: container, view: new View({ center: [0, 0], zoom: 10 }) });
}

/** Creates a test Feature that mimics a saved parcel. */
function makeParcelFeature() {
  const ring = [
    fromLonLat([-46.63, -23.55]),
    fromLonLat([-46.62, -23.55]),
    fromLonLat([-46.62, -23.56]),
    fromLonLat([-46.63, -23.56]),
    fromLonLat([-46.63, -23.55]),
  ];
  const feature = new Feature({ geometry: new Polygon([ring]) });
  feature.setId('parcel-uuid-1');
  feature.set('title', 'Riverside lot');
  feature.set('status', 'AVAILABLE');
  feature.set('totalPrice', 200000);
  feature.set('currency', 'BRL');
  return feature;
}

// ── Tests ────────────────────────────────────────────────────────────────────

describe('useFeaturePopup', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('initializes with no selection and exposes a close function', () => {
    const map = makeMap();
    const popupEl = document.createElement('div');
    const popupRef = { current: popupEl };

    const { result, unmount } = renderHook(() => useFeaturePopup(map, popupRef, true));

    expect(result.current.selected).toBeNull();
    expect(typeof result.current.close).toBe('function');

    unmount();
  });

  it('resets selection when active transitions from true to false', () => {
    const map = makeMap();
    const popupEl = document.createElement('div');
    const popupRef = { current: popupEl };

    const { result, rerender } = renderHook(
      ({ active }) => useFeaturePopup(map, popupRef, active),
      { initialProps: { active: true } },
    );

    rerender({ active: false });
    expect(result.current.selected).toBeNull();
  });

  it('sets selected parcel when singleclick hits a feature', () => {
    const map = makeMap();
    const popupEl = document.createElement('div');
    const popupRef = { current: popupEl };
    const feature = makeParcelFeature();

    vi.spyOn(map, 'forEachFeatureAtPixel').mockImplementation((_, callback) => {
      return callback(feature, {} as never, {} as never);
    });

    const { result, unmount } = renderHook(() => useFeaturePopup(map, popupRef, true));

    act(() => {
      map.dispatchEvent({ type: 'singleclick', pixel: [0, 0], coordinate: [0, 0] } as never);
    });

    expect(result.current.selected).not.toBeNull();
    expect(result.current.selected?.id).toBe('parcel-uuid-1');
    expect(result.current.selected?.title).toBe('Riverside lot');
    expect(result.current.selected?.status).toBe('AVAILABLE');

    unmount();
  });

  it('clears selection when singleclick lands on empty map area', () => {
    const map = makeMap();
    const popupEl = document.createElement('div');
    const popupRef = { current: popupEl };
    const feature = makeParcelFeature();

    // First click: select
    const forEachSpy = vi.spyOn(map, 'forEachFeatureAtPixel').mockImplementation((_, callback) => {
      return callback(feature, {} as never, {} as never);
    });

    const { result, unmount } = renderHook(() => useFeaturePopup(map, popupRef, true));

    act(() => {
      map.dispatchEvent({ type: 'singleclick', pixel: [0, 0], coordinate: [0, 0] } as never);
    });
    expect(result.current.selected).not.toBeNull();

    // Second click: empty area
    forEachSpy.mockImplementation(() => undefined);
    act(() => {
      map.dispatchEvent({ type: 'singleclick', pixel: [100, 100], coordinate: [1, 1] } as never);
    });
    expect(result.current.selected).toBeNull();

    unmount();
  });

  it('close() resets selection to null', () => {
    const map = makeMap();
    const popupEl = document.createElement('div');
    const popupRef = { current: popupEl };
    const feature = makeParcelFeature();

    vi.spyOn(map, 'forEachFeatureAtPixel').mockImplementation((_, callback) => {
      return callback(feature, {} as never, {} as never);
    });

    const { result, unmount } = renderHook(() => useFeaturePopup(map, popupRef, true));

    act(() => {
      map.dispatchEvent({ type: 'singleclick', pixel: [0, 0], coordinate: [0, 0] } as never);
    });
    expect(result.current.selected).not.toBeNull();

    act(() => {
      result.current.close();
    });
    expect(result.current.selected).toBeNull();

    unmount();
  });

  it('does not throw and returns null selection when map is null', () => {
    const popupEl = document.createElement('div');
    const popupRef = { current: popupEl };

    const { result, unmount } = renderHook(() => useFeaturePopup(null, popupRef, true));

    expect(result.current.selected).toBeNull();
    unmount();
  });
});
