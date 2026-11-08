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
import me.magnum.melonds.parcelables.cheat.CheatFolderParcelable
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

    @Test fun toolbarSearchIsAvailableWithoutLosingTheExistingFolderSearch() {
        show(CheatFolder(1, "Inventory", listOf(Cheat(1, 1, "Max money", null, "not AR", false))))
        compose.onNodeWithContentDescription("Search…").assertIsDisplayed()
        compose.onNodeWithText("Search cheats and folders").assertIsDisplayed()
    }

    @Test fun toolbarSearchKeepsGeneratedOptionsAndRequiresExplicitEnable() {
        val selector = Cheat(90, 1, "Choose", null, WildEncounterCheat.code(1, 5), false)
        show(CheatFolder(9, "Wild", listOf(selector)))
        compose.onNodeWithContentDescription("Search…").performClick()
        compose.onNodeWithText("Search…").performTextInput("Pikachu")
        awaitText("#025 Pikachu")
        compose.onNodeWithText("#025 Pikachu").performClick()
        awaitText("Enable")
        assertUnchanged()
        compose.onNodeWithText("Cancel").performClick()
        compose.runOnIdle { back.onBackPressed() }
        compose.onNode(hasSetTextAction() and hasText("Pikachu")).assertExists()
        assertUnchanged()
    }

    @Test fun toolbarSearchInAFolderDoesNotIncludeAnotherFolderWithTheSameName() {
        show(
            CheatFolder(1, "Same", listOf(Cheat(1, 1, "First money", null, "not AR1", false))),
            CheatFolder(2, "Same", listOf(Cheat(2, 1, "Second money", null, "not AR2", false))),
        )
        compose.onAllNodesWithText("Same").onLast().performClick()
        awaitText("Second money")
        compose.onNodeWithContentDescription("Search…").performClick()
        compose.onNodeWithText("Search…").performTextInput("money")
        awaitText("Second money")
        compose.onNodeWithText("First money").assertDoesNotExist()
        assertUnchanged()
    }

    @Test fun toolbarToggleIsPendingAndBackClearsThenClosesSearch() {
        show(CheatFolder(1, "Inventory", listOf(Cheat(1, 1, "Max money", null, "not AR", false))))
        compose.onNodeWithContentDescription("Search…").performClick()
        compose.onNodeWithText("Search…").performTextInput("money")
        awaitText("Max money")
        compose.onNodeWithText("Max money").performClick()
        compose.waitUntil(5_000) { state.get<List<CheatParcelable>>(CheatsViewModel.KEY_MODIFIED_CHEATS).orEmpty().any { it.toCheat().enabled } }
        compose.runOnIdle { back.onBackPressed() }
        compose.onNodeWithText("Search…").assertExists()
        compose.onNodeWithText("Max money").assertIsDisplayed()
        compose.runOnIdle { back.onBackPressed() }
        awaitText("Search cheats and folders")
        compose.runOnIdle { assertEquals(0, repo.writes) }
    }

    @Test fun toolbarEditRefreshesTheSearchAndKeepsThePendingToggle() {
        val original = Cheat(1, 1, "Max money", null, "1206E012 0000201F 1206E028 0000201F", false)
        show(CheatFolder(1, "Inventory", listOf(original)))
        compose.onNodeWithContentDescription("Search…").performClick()
        compose.onNodeWithText("Search…").performTextInput("money")
        awaitText(original.name)
        compose.onNodeWithText(original.name).performClick()
        compose.waitUntil(5_000) {
            state.get<List<CheatParcelable>>(CheatsViewModel.KEY_MODIFIED_CHEATS).orEmpty().any { it.toCheat().enabled }
        }
        compose.onNodeWithContentDescription("Options").performClick()
        compose.onNodeWithText("Edit").performClick()
        awaitText("Edit cheat")
        compose.onNode(hasSetTextAction() and hasText(original.name)).performTextReplacement("Max coins")
        compose.onNodeWithText("Save", ignoreCase = true).performClick()
        awaitText("No cheats matching your search were found")
        compose.onNodeWithText(original.name).assertDoesNotExist()
        compose.onNode(hasSetTextAction() and hasText("money")).performTextReplacement("coins")
        awaitText("Max coins")
        compose.runOnIdle {
            val edited = repo.folder(1).cheats.single()
            assertEquals(original.copy(name = "Max coins", enabled = true), edited)
            assertEquals(listOf(edited), state.get<List<CheatParcelable>>(CheatsViewModel.KEY_MODIFIED_CHEATS)!!.map { it.toCheat() })
            assertEquals(1, repo.writes)
        }
    }

    @Test fun toolbarDeleteAndUndoUseTheResultsFolderAndEffectivePendingCheat() {
        val unrelated = Cheat(1, 1, "Other", null, "1206E012 0000201F", false)
        val original = Cheat(2, 1, "Max money", null, "1206E028 0000201F", false)
        show(CheatFolder(1, "Original", listOf(unrelated)), CheatFolder(2, "Inventory", listOf(original)))
        compose.onNodeWithText("Original").performClick()
        awaitText("Other")
        compose.runOnIdle { back.onBackPressed() }
        awaitText("Search cheats and folders")
        compose.onNodeWithContentDescription("Search…").performClick()
        compose.onNodeWithText("Search…").performTextInput("money")
        awaitText(original.name)
        compose.onNodeWithText(original.name).performClick()
        compose.waitUntil(5_000) {
            state.get<List<CheatParcelable>>(CheatsViewModel.KEY_MODIFIED_CHEATS).orEmpty().any { it.toCheat().id == original.id && it.toCheat().enabled }
        }
        compose.onNodeWithContentDescription("Options").performClick()
        compose.onNodeWithText("Delete").performClick()
        awaitText("No cheats matching your search were found")
        compose.runOnIdle {
            assertTrue(repo.folder(2).cheats.isEmpty())
            assertTrue(state.get<List<CheatParcelable>>(CheatsViewModel.KEY_MODIFIED_CHEATS).isNullOrEmpty())
            assertEquals(1, repo.writes)
        }
        compose.onNodeWithText("Undo").performClick()
        awaitText(original.name)
        compose.runOnIdle {
            val restored = repo.folder(2).cheats.single()
            assertNotEquals(original.id, restored.id)
            assertEquals(original.copy(id = restored.id, enabled = true), restored)
            assertEquals(listOf(unrelated), repo.folder(1).cheats)
            assertEquals(1L, state.get<CheatFolderParcelable>(CheatsViewModel.KEY_SELECTED_FOLDER)!!.toCheatFolder().id)
            assertTrue(state.get<List<CheatParcelable>>(CheatsViewModel.KEY_MODIFIED_CHEATS).isNullOrEmpty())
            assertEquals(2, repo.writes)
        }
    }

    @Test fun toolbarRestorationKeepsQueryFolderIdentityAndPendingChanges() {
        val restoration = show(
            CheatFolder(1, "Same", listOf(Cheat(1, 1, "First money", null, "not AR1", false))),
            CheatFolder(2, "Same", listOf(Cheat(2, 1, "Second money", null, "not AR2", false))),
        )
        compose.onAllNodesWithText("Same").onLast().performClick()
        awaitText("Second money")
        compose.onNodeWithContentDescription("Search…").performClick()
        compose.onNodeWithText("Search…").performTextInput("money")
        awaitText("Second money")
        compose.onNodeWithText("Second money").performClick()
        compose.waitUntil(5_000) {
            state.get<List<CheatParcelable>>(CheatsViewModel.KEY_MODIFIED_CHEATS).orEmpty().any { it.toCheat().enabled }
        }
        val before = state.get<List<CheatParcelable>>(CheatsViewModel.KEY_MODIFIED_CHEATS)!!.map { it.toCheat() }
        restoration.emulateSavedInstanceStateRestore()
        compose.onNode(hasSetTextAction() and hasText("money")).assertExists()
        awaitText("Second money")
        compose.onNodeWithText("First money").assertDoesNotExist()
        compose.runOnIdle {
            assertEquals(before, state.get<List<CheatParcelable>>(CheatsViewModel.KEY_MODIFIED_CHEATS)!!.map { it.toCheat() })
            assertFalse(repo.folder(2).cheats.single().enabled)
            assertEquals(0, repo.writes)
        }
        compose.runOnIdle { back.onBackPressed() }
        compose.onNodeWithText("Search…").assertExists()
        compose.onNodeWithText("Second money").assertIsDisplayed()
        compose.onNodeWithText("First money").assertDoesNotExist()
        compose.runOnIdle { back.onBackPressed() }
        awaitText("Second money")
        compose.onNodeWithContentDescription("Search…").assertIsDisplayed()
        compose.runOnIdle { assertEquals(0, repo.writes) }
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
    private class SearchRepository(folders: List<CheatFolder>) : CheatsRepository {
        private val snapshots = MutableStateFlow(folders)
        private var nextId = 1000L
        var writes = 0
        fun folder(id: Long) = snapshots.value.single { it.id == id }
        override fun getAllGameCheats(game: Game) = snapshots.asStateFlow()
        override fun getFolderCheats(folder: CheatFolder) = snapshots.map { folders -> folders.single { it.id == folder.id }.cheats }.distinctUntilChanged()
        override suspend fun updateCheats(cheats: List<Cheat>) {
            val replacements = cheats.associateBy { it.id }
            snapshots.update { folders -> folders.map { folder -> folder.copy(cheats = folder.cheats.map { replacements[it.id] ?: it }) } }
            writes++
        }
        override suspend fun updateCheatsStatus(cheats: List<Cheat>) = updateCheats(cheats)
        override suspend fun updateCheat(cheat: Cheat) = updateCheats(listOf(cheat))
        override suspend fun getGames(): List<Game> = error("unused")
        override suspend fun findGameForRom(romInfo: RomInfo): Game? = error("unused")
        override suspend fun getRomEnabledCheats(romInfo: RomInfo): List<Cheat> = error("unused")
        override suspend fun addCheatFolder(folderName: String, game: Game): Unit = error("unused")
        override suspend fun replaceImportedCheats(events: kotlinx.coroutines.flow.Flow<me.magnum.melonds.domain.model.CheatImportEvent>): Unit = error("Import is outside this UI fixture")
        override suspend fun deleteImportedCheatDatabases(): Unit = error("unused")
        override suspend fun addCheatDatabase(databaseName: String): CheatDatabase = error("unused")
        override suspend fun addGameCheats(game: Game): Game = error("unused")
        override suspend fun addCheat(folder: CheatFolder, cheat: Cheat) {
            val restored = cheat.copy(id = nextId++)
            snapshots.update { folders -> folders.map { if (it.id == folder.id) it.copy(cheats = it.cheats + restored) else it } }
            writes++
        }
        override suspend fun addCustomCheat(folder: CheatFolder, cheatForm: CheatSubmissionForm): Unit = error("unused")
        override suspend fun deleteCheat(cheat: Cheat) {
            snapshots.update { folders -> folders.map { folder -> folder.copy(cheats = folder.cheats.filterNot { it.id == cheat.id }) } }
            writes++
        }
        override fun importCheats(uri: Uri): Unit = error("unused")
        override fun isCheatImportOngoing() = false
        override fun getCheatImportProgress(): Flow<CheatImportProgress> = emptyFlow()
    }
}
