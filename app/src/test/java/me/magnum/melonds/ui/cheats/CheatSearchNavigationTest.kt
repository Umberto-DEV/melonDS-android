package me.magnum.melonds.ui.cheats

import android.net.Uri
import androidx.activity.OnBackPressedDispatcher
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.lifecycle.SavedStateHandle
import kotlinx.coroutines.flow.*
import me.magnum.melonds.common.cheats.WildEncounterCheat
import me.magnum.melonds.domain.model.*
import me.magnum.melonds.domain.repositories.CheatsRepository
import me.magnum.melonds.parcelables.cheat.CheatParcelable
import me.magnum.melonds.parcelables.cheat.GameParcelable
import me.magnum.melonds.ui.cheats.model.CheatSubmissionForm
import me.magnum.melonds.ui.cheats.ui.CheatsScreen
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
class CheatSearchNavigationTest {
    @get:Rule val compose = createComposeRule()
    private lateinit var state: SavedStateHandle
    private lateinit var back: OnBackPressedDispatcher
    private lateinit var repo: SearchRepository

    private fun show(vararg folders: CheatFolder): StateRestorationTester {
        repo = SearchRepository(folders.toList())
        val game = Game(1, "SGP", "IPKE", "19D1EEBB", folders.toList())
        state = SavedStateHandle(mapOf(CheatsViewModel.KEY_SELECTED_GAME to GameParcelable.fromGame(game)))
        val vm = CheatsViewModel(repo, state)
        val restoration = StateRestorationTester(compose)
        restoration.setContent { MelonTheme {
            back = LocalOnBackPressedDispatcherOwner.current!!.onBackPressedDispatcher
            CheatsScreen(vm, CheatsNavigation.GameFolders(game.name))
        } }
        awaitText("Search cheats and folders")
        return restoration
    }

    private fun awaitText(text: String) = compose.waitUntil(5_000) {
        compose.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()
    }

    private fun search(query: String, result: String) {
        compose.onNodeWithText("Search cheats and folders").performTextInput(query)
        val resultRow = hasText(result) and !hasSetTextAction()
        compose.waitUntil(5_000) { compose.onAllNodes(resultRow).fetchSemanticsNodes().isNotEmpty() }
        compose.onNode(resultRow).performClick()
    }

    private fun assertUnchanged() {
        compose.runOnIdle {
            assertTrue(state.get<List<CheatParcelable>>(CheatsViewModel.KEY_MODIFIED_CHEATS).isNullOrEmpty())
            assertEquals(0, repo.writes)
        }
    }

    @Test fun plainResultScrollsToItsRowWithoutTogglingAndBackKeepsQuery() {
        val cheats = (1..100).map { Cheat(it.toLong(), 1, "Plain $it", null, "not AR $it", false) }
        show(CheatFolder(1, "Ordinary", cheats))
        search("Plain 100", "Plain 100")
        compose.onNodeWithText("Search cheats and folders").assertDoesNotExist()
        compose.onNodeWithText("Plain 100").assertIsDisplayed()
        compose.onNodeWithText("Plain 1").assertDoesNotExist()
        assertUnchanged()
        compose.runOnIdle { back.onBackPressed() }
        awaitText("Search cheats and folders")
        compose.onNode(hasSetTextAction() and hasText("Plain 100")).assertExists()
        assertUnchanged()
    }

    @Test fun generatedResultOpensTheMatchingOptionButOnlyEnableStagesIt() {
        val selector = Cheat(90, 1, "Choose", null, WildEncounterCheat.code(1, 5), false)
        show(CheatFolder(9, "Wild", listOf(selector)))
        search("Pikachu", "#025 Pikachu")
        awaitText("Enable")
        compose.onNode(isSelected()).assertExists()
        compose.onNode(hasSetTextAction() and hasText("#025 Pikachu")).assertExists()
        assertUnchanged()
        compose.onNodeWithText("Cancel").performClick()
        compose.onNodeWithText("Enable").assertDoesNotExist()
        assertUnchanged()
        compose.runOnIdle { back.onBackPressed() }
        awaitText("Search cheats and folders")
        compose.onNodeWithText("#025 Pikachu").performClick()
        awaitText("Enable")
        compose.onNodeWithText("Enable").performClick()
        compose.waitUntil(5_000) { state.get<List<CheatParcelable>>(CheatsViewModel.KEY_MODIFIED_CHEATS).orEmpty().isNotEmpty() }
        compose.runOnIdle {
            val enabled = state.get<List<CheatParcelable>>(CheatsViewModel.KEY_MODIFIED_CHEATS)!!.map { it.toCheat() }.single { it.enabled }
            assertEquals(90L, enabled.id)
            assertEquals(WildEncounterCheat.code(25, 5), enabled.code)
            assertEquals(0, repo.writes)
        }
    }

