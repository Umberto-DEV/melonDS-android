package me.magnum.melonds.ui.emulator

import android.content.Context
import android.util.AttributeSet
import android.view.View
import androidx.core.view.isGone
import androidx.core.view.isVisible
import dagger.hilt.android.AndroidEntryPoint
import me.magnum.melonds.common.vibration.TouchVibrator
import me.magnum.melonds.domain.model.Input
import me.magnum.melonds.domain.model.input.SoftInputBehaviour
import me.magnum.melonds.domain.model.layout.LayoutComponent
import me.magnum.melonds.ui.common.LayoutView
import me.magnum.melonds.ui.emulator.input.BaseInputHandler
import me.magnum.melonds.ui.emulator.input.ButtonsInputHandler
import me.magnum.melonds.ui.emulator.input.DpadInputHandler
import me.magnum.melonds.ui.emulator.input.FrontendInputHandler
import me.magnum.melonds.ui.emulator.input.IInputListener
import me.magnum.melonds.ui.emulator.input.SingleButtonInputHandler
import me.magnum.melonds.ui.emulator.input.TouchscreenInputHandler
import me.magnum.melonds.ui.emulator.input.view.ToggleableImageView
import me.magnum.melonds.ui.emulator.model.ConnectedControllersState
import me.magnum.melonds.ui.emulator.model.RuntimeInputLayoutConfiguration
import me.magnum.melonds.ui.layouteditor.model.LayoutTarget
import javax.inject.Inject

@AndroidEntryPoint
class RuntimeLayoutView(context: Context, attrs: AttributeSet? = null) : LayoutView(context, attrs) {

    @Inject
    lateinit var touchVibrator: TouchVibrator

    private var currentRuntimeLayout: RuntimeInputLayoutConfiguration? = null
    private var frontendInputHandler: IInputListener? = null
    private var systemInputHandler: IInputListener? = null
    // Every input handler currently installed on a view. Kept so that their pressed keys can be
    // released before the handlers are replaced or the layout is torn down.
    private val buttonInputHandlers = mutableListOf<BaseInputHandler>()
    private var screenInputHandler: BaseInputHandler? = null
    private var isSoftInputVisible = true
    private var areScreensSwapped = false
    private var connectedControllersState: ConnectedControllersState = ConnectedControllersState.NoControllers

    fun setFrontendInputHandler(frontendInputHandler: FrontendInputHandler) {
        this.frontendInputHandler = frontendInputHandler
        updateInputs()
    }

    fun setSystemInputHandler(systemInputHandler: IInputListener) {
        this.systemInputHandler = systemInputHandler
        updateInputs()
    }

    fun setConnectedControllersState(state: ConnectedControllersState) {
        connectedControllersState = state
        updateVisibility()
    }

    fun toggleSoftInputVisibility() {
        isSoftInputVisible = !isSoftInputVisible
        setLayoutComponentToggleState(LayoutComponent.BUTTON_TOGGLE_SOFT_INPUT, isSoftInputVisible)
        updateVisibility()
    }

    fun swapScreens() {
        areScreensSwapped = !areScreensSwapped
        updateScreenInputs()
    }

    fun areScreensSwapped(): Boolean {
        return areScreensSwapped
    }

    fun setLayoutComponentToggleState(layoutComponent: LayoutComponent, isEnabled: Boolean) {
        val toggleableImageView = getLayoutComponentView(layoutComponent)?.view as? ToggleableImageView ?: return
        toggleableImageView.setToggleState(isEnabled)
    }

    override fun destroyLayout() {
        releaseButtonInputHandlers()
        releaseScreenInputHandler()
        super.destroyLayout()
    }

    fun instantiateLayout(runtimeLayout: RuntimeInputLayoutConfiguration, layoutTarget: LayoutTarget) {
        currentRuntimeLayout = runtimeLayout
        instantiateLayout(runtimeLayout.layout, layoutTarget)
        updateInputs()
        updateVisibility()
        setLayoutComponentToggleState(LayoutComponent.BUTTON_TOGGLE_SOFT_INPUT, isSoftInputVisible)
    }

