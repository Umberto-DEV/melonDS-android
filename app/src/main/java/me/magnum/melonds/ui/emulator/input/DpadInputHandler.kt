package me.magnum.melonds.ui.emulator.input

import me.magnum.melonds.common.vibration.TouchVibrator
import me.magnum.melonds.domain.model.Input

class DpadInputHandler(inputListener: IInputListener, enableHapticFeedback: Boolean, touchVibrator: TouchVibrator?) : MultiButtonInputHandler(inputListener, enableHapticFeedback, touchVibrator) {
    override fun getTopInput() = Input.UP
    override fun getLeftInput() = Input.LEFT
    override fun getBottomInput() = Input.DOWN
    override fun getRightInput() = Input.RIGHT

    // Two fingers can cover UP and DOWN (or LEFT and RIGHT) at once, which a physical D-pad can
    // never report; games do not expect it, so the conflicting axis is dropped instead of sent.
    override fun filterInputs(mask: Int) = removeOpposingDirections(mask)
}
