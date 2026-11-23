# Android Manual Acceptance Checklist

Run this checklist on a physical Android device using the debug APK produced by the corresponding GitHub Actions run.

## Build and launch
- [ ] Install the debug APK successfully.
- [ ] Launch PhotoEditor and confirm the workspace opens without crashing.
- [ ] Switch Arabic/English and confirm the interface direction and labels update.

## Multilayer document
- [ ] Import one image; it becomes the first layer and defines the document dimensions.
- [ ] Import a second image; it appears as a separate layer, centered and aspect-preserved.
- [ ] Select each layer and confirm the selected row is highlighted.
- [ ] Hide and show a layer; the preview updates immediately.
- [ ] Change layer opacity; the preview updates and the displayed percentage follows.
- [ ] Move a layer up and down; compositing order changes as expected.
- [ ] Delete a layer; it disappears from the preview and the layer list.
- [ ] Undo and redo import, visibility, opacity, reorder, and delete operations.
- [ ] Apply grayscale and tonal adjustments to the selected bitmap layer.

## New document
- [ ] Open File → New Document.
- [ ] Create a 1080 × 1080 px white document.
- [ ] Create a 1080 × 1350 px document from its preset.
- [ ] Enter A4 dimensions in millimeters at 300 DPI and confirm the pixel dimensions.
- [ ] Try invalid DPI, zero/negative dimensions, and an oversized document; creation must be rejected with a visible message.
- [ ] Create a new document while another document is open; the old layers/history must not remain.

## Canvas viewport
- [ ] Pinch with two fingers to zoom in and out.
- [ ] Drag on the canvas to pan.
- [ ] Confirm zoom remains anchored near the gesture centroid.
- [ ] Use + and − controls and confirm the percentage updates.
- [ ] Use Fit and confirm the canvas returns to its initial view.
- [ ] Confirm zoomed content is clipped to the canvas workspace and does not cover side panels.

## Export and stability
- [ ] Export the composite as PNG and open the exported file in the phone gallery.
- [ ] Compare the exported image with the visible composite.
- [ ] Repeat import, adjustment, undo/redo, and deletion for several minutes.
- [ ] Confirm there are no crashes, frozen controls, blank previews, or obvious memory-related slowdowns.

## Current known limitations
- Native .photoeditor project save/open is not implemented yet; current export is flattened PNG.
- Brush, eraser, text, shape, crop, and per-layer move/transform tools are not yet connected to the canvas.
- Rulers, guides, grid, snapping, and print preview remain future canvas/document stages.
- CI verifies unit tests and APK compilation; it does not replace this physical-device checklist.
