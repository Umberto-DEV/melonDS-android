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
 * The ABXY pad: top = X, left = Y, bottom = B, right = A.
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class MultiButtonInputHandlerTest {

    private val listener = RecordingInputListener()
    private val view = testView()
    private val handler = ButtonsInputHandler(listener, false, null)

    @Test
    fun `one finger presses only the button it sits on`() {
        handler.onTouch(view, touchEvent(MotionEvent.ACTION_DOWN, listOf(RIGHT_POINT)))

        assertEquals(listOf(RecordedInput.Press(Input.A)), listener.events)
    }

    @Test
    fun `a finger between the buttons presses nothing`() {
        handler.onTouch(view, touchEvent(MotionEvent.ACTION_DOWN, listOf(CENTRE_POINT)))

        assertEquals(emptyList<RecordedInput>(), listener.events)
    }

    @Test
    fun `two fingers on two buttons press both`() {
        handler.onTouch(view, touchEvent(MotionEvent.ACTION_DOWN, listOf(RIGHT_POINT)))
        handler.onTouch(view, touchEvent(MotionEvent.ACTION_POINTER_DOWN, listOf(RIGHT_POINT, BOTTOM_POINT), actionIndex = 1))

        assertEquals(setOf(RecordedInput.Press(Input.A), RecordedInput.Press(Input.B)), listener.events.toSet())
        assertEquals(2, listener.events.size)
    }

    @Test
    fun `lifting one finger keeps the other button pressed`() {
        handler.onTouch(view, touchEvent(MotionEvent.ACTION_DOWN, listOf(RIGHT_POINT)))
        handler.onTouch(view, touchEvent(MotionEvent.ACTION_POINTER_DOWN, listOf(RIGHT_POINT, LEFT_POINT), actionIndex = 1))
        handler.onTouch(view, touchEvent(MotionEvent.ACTION_POINTER_UP, listOf(RIGHT_POINT, LEFT_POINT), actionIndex = 0))

        assertEquals(
            listOf(RecordedInput.Press(Input.A), RecordedInput.Press(Input.Y), RecordedInput.Release(Input.A)),
            listener.events,
        )
    }

    @Test
    fun `a cancelled gesture releases every pressed button`() {
        handler.onTouch(view, touchEvent(MotionEvent.ACTION_DOWN, listOf(RIGHT_POINT)))
        handler.onTouch(view, touchEvent(MotionEvent.ACTION_POINTER_DOWN, listOf(RIGHT_POINT, LEFT_POINT), actionIndex = 1))
        listener.events.clear()

        handler.onTouch(view, touchEvent(MotionEvent.ACTION_CANCEL, listOf(RIGHT_POINT, LEFT_POINT)))

        assertEquals(setOf(RecordedInput.Release(Input.A), RecordedInput.Release(Input.Y)), listener.events.toSet())
        assertEquals(2, listener.events.size)
    }

    @Test
    fun `repeated moves on the same button do not repeat the press`() {
        handler.onTouch(view, touchEvent(MotionEvent.ACTION_DOWN, listOf(RIGHT_POINT)))
        handler.onTouch(view, touchEvent(MotionEvent.ACTION_MOVE, listOf(RIGHT_POINT)))
        handler.onTouch(view, touchEvent(MotionEvent.ACTION_MOVE, listOf(498f to 250f)))

        assertEquals(listOf(RecordedInput.Press(Input.A)), listener.events)
    }

    @Test
    fun `a finger sliding to another button swaps the pressed button`() {
        handler.onTouch(view, touchEvent(MotionEvent.ACTION_DOWN, listOf(TOP_POINT)))
        handler.onTouch(view, touchEvent(MotionEvent.ACTION_MOVE, listOf(BOTTOM_POINT)))

        assertEquals(
            listOf(RecordedInput.Press(Input.X), RecordedInput.Release(Input.X), RecordedInput.Press(Input.B)),
            listener.events,
        )
    }

    @Test
    fun `releaseAll releases every pressed button, and only once`() {
        handler.onTouch(view, touchEvent(MotionEvent.ACTION_DOWN, listOf(RIGHT_POINT)))
        handler.onTouch(view, touchEvent(MotionEvent.ACTION_POINTER_DOWN, listOf(RIGHT_POINT, LEFT_POINT), actionIndex = 1))
        listener.events.clear()

        handler.releaseAll()
        handler.releaseAll()

        assertEquals(setOf(RecordedInput.Release(Input.A), RecordedInput.Release(Input.Y)), listener.events.toSet())
        assertEquals(2, listener.events.size)
    }

    @Test
    fun `the hit circles follow the view when it is resized`() {
        handler.onTouch(view, touchEvent(MotionEvent.ACTION_DOWN, listOf(RIGHT_POINT)))
        handler.onTouch(view, touchEvent(MotionEvent.ACTION_UP, listOf(RIGHT_POINT)))
        listener.events.clear()

        // Same handler, view now half the size: the old circles would put this point nowhere near
        // the right button (a layout change or a rotation does exactly this).
        view.layout(0, 0, TEST_VIEW_SIZE / 2, TEST_VIEW_SIZE / 2)
        handler.onTouch(view, touchEvent(MotionEvent.ACTION_DOWN, listOf(250f to 128f)))

        assertEquals(listOf(RecordedInput.Press(Input.A)), listener.events)
    }
}
