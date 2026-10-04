/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmytro Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */

import { useRef } from "react";
import { Link } from "react-router";
import type { Scope } from "../api/client";
import { usePopover } from "../app/usePopover";
import { SCOPE_LEVELS, type ScopeLevel, scopeOptions, switchScope } from "./switcher";

interface ScopeSwitcherProps {
  readonly current: Scope;
  readonly scopes: readonly Scope[];
  readonly onSwitch: (target: Scope) => void;
}

const LEVEL_LABELS: Record<ScopeLevel, string> = {
  project: "Project",
  environment: "Environment",
  cluster: "Cluster",
};

const VALUE_CLASSES: Record<ScopeLevel, string | undefined> = {
  project: undefined,
  environment: "badge b-kind",
  cluster: "mono",
};

export function ScopeSwitcher({ current, scopes, onSwitch }: ScopeSwitcherProps) {
  return (
    <nav className="breadcrumb" aria-label="Scope">
      <Link to="/">Projects</Link>
      {SCOPE_LEVELS.map((level) => (
        <span className="crumb" key={level}>
          <span aria-hidden="true">/</span>
          <ScopeSwitch
            level={level}
            current={current}
            options={scopeOptions(scopes, current, level)}
            onPick={(value) => {
              const target = switchScope(scopes, current, level, value);
              if (target !== undefined) {
                onSwitch(target);
              }
            }}
          />
        </span>
      ))}
    </nav>
  );
}

interface ScopeSwitchProps {
  readonly level: ScopeLevel;
  readonly current: Scope;
  readonly options: readonly string[];
  readonly onPick: (value: string) => void;
}

function ScopeSwitch({ level, current, options, onPick }: ScopeSwitchProps) {
  const container = useRef<HTMLSpanElement>(null);
  const popover = usePopover(container);
  const value = current[level];
  const alternatives = options.filter((option) => option !== value);
  const valueClass = VALUE_CLASSES[level];
  if (alternatives.length === 0) {
    return <span className={valueClass}>{value}</span>;
  }
  return (
    <span className="facet" ref={container}>
      <button
        type="button"
        className="crumb-btn"
        aria-expanded={popover.open}
        aria-controls={popover.id}
        onClick={popover.toggle}
      >
        <span className="sr-only">{LEVEL_LABELS[level]}</span>{" "}
        <span className={valueClass}>{value}</span>
        <span className="caret" aria-hidden="true" />
      </button>
      {popover.open && (
        <fieldset className="popover" id={popover.id}>
          <legend className="sr-only">Switch {level}</legend>
          {options.map((option) => (
            <button
              key={option}
              type="button"
              className="pop-item"
              aria-pressed={option === value}
              onClick={() => {
                popover.close();
                if (option !== value) {
                  onPick(option);
                }
              }}
            >
              <span className={level === "project" ? undefined : "mono"}>{option}</span>
            </button>
          ))}
        </fieldset>
      )}
    </span>
  );
}
