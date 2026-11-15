package com.photoeditor.engine.color

import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/** Device-independent 8-bit sRGB color value used by every editor color control. */
data class ColorValue(
    val red: Int,
    val green: Int,
    val blue: Int,
    val alpha: Int = 255
) {
    init {
        require(red in 0..255 && green in 0..255 && blue in 0..255 && alpha in 0..255) {
            "RGBA channels must be in the range 0..255"
        }
    }

    fun toHex(includeAlpha: Boolean = false): String =
        if (includeAlpha) "#%02X%02X%02X%02X".format(alpha, red, green, blue)
        else "#%02X%02X%02X".format(red, green, blue)

    fun toHsl(): HslColor {
        val r = red / 255.0
        val g = green / 255.0
        val b = blue / 255.0
        val maxChannel = max(r, max(g, b))
        val minChannel = min(r, min(g, b))
        val delta = maxChannel - minChannel
        val lightness = (maxChannel + minChannel) / 2.0

        val hue = when {
            delta == 0.0 -> 0.0
            maxChannel == r -> 60.0 * (((g - b) / delta) % 6.0)
            maxChannel == g -> 60.0 * (((b - r) / delta) + 2.0)
            else -> 60.0 * (((r - g) / delta) + 4.0)
        }.let { if (it < 0.0) it + 360.0 else it }

        val saturation = if (delta == 0.0) 0.0
        else delta / (1.0 - kotlin.math.abs(2.0 * lightness - 1.0))

        return HslColor(hue, saturation * 100.0, lightness * 100.0)
    }

    fun toCmyk(): CmykColor {
        val r = red / 255.0
        val g = green / 255.0
        val b = blue / 255.0
        val k = 1.0 - max(r, max(g, b))
        if (k >= 1.0) return CmykColor(0.0, 0.0, 0.0, 100.0)
        val denominator = 1.0 - k
        return CmykColor(
            ((1.0 - r - k) / denominator * 100.0).coerceIn(0.0, 100.0),
            ((1.0 - g - k) / denominator * 100.0).coerceIn(0.0, 100.0),
            ((1.0 - b - k) / denominator * 100.0).coerceIn(0.0, 100.0),
            k * 100.0
        )
    }

    companion object {
        fun fromHex(value: String): ColorValue? {
            val digits = value.trim().removePrefix("#")
            return runCatching {
                when (digits.length) {
                    3 -> ColorValue(
                        digits[0].digitToInt(16) * 17,
                        digits[1].digitToInt(16) * 17,
                        digits[2].digitToInt(16) * 17
                    )
                    6 -> ColorValue(
                        digits.substring(0, 2).toInt(16),
                        digits.substring(2, 4).toInt(16),
                        digits.substring(4, 6).toInt(16)
                    )
                    8 -> ColorValue(
                        digits.substring(2, 4).toInt(16),
                        digits.substring(4, 6).toInt(16),
                        digits.substring(6, 8).toInt(16),
                        digits.substring(0, 2).toInt(16)
                    )
                    else -> return null
                }
            }.getOrNull()
        }

        fun fromHsl(hsl: HslColor, alpha: Int = 255): ColorValue {
            val h = ((hsl.hue % 360.0) + 360.0) % 360.0
            val s = (hsl.saturation / 100.0).coerceIn(0.0, 1.0)
            val l = (hsl.lightness / 100.0).coerceIn(0.0, 1.0)
            val chroma = (1.0 - kotlin.math.abs(2.0 * l - 1.0)) * s
            val x = chroma * (1.0 - kotlin.math.abs((h / 60.0) % 2.0 - 1.0))
            val m = l - chroma / 2.0
            val (r, g, b) = when {
                h < 60.0 -> Triple(chroma, x, 0.0)
                h < 120.0 -> Triple(x, chroma, 0.0)
                h < 180.0 -> Triple(0.0, chroma, x)
                h < 240.0 -> Triple(0.0, x, chroma)
                h < 300.0 -> Triple(x, 0.0, chroma)
                else -> Triple(chroma, 0.0, x)
            }
            return ColorValue(
                ((r + m) * 255.0).roundToInt().coerceIn(0, 255),
                ((g + m) * 255.0).roundToInt().coerceIn(0, 255),
                ((b + m) * 255.0).roundToInt().coerceIn(0, 255),
                alpha.coerceIn(0, 255)
            )
        }

        fun fromCmyk(cmyk: CmykColor, alpha: Int = 255): ColorValue {
            val c = (cmyk.cyan / 100.0).coerceIn(0.0, 1.0)
            val m = (cmyk.magenta / 100.0).coerceIn(0.0, 1.0)
            val y = (cmyk.yellow / 100.0).coerceIn(0.0, 1.0)
            val k = (cmyk.black / 100.0).coerceIn(0.0, 1.0)
            return ColorValue(
                ((1.0 - c) * (1.0 - k) * 255.0).roundToInt(),
                ((1.0 - m) * (1.0 - k) * 255.0).roundToInt(),
                ((1.0 - y) * (1.0 - k) * 255.0).roundToInt(),
                alpha.coerceIn(0, 255)
            )
        }
    }
}

data class HslColor(val hue: Double, val saturation: Double, val lightness: Double)
data class CmykColor(val cyan: Double, val magenta: Double, val yellow: Double, val black: Double)
