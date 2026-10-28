package me.magnum.melonds.ui.emulator

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class EmulatorSessionCoroutineScopeTest {
    @Test fun `no emulator work runs before the first validated session`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val scope = EmulatorSessionCoroutineScope()
        try {
            var effects = 0
            scope.launch { effects++ }.join()
            assertEquals(0, effects)
        } finally {
            scope.cancel()
            Dispatchers.resetMain()
        }
    }

    @Test fun `replacing a session cancels its suspended effects but permits new work`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val scope = EmulatorSessionCoroutineScope()
        try {
            val gate = CompletableDeferred<Unit>()
            var effects = 0
            scope.notifyNewSessionStarted()
            scope.launch { gate.await(); effects += 100 }
            runCurrent()
            scope.notifyNewSessionStarted()
            gate.complete(Unit)
            scope.launch { effects++ }
            runCurrent()
            assertEquals(1, effects)
        } finally {
            scope.cancel()
            Dispatchers.resetMain()
        }
    }

    @Test fun `cleanup cancels pending work and rejects later work until reactivation`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val scope = EmulatorSessionCoroutineScope()
        try {
            val gate = CompletableDeferred<Unit>()
            var effects = 0
            scope.notifyNewSessionStarted()
            scope.launch { gate.await(); effects++ }
            runCurrent()
            scope.cancel()
            gate.complete(Unit)
            scope.launch { effects++ }
            runCurrent()
            assertEquals(0, effects)
        } finally {
            scope.cancel()
            Dispatchers.resetMain()
        }
    }
}
