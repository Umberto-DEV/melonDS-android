package me.magnum.melonds.ui.emulator.input

import android.app.Application
import android.view.MotionEvent
import me.magnum.melonds.domain.model.Input
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * The D-pad: top = UP, left = LEFT, bottom = DOWN, right = RIGHT.
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class DpadInputHandlerTest {

    private val listener = RecordingInputListener()
    private val view = testView()
    private val handler = DpadInputHandler(listener, false, null)

    @Test
    fun `two adjacent directions still produce a diagonal`() {
        handler.onTouch(view, touchEvent(MotionEvent.ACTION_DOWN, listOf(TOP_LEFT_POINT)))

        assertEquals(setOf(RecordedInput.Press(Input.UP), RecordedInput.Press(Input.LEFT)), listener.events.toSet())
        assertEquals(2, listener.events.size)
    }

    @Test
    fun `up and down at the same time produce no direction at all`() {
        // The defect: a real D-pad cannot report both, and two fingers used to send UP+DOWN.
        handler.onTouch(view, touchEvent(MotionEvent.ACTION_DOWN, listOf(TOP_POINT)))
        assertEquals(listOf(RecordedInput.Press(Input.UP)), listener.events)

        handler.onTouch(view, touchEvent(MotionEvent.ACTION_POINTER_DOWN, listOf(TOP_POINT, BOTTOM_POINT), actionIndex = 1))

        assertEquals(
            listOf(RecordedInput.Press(Input.UP), RecordedInput.Release(Input.UP)),
            listener.events,
        )
    }

    @Test
    fun `left and right at the same time produce no direction at all`() {
        handler.onTouch(view, touchEvent(MotionEvent.ACTION_DOWN, listOf(LEFT_POINT)))
        listener.events.clear()

        handler.onTouch(view, touchEvent(MotionEvent.ACTION_POINTER_DOWN, listOf(LEFT_POINT, RIGHT_POINT), actionIndex = 1))

        assertEquals(listOf(RecordedInput.Release(Input.LEFT)), listener.events)
    }

    @Test
    fun `lifting one of two opposite fingers restores the other direction`() {
        handler.onTouch(view, touchEvent(MotionEvent.ACTION_DOWN, listOf(TOP_POINT)))
        handler.onTouch(view, touchEvent(MotionEvent.ACTION_POINTER_DOWN, listOf(TOP_POINT, BOTTOM_POINT), actionIndex = 1))
        listener.events.clear()

        handler.onTouch(view, touchEvent(MotionEvent.ACTION_POINTER_UP, listOf(TOP_POINT, BOTTOM_POINT), actionIndex = 1))

        assertEquals(listOf(RecordedInput.Press(Input.UP)), listener.events)
    }

    @Test
    fun `an axis conflict does not cancel the other axis`() {
        // UP + DOWN + LEFT: only the vertical pair is dropped.
        handler.onTouch(view, touchEvent(MotionEvent.ACTION_DOWN, listOf(TOP_POINT, BOTTOM_POINT, LEFT_POINT)))

        assertEquals(listOf(RecordedInput.Press(Input.LEFT)), listener.events)
    }
}
