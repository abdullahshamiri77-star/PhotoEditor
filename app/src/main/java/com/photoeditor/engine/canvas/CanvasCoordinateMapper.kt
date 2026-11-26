package com.photoeditor.engine.canvas

/**
 * Converts pointer coordinates from the canvas viewport into document pixels.
 * The image is fitted inside the canvas, then viewport zoom/pan is applied
 * around the canvas center. This mapper reverses those operations.
 */
object CanvasCoordinateMapper {
    fun screenToDocument(
        canvasWidth: Float,
        canvasHeight: Float,
        documentWidth: Int,
        documentHeight: Int,
        screenX: Float,
        screenY: Float,
        viewportScale: Float,
        viewportOffsetX: Float,
        viewportOffsetY: Float
    ): Pair<Float, Float>? {
        if (!canvasWidth.isFinite() || !canvasHeight.isFinite() ||
            !screenX.isFinite() || !screenY.isFinite() ||
            !viewportScale.isFinite() || viewportScale <= 0f ||
            !viewportOffsetX.isFinite() || !viewportOffsetY.isFinite() ||
            documentWidth <= 0 || documentHeight <= 0 ||
            canvasWidth <= 0f || canvasHeight <= 0f
        ) return null

        val fitScale = minOf(canvasWidth / documentWidth, canvasHeight / documentHeight)
        if (!fitScale.isFinite() || fitScale <= 0f) return null

        val canvasCenterX = canvasWidth / 2f
        val canvasCenterY = canvasHeight / 2f
        val unzoomedX = canvasCenterX + (screenX - canvasCenterX - viewportOffsetX) / viewportScale
        val unzoomedY = canvasCenterY + (screenY - canvasCenterY - viewportOffsetY) / viewportScale
        val imageLeft = (canvasWidth - documentWidth * fitScale) / 2f
        val imageTop = (canvasHeight - documentHeight * fitScale) / 2f
        return ((unzoomedX - imageLeft) / fitScale) to ((unzoomedY - imageTop) / fitScale)
    }
}
