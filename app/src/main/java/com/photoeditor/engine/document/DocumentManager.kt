package com.photoeditor.engine.document

import kotlin.math.roundToInt

/** Units accepted by the New Document dialog. */
enum class DocumentSizeUnit {
    PIXELS,
    INCHES,
    MILLIMETERS
}

/** Standard starting points; dimensions are stored in pixels. */
enum class DocumentPreset(
    val widthPx: Int,
    val heightPx: Int,
    val dpi: Int
) {
    SQUARE_1080(1080, 1080, 300),
    PORTRAIT_4_5(1080, 1350, 300),
    LANDSCAPE_HD(1920, 1080, 300),
    UHD_4K(3840, 2160, 300),
    A5_300(1748, 2480, 300),
    A4_300(2480, 3508, 300),
    A3_300(3508, 4961, 300)
}

/**
 * User-entered document settings. Pixel dimensions are canonical so rendering,
 * transforms, and export all share one coordinate space.
 */
data class DocumentSpec(
    val widthPx: Int,
    val heightPx: Int,
    val dpi: Int = 300,
    val backgroundArgb: Int = 0x00000000
) {
    init {
        require(widthPx > 0) { "Document width must be positive" }
        require(heightPx > 0) { "Document height must be positive" }
        require(dpi in MIN_DPI..MAX_DPI) { "DPI must be between $MIN_DPI and $MAX_DPI" }
        require(widthPx <= MAX_DIMENSION && heightPx <= MAX_DIMENSION) {
            "Document dimensions exceed the supported maximum of $MAX_DIMENSION pixels"
        }
        require(widthPx.toLong() * heightPx.toLong() <= MAX_PIXELS) {
            "Document exceeds the safe pixel budget of $MAX_PIXELS pixels"
        }
    }

    companion object {
        const val MIN_DPI = 30
        const val MAX_DPI = 1200
        const val MAX_DIMENSION = 16_384
        const val MAX_PIXELS = 32_000_000L

        fun fromPhysicalSize(
            width: Double,
            height: Double,
            unit: DocumentSizeUnit,
            dpi: Int = 300,
            backgroundArgb: Int = 0x00000000
        ): DocumentSpec {
            require(width.isFinite() && height.isFinite() && width > 0.0 && height > 0.0) {
                "Physical dimensions must be finite and positive"
            }
            require(dpi in MIN_DPI..MAX_DPI) { "DPI must be between $MIN_DPI and $MAX_DPI" }
            val pixelsPerUnit = when (unit) {
                DocumentSizeUnit.PIXELS -> 1.0
                DocumentSizeUnit.INCHES -> dpi.toDouble()
                DocumentSizeUnit.MILLIMETERS -> dpi.toDouble() / 25.4
            }
            return DocumentSpec(
                widthPx = (width * pixelsPerUnit).roundToInt(),
                heightPx = (height * pixelsPerUnit).roundToInt(),
                dpi = dpi,
                backgroundArgb = backgroundArgb
            )
        }

        fun fromPreset(
            preset: DocumentPreset,
            backgroundArgb: Int = 0x00000000
        ): DocumentSpec = DocumentSpec(
            widthPx = preset.widthPx,
            heightPx = preset.heightPx,
            dpi = preset.dpi,
            backgroundArgb = backgroundArgb
        )
    }
}

/** Creates validated blank editing sessions from document specifications. */
object DocumentManager {
    fun create(spec: DocumentSpec): EditorSession = EditorSession.newDocument(
        width = spec.widthPx,
        height = spec.heightPx,
        dpi = spec.dpi,
        backgroundArgb = spec.backgroundArgb
    )

    fun create(preset: DocumentPreset, backgroundArgb: Int = 0x00000000): EditorSession =
        create(DocumentSpec.fromPreset(preset, backgroundArgb))
}
