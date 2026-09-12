package me.magnum.melonds.ui.emulator.input

import android.view.MotionEvent
import android.view.View
import me.magnum.melonds.common.vibration.TouchVibrator
import me.magnum.melonds.domain.model.Input

abstract class MultiButtonInputHandler(inputListener: IInputListener, enableHapticFeedback: Boolean, touchVibrator: TouchVibrator?) : FeedbackInputHandler(inputListener, enableHapticFeedback, touchVibrator) {

    private companion object {
        const val INITIAL_POINTER_CAPACITY = 4
    }

    private var buttonCircles: Array<ButtonCircle> = emptyArray()
    private var circlesViewWidth = 0
    private var circlesViewHeight = 0

    // Pointer coordinates and the pressed button set are kept in preallocated primitives, so that
    // a touch event - which arrives for every finger movement - allocates nothing.
    private var pointerX = FloatArray(INITIAL_POINTER_CAPACITY)
    private var pointerY = FloatArray(INITIAL_POINTER_CAPACITY)
    private var pressedMask = 0

    override fun onTouch(v: View, event: MotionEvent): Boolean {
        // Rebuilt whenever the view is resized (layout change, rotation): the circles are derived
        // from the view's pixel size, and the handler outlives a single size.
        if (v.width != circlesViewWidth || v.height != circlesViewHeight) {
            circlesViewWidth = v.width
            circlesViewHeight = v.height
            buttonCircles = buildButtonCircles(v.width, v.height, getTopInput(), getLeftInput(), getBottomInput(), getRightInput())
        }

        var activePointers = 0
        // event.actionMasked, not event.action: the raw action of ACTION_POINTER_DOWN/UP has the
        // pointer index packed into it and matches none of these constants.
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN, MotionEvent.ACTION_MOVE -> {
                ensurePointerCapacity(event.pointerCount)
                for (i in 0 until event.pointerCount) {
                    pointerX[activePointers] = event.getX(i)
                    pointerY[activePointers] = event.getY(i)
                    activePointers++
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP -> {
                // The departing pointer is still reported here; event.actionIndex says which one.
                // Excluding only that one is what lets a second finger stay pressed.
                ensurePointerCapacity(event.pointerCount)
                val liftingIndex = event.actionIndex
                for (i in 0 until event.pointerCount) {
                    if (i != liftingIndex) {
                        pointerX[activePointers] = event.getX(i)
                        pointerY[activePointers] = event.getY(i)
                        activePointers++
                    }
                }
            }
            // ACTION_CANCEL (the system reclaiming the gesture) and anything else we don't track:
            // no pointer is down any more, so every button is released below.
            else -> {}
        }

        applyPressedMask(filterInputs(computePressedMask(buttonCircles, pointerX, pointerY, activePointers)), v)
        return true
    }

    override fun releaseAll() {
        applyPressedMask(0, null)
    }

    /**
     * Hook for subclasses that need to reject impossible combinations. Returns the mask unchanged
     * by default.
     */
    protected open fun filterInputs(mask: Int): Int = mask

    private fun ensurePointerCapacity(pointerCount: Int) {
        if (pointerCount > pointerX.size) {
            pointerX = FloatArray(pointerCount)
            pointerY = FloatArray(pointerCount)
        }
    }

    private fun applyPressedMask(newMask: Int, view: View?) {
        val releasedMask = pressedMask and newMask.inv()
        val newlyPressedMask = newMask and pressedMask.inv()
        pressedMask = newMask

        if (releasedMask != 0) {
            notifyInputs(releasedMask, press = false)
            // No haptic feedback on releaseAll: it is not a user gesture.
            view?.let { performHapticFeedback(it, HapticFeedbackType.KEY_RELEASE) }
        }
        if (newlyPressedMask != 0) {
            notifyInputs(newlyPressedMask, press = true)
            view?.let { performHapticFeedback(it, HapticFeedbackType.KEY_PRESS) }
        }
    }

    private fun notifyInputs(mask: Int, press: Boolean) {
        for (i in buttonCircles.indices) {
            val input = buttonCircles[i].input
            if (mask and inputMask(input) != 0) {
                if (press) {
                    inputListener.onKeyPress(input)
                } else {
                    inputListener.onKeyReleased(input)
                }
            }
        }
    }

    abstract fun getTopInput(): Input
    abstract fun getLeftInput(): Input
    abstract fun getBottomInput(): Input
    abstract fun getRightInput(): Input
}
