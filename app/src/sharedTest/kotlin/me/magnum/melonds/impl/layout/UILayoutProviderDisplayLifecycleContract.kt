package me.magnum.melonds.impl.layout

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import me.magnum.melonds.domain.model.Point
import me.magnum.melonds.domain.model.Rect
import me.magnum.melonds.domain.model.layout.Insets
import me.magnum.melonds.domain.model.layout.LayoutComponent
import me.magnum.melonds.domain.model.layout.LayoutConfiguration
import me.magnum.melonds.domain.model.layout.LayoutDisplay
import me.magnum.melonds.domain.model.layout.LayoutDisplayPair
import me.magnum.melonds.domain.model.layout.PositionedLayoutComponent
import me.magnum.melonds.domain.model.layout.ScreenLayout
import me.magnum.melonds.domain.model.layout.UILayout
import me.magnum.melonds.domain.model.layout.UILayoutVariant
import me.magnum.melonds.domain.model.ui.Orientation
import me.magnum.melonds.impl.ScreenUnitsConverter
import org.junit.Assert.assertEquals
import org.junit.Test

/** Public-flow regression shared by Robolectric and Android ART. */
abstract class UILayoutProviderDisplayLifecycleContract {
    private suspend fun layout(provider: UILayoutProvider) = withTimeout(5000) {
        provider.currentLayout.filterNotNull().first().second
    }

    private val primary = LayoutDisplay(0, LayoutDisplay.Type.BUILT_IN, 1080, 1920)
    private val external = LayoutDisplay(1, LayoutDisplay.Type.EXTERNAL, 1920, 1080)
    private val splitVariant = UILayoutVariant(
        Point(1080, 1920), Insets.Zero, Orientation.PORTRAIT, emptyList(),
        LayoutDisplayPair(primary, external),
    )
    private val splitLayout = UILayout(
        ScreenLayout(listOf(
            PositionedLayoutComponent(Rect(0, 0, 1080, 810), LayoutComponent.BOTTOM_SCREEN),
            PositionedLayoutComponent(Rect(100, 1000, 100, 100), LayoutComponent.BUTTON_PAUSE),
        )),
        ScreenLayout(listOf(
            PositionedLayoutComponent(Rect(240, 0, 1440, 1080), LayoutComponent.TOP_SCREEN),
        )),
    )

    private fun provider(saved: Map<UILayoutVariant, UILayout>, variant: UILayoutVariant): UILayoutProvider {
        val context = ApplicationProvider.getApplicationContext<Context>()
        return UILayoutProvider(DefaultLayoutProvider(context, ScreenUnitsConverter(context))).apply {
            setCurrentLayoutConfiguration(LayoutConfiguration.newCustom().copy(layoutVariants = saved))
            updateUiSize(variant.uiSize.x, variant.uiSize.y)
            updateUiInsets(variant.uiInsets)
            updateCurrentOrientation(variant.orientation)
            updateFolds(variant.folds)
            updateDisplays(variant.displays)
        }
    }

    @Test
    fun disconnectRestoresBothScreens() = runBlocking {
        val provider = provider(mapOf(splitVariant to splitLayout), splitVariant)
        val before = layout(provider)
        assertEquals(listOf(LayoutComponent.BOTTOM_SCREEN), before.mainScreenLayout.components!!.filter { it.isScreen() }.map { it.component })

        provider.updateDisplays(LayoutDisplayPair(primary, null))
        val after = layout(provider)

        assertEquals(
            setOf(LayoutComponent.TOP_SCREEN, LayoutComponent.BOTTOM_SCREEN),
            after.mainScreenLayout.components!!.filter { it.isScreen() }.map { it.component }.toSet(),
        )
        assertEquals(emptyList<PositionedLayoutComponent>(), after.secondaryScreenLayout.components.orEmpty())
    }

    @Test
    fun exactSingleDisplayLayoutRespectsHiddenScreen() = runBlocking {
        val singleVariant = splitVariant.copy(displays = LayoutDisplayPair(primary, null))
        val singleLayout = UILayout(splitLayout.mainScreenLayout, ScreenLayout(emptyList()))
        val provider = provider(mapOf(splitVariant to splitLayout, singleVariant to singleLayout), singleVariant)
        val result = layout(provider)

        assertEquals(singleLayout, result)
    }

    @Test
    fun equivalentDisplayReconnectPreservesCustomLayout() = runBlocking {
        val reconnectedVariant = splitVariant.copy(displays = LayoutDisplayPair(primary, external.copy(id = 42)))
        val provider = provider(mapOf(splitVariant to splitLayout), reconnectedVariant)
        val result = layout(provider)

        assertEquals(splitLayout, result)
    }

    @Test
    fun equivalentDisplayReconnectWithRotatedInsetsPreservesCustomLayout() = runBlocking {
        val saved = splitVariant.copy(uiInsets = Insets(24, 0, 0, 0))
        val rotated = saved.copy(uiInsets = Insets(0, 0, 24, 0), displays = LayoutDisplayPair(primary, external.copy(id = 42)))
        val provider = provider(mapOf(saved to splitLayout), rotated)
        assertEquals(splitLayout, layout(provider))
    }
}
