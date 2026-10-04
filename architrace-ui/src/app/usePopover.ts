/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmytro Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */

import { type RefObject, useCallback, useEffect, useId, useState } from "react";

export interface Popover {
  readonly open: boolean;
  readonly id: string;
  readonly toggle: () => void;
  readonly close: () => void;
}

export function usePopover(container: RefObject<HTMLElement | null>): Popover {
  const [open, setOpen] = useState(false);
  const id = useId();

  useEffect(() => {
    if (!open) {
      return undefined;
    }
    const close = (event: MouseEvent | KeyboardEvent) => {
      if (
        event instanceof KeyboardEvent
          ? event.key === "Escape"
          : !container.current?.contains(event.target as Node)
      ) {
        setOpen(false);
      }
    };
    document.addEventListener("mousedown", close);
    document.addEventListener("keydown", close);
    return () => {
      document.removeEventListener("mousedown", close);
      document.removeEventListener("keydown", close);
    };
  }, [open, container]);

  const toggle = useCallback(() => {
    setOpen((current) => !current);
  }, []);
  const close = useCallback(() => {
    setOpen(false);
  }, []);

  return { open, id, toggle, close };
}
