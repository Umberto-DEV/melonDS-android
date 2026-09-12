package me.magnum.melonds.ui.emulator.input

import me.magnum.melonds.domain.model.Input
import me.magnum.melonds.domain.model.Point

interface IInputListener {
    fun onKeyPress(key: Input)
    fun onKeyReleased(key: Input)
    fun onTouch(point: Point)

    /**
     * The stylus left the touch screen. Part of the listener contract (instead of a direct call
     * into the native emulator) so that touch handlers can be exercised without loading the JNI
     * library.
     */
    fun onScreenRelease()
}