    private fun updateInputs() {
        // The handlers installed so far are about to be replaced by new instances. Without this,
        // any key held down at that moment stays down in the emulator forever.
        releaseButtonInputHandlers()

        val currentRuntimeLayout = currentRuntimeLayout
        if (currentRuntimeLayout == null) {
            isGone = true
            return
        }

        isVisible = true
        val inputAlpha = currentRuntimeLayout.softInputOpacity / 100f

        val enableHapticFeedback = currentRuntimeLayout.isHapticFeedbackEnabled
        systemInputHandler?.let {
            getLayoutComponentView(LayoutComponent.DPAD)?.view?.installInputHandler(DpadInputHandler(it, enableHapticFeedback, touchVibrator))
            getLayoutComponentView(LayoutComponent.BUTTONS)?.view?.installInputHandler(ButtonsInputHandler(it, enableHapticFeedback, touchVibrator))
            getLayoutComponentView(LayoutComponent.BUTTON_L)?.view?.installInputHandler(SingleButtonInputHandler(it, Input.L, enableHapticFeedback, touchVibrator))
            getLayoutComponentView(LayoutComponent.BUTTON_R)?.view?.installInputHandler(SingleButtonInputHandler(it, Input.R, enableHapticFeedback, touchVibrator))
            getLayoutComponentView(LayoutComponent.BUTTON_SELECT)?.view?.installInputHandler(SingleButtonInputHandler(it, Input.SELECT, enableHapticFeedback, touchVibrator))
            getLayoutComponentView(LayoutComponent.BUTTON_START)?.view?.installInputHandler(SingleButtonInputHandler(it, Input.START, enableHapticFeedback, touchVibrator))
            getLayoutComponentView(LayoutComponent.BUTTON_HINGE)?.view?.installInputHandler(SingleButtonInputHandler(it, Input.HINGE, enableHapticFeedback, touchVibrator))
        }
        frontendInputHandler?.let {
            getLayoutComponentView(LayoutComponent.BUTTON_RESET)?.view?.installInputHandler(SingleButtonInputHandler(it, Input.RESET, enableHapticFeedback, touchVibrator))
            getLayoutComponentView(LayoutComponent.BUTTON_PAUSE)?.view?.installInputHandler(SingleButtonInputHandler(it, Input.PAUSE, enableHapticFeedback, touchVibrator))
            getLayoutComponentView(LayoutComponent.BUTTON_FAST_FORWARD_TOGGLE)?.view?.installInputHandler(SingleButtonInputHandler(it, Input.FAST_FORWARD, enableHapticFeedback, touchVibrator))
            getLayoutComponentView(LayoutComponent.BUTTON_MICROPHONE_TOGGLE)?.view?.installInputHandler(SingleButtonInputHandler(it, Input.MICROPHONE, enableHapticFeedback, touchVibrator))
            getLayoutComponentView(LayoutComponent.BUTTON_TOGGLE_SOFT_INPUT)?.view?.installInputHandler(SingleButtonInputHandler(it, Input.TOGGLE_SOFT_INPUT, enableHapticFeedback, touchVibrator))
            getLayoutComponentView(LayoutComponent.BUTTON_SWAP_SCREENS)?.view?.installInputHandler(SingleButtonInputHandler(it, Input.SWAP_SCREENS, enableHapticFeedback, touchVibrator))
            getLayoutComponentView(LayoutComponent.BUTTON_QUICK_SAVE)?.view?.installInputHandler(SingleButtonInputHandler(it, Input.QUICK_SAVE, enableHapticFeedback, touchVibrator))
            getLayoutComponentView(LayoutComponent.BUTTON_QUICK_LOAD)?.view?.installInputHandler(SingleButtonInputHandler(it, Input.QUICK_LOAD, enableHapticFeedback, touchVibrator))
            getLayoutComponentView(LayoutComponent.BUTTON_REWIND)?.view?.installInputHandler(SingleButtonInputHandler(it, Input.REWIND, enableHapticFeedback, touchVibrator))
        }

        getLayoutComponentViews().forEach {
            if (!it.component.isScreen()) {
                it.view.apply {
                    alpha = inputAlpha
                }
            }
        }

        updateScreenInputs()
    }

