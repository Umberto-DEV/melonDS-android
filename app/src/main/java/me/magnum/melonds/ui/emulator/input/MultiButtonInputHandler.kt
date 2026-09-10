package me.magnum.melonds.ui.emulator.input

import android.view.MotionEvent
import android.view.View
import me.magnum.melonds.common.vibration.TouchVibrator
import me.magnum.melonds.domain.model.Input

abstract class MultiButtonInputHandler(inputListener: IInputListener, enableHapticFeedback: Boolean, touchVibrator: TouchVibrator) : FeedbackInputHandler(inputListener, enableHapticFeedback, touchVibrator) {
    private var areDimensionsInitialized = false
    private var buttonCircles: List<ButtonCircle> = emptyList()
    private val pressedInputs = mutableListOf<Input>()
    private val newPressedInputs = mutableListOf<Input>()
    // Reusable input list to avoid memory allocations
    private val tempInputList = mutableListOf<Input>()
    // Reusable pointer list, rebuilt from the MotionEvent on every call. See
    // computePressedInputs (MultiButtonInputLogic.kt) for the pure classification logic.
    private val activePointers = mutableListOf<PointerPosition>()

    override fun onTouch(v: View, event: MotionEvent): Boolean {
        if (!areDimensionsInitialized) {
            buttonCircles = buildButtonCircles(v.width, v.height, getTopInput(), getLeftInput(), getBottomInput(), getRightInput())
            areDimensionsInitialized = true
        }

        activePointers.clear()

        // event.actionMasked (not the raw event.action) strips the pointer index that
        // ACTION_POINTER_DOWN/ACTION_POINTER_UP encode into the action value. Comparing the raw
        // event.action, as before, meant a second finger's ACTION_POINTER_DOWN never matched
        // ACTION_DOWN/ACTION_MOVE at all (MELONDS-DOCS/DUE-DITA.md, first defect).
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN, MotionEvent.ACTION_MOVE -> {
                // Every currently-down pointer matters here, not just index 0 (the second
                // defect): event.x/event.y are the index-0-only overloads and silently ignore
                // every other finger. event.getX(i)/getY(i) over the full pointerCount is what
                // makes real two-finger input -- including opposite buttons -- possible.
                for (i in 0 until event.pointerCount) {
                    activePointers.add(PointerPosition(event.getX(i), event.getY(i)))
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP -> {
                // The departing pointer is still reported by getPointerCount()/getX(i)/getY(i) at
                // this point; event.actionIndex says which one it is. Excluding only that pointer
                // -- not every pointer -- is what lets a second finger stay pressed when the
                // first one lifts, instead of releasing everything.
                val liftingIndex = event.actionIndex
                for (i in 0 until event.pointerCount) {
                    if (i != liftingIndex) {
                        activePointers.add(PointerPosition(event.getX(i), event.getY(i)))
                    }
                }
            }
            // ACTION_CANCEL (the system reclaiming the gesture -- a parent intercepting it, the
            // window losing focus, etc.) and any other action we don't explicitly track: none of
            // our pointers are down any more, so activePointers stays empty and every button is
            // released below. This is the branch every earlier report of "buttons get stuck
            // pressed" was missing.
            else -> {}
        }

        newPressedInputs.clear()
        newPressedInputs.addAll(computePressedInputs(buttonCircles, activePointers))

        tempInputList.clear()
        pressedInputs.filterNotTo(tempInputList) {
            it in newPressedInputs
        }.forEach {
            inputListener.onKeyReleased(it)
        }

        if (tempInputList.isNotEmpty()) {
            performHapticFeedback(v, HapticFeedbackType.KEY_RELEASE)
        }

        tempInputList.clear()
        newPressedInputs.filterNotTo(tempInputList) {
            it in pressedInputs
        }.forEach {
            inputListener.onKeyPress(it)
        }

        if (tempInputList.isNotEmpty()) {
            performHapticFeedback(v, HapticFeedbackType.KEY_PRESS)
        }

        pressedInputs.clear()
        pressedInputs.addAll(newPressedInputs)

        return true
    }

    abstract fun getTopInput(): Input
    abstract fun getLeftInput(): Input
    abstract fun getBottomInput(): Input
    abstract fun getRightInput(): Input
}
