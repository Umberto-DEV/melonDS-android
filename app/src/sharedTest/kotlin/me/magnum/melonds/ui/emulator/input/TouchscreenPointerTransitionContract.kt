package me.magnum.melonds.ui.emulator.input

import android.view.MotionEvent
import org.junit.Assert.assertEquals
import org.junit.Test

/** Same real MotionEvent transitions on Robolectric and Android ART. */
abstract class TouchscreenPointerTransitionContract {
    private val listener = RecordingInputListener()
    private val view = testView().apply { layout(0, 0, 256, 192) }
    private val handler = TouchscreenInputHandler(listener)

    @Test
    fun secondFingerImmediatelyUpdatesPosition() {
        handler.onTouch(view, touchEvent(MotionEvent.ACTION_DOWN, 64f, 96f))
        listener.events.clear()

        handler.onTouch(view, touchEvent(MotionEvent.ACTION_POINTER_DOWN, listOf(64f to 96f, 192f to 96f), actionIndex = 1))

        assertEquals(listOf(RecordedInput.Touch(128, 96)), listener.events)
    }

    @Test
    fun liftingSecondFingerImmediatelyRestoresPosition() {
        handler.onTouch(view, touchEvent(MotionEvent.ACTION_DOWN, 64f, 96f))
        handler.onTouch(view, touchEvent(MotionEvent.ACTION_POINTER_DOWN, listOf(64f to 96f, 192f to 96f), actionIndex = 1))
        handler.onTouch(view, touchEvent(MotionEvent.ACTION_MOVE, listOf(64f to 96f, 192f to 96f)))
        listener.events.clear()

        handler.onTouch(view, touchEvent(MotionEvent.ACTION_POINTER_UP, listOf(64f to 96f, 192f to 96f), actionIndex = 1))

        assertEquals(listOf(RecordedInput.Touch(64, 96)), listener.events)
    }

    @Test
    fun liftingFirstFingerKeepsSecondFingerPosition() {
        handler.onTouch(view, touchEvent(MotionEvent.ACTION_DOWN, 64f, 96f))
        handler.onTouch(view, touchEvent(MotionEvent.ACTION_POINTER_DOWN, listOf(64f to 96f, 192f to 96f), actionIndex = 1))
        listener.events.clear()
        handler.onTouch(view, touchEvent(MotionEvent.ACTION_POINTER_UP, listOf(64f to 96f, 192f to 96f), actionIndex = 0))
        assertEquals(listOf(RecordedInput.Touch(192, 96)), listener.events)
    }

    @Test
    fun threeFingersUpdateCentroidWithoutExtraPressOrRelease() {
        handler.onTouch(view, touchEvent(MotionEvent.ACTION_DOWN, 48f, 48f))
        handler.onTouch(view, touchEvent(MotionEvent.ACTION_POINTER_DOWN, listOf(48f to 48f, 144f to 48f), actionIndex = 1))
        handler.onTouch(view, touchEvent(MotionEvent.ACTION_POINTER_DOWN, listOf(48f to 48f, 144f to 48f, 96f to 144f), actionIndex = 2))
        handler.onTouch(view, touchEvent(MotionEvent.ACTION_POINTER_UP, listOf(48f to 48f, 144f to 48f, 96f to 144f), actionIndex = 1))
        handler.onTouch(view, touchEvent(MotionEvent.ACTION_CANCEL, 72f, 96f))
        assertEquals(listOf(
            RecordedInput.Press(me.magnum.melonds.domain.model.Input.TOUCHSCREEN),
            RecordedInput.Touch(48, 48), RecordedInput.Touch(96, 48),
            RecordedInput.Touch(96, 80), RecordedInput.Touch(72, 96),
            RecordedInput.Release(me.magnum.melonds.domain.model.Input.TOUCHSCREEN),
            RecordedInput.ScreenRelease,
        ), listener.events)
    }
}
