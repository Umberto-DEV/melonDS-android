package me.magnum.melonds.ui.emulator.input

import android.content.Context
import android.os.SystemClock
import android.view.InputDevice
import android.view.MotionEvent
import android.view.View
import androidx.test.core.app.ApplicationProvider
import me.magnum.melonds.domain.model.Input
import me.magnum.melonds.domain.model.Point

/**
 * What a handler told its listener, in order. [Point] is reused by the handlers, so the touch
 * coordinates are copied out immediately.
 */
sealed interface RecordedInput {
    data class Press(val input: Input) : RecordedInput
    data class Release(val input: Input) : RecordedInput
    data class Touch(val x: Int, val y: Int) : RecordedInput
    data object ScreenRelease : RecordedInput
}

class RecordingInputListener : IInputListener {
    val events = mutableListOf<RecordedInput>()

    val keyEvents: List<RecordedInput>
        get() = events.filter { it is RecordedInput.Press || it is RecordedInput.Release }

    override fun onKeyPress(key: Input) {
        events += RecordedInput.Press(key)
    }

    override fun onKeyReleased(key: Input) {
        events += RecordedInput.Release(key)
    }

    override fun onTouch(point: Point) {
        events += RecordedInput.Touch(point.x, point.y)
    }

    override fun onScreenRelease() {
        events += RecordedInput.ScreenRelease
    }
}

const val TEST_VIEW_SIZE = 512

fun testView(size: Int = TEST_VIEW_SIZE): View {
    val context = ApplicationProvider.getApplicationContext<Context>()
    return View(context).apply { layout(0, 0, size, size) }
}

/**
 * Builds a real multi-pointer [MotionEvent], the way the framework delivers it: [actionIndex] is
 * packed into the action for ACTION_POINTER_DOWN/ACTION_POINTER_UP, and every pointer that is
 * still down (the departing one included, as the framework does) is listed in [points].
 */
fun touchEvent(action: Int, points: List<Pair<Float, Float>>, actionIndex: Int = 0): MotionEvent {
    val properties = Array(points.size) { i ->
        MotionEvent.PointerProperties().apply {
            id = i
            toolType = MotionEvent.TOOL_TYPE_FINGER
        }
    }
    val coordinates = Array(points.size) { i ->
        MotionEvent.PointerCoords().apply {
            x = points[i].first
            y = points[i].second
            pressure = 1f
            size = 1f
        }
    }
    val packedAction = action or (actionIndex shl MotionEvent.ACTION_POINTER_INDEX_SHIFT)
    val time = SystemClock.uptimeMillis()
    return MotionEvent.obtain(time, time, packedAction, points.size, properties, coordinates, 0, 0, 1f, 1f, 0, 0, InputDevice.SOURCE_TOUCHSCREEN, 0)
}

fun touchEvent(action: Int, x: Float, y: Float): MotionEvent = touchEvent(action, listOf(x to y))

// Points inside each button circle of a 512x512 four-button view, comfortably outside every other
// circle. See buildButtonCircles: the circles sit just beyond each edge, with radius = width / 2.
val TOP_POINT = 256f to 12f
val BOTTOM_POINT = 256f to 500f
val LEFT_POINT = 12f to 256f
val RIGHT_POINT = 500f to 256f

// Inside the top and the left circle at the same time: a real diagonal.
val TOP_LEFT_POINT = 100f to 100f

// Dead centre of the view: inside no circle at all.
val CENTRE_POINT = 256f to 256f
