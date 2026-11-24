# PhotoEditor — Project Continuity Log

> This file is the durable handover record for the project. Update it whenever a milestone is completed, a pull request changes state, or a technical decision is made. Never describe planned work as shipped work.

## Product north star

Build a professional, standalone Android image editor inspired functionally by desktop-class tools. The target is a real working application, not a visual mock-up. Every visible control must either work or be clearly marked as not implemented. Prioritize correctness, non-destructive editing, recoverability, performance, and a polished Arabic/English experience.

## Engineering rules

1. Work in focused branches and pull requests; keep `main` buildable.
2. Do not merge before reviewing the diff and passing relevant tests/build.
3. Never claim a feature works until it is wired into the app and verified.
4. Record shipped, in-progress, blocked, and not-yet-implemented work separately.
5. Preserve working flows (open/import, edits, undo/redo, export) during architectural changes.
6. Prefer small reversible steps over broad rewrites.

## Verified baseline

- Default branch: `main`.
- Main baseline after PR #3 merge: `738920663af1c7eb16d6557244f50f048334e734` (22 Nov 2026).
- PR #3 merged: document/layer metadata model and unit tests.
- Existing app supports image import, PNG export, bitmap effects (grayscale, rotate, brightness/contrast/saturation), and bitmap-snapshot undo/redo.
- Existing UI is a desktop-inspired Compose shell with menus, tool rail, canvas preview, and panels. Several menu/tool items remain placeholders.
- Color foundation exists for RGB/alpha, HEX, HSL, CMYK conversion, and multi-stop gradient data; it is not yet a fully integrated color workspace. CMYK is not ICC-accurate.

## Current in-progress work

### PR #5 — Bitmap layer compositor

- Branch: `feature/bitmap-layer-compositor`
- PR: https://github.com/abdullahshamiri77-star/PhotoEditor/pull/5
- Status at last verification (22 Nov 2026): open, draft, not merged.
- Head SHA: `6a04c636c4dc187ed2e3835083667a3df7f4cca4` (last recorded PR head).
- CI run: https://github.com/abdullahshamiri77-star/PhotoEditor/actions/runs/45049270363
- Last observed CI result: success; unit tests, debug APK build, and artifact upload completed.
- Adds `BitmapLayerCompositor`, which flattens visible bitmap layers bottom-to-top with opacity, transforms, and NORMAL/MULTIPLY/SCREEN/OVERLAY PorterDuff modes.
- Adds a helper to list visible bitmap layer IDs and a unit test for layer visibility/order.
- Important limitation: this is an engine foundation only. It is not yet connected to `MainActivity`, the Compose Layers panel, or the live canvas. Text/shape/drawing/gradient/adjustment renderers are not implemented.

## Architecture observations from source review

- `EditorDocument` is immutable metadata; layer pixel payloads are held separately by stable layer ID.
- Layer order is bottom-to-top: index 0 is the bottom layer, last index is top.
- Current `MainActivity` still owns a single `currentBitmap` and `importedBitmap`; existing history stores bitmap snapshots.
- Current `EditorScreen` displays one bitmap and explicitly labels independent layers as not implemented.
- The compositor currently applies transforms around the layer origin (0,0); no center-pivot/anchor model exists yet.
- Current tests do not verify actual Android pixel compositing output; the existing compositor test checks visible bitmap layer IDs/order only.

## Next engineering steps

1. Review PR #5 diff and CI logs; keep it unmerged until correctness concerns are resolved.
2. Define an `EditorSession` state that pairs immutable `EditorDocument` metadata with bitmap payloads keyed by stable layer IDs.
3. Decide and document first-import sizing behavior (document dimensions vs. fit-to-canvas), transform pivot/anchor behavior, and bitmap ownership/lifecycle.
4. Integrate image import as a bitmap layer without breaking the current open/edit/undo/redo/export workflow.
5. Render a composited preview and connect real Layers panel actions: select, visibility, opacity, reorder, delete, and add/import.
6. Add meaningful tests, including pixel-level or instrumentation coverage for compositor behavior where feasible.
7. Only then consider merging the integration work after CI and review.

## Milestone ledger

| Date | Milestone | Status |
|---|---|---|
| 22 Nov 2026 | PR #3 document/layer metadata foundation | Merged |
| 22 Nov 2026 | PR #5 bitmap compositor foundation | CI passed; draft PR open |
| Pending | Session state + live compositor integration | Not started |
| Pending | Functional Layers panel | Not started |
| Pending | Multi-layer save/reopen project format | Not started |

## Update protocol

For every future update, append or revise:
- date and exact commit/PR reference;
- what changed in user-visible behavior;
- tests/builds actually run and their result;
- known limitations and next acceptance criteria.

Do not erase historical milestone entries when updating this file.
