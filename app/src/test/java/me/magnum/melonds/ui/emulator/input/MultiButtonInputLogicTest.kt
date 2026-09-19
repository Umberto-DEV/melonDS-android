package me.magnum.melonds.ui.emulator.input

import me.magnum.melonds.domain.model.Input
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Pure geometry and bit mask logic, on the same 512x512 view and the same ABXY mapping the
 * handlers use (top = X, left = Y, bottom = B, right = A). The behaviour of the handlers
 * themselves is covered by [MultiButtonInputHandlerTest] and [DpadInputHandlerTest], which feed
 * them real MotionEvents.
 */
class MultiButtonInputLogicTest {

    private val circles = buildButtonCircles(TEST_VIEW_SIZE, TEST_VIEW_SIZE, topInput = Input.X, leftInput = Input.Y, bottomInput = Input.B, rightInput = Input.A)

    private fun maskOf(vararg points: Pair<Float, Float>): Int {
        val x = FloatArray(points.size) { points[it].first }
        val y = FloatArray(points.size) { points[it].second }
        return computePressedMask(circles, x, y, points.size)
    }

    private fun mask(vararg inputs: Input) = inputs.fold(0) { acc, input -> acc or inputMask(input) }

    @Test
    fun `each button is pressed by a touch on its own side of the view`() {
        assertEquals(mask(Input.A), maskOf(RIGHT_POINT))
        assertEquals(mask(Input.B), maskOf(BOTTOM_POINT))
        assertEquals(mask(Input.X), maskOf(TOP_POINT))
        assertEquals(mask(Input.Y), maskOf(LEFT_POINT))
    }

    @Test
    fun `the middle of the view presses nothing`() {
        assertEquals(0, maskOf(CENTRE_POINT))
    }

    @Test
    fun `a single point where two adjacent circles overlap presses both buttons`() {
        assertEquals(mask(Input.X, Input.Y), maskOf(TOP_LEFT_POINT))
    }

    @Test
    fun `two points press opposite buttons, which no single point can do`() {
        // The circles of two opposite buttons never overlap, so this is only reachable by testing
        // each pointer separately.
        assertEquals(mask(Input.A, Input.Y), maskOf(RIGHT_POINT, LEFT_POINT))
        assertEquals(0, maskOf(((RIGHT_POINT.first + LEFT_POINT.first) / 2) to RIGHT_POINT.second))
    }

    @Test
    fun `only the first pointerCount entries of the arrays are read`() {
        val x = floatArrayOf(RIGHT_POINT.first, LEFT_POINT.first)
        val y = floatArrayOf(RIGHT_POINT.second, LEFT_POINT.second)

        assertEquals(mask(Input.A), computePressedMask(circles, x, y, 1))
    }

    @Test
    fun `the circles scale with the view`() {
        val smallCircles = buildButtonCircles(256, 256, topInput = Input.X, leftInput = Input.Y, bottomInput = Input.B, rightInput = Input.A)
        val x = floatArrayOf(250f)
        val y = floatArrayOf(128f)

        assertEquals(mask(Input.A), computePressedMask(smallCircles, x, y, 1))
    }

    @Test
    fun `opposing directions cancel each other out, one axis at a time`() {
        assertEquals(0, removeOpposingDirections(mask(Input.UP, Input.DOWN)))
        assertEquals(0, removeOpposingDirections(mask(Input.LEFT, Input.RIGHT)))
        assertEquals(mask(Input.LEFT), removeOpposingDirections(mask(Input.UP, Input.DOWN, Input.LEFT)))
        assertEquals(mask(Input.UP, Input.LEFT), removeOpposingDirections(mask(Input.UP, Input.LEFT)))
    }
}
