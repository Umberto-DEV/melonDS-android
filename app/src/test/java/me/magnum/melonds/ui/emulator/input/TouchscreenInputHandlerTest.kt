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
class TouchscreenInputHandlerTest {

    private val listener = RecordingInputListener()
    private val view = testView()
    private val handler = TouchscreenInputHandler(listener)

    @Test
    fun `a touch down presses the stylus and reports the DS coordinates`() {
        handler.onTouch(view, touchEvent(MotionEvent.ACTION_DOWN, 256f, 96f))

        assertEquals(listOf(RecordedInput.Press(Input.TOUCHSCREEN), RecordedInput.Touch(128, 36)), listener.events)
    }

    @Test
    fun `a touch up releases the stylus`() {
        handler.onTouch(view, touchEvent(MotionEvent.ACTION_DOWN, 256f, 96f))
        listener.events.clear()

        handler.onTouch(view, touchEvent(MotionEvent.ACTION_UP, 256f, 96f))

        assertEquals(listOf(RecordedInput.Release(Input.TOUCHSCREEN), RecordedInput.ScreenRelease), listener.events)
    }

    @Test
    fun `a cancelled gesture releases the stylus`() {
        // The defect: with ACTION_CANCEL unhandled the stylus stays down in the emulator forever.
        handler.onTouch(view, touchEvent(MotionEvent.ACTION_DOWN, 256f, 96f))
        listener.events.clear()

        handler.onTouch(view, touchEvent(MotionEvent.ACTION_CANCEL, 256f, 96f))

        assertEquals(listOf(RecordedInput.Release(Input.TOUCHSCREEN), RecordedInput.ScreenRelease), listener.events)
    }

    @Test
    fun `a move updates the position without pressing again`() {
        handler.onTouch(view, touchEvent(MotionEvent.ACTION_DOWN, 256f, 96f))
        handler.onTouch(view, touchEvent(MotionEvent.ACTION_MOVE, 128f, 96f))

        assertEquals(
            listOf(RecordedInput.Press(Input.TOUCHSCREEN), RecordedInput.Touch(128, 36), RecordedInput.Touch(64, 36)),
            listener.events,
        )
    }

    @Test
    fun `releaseAll releases a stylus that is still down, and only once`() {
        handler.onTouch(view, touchEvent(MotionEvent.ACTION_DOWN, 256f, 96f))
        listener.events.clear()

        handler.releaseAll()
        handler.releaseAll()

        assertEquals(listOf(RecordedInput.Release(Input.TOUCHSCREEN), RecordedInput.ScreenRelease), listener.events)
    }

    @Test
    fun `releaseAll does nothing when the stylus is not down`() {
        handler.releaseAll()

        assertEquals(emptyList<RecordedInput>(), listener.events)
    }
}
