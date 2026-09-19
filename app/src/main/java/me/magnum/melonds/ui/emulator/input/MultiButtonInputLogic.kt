package me.magnum.melonds.ui.emulator.input

import me.magnum.melonds.domain.model.Input
import kotlin.math.pow

/**
 * The set of buttons a [MultiButtonInputHandler] holds down is kept as a bit mask over
 * [Input.ordinal] (there are fewer than 32 inputs), so that a touch event allocates nothing.
 */
fun inputMask(input: Input): Int = 1 shl input.ordinal

/**
 * One button's circular hit area, in local view coordinates.
 */
class ButtonCircle(val centerX: Float, val centerY: Float, val radiusSquared: Float, val input: Input) {
    fun contains(x: Float, y: Float): Boolean {
        val dx = x - centerX
        val dy = y - centerY
        return dx * dx + dy * dy <= radiusSquared
    }
}

/**
 * Builds the four button hit circles for a [MultiButtonInputHandler]-based view of the given pixel
 * size. Geometry only: each circle is placed on the side of each button, near the edge of the whole
 * image. This allows for a margin of error and the circles also intersect, allowing multiple
 * buttons to be pressed at the same time.
 */
fun buildButtonCircles(viewWidth: Int, viewHeight: Int, topInput: Input, leftInput: Input, bottomInput: Input, rightInput: Input): Array<ButtonCircle> {
    val radiusSquared = (viewWidth * 256f / 512f).pow(2)
    // Centres are truncated to whole pixels, as they have always been.
    val localX: (Float) -> Float = { x -> (viewWidth * x / 512f).toInt().toFloat() }
    val localY: (Float) -> Float = { y -> (viewHeight * y / 512f).toInt().toFloat() }

    return arrayOf(
        ButtonCircle(localX(512f + 36f), localY(256f), radiusSquared, rightInput),
        ButtonCircle(localX(256f), localY(512f + 36f), radiusSquared, bottomInput),
        ButtonCircle(localX(256f), localY(-36f), radiusSquared, topInput),
        ButtonCircle(localX(-36f), localY(256f), radiusSquared, leftInput),
    )
}

/**
 * Given the button circles and every pointer currently down on the view (the first [pointerCount]
 * entries of [pointerX]/[pointerY]), returns the mask of the buttons that should be pressed.
 *
 * Each pointer is tested against every circle independently: that is what makes two simultaneous
 * fingers work even on opposite buttons, whose circles never overlap and so could never both be
 * satisfied by a single averaged point.
 */
fun computePressedMask(buttonCircles: Array<ButtonCircle>, pointerX: FloatArray, pointerY: FloatArray, pointerCount: Int): Int {
    var mask = 0
    for (pointer in 0 until pointerCount) {
        for (circle in buttonCircles.indices) {
            val buttonCircle = buttonCircles[circle]
            if (buttonCircle.contains(pointerX[pointer], pointerY[pointer])) {
                mask = mask or inputMask(buttonCircle.input)
            }
        }
    }
    return mask
}

private val VERTICAL_AXIS = inputMask(Input.UP) or inputMask(Input.DOWN)
private val HORIZONTAL_AXIS = inputMask(Input.LEFT) or inputMask(Input.RIGHT)

/**
 * Drops both directions of an axis whenever they are pressed together. A physical D-pad cannot
 * report UP+DOWN or LEFT+RIGHT, so games are not written to handle it and react unpredictably.
 */
fun removeOpposingDirections(mask: Int): Int {
    var result = mask
    if (result and VERTICAL_AXIS == VERTICAL_AXIS) {
        result = result and VERTICAL_AXIS.inv()
    }
    if (result and HORIZONTAL_AXIS == HORIZONTAL_AXIS) {
        result = result and HORIZONTAL_AXIS.inv()
    }
    return result
}
