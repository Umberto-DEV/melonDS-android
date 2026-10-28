package me.magnum.melonds.ui.emulator

import androidx.constraintlayout.widget.ConstraintLayout
import me.magnum.melonds.domain.model.FpsCounterPosition

internal fun ConstraintLayout.LayoutParams.positionFpsCounter(position: FpsCounterPosition) {
    topToTop = ConstraintLayout.LayoutParams.UNSET
    bottomToBottom = ConstraintLayout.LayoutParams.UNSET
    leftToLeft = ConstraintLayout.LayoutParams.UNSET
    rightToRight = ConstraintLayout.LayoutParams.UNSET
    when (position) {
        FpsCounterPosition.TOP_LEFT -> {
            topToTop = ConstraintLayout.LayoutParams.PARENT_ID
            leftToLeft = ConstraintLayout.LayoutParams.PARENT_ID
        }
        FpsCounterPosition.TOP_CENTER -> {
            topToTop = ConstraintLayout.LayoutParams.PARENT_ID
            leftToLeft = ConstraintLayout.LayoutParams.PARENT_ID
            rightToRight = ConstraintLayout.LayoutParams.PARENT_ID
        }
        FpsCounterPosition.TOP_RIGHT -> {
            topToTop = ConstraintLayout.LayoutParams.PARENT_ID
            rightToRight = ConstraintLayout.LayoutParams.PARENT_ID
        }
        FpsCounterPosition.BOTTOM_LEFT -> {
            bottomToBottom = ConstraintLayout.LayoutParams.PARENT_ID
            leftToLeft = ConstraintLayout.LayoutParams.PARENT_ID
        }
        FpsCounterPosition.BOTTOM_CENTER -> {
            bottomToBottom = ConstraintLayout.LayoutParams.PARENT_ID
            leftToLeft = ConstraintLayout.LayoutParams.PARENT_ID
            rightToRight = ConstraintLayout.LayoutParams.PARENT_ID
        }
        FpsCounterPosition.BOTTOM_RIGHT -> {
            bottomToBottom = ConstraintLayout.LayoutParams.PARENT_ID
            rightToRight = ConstraintLayout.LayoutParams.PARENT_ID
        }
        FpsCounterPosition.HIDDEN -> { /* Do nothing here */ }
    }
}
