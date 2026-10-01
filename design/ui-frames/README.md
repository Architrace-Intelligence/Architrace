# UI frames

Static design frames of the initial UI release. `MVP-frames.dc.html` is a Claude Design
canvas (`design_doc_mode=canvas`) with five 1440 × 900 frames:

| Frame | Screen |
|-------|--------|
| 1 | Projects: one row per scope (project × environment × cluster), grouped by project |
| 2 | Projects filtered to PROD, grouped by cluster, cluster facet open |
| 3 | Service map of one scope with every service, data store, data stream and external |
| 4 | Service map with a service selected |
| 5 | Service map with a data stream selected and the Data streams lens |

The file is the one uploaded to the Claude Design project "Architrace UI"; the design page
`project/features/ui-design` on the documentation site explains the screens. The frames
share the tokens and component classes of [`../ui-prototype/`](../ui-prototype/) and stay
in sync with them by hand.

## Preview

The file needs the Design Components runtime `support.js` beside it. Claude Design writes the
runtime into the project; locally, place a copy next to the file and serve the folder with any
static file server. Nodes and edges of the map frames are rendered from the data at the end of
the file, everything else is plain markup.
