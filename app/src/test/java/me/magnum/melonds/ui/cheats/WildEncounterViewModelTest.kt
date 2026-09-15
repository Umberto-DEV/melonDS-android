package me.magnum.melonds.ui.cheats

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.*
import me.magnum.melonds.common.cheats.WildEncounterCheat
import me.magnum.melonds.domain.model.*
import me.magnum.melonds.domain.repositories.CheatsRepository
import me.magnum.melonds.parcelables.cheat.*
import me.magnum.melonds.ui.cheats.model.CheatSubmissionForm
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = android.app.Application::class)
class WildEncounterViewModelTest {
    private val selected = Cheat(1, 1, "Choose", null, WildEncounterCheat.code(25, 10), false)
    private val prior = selected.copy(id = 2, name = "Prior", enabled = true)
    private val folder = CheatFolder(1, "Wild", listOf(selected, prior))
    private val game = Game(1, "SGP", "IPKE", "19D1EEBB", listOf(folder))
    private val form = CheatSubmissionForm("Arceus 100", "Instructions", WildEncounterCheat.code(493, 100))

    private fun handle() = SavedStateHandle(mapOf(
        CheatsViewModel.KEY_SELECTED_GAME to GameParcelable.fromGame(game),
        CheatsViewModel.KEY_SELECTED_FOLDER to CheatFolderParcelable.fromCheatFolder(folder),
    ))
    private fun pending(handle: SavedStateHandle) = handle.get<List<CheatParcelable>>(CheatsViewModel.KEY_MODIFIED_CHEATS).orEmpty().map { it.toCheat() }

    @Test fun genderAndNatureSelectionsAreExclusiveAndKeepTheSpeciesSelector() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val codes = requireNotNull(javaClass.getResourceAsStream("/sgp-nature-codes.tsv")).bufferedReader().readLines()
                .map { it.split('\t') }.filter { it[1] == "3" }.associate { it[0] to it[2] }
            val male = selected.copy(id = 3, code = codes.getValue("male"), enabled = true)
            val female = selected.copy(id = 4, code = codes.getValue("female"))
            val nature = selected.copy(id = 5, code = codes.getValue("nature"))
            val repo = FakeRepository(folder.copy(cheats = listOf(prior, male, female, nature)))
            val state = handle()
            val vm = CheatsViewModel(repo, state)
            vm.toggleCheat(female)
            runCurrent()
            assertEquals(listOf(4L), pending(state).filter { it.enabled }.map { it.id })
            vm.toggleCheat(nature)
            runCurrent()
            vm.commitCheatChanges()
            runCurrent()
            assertEquals(setOf(2L, 5L), repo.rows.values.filter { it.enabled }.map { it.id }.toSet())
        } finally { Dispatchers.resetMain() }
    }

    @Test fun restoredFolderCanConfigureWithoutEverOpeningFolderList() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val repo = FakeRepository(folder)
            val state = handle()
            val vm = CheatsViewModel(repo, state)
            vm.configureWildEncounter(selected, form)
            runCurrent()
            assertEquals(setOf(1L, 2L), pending(state).map { it.id }.toSet())
            assertEquals(form.code, pending(state).single { it.enabled }.code)
            vm.commitCheatChanges()
            runCurrent()
            assertEquals(form.code, repo.rows.getValue(1).code)
            assertFalse(repo.rows.getValue(2).enabled)
        } finally { Dispatchers.resetMain() }
    }

    @Test fun configurationBlocksCommitUntilFreshRepositoryReadCompletes() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val repo = FakeRepository(folder).apply { readGate = CompletableDeferred() }
            val state = handle()
            val vm = CheatsViewModel(repo, state)
            vm.configureWildEncounter(selected, form)
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
            assertEquals(form.code, repo.rows.getValue(1).code)
        } finally { Dispatchers.resetMain() }
    }

    @Test fun deletingPendingCheatDoesNotPoisonTheRemainingBatch() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
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
        } finally { Dispatchers.resetMain() }
    }

    @Test fun undoKeepsEffectiveSelectionWithoutReintroducingDeletedId() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
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
        } finally { Dispatchers.resetMain() }
    }

    @Test fun enablingStandaloneLevelDisablesSelectorAndKeepsStandaloneSpecies() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val species = selected.copy(id = 3, code = "94000130 FDFF0000 6211186C 00000000 B211186C 00000000 D5000000 00000019 C0000000 00000027 D7000000 00032A48 D2000000 00000000", enabled = true)
            val level = selected.copy(id = 4, code = "52246C94 28038800 12247BEC 00002064 D2000000 00000000")
            val repo = FakeRepository(folder.copy(cheats = listOf(prior, species, level)))
            val state = handle()
            val vm = CheatsViewModel(repo, state)
            vm.toggleCheat(level)
            runCurrent()
            assertEquals(setOf(2L, 4L), pending(state).map { it.id }.toSet())
            assertEquals(listOf(4L), pending(state).filter { it.enabled }.map { it.id })
            vm.commitCheatChanges()
            runCurrent()
            assertTrue(repo.rows.getValue(3).enabled)
            assertTrue(repo.rows.getValue(4).enabled)
            assertFalse(repo.rows.getValue(2).enabled)
        } finally { Dispatchers.resetMain() }
    }

    @Test fun failedConfigurationKeepsPendingChangesAndReportsWithoutExit() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val repo = FakeRepository(folder).apply { failRead = true }
            val state = handle()
            val vm = CheatsViewModel(repo, state)
            val unrelated = selected.copy(id = 3, code = "D2000000 00000000")
            vm.toggleCheat(unrelated)
            val before = pending(state)
            vm.configureWildEncounter(selected, form)
            runCurrent()
            assertEquals(before, pending(state))
            assertFalse(vm.committingCheatsChangesState.value)
            assertEquals(Unit, vm.cheatModificationFailedEvent.first())
            assertEquals(0, repo.commits)
        } finally { Dispatchers.resetMain() }
    }

    @Test fun undoOfActiveSelectorReplacesASelectionEnabledAfterDeletion() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
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
        } finally { Dispatchers.resetMain() }
    }

    private class FakeRepository(private val folder: CheatFolder) : CheatsRepository {
        val rows = folder.cheats.associateBy { requireNotNull(it.id) }.toMutableMap()
        var commits = 0
        var failRead = false
        var readGate: CompletableDeferred<Unit>? = null
        override fun getAllGameCheats(game: Game): Flow<List<CheatFolder>> = flow {
            check(!failRead)
            readGate?.await()
            emit(listOf(folder.copy(cheats = rows.values.toList())))
        }
        override fun getFolderCheats(folder: CheatFolder) = flowOf(rows.values.toList())
        override suspend fun updateCheats(cheats: List<Cheat>) {
            check(cheats.all { rows.containsKey(it.id) })
            cheats.forEach { rows[requireNotNull(it.id)] = it }
            commits++
        }
        override suspend fun deleteCheat(cheat: Cheat) { rows.remove(cheat.id) }
        override suspend fun addCheat(folder: CheatFolder, cheat: Cheat) { rows[1000] = cheat.copy(id = 1000) }
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
