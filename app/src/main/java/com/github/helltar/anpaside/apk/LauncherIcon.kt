package com.github.helltar.anpaside.apk

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import java.io.ByteArrayOutputStream
import java.io.File
import kotlin.math.roundToInt

/**
 * Turns a midlet icon into the launcher icon of an exported apk.
 *
 * A midlet icon is a sprite of 16 to 64 pixels and Android asks for 108dp of adaptive icon, so
 * something has to do the scaling. Left to the launcher it is done with smoothing, which turns
 * pixel art into mush; here it is scaled by a whole number instead, with filtering off, and
 * placed in the part of the foreground layer that no launcher mask can crop. The layer below
 * it is a flat colour that ships with the template and never changes.
 */
object LauncherIcon {

    // the 108dp adaptive icon grid at xxxhdpi, and the 66dp of it that stays visible under
    // every mask shape
    const val CANVAS_SIZE = 432
    const val SAFE_ZONE_SIZE = 264

    fun compose(icon: File): ByteArray? {
        val source = BitmapFactory.decodeFile(icon.path) ?: return null
        val longest = maxOf(source.width, source.height)

        if (longest <= 0) {
            source.recycle()
            return null
        }

        val drawn = contentSize(longest)
        val scale = drawn.toFloat() / longest
        val width = (source.width * scale).roundToInt().coerceAtLeast(1)
        val height = (source.height * scale).roundToInt().coerceAtLeast(1)
        val left = (CANVAS_SIZE - width) / 2
        val top = (CANVAS_SIZE - height) / 2

        val target = Bitmap.createBitmap(CANVAS_SIZE, CANVAS_SIZE, Bitmap.Config.ARGB_8888)
        val paint =
            Paint().apply {
                // filtering only helps where the sprite had to be resized by a fraction
                isFilterBitmap = drawn % longest != 0
                isDither = false
            }

        Canvas(target).drawBitmap(
            source,
            null,
            Rect(left, top, left + width, top + height),
            paint
        )

        val png =
            ByteArrayOutputStream().let { output ->
                target.compress(Bitmap.CompressFormat.PNG, 100, output)
                output.toByteArray()
            }

        source.recycle()
        target.recycle()

        return png
    }

    /**
     * The size the longest side of the sprite is drawn at.
     *
     * A whole multiple of the sprite is what keeps its pixels sharp, but the largest one that
     * fits can leave an odd sized icon covering half of the tile, and a small sharp icon looks
     * worse than a large smooth one - below three quarters of the safe zone the sprite is
     * simply resized to fill it.
     */
    fun contentSize(sourceSize: Int, safeSize: Int = SAFE_ZONE_SIZE): Int {
        if (sourceSize <= 0 || sourceSize >= safeSize) {
            return safeSize
        }

        val whole = (safeSize / sourceSize) * sourceSize

        return if (whole * 4 >= safeSize * 3) whole else safeSize
    }
}
