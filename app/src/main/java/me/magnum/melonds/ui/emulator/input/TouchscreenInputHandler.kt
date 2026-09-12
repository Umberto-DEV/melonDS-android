package me.magnum.melonds.ui.emulator.input

import android.annotation.SuppressLint
import android.view.MotionEvent
import android.view.MotionEvent.PointerCoords
import android.view.View
import me.magnum.melonds.domain.model.Input
import me.magnum.melonds.domain.model.Point

class TouchscreenInputHandler(inputListener: IInputListener) : BaseInputHandler(inputListener) {
    private val touchPoint: Point = Point()
    private val pointerCoordinates = PointerCoords()
    private var isStylusDown = false

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouch(v: View, event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                // Avoids per-frame batching of ACTION_MOVE while dragging. The MotionEvent overload
                // is API 21+; the Int one requires API 30+ and crashes below it (minSdkVersion 24).
                v.requestUnbufferedDispatch(event)
                isStylusDown = true
                inputListener.onKeyPress(Input.TOUCHSCREEN)
                inputListener.onTouch(normalizeTouchCoordinates(event, v.width, v.height))
            }
            MotionEvent.ACTION_MOVE -> {
                inputListener.onTouch(normalizeTouchCoordinates(event, v.width, v.height))
            }
            // ACTION_CANCEL means the system took the gesture away: without it the stylus would
            // stay down in the emulator forever.
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                releaseStylus()
            }
        }
        return true
    }

    override fun releaseAll() {
        releaseStylus()
    }

    private fun releaseStylus() {
        if (isStylusDown) {
            isStylusDown = false
            inputListener.onKeyReleased(Input.TOUCHSCREEN)
            inputListener.onScreenRelease()
        }
    }

    private fun normalizeTouchCoordinates(event: MotionEvent, viewWidth: Int, viewHeight: Int): Point {
        var averageTouchX = 0f
        var averageTouchY = 0f

        // Average out touch positions. Even though the DS has a resistive touch screen, some games rely on the nuances
        // of this technology for some mechanics. Averaging out the coordinates of the touch position allows us to
        // simulate those nuances to some degree
        for (i in 0 until event.pointerCount) {
            event.getPointerCoords(i, pointerCoordinates)
            averageTouchX += pointerCoordinates.x
            averageTouchY += pointerCoordinates.y
        }
        averageTouchX /= event.pointerCount
        averageTouchY /= event.pointerCount

        touchPoint.x = (averageTouchX / viewWidth * 256).toInt().coerceIn(0, 255)
        touchPoint.y = (averageTouchY / viewHeight * 192).toInt().coerceIn(0, 191)
        return touchPoint
    }
}
