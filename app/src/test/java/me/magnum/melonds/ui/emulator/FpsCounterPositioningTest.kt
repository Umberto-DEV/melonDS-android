package me.magnum.melonds.ui.emulator

import android.app.Application
import android.view.View
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.test.core.app.ApplicationProvider
import me.magnum.melonds.domain.model.FpsCounterPosition
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class FpsCounterPositioningTest {
    @Test fun `repositioning the same counter reaches every requested corner and center`() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val layout = ConstraintLayout(context)
        val counter = TextView(context).apply { id = View.generateViewId() }
        layout.addView(counter, ConstraintLayout.LayoutParams(40, 20))
        val cases = listOf(
            Triple(FpsCounterPosition.BOTTOM_RIGHT, 280, 220),
            Triple(FpsCounterPosition.TOP_LEFT, 0, 0),
            Triple(FpsCounterPosition.BOTTOM_CENTER, 140, 220),
            Triple(FpsCounterPosition.TOP_RIGHT, 280, 0),
            Triple(FpsCounterPosition.BOTTOM_LEFT, 0, 220),
            Triple(FpsCounterPosition.TOP_CENTER, 140, 0),
        )
        for ((position, x, y) in cases) {
            counter.layoutParams = (counter.layoutParams as ConstraintLayout.LayoutParams).apply {
                positionFpsCounter(position)
            }
            layout.measure(
                View.MeasureSpec.makeMeasureSpec(320, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(240, View.MeasureSpec.EXACTLY),
            )
            layout.layout(0, 0, 320, 240)
            assertEquals("$position horizontal", x, counter.left)
            assertEquals("$position vertical", y, counter.top)
        }
    }
}