    private fun updateScreenInputs() {
        val (touchScreenComponent, nonTouchScreenComponent) = if (areScreensSwapped) {
            LayoutComponent.TOP_SCREEN to LayoutComponent.BOTTOM_SCREEN
        } else {
            LayoutComponent.BOTTOM_SCREEN to LayoutComponent.TOP_SCREEN
        }
        // Same reasoning as updateInputs, and this path also runs on its own when the screens are
        // swapped while the stylus is down.
        releaseScreenInputHandler()

        systemInputHandler?.let { listener ->
            getLayoutComponentView(touchScreenComponent)?.view?.let { view ->
                val handler = TouchscreenInputHandler(listener)
                view.setOnTouchListener(handler)
                screenInputHandler = handler
            }
        }
        getLayoutComponentView(nonTouchScreenComponent)?.view?.setOnTouchListener(null)
    }

    private fun View.installInputHandler(handler: BaseInputHandler) {
        setOnTouchListener(handler)
        buttonInputHandlers += handler
    }

    private fun releaseButtonInputHandlers() {
        buttonInputHandlers.forEach { it.releaseAll() }
        buttonInputHandlers.clear()
    }

    private fun releaseScreenInputHandler() {
        screenInputHandler?.releaseAll()
        screenInputHandler = null
    }

    private fun updateVisibility() {
        val currentConnectedControllersState = connectedControllersState
        var hiddenComponents = when(currentRuntimeLayout?.softInputBehaviour) {
            SoftInputBehaviour.ALWAYS_VISIBLE -> emptyList()
            SoftInputBehaviour.HIDE_SYSTEM_BUTTONS_WHEN_CONTROLLERS_CONNECTED, null -> {
                when(currentConnectedControllersState) {
                    ConnectedControllersState.NoControllers -> emptyList()
                    is ConnectedControllersState.ControllersConnected -> listOf(
                        LayoutComponent.BUTTONS,
                        LayoutComponent.DPAD,
                        LayoutComponent.BUTTON_L,
                        LayoutComponent.BUTTON_R,
                        LayoutComponent.BUTTON_START,
                        LayoutComponent.BUTTON_SELECT
                    )
                }
            }
            SoftInputBehaviour.HIDE_ALL_BUTTONS_ASSIGNED_TO_CONNECTED_CONTROLLERS -> when(currentConnectedControllersState) {
                ConnectedControllersState.NoControllers -> emptyList()
                is ConnectedControllersState.ControllersConnected -> {
                    LayoutComponent.entries.filter {
                        // The component can be hidden if all matching inputs are assigned to connected controllers
                        it.matchingInputs.all {
                            currentConnectedControllersState.assignedInputs.contains(it)
                        }
                    }
                }
            }
            SoftInputBehaviour.ALWAYS_INVISIBLE -> LayoutComponent.entries.toList()
        }

        if (!isSoftInputVisible) {
            // Hide everything except the soft input toggle button if it was not already hidden by the soft input behaviour logic
            hiddenComponents = if (hiddenComponents.contains(LayoutComponent.BUTTON_TOGGLE_SOFT_INPUT)) {
                LayoutComponent.entries.toList()
            } else {
                LayoutComponent.entries.toList().filter { it != LayoutComponent.BUTTON_TOGGLE_SOFT_INPUT}
            }
        }

        val visibleComponents = LayoutComponent.entries.filter { !hiddenComponents.contains(it) }

        hiddenComponents.forEach { component ->
            if (!component.isScreen()) {
                getLayoutComponentView(component)?.view?.isVisible = false
            }
        }
        visibleComponents.forEach { component ->
            if (!component.isScreen()) {
                getLayoutComponentView(component)?.view?.isVisible = true
            }
        }
    }
}