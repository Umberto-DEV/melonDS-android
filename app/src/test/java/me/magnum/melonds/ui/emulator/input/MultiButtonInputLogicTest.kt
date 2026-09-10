package me.magnum.melonds.ui.emulator.input

import me.magnum.melonds.domain.model.Input
import org.junit.Assert.assertEquals
import org.junit.Test

// Tests the pure function at the heart of the two-finger ABXY/D-pad fix
// (MELONDS-DOCS/DUE-DITA.md, MELONDS-DOCS/ABXY-CORREZIONE.md). MultiButtonInputHandler.onTouch
// itself is Android glue (MotionEvent is a final Android class, not mockable, and this project
// deliberately has no mocking library) -- it is exercised here only indirectly, by feeding
// computePressedInputs exactly the pointer sets onTouch builds for each MotionEvent action.
//
// Geometry is NOT reinvented here: buildButtonCircles is the exact function onTouch calls, with
// the same ButtonsInputHandler mapping (top=X, left=Y, bottom=B, right=A) and a 512x512 view, so
// the circle radii/positions below match production exactly (DUE-DITA.md: adjacent circles
// overlap at ~0.81x their radius sum, opposite ones never overlap at ~1.14x).
//
// Both mutation directions (always-all-pressed, always-none-pressed) are exercised in the
// separate mutant run recorded in ABXY-CORREZIONE.md, not in this file -- this is the suite that
// must go red under each of them.
class MultiButtonInputLogicTest {

    private companion object {
        const val VIEW_SIZE = 512
    }

    private val circles = buildButtonCircles(VIEW_SIZE, VIEW_SIZE, topInput = Input.X, leftInput = Input.Y, bottomInput = Input.B, rightInput = Input.A)

    private fun centerOf(input: Input): PointerPosition {
        val circle = circles.first { it.input == input }
        return PointerPosition(circle.center.x.toFloat(), circle.center.y.toFloat())
    }

    @Test
    fun `a single finger on one button presses only that button`() {
        val pressed = computePressedInputs(circles, listOf(centerOf(Input.A)))
        assertEquals(setOf(Input.A), pressed)
    }

    @Test
    fun `two fingers on adjacent buttons press both`() {
        // A (right) and B (bottom) are adjacent: their circles overlap, but each finger sits on
        // ITS OWN button's center, not in the overlap lens -- this is real two-finger input, not
        // the single-point workaround the maintainer describes in issue #1446.
        val pressed = computePressedInputs(circles, listOf(centerOf(Input.A), centerOf(Input.B)))
        assertEquals(setOf(Input.A, Input.B), pressed)
    }

    @Test
    fun `two fingers on opposite buttons press both -- the case that is geometrically impossible with a single point`() {
        // A (right) and Y (left) are opposite: DUE-DITA.md shows their circles never overlap
        // (center distance about 1.14x the radius sum), so one coordinate could never satisfy
        // both circles. With one point per finger, evaluated independently, both register.
        val pressed = computePressedInputs(circles, listOf(centerOf(Input.A), centerOf(Input.Y)))
        assertEquals(setOf(Input.A, Input.Y), pressed)
    }

    @Test
    fun `a finger sliding from one button to another presses only the button it currently sits on`() {
        val whileOnX = computePressedInputs(circles, listOf(centerOf(Input.X)))
        assertEquals(setOf(Input.X), whileOnX)

        val afterSlidingToB = computePressedInputs(circles, listOf(centerOf(Input.B)))
        assertEquals(setOf(Input.B), afterSlidingToB)
    }

    @Test
    fun `releasing one finger leaves the other button still pressed`() {
        // Simulates onTouch's ACTION_POINTER_UP/ACTION_UP handling: the lifted pointer is
        // excluded from the active list passed on the next call; every other pointer keeps its
        // own unchanged position.
        val bothDown = computePressedInputs(circles, listOf(centerOf(Input.A), centerOf(Input.Y)))
        assertEquals(setOf(Input.A, Input.Y), bothDown)

        val afterLiftingA = computePressedInputs(circles, listOf(centerOf(Input.Y)))
        assertEquals(setOf(Input.Y), afterLiftingA)
    }

    @Test
    fun `ACTION_CANCEL -- an empty pointer list -- releases every button`() {
        // Simulates onTouch's ACTION_CANCEL branch, which passes an empty activePointers list
        // regardless of what MotionEvent still reports.
        val pressed = computePressedInputs(circles, emptyList())
        assertEquals(emptySet<Input>(), pressed)
    }

    @Test
    fun `a finger in the empty space between all four buttons presses nothing`() {
        val centerOfWholeView = PointerPosition(VIEW_SIZE / 2f, VIEW_SIZE / 2f)
        val pressed = computePressedInputs(circles, listOf(centerOfWholeView))
        assertEquals(emptySet<Input>(), pressed)
    }
}
