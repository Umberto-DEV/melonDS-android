package me.magnum.melonds.ui.emulator.input

import android.view.View.OnTouchListener

abstract class BaseInputHandler(protected var inputListener: IInputListener) : OnTouchListener {

    /**
     * Releases every input this handler currently holds down, without any haptic feedback (this
     * is not a user gesture). Called when the handler is about to be detached from its view, so
     * that a layout change cannot leave a key stuck down in the emulator.
     */
    open fun releaseAll() {
    }
}