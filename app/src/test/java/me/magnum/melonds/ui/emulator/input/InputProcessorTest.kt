package me.magnum.melonds.ui.emulator.input

import android.app.Application
import android.view.InputDevice
import android.view.KeyEvent
import android.view.MotionEvent
import me.magnum.melonds.domain.model.ControllerConfiguration
import me.magnum.melonds.domain.model.Input
import me.magnum.melonds.domain.model.InputConfig
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class InputProcessorTest {
    private val systemListener = RecordingInputListener()
    private val frontendListener = RecordingInputListener()

    @Test
    fun `focus loss releases a held combo and allows the same combo again`() {
        val processor = processor(combo(Input.FAST_FORWARD_HOLD))
        pressCombo(processor)
        frontendListener.events.clear()

        processor.clearPressedKeys()
        pressCombo(processor)

        assertEquals(
            listOf(RecordedInput.Release(Input.FAST_FORWARD_HOLD), RecordedInput.Press(Input.FAST_FORWARD_HOLD)),
            frontendListener.events,
        )
    }

    @Test
    fun `focus loss also releases a combo assigned to a DS button`() {
        val processor = processor(combo(Input.A))
        pressCombo(processor)
        systemListener.events.clear()

        processor.clearPressedKeys()

        assertEquals(listOf(RecordedInput.Release(Input.A)), systemListener.events)
    }

    @Test
    fun `focus loss releases a simple physical key and a fresh press works`() {
        val processor = processor(InputConfig(Input.A, InputConfig.Assignment.Key(null, KeyEvent.KEYCODE_BUTTON_A)))
        processor.onKeyEvent(keyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_BUTTON_A))
        systemListener.events.clear()

        processor.clearPressedKeys()
        processor.onKeyEvent(keyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_BUTTON_A))
        processor.onKeyEvent(keyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_BUTTON_A))

        assertEquals(
            listOf(RecordedInput.Release(Input.A), RecordedInput.Press(Input.A), RecordedInput.Release(Input.A)),
            systemListener.events,
        )
    }

    @Test
    fun `focus loss releases and resets a held axis so the next deflection presses again`() {
        val processor = processor(
            InputConfig(Input.RIGHT, InputConfig.Assignment.Axis(null, MotionEvent.AXIS_X, InputConfig.Assignment.Axis.Direction.POSITIVE)),
        )
        dispatchAxis(processor, 0.8f)
        systemListener.events.clear()

        processor.clearPressedKeys()
        dispatchAxis(processor, 0.8f)
        dispatchAxis(processor, 0f)

        assertEquals(
            listOf(RecordedInput.Release(Input.RIGHT), RecordedInput.Press(Input.RIGHT), RecordedInput.Release(Input.RIGHT)),
            systemListener.events,
        )
    }

    @Test
    fun `repeated cleanup and late key ups do not release an input twice`() {
        val processor = processor(
            InputConfig(Input.A, InputConfig.Assignment.Key(null, KeyEvent.KEYCODE_BUTTON_A)),
            combo(Input.FAST_FORWARD_HOLD),
        )
        processor.onKeyEvent(keyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_BUTTON_A))
        pressCombo(processor)
        systemListener.events.clear()
        frontendListener.events.clear()

        processor.clearPressedKeys()
        processor.clearPressedKeys()

        assertEquals(listOf(RecordedInput.Release(Input.A)), systemListener.events)
        assertEquals(listOf(RecordedInput.Release(Input.FAST_FORWARD_HOLD)), frontendListener.events)

        processor.onKeyEvent(keyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_BUTTON_A))
        processor.onKeyEvent(keyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_BUTTON_L1))
        processor.onKeyEvent(keyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_BUTTON_R1))

        assertEquals(listOf(RecordedInput.Release(Input.A)), systemListener.events)
        assertEquals(listOf(RecordedInput.Release(Input.FAST_FORWARD_HOLD)), frontendListener.events)
    }

    @Test
    fun `cleanup does not release an input already released normally`() {
        val processor = processor(InputConfig(Input.A, InputConfig.Assignment.Key(null, KeyEvent.KEYCODE_BUTTON_A)))
        processor.onKeyEvent(keyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_BUTTON_A))
        processor.onKeyEvent(keyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_BUTTON_A))
        systemListener.events.clear()

        processor.clearPressedKeys()

        assertEquals(emptyList<RecordedInput>(), systemListener.events)
    }

    @Test
    fun `a modifier from before focus loss cannot complete a combo afterward`() {
        val processor = processor(combo(Input.FAST_FORWARD_HOLD))
        processor.onKeyEvent(keyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_BUTTON_L1))

        processor.clearPressedKeys()
        processor.onKeyEvent(keyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_BUTTON_R1))

        assertEquals(emptyList<RecordedInput>(), frontendListener.events)
    }

    private fun processor(vararg inputs: InputConfig): InputProcessor =
        InputProcessor(ControllerConfiguration(inputs.toList()), systemListener, frontendListener)

    private fun combo(input: Input) = InputConfig(
        input,
        InputConfig.Assignment.Key(null, KeyEvent.KEYCODE_BUTTON_R1, listOf(KeyEvent.KEYCODE_BUTTON_L1)),
    )

    private fun pressCombo(processor: InputProcessor) {
        processor.onKeyEvent(keyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_BUTTON_L1))
        processor.onKeyEvent(keyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_BUTTON_R1))
    }

    private fun keyEvent(action: Int, keyCode: Int) =
        KeyEvent(0, 0, action, keyCode, 0, 0, 7, 0, 0, InputDevice.SOURCE_GAMEPAD)

    private fun dispatchAxis(processor: InputProcessor, x: Float) {
        val properties = arrayOf(MotionEvent.PointerProperties().apply { id = 0 })
        val coords = arrayOf(MotionEvent.PointerCoords().apply { setAxisValue(MotionEvent.AXIS_X, x) })
        val event = MotionEvent.obtain(
            0, 0, MotionEvent.ACTION_MOVE, 1, properties, coords,
            0, 0, 1f, 1f, 7, 0, InputDevice.SOURCE_JOYSTICK, 0,
        )
        try {
            processor.onMotionEvent(event)
        } finally {
            event.recycle()
        }
    }
}