    @Test fun enumeratedResultOpensItsCollapsedFamilyAndCancellationDoesNotReopenIt() {
        val cheats = (1..5).map { Cheat(100L + it, 1, "Level $it", null,
            "52246C94 28038800 12247BEC %08X D2000000 00000000".format(0x2000 + it), false) }
        show(CheatFolder(10, "Wild levels", cheats))
        search("Level 3", "Level 3")
        awaitText("Enable")
        compose.onNode(hasSetTextAction() and hasText("Level 3")).assertExists()
        compose.onNode(isSelected()).assertExists()
        assertUnchanged()
        compose.onNodeWithText("Cancel").performClick()
        compose.onNodeWithText("Enable").assertDoesNotExist()
        compose.mainClock.advanceTimeBy(1_000)
        compose.onNodeWithText("Enable").assertDoesNotExist()
        assertUnchanged()
    }

    @Test fun restoringTheScreenKeepsAnOpenPickerButDoesNotReopenACancelledOne() {
        val selector = Cheat(90, 1, "Choose", null, WildEncounterCheat.code(1, 5), false)
        val restoration = show(CheatFolder(9, "Wild", listOf(selector)))
        search("Pikachu", "#025 Pikachu")
        awaitText("Enable")
        restoration.emulateSavedInstanceStateRestore()
        awaitText("Enable")
        compose.onNode(hasSetTextAction() and hasText("#025 Pikachu")).assertExists()
        assertUnchanged()
        compose.onNodeWithText("Cancel").performClick()
        restoration.emulateSavedInstanceStateRestore()
        compose.onNodeWithText("Enable").assertDoesNotExist()
        assertUnchanged()
    }

    /** Only the repository boundary is replaced: navigation, ViewModel and selectors are real. */
    private class SearchRepository(private val folders: List<CheatFolder>) : CheatsRepository {
        var writes = 0
        override fun getAllGameCheats(game: Game) = flowOf(folders)
        override fun getFolderCheats(folder: CheatFolder) = flowOf(folders.single { it.id == folder.id }.cheats)
        override suspend fun updateCheats(cheats: List<Cheat>) { writes++ }
        override suspend fun updateCheatsStatus(cheats: List<Cheat>) { writes++ }
        override suspend fun updateCheat(cheat: Cheat) { writes++ }
        override suspend fun getGames(): List<Game> = error("unused")
        override suspend fun findGameForRom(romInfo: RomInfo): Game? = error("unused")
        override suspend fun getRomEnabledCheats(romInfo: RomInfo): List<Cheat> = error("unused")
        override suspend fun addCheatFolder(folderName: String, game: Game): Unit = error("unused")
        override suspend fun replaceImportedCheats(events: kotlinx.coroutines.flow.Flow<me.magnum.melonds.domain.model.CheatImportEvent>): Unit = error("Import is outside this UI fixture")
        override suspend fun deleteImportedCheatDatabases(): Unit = error("unused")
        override suspend fun addCheatDatabase(databaseName: String): CheatDatabase = error("unused")
        override suspend fun addGameCheats(game: Game): Game = error("unused")
        override suspend fun addCheat(folder: CheatFolder, cheat: Cheat): Unit = error("unused")
        override suspend fun addCustomCheat(folder: CheatFolder, cheatForm: CheatSubmissionForm): Unit = error("unused")
        override suspend fun deleteCheat(cheat: Cheat): Unit = error("unused")
        override fun importCheats(uri: Uri): Unit = error("unused")
        override fun isCheatImportOngoing() = false
        override fun getCheatImportProgress(): Flow<CheatImportProgress> = emptyFlow()
    }
}
