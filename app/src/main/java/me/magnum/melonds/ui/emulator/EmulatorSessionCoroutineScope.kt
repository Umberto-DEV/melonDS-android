package me.magnum.melonds.ui.emulator

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlin.coroutines.CoroutineContext

internal class EmulatorSessionCoroutineScope : CoroutineScope {
    // Firmware validation can precede the first session. Never launch unowned Default jobs.
    private var currentCoroutineContext: CoroutineContext =
        SupervisorJob().apply { cancel() } + Dispatchers.Main.immediate

    override val coroutineContext: CoroutineContext get() = currentCoroutineContext

    fun notifyNewSessionStarted() {
        cancel()
        currentCoroutineContext = SupervisorJob() + Dispatchers.Main.immediate
    }

    fun cancel() {
        currentCoroutineContext.cancel()
    }
}
