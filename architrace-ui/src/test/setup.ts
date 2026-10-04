/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmytro Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */

import "@testing-library/jest-dom/vitest";
import { cleanup } from "@testing-library/react";
import { afterEach } from "vitest";

class ResizeObserverStub {
  private readonly callback: ResizeObserverCallback;

  constructor(callback: ResizeObserverCallback) {
    this.callback = callback;
  }

  observe(target: Element): void {
    queueMicrotask(() => {
      const contentRect = target.getBoundingClientRect();
      this.callback([{ target, contentRect } as ResizeObserverEntry], this);
    });
  }

  unobserve(): void {
    return;
  }

  disconnect(): void {
    return;
  }
}

class DOMMatrixReadOnlyStub {
  readonly m22: number;

  constructor(transform?: string) {
    const scale = transform?.match(/scale\(([\d.]+)\)/)?.[1];
    this.m22 = scale === undefined ? 1 : Number(scale);
  }
}

globalThis.ResizeObserver = ResizeObserverStub;
globalThis.DOMMatrixReadOnly = DOMMatrixReadOnlyStub as unknown as typeof DOMMatrixReadOnly;
Object.defineProperties(HTMLElement.prototype, {
  offsetHeight: {
    get(this: HTMLElement) {
      return Number.parseFloat(this.style.height) || 1;
    },
  },
  offsetWidth: {
    get(this: HTMLElement) {
      return Number.parseFloat(this.style.width) || 1;
    },
  },
});
(SVGElement.prototype as SVGGraphicsElement).getBBox = () =>
  ({ x: 0, y: 0, width: 0, height: 0 }) as DOMRect;

afterEach(() => {
  cleanup();
});
