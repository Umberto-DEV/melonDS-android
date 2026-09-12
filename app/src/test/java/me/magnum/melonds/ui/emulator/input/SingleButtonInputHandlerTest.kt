package me.magnum.melonds.ui.emulator.input

import android.app.Application
import android.view.MotionEvent
import me.magnum.melonds.domain.model.Input
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class SingleButtonInputHandlerTest {

    private val listener = RecordingInputListener()
    private val view = testView()
    private val handler = SingleButtonInputHandler(listener, Input.L, false, null)

    @Test
    fun `a press and a release are reported once each`() {
        handler.onTouch(view, touchEvent(MotionEvent.ACTION_DOWN, 10f, 10f))
        handler.onTouch(view, touchEvent(MotionEvent.ACTION_UP, 10f, 10f))

        assertEquals(listOf(RecordedInput.Press(Input.L), RecordedInput.Release(Input.L)), listener.events)
    }

    @Test
    fun `a cancelled gesture releases the button`() {
        // The defect: with ACTION_CANCEL unhandled the button stays down in the emulator forever.
        handler.onTouch(view, touchEvent(MotionEvent.ACTION_DOWN, 10f, 10f))
        listener.events.clear()

        handler.onTouch(view, touchEvent(MotionEvent.ACTION_CANCEL, 10f, 10f))

        assertEquals(listOf(RecordedInput.Release(Input.L)), listener.events)
    }

    @Test
    fun `a second finger lifting off the same button does not release it`() {
        handler.onTouch(view, touchEvent(MotionEvent.ACTION_DOWN, 10f, 10f))
        handler.onTouch(view, touchEvent(MotionEvent.ACTION_POINTER_DOWN, listOf(10f to 10f, 20f to 20f), actionIndex = 1))
        handler.onTouch(view, touchEvent(MotionEvent.ACTION_POINTER_UP, listOf(10f to 10f, 20f to 20f), actionIndex = 1))

        assertEquals(listOf(RecordedInput.Press(Input.L)), listener.events)

        handler.onTouch(view, touchEvent(MotionEvent.ACTION_UP, 10f, 10f))

        assertEquals(listOf(RecordedInput.Press(Input.L), RecordedInput.Release(Input.L)), listener.events)
    }

    @Test
    fun `a cancel with no press before it emits nothing`() {
        handler.onTouch(view, touchEvent(MotionEvent.ACTION_CANCEL, 10f, 10f))

        assertEquals(emptyList<RecordedInput>(), listener.events)
    }

    @Test
    fun `releaseAll releases a held button, and only once`() {
        handler.onTouch(view, touchEvent(MotionEvent.ACTION_DOWN, 10f, 10f))
        listener.events.clear()

        handler.releaseAll()
        handler.releaseAll()

        assertEquals(listOf(RecordedInput.Release(Input.L)), listener.events)
    }
}
