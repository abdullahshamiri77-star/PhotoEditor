package com.photoeditor.engine.document

/**
 * Immutable document/layer metadata for the editor.
 *
 * Layer order is bottom-to-top: index 0 is the bottom layer and the last
 * element is the top layer. Pixel payloads are intentionally kept outside
 * this model so document operations remain cheap and independently testable.
 */
enum class LayerKind {
    BACKGROUND, BITMAP, TEXT, SHAPE, DRAWING, GRADIENT, ADJUSTMENT, GROUP
}

enum class LayerBlendMode {
    NORMAL, MULTIPLY, SCREEN, OVERLAY
}

data class LayerTransform(
    val x: Float = 0f,
    val y: Float = 0f,
    val scaleX: Float = 1f,
    val scaleY: Float = 1f,
    val rotationDegrees: Float = 0f
) {
    init {
        require(scaleX > 0f && scaleY > 0f) { "Layer scale must be positive" }
    }
}

data class EditorLayer(
    val id: String,
    val name: String,
    val kind: LayerKind,
    val visible: Boolean = true,
    val opacity: Float = 1f,
    val locked: Boolean = false,
    val blendMode: LayerBlendMode = LayerBlendMode.NORMAL,
    val transform: LayerTransform = LayerTransform()
) {
    init {
        require(id.isNotBlank()) { "Layer id must not be blank" }
        require(name.isNotBlank()) { "Layer name must not be blank" }
        require(opacity in 0f..1f) { "Layer opacity must be between 0 and 1" }
    }
}

data class EditorDocument(
    val width: Int,
    val height: Int,
    val dpi: Int = 300,
    val backgroundArgb: Int = 0x00000000,
    val layers: List<EditorLayer> = emptyList(),
    val selectedLayerId: String? = null
) {
    init {
        require(width > 0 && height > 0) { "Document dimensions must be positive" }
        require(dpi > 0) { "Document DPI must be positive" }
        require(layers.map { it.id }.toSet().size == layers.size) { "Layer ids must be unique" }
        require(selectedLayerId == null || layers.any { it.id == selectedLayerId }) {
            "Selected layer must belong to this document"
        }
    }

    fun addLayer(layer: EditorLayer, index: Int = layers.size): EditorDocument {
        require(layers.none { it.id == layer.id }) { "Layer id already exists" }
        require(index in 0..layers.size) { "Layer insertion index is out of range" }
        val updated = layers.toMutableList().apply { add(index, layer) }
        return copy(layers = updated, selectedLayerId = layer.id)
    }

    fun removeLayer(id: String): EditorDocument {
        val index = layers.indexOfFirst { it.id == id }
        if (index < 0) return this
        val updated = layers.toMutableList().apply { removeAt(index) }
        val nextSelection = if (selectedLayerId == id) {
            updated.getOrNull(index.coerceAtMost(updated.lastIndex))?.id
                ?: updated.lastOrNull()?.id
        } else selectedLayerId
        return copy(layers = updated, selectedLayerId = nextSelection)
    }

    fun selectLayer(id: String): EditorDocument {
        require(layers.any { it.id == id }) { "Cannot select a layer outside this document" }
        return copy(selectedLayerId = id)
    }

    fun moveLayer(fromIndex: Int, toIndex: Int): EditorDocument {
        require(fromIndex in layers.indices) { "Source layer index is out of range" }
        require(toIndex in layers.indices) { "Destination layer index is out of range" }
        if (fromIndex == toIndex) return this
        val updated = layers.toMutableList()
        val moved = updated.removeAt(fromIndex)
        updated.add(toIndex, moved)
        return copy(layers = updated)
    }

    fun updateLayer(id: String, transform: (EditorLayer) -> EditorLayer): EditorDocument {
        val index = layers.indexOfFirst { it.id == id }
        require(index >= 0) { "Layer does not belong to this document" }
        val updatedLayer = transform(layers[index])
        require(updatedLayer.id == id) { "Layer identity cannot be changed" }
        return copy(layers = layers.toMutableList().apply { set(index, updatedLayer) })
    }

    fun renameLayer(id: String, name: String): EditorDocument =
        updateLayer(id) { it.copy(name = name) }

    fun setLayerVisibility(id: String, visible: Boolean): EditorDocument =
        updateLayer(id) { it.copy(visible = visible) }

    fun setLayerOpacity(id: String, opacity: Float): EditorDocument =
        updateLayer(id) { it.copy(opacity = opacity.coerceIn(0f, 1f)) }

    fun setLayerLocked(id: String, locked: Boolean): EditorDocument =
        updateLayer(id) { it.copy(locked = locked) }
}
