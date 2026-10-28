package me.magnum.melonds.ui.emulator.input

import android.app.Application
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class TouchscreenPointerTransitionTest : TouchscreenPointerTransitionContract()
