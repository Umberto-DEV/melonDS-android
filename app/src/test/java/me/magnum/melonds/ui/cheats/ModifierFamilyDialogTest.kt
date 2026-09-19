package me.magnum.melonds.ui.cheats

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import me.magnum.melonds.common.cheats.ModifierFamilies
import me.magnum.melonds.common.cheats.ModifierFamily
import me.magnum.melonds.domain.model.Cheat
import me.magnum.melonds.ui.cheats.ui.ModifierFamilyDialog
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
class ModifierFamilyDialogTest {
    @get:Rule val compose = createComposeRule()

    private val hgSpeciesAndLevel = "94000130 FCFF0000 6211186C 00000000 B211186C 00000000 0000DCF4 01ED0001 0000DCF8 00640002 D2000000 00000000 " +
        "94000130 FDFF0000 6211186C 00000000 B211186C 00000000 DA000000 0000DCF6 C0000000 00000027 D7000000 00032A48 D2000000 00000000 " +
        "94000130 FDFF0000 6211186C 00000000 B211186C 00000000 DB000000 0000DCFA C0000000 0000000B D8000000 00032A3C D2000000 00000000"
    private val itemLoad: ModifierFamily = ModifierFamilies.recognize("Wild Pokemon Modifier Codes",
        listOf(Cheat(1, 1, "Wild Pokemon and Level Modifier", "(Press L+R): hold L before the encounter.", hgSpeciesAndLevel, false))).single()
    private val hgV1 = "94000130 FCFF0000 6211186C 00000000 B211186C 00000000 0000DCF4 01ED0001 D2000000 00000000 " +
        "94000130 FDFF0000 6211186C 00000000 B211186C 00000000 DA000000 0000DCF6 C0000000 00000027 D7000000 00032A48 D2000000 00000000"
    private fun levelFamily(): ModifierFamily = ModifierFamilies.recognize("49 - Wild · level 1-100",
        (1..10).map { Cheat(100L + it, 1, "Level $it", null, "52246C94 28038800 12247BEC %08X D2000000 00000000".format(0x2000 + it), false) }).single()
    private val names = listOf("Bulbasaur", "Ivysaur", "Venusaur", "Charmander", "Charmeleon")
    private fun enumerated(activeIndex: Int? = null): ModifierFamily = ModifierFamilies.recognize("Wild Pokemon Modifier - Generation 1",
        names.mapIndexed { i, n -> Cheat(10L + i, 1, n, null,
            "94000130 FFFB0000 6214617C 00000000 B214617C 00000000 C0000000 0000002F 00005DD4 %08X DC000000 00000004 D2000000 00000000".format(i + 1), i == activeIndex) }).single()

    @Test fun searchSelectValidateAndConfirm() {
        var result: Pair<Int, Int?>? = null
        compose.setContent { MelonTheme { ModifierFamilyDialog(itemLoad, {}, {}, { v, l -> result = v to l }) } }
        compose.onNodeWithText("Search name or Pokédex number").performTextInput("493")
        compose.onNodeWithText("#493 Arceus").performClick()
        compose.onNodeWithText("Level 1–100").performTextReplacement("101")
        compose.onNodeWithText("Enable").assertIsNotEnabled()
        compose.onNodeWithText("Level 1–100").performTextReplacement("100")
        compose.onNodeWithText("Enable").performClick()
        assertEquals(493 to 100, result)
    }

    @Test fun enumeratedFamilyHasNoLevelFieldAndConfirmsTheValue() {
        var result: Pair<Int, Int?>? = null
        compose.setContent { MelonTheme { ModifierFamilyDialog(enumerated(), {}, {}, { v, l -> result = v to l }) } }
        compose.onNodeWithText("Level 1–100").assertDoesNotExist()
        compose.onNodeWithText("Disable").assertDoesNotExist()
        compose.onNodeWithText("Search name or Pokédex number").performTextInput("charm")
        compose.onNodeWithText("Charmeleon").assertExists()
        compose.onNodeWithText("Charmander").performClick()
        compose.onNodeWithText("Enable").performClick()
        assertEquals(4 to null, result)
    }

    @Test fun disableIsOfferedOnlyWhenSomethingIsActive() {
        var disabled = false
        compose.setContent { MelonTheme { ModifierFamilyDialog(enumerated(activeIndex = 2), {}, { disabled = true }, { _, _ -> }) } }
        compose.onAllNodesWithText("Venusaur").assertCountEquals(2) // selected label + list row
        compose.onNodeWithText("Disable").performClick()
        assertTrue(disabled)
    }

    @Test fun cancelDoesNotSave() {
        var cancelled = false
        var saved = false
        compose.setContent { MelonTheme { ModifierFamilyDialog(itemLoad, { cancelled = true }, {}, { _, _ -> saved = true }) } }
        compose.onNodeWithText("Search name or Pokédex number").performTextInput("not a pokemon")
        compose.onNodeWithText("No Pokémon found").assertExists()
        compose.onNodeWithText("Cancel").performClick()
        assertTrue(cancelled)
        assertFalse(saved)
    }

    @Test
    @Config(qualifiers = "it-rIT-w640dp-h360dp")
    fun landscapeKeepsSearchResultsAndActionsVisible() {
        compose.setContent { MelonTheme { ModifierFamilyDialog(itemLoad, {}, {}, { _, _ -> }) } }
        compose.onNodeWithText("Cerca nome o numero Pokédex").performTextInput("pikachu")
        compose.onNodeWithText("#025 Pikachu").assertIsDisplayed().performClick()
        compose.onNodeWithText("Attiva").assertIsDisplayed().assertIsEnabled()
    }

    @Test fun numericSearchFallsBackToTheLabelForLevelFamilies() {
        var result: Pair<Int, Int?>? = null
        compose.setContent { MelonTheme { ModifierFamilyDialog(levelFamily(), {}, {}, { v, l -> result = v to l }) } }
        compose.onNodeWithText("Level 1–100").assertDoesNotExist()
        compose.onNodeWithText("Search name or Pokédex number").performTextInput("7")
        compose.onNodeWithText("Level 7").performClick()
        compose.onNodeWithText("Enable").performClick()
        assertEquals(0x2007 to null, result)
    }

    @Test fun loadFormActiveFamilyOffersDisable() {
        var disabled = false
        val active = ModifierFamilies.recognize("Wild Pokemon Modifier Codes", listOf(Cheat(1, 1, "Wild Pokemon Modifier v1", null, hgV1, true))).single()
        compose.setContent { MelonTheme { ModifierFamilyDialog(active, {}, { disabled = true }, { _, _ -> }) } }
        compose.onNodeWithText("Disable").performClick()
        assertTrue(disabled)
    }
}
