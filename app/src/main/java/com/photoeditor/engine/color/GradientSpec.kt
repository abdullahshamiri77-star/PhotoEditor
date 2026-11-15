package com.photoeditor.engine.color

import kotlin.math.roundToInt

/** A movable color stop on a gradient track. Position is normalized to 0..1. */
data class GradientStop(
    val id: String,
    val position: Float,
    val color: ColorValue
) {
    init {
        require(id.isNotBlank()) { "Gradient stop id cannot be blank" }
        require(position.isFinite() && position in 0f..1f) { "Gradient stop position must be in 0..1" }
    }
}

/**
 * Editable multi-stop gradient data. The angle is clockwise in degrees.
 * This model is independent from the UI so it can be reused by fills, text,
 * strokes, backgrounds and layer effects.
 */
data class GradientSpec(
    val stops: List<GradientStop>,
    val angleDegrees: Float = 0f
) {
    init {
        require(stops.isNotEmpty()) { "A gradient needs at least one color stop" }
        require(angleDegrees.isFinite()) { "Gradient angle must be finite" }
        require(stops.map { it.id }.distinct().size == stops.size) { "Gradient stop ids must be unique" }
    }

    fun orderedStops(): List<GradientStop> = stops.sortedBy { it.position }

    fun moveStop(id: String, newPosition: Float): GradientSpec {
        require(newPosition.isFinite()) { "Gradient stop position must be finite" }
        require(stops.any { it.id == id }) { "Unknown gradient stop: $id" }
        return copy(stops = stops.map {
            if (it.id == id) it.copy(position = newPosition.coerceIn(0f, 1f)) else it
        })
    }

    fun updateStopColor(id: String, color: ColorValue): GradientSpec {
        require(stops.any { it.id == id }) { "Unknown gradient stop: $id" }
        return copy(stops = stops.map { if (it.id == id) it.copy(color = color) else it })
    }

    fun addStop(stop: GradientStop): GradientSpec {
        require(stops.none { it.id == stop.id }) { "Gradient stop id already exists: ${stop.id}" }
        return copy(stops = stops + stop)
    }

    fun removeStop(id: String): GradientSpec {
        require(stops.any { it.id == id }) { "Unknown gradient stop: $id" }
        require(stops.size > 2) { "A gradient must keep at least two stops" }
        return copy(stops = stops.filterNot { it.id == id })
    }

    /** Linear interpolation of RGBA channels at a normalized position. */
    fun colorAt(position: Float): ColorValue {
        val sorted = orderedStops()
        val p = position.coerceIn(0f, 1f)
        if (sorted.size == 1 || p <= sorted.first().position) return sorted.first().color
        if (p >= sorted.last().position) return sorted.last().color

        val rightIndex = sorted.indexOfFirst { it.position >= p }.coerceAtLeast(1)
        val left = sorted[rightIndex - 1]
        val right = sorted[rightIndex]
        val span = right.position - left.position
        val fraction = if (span <= 0f) 0f else ((p - left.position) / span).coerceIn(0f, 1f)

        fun blend(a: Int, b: Int): Int =
            (a + (b - a) * fraction).roundToInt().coerceIn(0, 255)

        return ColorValue(
            blend(left.color.red, right.color.red),
            blend(left.color.green, right.color.green),
            blend(left.color.blue, right.color.blue),
            blend(left.color.alpha, right.color.alpha)
        )
    }

    companion object {
        fun twoColor(start: ColorValue, end: ColorValue): GradientSpec = GradientSpec(
            listOf(
                GradientStop("start", 0f, start),
                GradientStop("end", 1f, end)
            )
        )
    }
}
