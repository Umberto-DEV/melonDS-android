package me.magnum.melonds.ui.emulator.input

import me.magnum.melonds.domain.model.Input
import me.magnum.melonds.domain.model.Point
import kotlin.math.pow

/**
 * Position of a single active touch pointer, already expressed in the same local coordinate
 * space as [ButtonCircle] (i.e. scaled to the view's actual pixel width/height, the way
 * [buildButtonCircles]'s caller does with MotionEvent.getX(i)/getY(i)).
 */
data class PointerPosition(val x: Float, val y: Float)

/**
 * One button's circular hit area, in local view coordinates.
 */
data class ButtonCircle(val center: Point, val radiusSquared: Float, val input: Input) {
    fun containsPoint(pointerPosition: PointerPosition): Boolean {
        return (pointerPosition.x - center.x).pow(2) + (pointerPosition.y - center.y).pow(2) <= radiusSquared
    }
}

/**
 * Builds the four button hit circles for a [MultiButtonInputHandler]-based view of the given
 * pixel size. Geometry only, deliberately UNCHANGED by the two-finger fix (see
 * MELONDS-DOCS/DUE-DITA.md and MELONDS-DOCS/ABXY-CORREZIONE.md): centers and radii are exactly as
 * before. Only how many pointers get tested against them changes, in [computePressedInputs].
 */
fun buildButtonCircles(viewWidth: Int, viewHeight: Int, topInput: Input, leftInput: Input, bottomInput: Input, rightInput: Input): List<ButtonCircle> {
    val radiusSquared = (viewWidth * 256f / 512f).pow(2)
    val pointToLocal: (Float, Float) -> Point = { x, y ->
        Point().apply {
            this.x = (viewWidth * x / 512f).toInt()
            this.y = (viewHeight * y / 512f).toInt()
        }
    }

    // Each circle is placed on the side of each button, near the edge of the whole image. This allows for a margin of error and the circles also intersect, allowing
    // multiple buttons to be pressed at the same time
    return listOf(
        ButtonCircle(pointToLocal(512f + 36f, 256f), radiusSquared, rightInput),
        ButtonCircle(pointToLocal(256f, 512f + 36f), radiusSquared, bottomInput),
        ButtonCircle(pointToLocal(256f, -36f), radiusSquared, topInput),
        ButtonCircle(pointToLocal(-36f, 256f), radiusSquared, leftInput),
    )
}

/**
 * Pure function at the heart of the two-finger fix (MELONDS-DOCS/DUE-DITA.md): given the button
 * circles and EVERY pointer currently down on the view, returns the full set of buttons that
 * should be considered pressed right now.
 *
 * Each pointer is tested against every circle independently. This is what makes two simultaneous
 * fingers on two different buttons work -- including opposite pairs (A+Y, X+B) whose circles
 * never overlap -- without touching a single radius or center: two distinct points don't need
 * their circles to overlap for both to register; only a single shared point did.
 */
fun computePressedInputs(buttonCircles: List<ButtonCircle>, activePointers: List<PointerPosition>): Set<Input> {
    val pressed = mutableSetOf<Input>()
    for (pointer in activePointers) {
        for (circle in buttonCircles) {
            if (circle.containsPoint(pointer)) {
                pressed.add(circle.input)
            }
        }
    }
    return pressed
}
