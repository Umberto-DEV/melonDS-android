package me.magnum.melonds.ui.emulator.input

import android.view.MotionEvent
import android.view.View
import me.magnum.melonds.common.vibration.TouchVibrator
import me.magnum.melonds.domain.model.Input

class SingleButtonInputHandler(inputListener: IInputListener, private val input: Input, enableHapticFeedback: Boolean, touchVibrator: TouchVibrator?) : FeedbackInputHandler(inputListener, enableHapticFeedback, touchVibrator) {
    private var isPressed = false

    override fun onTouch(v: View, event: MotionEvent): Boolean {
        // ACTION_POINTER_DOWN/UP are deliberately ignored: on a single button an extra finger adds
        // nothing, and releasing on its lift would release the button while a finger is still down.
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                if (!isPressed) {
                    isPressed = true
                    inputListener.onKeyPress(input)
                    performHapticFeedback(v, HapticFeedbackType.KEY_PRESS)
                }
            }
            // ACTION_CANCEL means the system took the gesture away: without it the button would
            // stay down in the emulator forever.
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (isPressed) {
                    isPressed = false
                    inputListener.onKeyReleased(input)
                    performHapticFeedback(v, HapticFeedbackType.KEY_RELEASE)
                }
            }
        }
        return true
    }

    override fun releaseAll() {
        if (isPressed) {
            isPressed = false
            // No haptic feedback: this is not a user gesture.
            inputListener.onKeyReleased(input)
        }
    }
}
