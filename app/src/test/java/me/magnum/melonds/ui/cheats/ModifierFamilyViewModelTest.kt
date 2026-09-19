package me.magnum.melonds.ui.cheats

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.*
import me.magnum.melonds.common.cheats.ModifierFamilies
import me.magnum.melonds.common.cheats.ModifierFamily
import me.magnum.melonds.common.cheats.WildEncounterCheat
import me.magnum.melonds.domain.model.*
import me.magnum.melonds.domain.repositories.CheatsRepository
import me.magnum.melonds.parcelables.cheat.*
import me.magnum.melonds.ui.cheats.model.CheatListItem
import me.magnum.melonds.ui.cheats.model.CheatSubmissionForm
import me.magnum.melonds.ui.cheats.model.CheatsScreenUiState
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = android.app.Application::class)
class ModifierFamilyViewModelTest {
    // Sacred Gold Plus: the canonical profile applies. "Choose" is the selector the user configures, "Prior" an older active selection.
    private val selected = Cheat(1, 1, "Choose", null, WildEncounterCheat.code(25, 10), false)
    private val prior = selected.copy(id = 2, name = "Prior", enabled = true)
    private val folder = CheatFolder(1, "Wild", listOf(selected, prior))
    private val game = Game(1, "SGP", "IPKE", "19D1EEBB", listOf(folder))

    private fun handle() = SavedStateHandle(mapOf(
        CheatsViewModel.KEY_SELECTED_GAME to GameParcelable.fromGame(game),
        CheatsViewModel.KEY_SELECTED_FOLDER to CheatFolderParcelable.fromCheatFolder(folder),
    ))
    private fun pending(handle: SavedStateHandle) = handle.get<List<CheatParcelable>>(CheatsViewModel.KEY_MODIFIED_CHEATS).orEmpty().map { it.toCheat() }
    private fun family(name: String, cheats: List<Cheat> = folder.cheats): ModifierFamily =
        ModifierFamilies.recognize(folder.name, cheats).single { it.title == name }
    private fun levelFolder(vararg levels: Int) = CheatFolder(49, "49 - Wild · level 1-100",
        levels.map { Cheat(40L + it, 1, "Level $it", null, "52246C94 28038800 12247BEC %08X D2000000 00000000".format(0x2000 + it), false) })
    private fun natureRows(kind: String) = requireNotNull(javaClass.getResourceAsStream("/sgp-nature-codes.tsv")).bufferedReader().readLines()
        .map { it.split('\t') }.filter { it[0] == kind }.take(5).map { it[2] }

