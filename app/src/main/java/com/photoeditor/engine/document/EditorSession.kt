package com.photoeditor.engine.document

import android.graphics.Bitmap
import java.util.UUID

/**
 * Runtime editing state that connects immutable document metadata to bitmap pixels.
 *
 * Bitmap payloads are keyed by stable layer IDs and remain outside EditorDocument,
 * so metadata operations stay immutable and cheap. This is the integration boundary
 * between the document model and bitmap renderer.
 *
 * Bitmaps supplied to this session are treated as borrowed, read-only payloads:
 * callers must not mutate or recycle them while the session is using them.
 */
data class EditorSession(
    val document: EditorDocument,
    val bitmapLayers: Map<String, Bitmap> = emptyMap()
) {
    init {
        val layerById = document.layers.associateBy { it.id }
        require(bitmapLayers.keys.all { id ->
            layerById[id]?.kind == LayerKind.BITMAP
        }) {
            "Every bitmap payload must belong to a bitmap layer in this document"
        }
        require(bitmapLayers.values.none { it.isRecycled }) {
            "A session cannot contain recycled bitmap payloads"
        }
    }

    /** Add a bitmap as a new top layer without changing the document dimensions. */
    fun addBitmapLayer(
        bitmap: Bitmap,
        name: String = "Image",
        id: String = UUID.randomUUID().toString(),
        index: Int = document.layers.size,
        transform: LayerTransform = LayerTransform()
    ): EditorSession {
        require(!bitmap.isRecycled) { "Cannot add a recycled bitmap" }
        require(bitmap.width > 0 && bitmap.height > 0) { "Bitmap dimensions must be positive" }
        require(bitmapLayers[id] == null) { "Layer id already has a bitmap payload" }

        val layer = EditorLayer(
            id = id,
            name = name,
            kind = LayerKind.BITMAP,
            transform = transform
        )
        val nextDocument = document.addLayer(layer, index)
        return copy(
            document = nextDocument,
            bitmapLayers = bitmapLayers + (id to bitmap)
        )
    }

    /** Remove metadata and its associated pixel payload together. */
    fun removeLayer(id: String): EditorSession {
        if (document.layers.none { it.id == id }) return this
        return copy(
            document = document.removeLayer(id),
            bitmapLayers = bitmapLayers - id
        )
    }

    /**
     * Apply a document operation and discard payloads whose bitmap layers no longer exist.
     * This keeps metadata and pixel storage consistent even when callers remove or
     * change a layer kind through a document-level operation.
     */
    fun updateDocument(transform: (EditorDocument) -> EditorDocument): EditorSession {
        val updatedDocument = transform(document)
        val validBitmapIds = updatedDocument.layers
            .filter { it.kind == LayerKind.BITMAP }
            .map { it.id }
            .toSet()
        return copy(
            document = updatedDocument,
            bitmapLayers = bitmapLayers.filterKeys { it in validBitmapIds }
        )
    }

    /** Replace a bitmap payload while retaining the layer's stable identity. */
    fun replaceBitmapPayload(id: String, bitmap: Bitmap): EditorSession {
        require(document.layers.any { it.id == id && it.kind == LayerKind.BITMAP }) {
            "A bitmap payload can only be assigned to an existing bitmap layer"
        }
        require(!bitmap.isRecycled) { "Cannot assign a recycled bitmap" }
        return copy(bitmapLayers = bitmapLayers + (id to bitmap))
    }

    companion object {
        fun newDocument(
            width: Int,
            height: Int,
            dpi: Int = 300,
            backgroundArgb: Int = 0x00000000
        ): EditorSession = EditorSession(
            document = EditorDocument(
                width = width,
                height = height,
                dpi = dpi,
                backgroundArgb = backgroundArgb
            )
        )

        /** Create a document whose dimensions match the first imported image. */
        fun fromBitmap(
            bitmap: Bitmap,
            name: String = "Background",
            dpi: Int = 300,
            id: String = UUID.randomUUID().toString()
        ): EditorSession {
            require(!bitmap.isRecycled) { "Cannot create a document from a recycled bitmap" }
            val document = EditorDocument(
                width = bitmap.width,
                height = bitmap.height,
                dpi = dpi
            ).addLayer(
                EditorLayer(id = id, name = name, kind = LayerKind.BITMAP)
            )
            return EditorSession(document, mapOf(id to bitmap))
        }
    }
}
