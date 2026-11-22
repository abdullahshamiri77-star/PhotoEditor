package com.photoeditor.engine.canvas

/**
 * View transform for the document canvas. Document pixels remain unchanged;
 * this state affects only how the canvas is presented in the workspace.
 */
data class CanvasViewport(
    val scale: Float = 1f,
    val offsetX: Float = 0f,
    val offsetY: Float = 0f
) {
    init {
        require(scale.isFinite() && scale in MIN_SCALE..MAX_SCALE) {
            "Viewport scale must be between $MIN_SCALE and $MAX_SCALE"
        }
        require(offsetX.isFinite() && offsetY.isFinite()) {
            "Viewport offsets must be finite"
        }
    }

    fun panBy(deltaX: Float, deltaY: Float): CanvasViewport {
        require(deltaX.isFinite() && deltaY.isFinite()) { "Pan values must be finite" }
        return copy(offsetX = offsetX + deltaX, offsetY = offsetY + deltaY)
    }

    /** Zoom around a workspace-space pivot while keeping that point stationary. */
    fun transform(
        zoomFactor: Float,
        panX: Float,
        panY: Float,
        pivotX: Float,
        pivotY: Float
    ): CanvasViewport {
        require(zoomFactor.isFinite() && zoomFactor > 0f) { "Zoom factor must be finite and positive" }
        require(panX.isFinite() && panY.isFinite() && pivotX.isFinite() && pivotY.isFinite()) {
            "Viewport gesture values must be finite"
        }
        val nextScale = (scale * zoomFactor).coerceIn(MIN_SCALE, MAX_SCALE)
        val appliedFactor = nextScale / scale
        return CanvasViewport(
            scale = nextScale,
            offsetX = pivotX - (pivotX - offsetX) * appliedFactor + panX,
            offsetY = pivotY - (pivotY - offsetY) * appliedFactor + panY
        )
    }

    fun zoomBy(factor: Float): CanvasViewport =
        transform(factor, 0f, 0f, 0f, 0f)

    fun reset(): CanvasViewport = CanvasViewport()

    companion object {
        const val MIN_SCALE = 0.25f
        const val MAX_SCALE = 8f
    }
}