    private fun <T> withMain(block: suspend TestScope.() -> T) = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try { block() } finally { Dispatchers.resetMain() }
    }

    @Test fun selectingAFamilyOptionRewritesTheCodeAndDisablesTheConflicts() = withMain {
        val repo = FakeRepository(folder)
        val state = handle()
        val vm = CheatsViewModel(repo, state)
        val items = (vm.folderItems.first { it is CheatsScreenUiState.Ready } as CheatsScreenUiState.Ready).data
        val family = items.filterIsInstance<CheatListItem.Family>().single { it.family.title == "Choose" }.family
        vm.selectFamilyOption(family, 493, 100)
        runCurrent()
        assertEquals(setOf(1L, 2L), pending(state).map { it.id }.toSet())
        assertEquals(WildEncounterCheat.code(493, 100), pending(state).single { it.enabled }.code)
        vm.commitCheatChanges()
        runCurrent()
        assertEquals(WildEncounterCheat.code(493, 100), repo.rows.getValue(1).code)
        assertFalse(repo.rows.getValue(2).enabled)
    }

    @Test fun folderItemsCollapseAFamilyIntoOneRowAtItsFirstMember() = withMain {
        val single = Cheat(9, 1, "Max IVs", null, "1206E012 0000201F 1206E028 0000201F", false)
        val levels = levelFolder(1, 5, 10, 20, 30)
        val mixed = levels.copy(cheats = listOf(single) + levels.cheats + selected)
        val repo = FakeRepository(mixed)
        val state = SavedStateHandle(mapOf(
            CheatsViewModel.KEY_SELECTED_GAME to GameParcelable.fromGame(game.copy(cheats = listOf(mixed))),
            CheatsViewModel.KEY_SELECTED_FOLDER to CheatFolderParcelable.fromCheatFolder(mixed),
        ))
        val vm = CheatsViewModel(repo, state)
        val items = (vm.folderItems.first { it is CheatsScreenUiState.Ready } as CheatsScreenUiState.Ready).data
        assertEquals(3, items.size)
        assertEquals(single, (items[0] as CheatListItem.Single).cheat)
        assertEquals(5, (items[1] as CheatListItem.Family).family.members.size)
        assertEquals("Choose", (items[2] as CheatListItem.Family).family.title)
    }

    @Test fun togglingAFamilyMemberFromTheExpandedListAppliesExclusions() = withMain {
        val levels = levelFolder(1, 5, 10, 20, 30)
        val repo = FakeRepository(folder, levels)
        val state = handle()
        val vm = CheatsViewModel(repo, state)
        vm.toggleCheat(levels.cheats[2])
        runCurrent()
        assertEquals(setOf(2L, 50L), pending(state).map { it.id }.toSet())
        assertEquals(listOf(50L), pending(state).filter { it.enabled }.map { it.id })
    }

    @Test fun disableFamilyTurnsTheActiveMemberOff() = withMain {
        val repo = FakeRepository(folder)
        val state = handle()
        val vm = CheatsViewModel(repo, state)
        vm.disableFamily(family("Prior"))
        runCurrent()
        assertEquals(listOf(prior.copy(enabled = false)), pending(state))
    }

    @Test fun genderAndNatureSelectionsAreExclusiveAndKeepTheSpeciesSelector() = withMain {
        fun folderOf(id: Long, name: String, codes: List<String>) = CheatFolder(id, name, codes.mapIndexed { i, c -> Cheat(id * 10 + i, 1, "Nature $i", null, c, false) })
        val natures = folderOf(44, "44 - Wild · nature", natureRows("nature"))
        val males = folderOf(45, "45 - Wild · male + nature", natureRows("male")).let { f -> f.copy(cheats = f.cheats.mapIndexed { i, c -> if (i == 0) c.copy(enabled = true) else c }) }
        val females = folderOf(46, "46 - Wild · female + nature", natureRows("female"))
        val repo = FakeRepository(folder, natures, males, females)
        val state = handle()
        val vm = CheatsViewModel(repo, state)
        vm.toggleCheat(females.cheats[0])
        runCurrent()
        assertEquals(listOf(460L), pending(state).filter { it.enabled }.map { it.id })
        assertFalse(pending(state).single { it.id == 450L }.enabled)
        vm.toggleCheat(natures.cheats[3])
        runCurrent()
        vm.commitCheatChanges()
        runCurrent()
        assertEquals(setOf(2L, 443L), repo.rows.values.filter { it.enabled }.map { it.id }.toSet())
    }

    @Test fun restoredFolderCanConfigureWithoutEverOpeningFolderList() = withMain {
        val repo = FakeRepository(folder)
        val state = handle()
        val vm = CheatsViewModel(repo, state)
        vm.selectFamilyOption(family("Choose"), 493, 100)
        runCurrent()
        assertEquals(setOf(1L, 2L), pending(state).map { it.id }.toSet())
        assertEquals(WildEncounterCheat.code(493, 100), pending(state).single { it.enabled }.code)
        vm.commitCheatChanges()
        runCurrent()
        assertEquals(WildEncounterCheat.code(493, 100), repo.rows.getValue(1).code)
        assertFalse(repo.rows.getValue(2).enabled)
    }

    @Test fun configurationBlocksCommitUntilFreshRepositoryReadCompletes() = withMain {
        val repo = FakeRepository(folder).apply { readGate = CompletableDeferred() }
        val state = handle()
        val vm = CheatsViewModel(repo, state)
        vm.selectFamilyOption(family("Choose"), 493, 100)
        assertTrue(vm.committingCheatsChangesState.value)
        vm.commitCheatChanges()
        runCurrent()
        assertEquals(0, repo.commits)
        assertTrue(vm.committingCheatsChangesState.value)
        repo.readGate!!.complete(Unit)
        runCurrent()
        assertFalse(vm.committingCheatsChangesState.value)
        vm.commitCheatChanges()
        runCurrent()
        assertEquals(1, repo.commits)
        assertEquals(WildEncounterCheat.code(493, 100), repo.rows.getValue(1).code)
    }

    @Test fun deletingPendingCheatDoesNotPoisonTheRemainingBatch() = withMain {
        val repo = FakeRepository(folder)
        val state = handle()
        val vm = CheatsViewModel(repo, state)
        vm.toggleCheat(selected)
        runCurrent()
        vm.deleteCheat(selected.copy(enabled = true))
        runCurrent()
        assertEquals(listOf(2L), pending(state).map { it.id })
        vm.commitCheatChanges()
        runCurrent()
        assertFalse(repo.rows.containsKey(1))
        assertFalse(repo.rows.getValue(2).enabled)
        assertEquals(1, repo.commits)
    }

    @Test fun undoKeepsEffectiveSelectionWithoutReintroducingDeletedId() = withMain {
        val repo = FakeRepository(folder)
        val state = handle()
        val vm = CheatsViewModel(repo, state)
        vm.toggleCheat(selected)
        runCurrent()
        vm.deleteCheat(selected.copy(enabled = true))
        runCurrent()
        vm.undoCheatDeletion(selected)
        runCurrent()
        assertFalse(pending(state).any { it.id == 1L })
        assertFalse(repo.rows.containsKey(1))
        assertTrue(repo.rows.getValue(1000).enabled)
        assertEquals(selected.code, repo.rows.getValue(1000).code)
    }

    @Test fun enablingStandaloneLevelDisablesSelectorAndKeepsStandaloneSpecies() = withMain {
        val species = selected.copy(id = 3, name = "Species only", code = "94000130 FDFF0000 6211186C 00000000 B211186C 00000000 D5000000 00000019 C0000000 00000027 D7000000 00032A48 D2000000 00000000", enabled = true)
        val levels = levelFolder(1, 5, 10, 20, 100)
        val repo = FakeRepository(folder.copy(cheats = listOf(prior, species)), levels)
        val state = handle()
        val vm = CheatsViewModel(repo, state)
        vm.toggleCheat(levels.cheats[4])
        runCurrent()
        assertEquals(setOf(2L, 140L), pending(state).map { it.id }.toSet())
        assertEquals(listOf(140L), pending(state).filter { it.enabled }.map { it.id })
        vm.commitCheatChanges()
        runCurrent()
        assertTrue(repo.rows.getValue(3).enabled)
        assertTrue(repo.rows.getValue(140).enabled)
        assertFalse(repo.rows.getValue(2).enabled)
    }

    @Test fun failedConfigurationKeepsPendingChangesAndReportsWithoutExit() = withMain {
        val repo = FakeRepository(folder).apply { failRead = true }
        val state = handle()
        val vm = CheatsViewModel(repo, state)
        val unrelated = selected.copy(id = 3, name = "Unrelated", code = "D2000000 00000000", enabled = true)
        vm.toggleCheat(unrelated)
        val before = pending(state)
        vm.selectFamilyOption(family("Choose"), 493, 100)
        runCurrent()
        assertEquals(before, pending(state))
        assertFalse(vm.committingCheatsChangesState.value)
        assertEquals(Unit, vm.cheatModificationFailedEvent.first())
        assertEquals(0, repo.commits)
    }

    @Test fun undoOfActiveSelectorReplacesASelectionEnabledAfterDeletion() = withMain {
        val repo = FakeRepository(folder)
        val state = handle()
        val vm = CheatsViewModel(repo, state)
        vm.toggleCheat(selected)
        runCurrent()
        vm.deleteCheat(selected.copy(enabled = true))
        runCurrent()
        vm.toggleCheat(prior.copy(enabled = false))
        runCurrent()
        vm.undoCheatDeletion(selected)
        runCurrent()
        vm.commitCheatChanges()
        runCurrent()
        assertEquals(listOf(1000L), repo.rows.values.filter { it.enabled }.map { it.id })
    }

    /** In-memory repository over one or more folders; cheats are keyed by id, folders keep their membership. */
    private class FakeRepository(vararg folders: CheatFolder) : CheatsRepository {
        private val shells = folders.map { it.copy(cheats = emptyList()) }
        private val folderOf = folders.flatMap { f -> f.cheats.map { requireNotNull(it.id) to f.id } }.toMap().toMutableMap()
        val rows = folders.flatMap { it.cheats }.associateBy { requireNotNull(it.id) }.toMutableMap()
        var commits = 0
        var failRead = false
        var readGate: CompletableDeferred<Unit>? = null
        private fun folders() = shells.map { shell -> shell.copy(cheats = rows.values.filter { folderOf[it.id] == shell.id }) }
        override fun getAllGameCheats(game: Game): Flow<List<CheatFolder>> = flow {
            check(!failRead)
            readGate?.await()
            emit(folders())
        }
        override fun getFolderCheats(folder: CheatFolder) = flowOf(rows.values.filter { folderOf[it.id] == folder.id })
        override suspend fun updateCheats(cheats: List<Cheat>) {
            check(cheats.all { rows.containsKey(it.id) })
            cheats.forEach { rows[requireNotNull(it.id)] = it }
            commits++
        }
        override suspend fun deleteCheat(cheat: Cheat) { rows.remove(cheat.id) }
        override suspend fun addCheat(folder: CheatFolder, cheat: Cheat) { rows[1000] = cheat.copy(id = 1000); folderOf[1000] = folder.id }
        override suspend fun updateCheat(cheat: Cheat) { rows[requireNotNull(cheat.id)] = cheat }
        override suspend fun getGames(): List<Game> = error("unused")
        override suspend fun findGameForRom(romInfo: RomInfo): Game? = error("unused")
        override suspend fun getRomEnabledCheats(romInfo: RomInfo): List<Cheat> = error("unused")
        override suspend fun updateCheatsStatus(cheats: List<Cheat>): Unit = error("unused")
        override suspend fun addCheatFolder(folderName: String, game: Game): Unit = error("unused")
        override suspend fun deleteImportedCheatDatabases(): Unit = error("unused")
        override suspend fun addCheatDatabase(databaseName: String): CheatDatabase = error("unused")
        override suspend fun addGameCheats(game: Game): Game = error("unused")
        override suspend fun addCustomCheat(folder: CheatFolder, cheatForm: CheatSubmissionForm): Unit = error("unused")
        override fun importCheats(uri: Uri): Unit = error("unused")
        override fun isCheatImportOngoing(): Boolean = false
        override fun getCheatImportProgress(): Flow<CheatImportProgress> = emptyFlow()
    }
}
