package me.magnum.melonds.ui.cheats

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.graphics.asAndroidBitmap
import me.magnum.melonds.common.cheats.WildEncounterCheat
import me.magnum.melonds.domain.model.Cheat
import me.magnum.melonds.ui.cheats.model.CheatSubmissionForm
import me.magnum.melonds.ui.cheats.ui.WildEncounterDialog
import me.magnum.melonds.ui.theme.MelonTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "en-rUS-w600dp-h800dp", application = android.app.Application::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class WildEncounterDialogTest {
    @get:Rule val compose = createComposeRule()
    private val cheat = Cheat(1, 1, "Wild Pokémon", null, WildEncounterCheat.code(1, 5), false)

    @Test fun searchSelectValidateAndConfirm() {
        var result: CheatSubmissionForm? = null
        compose.setContent { MelonTheme { WildEncounterDialog(cheat, {}, { result = it }) } }
        compose.onNodeWithText("Search name or Pokédex number").performTextInput("493")
        compose.onNodeWithText("#493 Arceus").performClick()
        compose.onNodeWithText("Level 1–100").performTextReplacement("101")
        compose.onNodeWithText("Enable").assertIsNotEnabled()
        compose.onNodeWithText("Level 1–100").performTextReplacement("100")
        compose.onNodeWithText("Enable").performClick()
        assertEquals(WildEncounterCheat.Selection(493, 100), WildEncounterCheat.selection(result!!.code))
    }

    @Test fun cancelDoesNotSave() {
        var cancelled = false
        var saved = false
        compose.setContent { MelonTheme { WildEncounterDialog(cheat, { cancelled = true }, { saved = true }) } }
        compose.onNodeWithText("Search name or Pokédex number").performTextInput("not a pokemon")
        compose.onNodeWithText("No Pokémon found").assertExists()
        compose.onNodeWithText("Cancel").performClick()
        assertTrue(cancelled)
        assertFalse(saved)
    }

    @Test
    @Config(qualifiers = "it-rIT-w640dp-h360dp")
    fun landscapeKeepsSearchResultsAndActionsVisible() {
        compose.setContent { MelonTheme { WildEncounterDialog(cheat, {}, {}) } }
        compose.onNodeWithText("Cerca nome o numero Pokédex").performTextInput("pikachu")
        compose.onNodeWithText("#025 Pikachu").assertIsDisplayed().performClick()
        compose.onNodeWithText("Attiva").assertIsDisplayed().assertIsEnabled()
        System.getenv("SGP_SELECTOR_SCREENSHOT_DIR")?.let { directory ->
            val output = java.io.File(directory, "selector-landscape.png")
            output.parentFile.mkdirs()
            val bitmap = compose.onNode(isDialog()).captureToImage().asAndroidBitmap()
            output.outputStream().use { bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }
        }
    }
}
