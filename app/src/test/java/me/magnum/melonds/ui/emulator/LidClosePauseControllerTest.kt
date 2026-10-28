package me.magnum.melonds.ui.emulator

import android.app.Application
import android.os.Handler
import android.os.Looper
import java.time.Duration
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [34])
@LooperMode(LooperMode.Mode.PAUSED)
class LidClosePauseControllerTest {
    private var pauses = 0
    private var serviceRunning = false
    private var rendering = true
    private val controller = LidClosePauseController(
        Handler(Looper.getMainLooper()),
        pause = { pauses++ },
        stopRendering = { rendering = false },
        startGracePeriod = { serviceRunning = true },
        finishGracePeriod = { serviceRunning = false },
    )

    private fun advance(millis: Long) = shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(millis))

    @Test fun `stopping the activity preserves the original native pause deadline`() {
        controller.schedulePause()
        controller.onStopped()
        assertFalse(rendering)
        advance(2_999)
        assertEquals(0, pauses)
        assertTrue(serviceRunning)
        advance(1)
        assertEquals(1, pauses)
        assertFalse(serviceRunning)
    }

    @Test fun `quick resume removes the old pause`() {
        controller.schedulePause()
        advance(1_000)
        controller.cancelPendingPause()
        advance(3_000)
        assertEquals(0, pauses)
        assertFalse(serviceRunning)
    }

    @Test fun `repeated screen off does not extend the deadline or pause twice`() {
        controller.schedulePause()
        advance(1_000)
        controller.schedulePause()
        advance(2_000)
        assertEquals(1, pauses)
        advance(3_000)
        assertEquals(1, pauses)
    }

    @Test fun `destroy consumes pending pause once and cannot schedule stale work`() {
        controller.schedulePause()
        advance(1_000)
        controller.destroy()
        assertEquals(1, pauses)
        assertFalse(serviceRunning)
        controller.destroy()
        controller.schedulePause()
        advance(4_000)
        assertEquals(1, pauses)
    }

    @Test fun `destroy after resume does not pause the current session`() {
        controller.schedulePause()
        controller.cancelPendingPause()
        controller.destroy()
        advance(4_000)
        assertEquals(0, pauses)
    }

    @Test fun `immediate app switch consumes pending pause without a later duplicate`() {
        controller.schedulePause()
        controller.pauseNow()
        advance(4_000)
        assertEquals(1, pauses)
        assertFalse(serviceRunning)
    }
}
