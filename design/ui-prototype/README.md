# UI prototype sources

Source of the interactive MVP user interface design: eight Design Components screens
(`.dc.html`) that open in Claude Design and render as working prototypes on demo data.
The design page on the documentation site (`project/features/ui-design`) explains the
screens, the foundations and the agent-first principles; this folder keeps the sources so
the design can be rebuilt and diffed like code.

## Layout

| Path | Content |
|------|---------|
| `src/shared.css` | design tokens for both themes and every component class |
| `src/shared.js` | demo topology, findings, agents, diff and layout helpers shared by the screens |
| `src/_head.html`, `src/_askbar.html`, `src/_insights.html` | fragments inlined into every screen |
| `src/main.html` | Service map |
| `src/overview.html`, `src/drift.html`, `src/findings.html`, `src/agents.html` | the other screens |
| `src/foundations.html`, `src/agentfirst.html`, `src/start.html` | foundations, principles, index |
| `src/build.py` | inlines the fragments and writes `project/<Screen>.dc.html` |

## Build and preview

```bash
python3 design/ui-prototype/src/build.py
```

The output lands in `design/ui-prototype/project/` (ignored by git). The files need the
Design Components runtime `support.js` beside them; Claude Design writes it into the project
when the screens are uploaded, and any static file server shows the pages locally once a copy
of the runtime is placed next to them.

## Conventions

- Screens are fluid pages: the context rail hides below 1180 px, the navigation rail below 860 px.
- Every colour is a CSS custom property on `[data-theme]`; the same names exist for dark and light.
- Node and edge rendering, diffing and the command palette live in `shared.js` so the service
  map and the drift map stay consistent.
