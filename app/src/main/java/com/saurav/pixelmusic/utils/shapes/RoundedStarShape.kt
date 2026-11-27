package com.saurav.pixelmusic.utils.shapes

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/**
 * Shape describing star with rounded corners
 *
 * Note: The shape draws within the minimum of provided width and height so can't be used to create stretched shape.
 *
 * @param sides number of sides.
 * @param curve a double value between 0.0 - 1.0 for modifying star curve.
 * @param rotation  value between 0 - 360
 * @param iterations a value between 0 - 360 that determines the quality of star shape.
 */
class RoundedStarShape(
    private val sides: Int,
    private val curve: Double = 0.09,
    private val rotation: Float = 0f,
    iterations: Int = 360,
) : Shape {

    private companion object {
        const val TWO_PI = 2 * PI
    }

    private val effectiveIterations = min(iterations, 72).coerceAtLeast(12)
    private val steps = TWO_PI / effectiveIterations
    private val rotationDegree = (PI / 180) * rotation

    private var cachedWidth: Float = -1f
    private var cachedHeight: Float = -1f
    private var cachedDensity: Float = -1f
    private var cachedOutline: Outline? = null

    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val currentOutline = cachedOutline
        if (size.width == cachedWidth && size.height == cachedHeight && density.density == cachedDensity && currentOutline != null) {
            return currentOutline
        }

        val r = min(size.height, size.width) * 0.4 * mapRange(1.0, 0.0, 0.5, 1.0, curve)
        val xCenter = size.width * 0.5f
        val yCenter = size.height * 0.5f

        val path = Path().apply {
            val startAngle = -rotationDegree
            val startFactor = 1.0 + curve
            val startX = (r * (cos(startAngle) * startFactor) + xCenter).toFloat()
            val startY = (r * (sin(startAngle) * startFactor) + yCenter).toFloat()
            moveTo(startX, startY)

            var t = steps
            while (t < TWO_PI) {
                val angle = t - rotationDegree
                val factor = 1.0 + curve * cos(sides * t)
                val x = (r * (cos(angle) * factor) + xCenter).toFloat()
                val y = (r * (sin(angle) * factor) + yCenter).toFloat()
                lineTo(x, y)
                t += steps
            }

            close()
        }

        val outline = Outline.Generic(path)
        cachedWidth = size.width
        cachedHeight = size.height
        cachedDensity = density.density
        cachedOutline = outline
        return outline
    }

    private fun mapRange(a: Double, b: Double, c: Double, d: Double, x: Double): Double {
        return (x - a) / (b - a) * (d - c) + c
    }
}
