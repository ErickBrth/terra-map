import '@testing-library/jest-dom/vitest';

// jsdom has no canvas and reports zero-sized elements; OpenLayers needs both
// to construct a Map without throwing.
Object.defineProperty(HTMLElement.prototype, 'clientWidth', { value: 800, configurable: true });
Object.defineProperty(HTMLElement.prototype, 'clientHeight', { value: 600, configurable: true });

global.ResizeObserver = class {
  observe() {}
  unobserve() {}
  disconnect() {}
};

HTMLCanvasElement.prototype.getContext = () => null;
