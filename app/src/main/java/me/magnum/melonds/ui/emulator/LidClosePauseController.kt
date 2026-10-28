package me.magnum.melonds.ui.emulator

import android.os.Handler

/** Owns the brief sound grace period after screen-off, independently of visible surfaces. */
internal class LidClosePauseController(
    private val handler: Handler,
    private val pause: () -> Unit,
    private val stopRendering: () -> Unit,
    private val startGracePeriod: () -> Unit,
    private val finishGracePeriod: () -> Unit,
    private val delayMillis: Long = 3_000,
) {
    private var pending = false
    private var destroyed = false
    private val pauseAfterLidClose = Runnable {
        if (pending && !destroyed) pauseNow()
    }

    fun schedulePause() {
        if (pending || destroyed) return
        pending = true
        startGracePeriod()
        handler.postDelayed(pauseAfterLidClose, delayMillis)
    }

    fun cancelPendingPause() {
        pending = false
        handler.removeCallbacks(pauseAfterLidClose)
        finishGracePeriod()
    }

    fun pauseNow() {
        if (destroyed) return
        pending = false
        handler.removeCallbacks(pauseAfterLidClose)
        try {
            stopRendering()
            pause()
        } finally {
            finishGracePeriod()
        }
    }

    fun onStopped() {
        stopRendering()
        // Removing visible surfaces does not cancel the obligation to pause native emulation.
        // Keep the original deadline so games can finish their lid-close sound.
    }

    fun destroy() {
        if (destroyed) return
        if (pending) pauseNow() else cancelPendingPause()
        destroyed = true
    }
}
