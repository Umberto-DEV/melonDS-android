package me.magnum.melonds.impl.layout

import android.app.Application
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [28])
class UILayoutProviderDisplayLifecycleTest : UILayoutProviderDisplayLifecycleContract()
