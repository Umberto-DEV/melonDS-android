package me.magnum.melonds.ui.cheats

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import me.magnum.melonds.common.suspendRunCatching
import me.magnum.melonds.domain.model.Cheat
import me.magnum.melonds.common.cheats.ModifierFamilies
import me.magnum.melonds.common.cheats.ModifierFamily
import me.magnum.melonds.common.cheats.WildEncounterCheat
import me.magnum.melonds.domain.model.CheatFolder
import me.magnum.melonds.domain.model.CheatInFolder
import me.magnum.melonds.domain.model.Game
import me.magnum.melonds.domain.repositories.CheatsRepository
import me.magnum.melonds.parcelables.RomInfoParcelable
import me.magnum.melonds.parcelables.cheat.CheatFolderParcelable
import me.magnum.melonds.parcelables.cheat.CheatParcelable
import me.magnum.melonds.parcelables.cheat.GameParcelable
import me.magnum.melonds.ui.cheats.model.CheatListItem
import me.magnum.melonds.ui.cheats.model.CheatSubmissionForm
import me.magnum.melonds.ui.cheats.model.CheatsScreenUiState
import me.magnum.melonds.ui.cheats.model.DeletedCheat
import me.magnum.melonds.ui.cheats.model.OpenScreenEvent
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class CheatsViewModel @Inject constructor(
    private val cheatsRepository: CheatsRepository,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    companion object {
        const val KEY_MODIFIED_CHEATS = "modified_cheats"
        const val KEY_SELECTED_GAME = "selected_game"
        const val KEY_SELECTED_FOLDER = "selected_folder"
    }

    private val romInfo = savedStateHandle.get<RomInfoParcelable>(CheatsActivity.KEY_ROM_INFO)?.toRomInfo()
    private val modifiedCheatSet = MutableStateFlow(savedStateHandle.get<List<CheatParcelable>>(KEY_MODIFIED_CHEATS).orEmpty().map { it.toCheat() })
    private val deletedCheats = mutableListOf<DeletedCheat>()

    private val selectedGame = savedStateHandle.getStateFlow<GameParcelable?>(KEY_SELECTED_GAME, null).map { it?.toGame() }
    private val selectedCheatFolder = savedStateHandle.getStateFlow<CheatFolderParcelable?>(KEY_SELECTED_FOLDER, null).map { it?.toCheatFolder() }

    val games by lazy {
        flow {
            emit(CheatsScreenUiState.Loading())
            val games = cheatsRepository.getGames()
            emit(CheatsScreenUiState.Ready(games))
        }.shareIn(viewModelScope, started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 1000L), replay = 1)
    }

    val folders by lazy {
        selectedGame.flatMapLatest {
            flow {
                if (it == null) {
                    // No game is selected. Try to load it based on the ROM info
                    if (romInfo == null) {
                        // Should never happen
                        emit(CheatsScreenUiState.Ready(emptyList()))
                    } else {
                        emit(CheatsScreenUiState.Loading())
                        val game = cheatsRepository.findGameForRom(romInfo)
                        if (game != null) {
                            // This will reset the flow and will load the folders for this game
                            savedStateHandle[KEY_SELECTED_GAME] = GameParcelable.fromGame(game)
                        } else {
                            emit(CheatsScreenUiState.Ready(emptyList()))
                        }
                    }
                } else {
                    emit(CheatsScreenUiState.Loading())
                    cheatsRepository.getAllGameCheats(it)
                        .map { CheatsScreenUiState.Ready(it) }
                        .collect(this)
                }
            }
        }.shareIn(viewModelScope, started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 1000L), replay = 1)
    }

    val folderCheats by lazy {
        selectedCheatFolder.filterNotNull()
            .flatMapLatest { cheatsRepository.getFolderCheats(it) }
            .flatMapLatest { cheats ->
                modifiedCheatSet.map { modifiedCheats ->
                    val cheats = cheats.toMutableList()
                    modifiedCheats.forEach { cheat ->
                        val originalCheatIndex = cheats.indexOfFirst { it.id == cheat.id }
                        if (originalCheatIndex >= 0) {
                            cheats[originalCheatIndex] = cheat
                        }
                    }

                    CheatsScreenUiState.Ready(cheats as List<Cheat>)
                }
            }.shareIn(viewModelScope, started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 1000L), replay = 1)
    }

    /** The folder list with every modifier family collapsed into one row, placed where its first member was. */
    val folderItems: SharedFlow<CheatsScreenUiState<List<CheatListItem>>> by lazy {
        combine(folderCheats, selectedCheatFolder.filterNotNull()) { state: CheatsScreenUiState<List<Cheat>>, folder: CheatFolder ->
            when (state) {
                is CheatsScreenUiState.Loading -> CheatsScreenUiState.Loading<List<CheatListItem>>()
                is CheatsScreenUiState.Ready -> CheatsScreenUiState.Ready<List<CheatListItem>>(listItems(folder.name, state.data))
            }
        }.shareIn(viewModelScope, started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 1000L), replay = 1)
    }

    private fun listItems(folderName: String, cheats: List<Cheat>): List<CheatListItem> {
        val families = ModifierFamilies.recognize(folderName, cheats)
        val placed = mutableSetOf<Long?>()
        return cheats.mapNotNull { cheat ->
            val family = families.firstOrNull { it.contains(cheat) } ?: return@mapNotNull CheatListItem.Single(cheat)
            val first = family.members.first().id
            if (first in placed) null else CheatListItem.Family(family).also { placed += first }
        }
    }

    private fun canonicalFor(game: Game): ((Int, Int) -> String)? =
        if (WildEncounterCheat.supports(game.gameCode, game.gameChecksum)) WildEncounterCheat::code else null

    /** Folders as the repository has them, with pending (uncommitted) changes applied on top. */
    private suspend fun currentGameFolders(game: Game): List<CheatFolder> {
        val pending = modifiedCheatSet.value.associateBy { it.id }
        return cheatsRepository.getAllGameCheats(game).first().map { folder -> folder.copy(cheats = folder.cheats.map { pending[it.id] ?: it }) }
    }

    fun selectFamilyOption(family: ModifierFamily, value: Int, level: Int?) {
        val game = savedStateHandle.get<GameParcelable>(KEY_SELECTED_GAME)?.toGame() ?: return
        modifyCheats {
            val families = currentGameFolders(game).flatMap { ModifierFamilies.recognize(it) }
            val live = families.firstOrNull { it.sameAs(family) } ?: return@modifyCheats
            stageCheats(ModifierFamilies.select(live, value, level, families, canonicalFor(game)))
        }
    }

    fun disableFamily(family: ModifierFamily) {
        val game = savedStateHandle.get<GameParcelable>(KEY_SELECTED_GAME)?.toGame() ?: return
        modifyCheats {
            val live = currentGameFolders(game).flatMap { ModifierFamilies.recognize(it) }.firstOrNull { it.sameAs(family) } ?: return@modifyCheats
            stageCheats(ModifierFamilies.disable(live))
        }
    }

    val selectedGameCheats: SharedFlow<CheatsScreenUiState<List<CheatInFolder>>> by lazy {
        selectedGame.flatMapLatest {
            if (it == null) {
                flowOf(emptyList())
            } else {
                cheatsRepository.getAllGameCheats(it)
            }
        }.flatMapLatest { allGameCheats ->
            // Enabled cheats when the user entered the screen
            val gameCheats = allGameCheats.flatMap { folder ->
                folder.cheats.filter { it.enabled }.map { CheatInFolder(it, folder.name) }
            }.toMutableList()

            // Merge initially enabled cheats with latest changes
            modifiedCheatSet.value.forEach { cheat ->
                val originalCheatIndex = gameCheats.indexOfFirst { it.cheat.id == cheat.id }
                if (originalCheatIndex >= 0) {
                    if (cheat.enabled) {
                        gameCheats[originalCheatIndex] = CheatInFolder(cheat, gameCheats[originalCheatIndex].folderName)
                    } else {
                        gameCheats.removeAt(originalCheatIndex)
                    }
                } else if (cheat.enabled) {
                    val folder = allGameCheats.firstOrNull { it.cheats.any { it.id == cheat.id } }
                    if (folder != null) {
                        gameCheats.add(CheatInFolder(cheat, folder.name))
                    }
                }
            }

            // Update cheats as they are modified. Skip first event since we already have the up-to-date list at this point
            modifiedCheatSet.drop(1).map {
                it.forEach { cheat ->
                    val originalCheatIndex = gameCheats.indexOfFirst { it.cheat.id == cheat.id }
                    if (originalCheatIndex >= 0) {
                        gameCheats[originalCheatIndex] = CheatInFolder(cheat, gameCheats[originalCheatIndex].folderName)
                    }
                }
                CheatsScreenUiState.Ready(gameCheats.toList())
            }.onStart { emit(CheatsScreenUiState.Ready(gameCheats.toList())) }
        }.shareIn(viewModelScope, started = SharingStarted.WhileSubscribed(), replay = 1)
    }

    private val _openGamesEvent = Channel<OpenScreenEvent>(Channel.CONFLATED)
    val openGamesEvent = _openGamesEvent.receiveAsFlow()

    private val _openFoldersEvent = Channel<OpenScreenEvent>(Channel.CONFLATED)
    val openFoldersEvent = _openFoldersEvent.receiveAsFlow()

    private val _openCheatsEvent = Channel<OpenScreenEvent>(Channel.CONFLATED)
    val openCheatsEvent = _openCheatsEvent.receiveAsFlow()

    private val _openEnabledCheatsEvent = Channel<Unit>(Channel.CONFLATED)
    val openEnabledCheatsEvent = _openEnabledCheatsEvent.receiveAsFlow()

    private val _committingCheatsChangesState = MutableStateFlow(false)
    val committingCheatsChangesState = _committingCheatsChangesState.asStateFlow()

    private val _cheatChangesCommittedEvent = Channel<Boolean>(Channel.CONFLATED)
    val cheatChangesCommittedEvent = _cheatChangesCommittedEvent.receiveAsFlow()

    private val _cheatModificationFailedEvent = Channel<Unit>(Channel.CONFLATED)
    val cheatModificationFailedEvent = _cheatModificationFailedEvent.receiveAsFlow()

    private fun stageCheats(changes: List<Cheat>) {
        modifiedCheatSet.update { old ->
            (old.filter { existing -> changes.none { it.id == existing.id } } + changes).also {
                savedStateHandle[KEY_MODIFIED_CHEATS] = it.map(CheatParcelable::fromCheat)
            }
        }
    }

    private fun modifyCheats(action: suspend () -> Unit) {
        if (committingCheatsChangesState.value) return
        // Set this before launching so Back cannot commit an incomplete selection.
        _committingCheatsChangesState.value = true
        viewModelScope.launch {
            try {
                suspendRunCatching { action() }
                    .onFailure { _cheatModificationFailedEvent.trySend(Unit) }
            } finally {
                _committingCheatsChangesState.value = false
            }
        }
    }

    fun setSelectedGame(game: Game) {
        savedStateHandle[KEY_SELECTED_GAME] = GameParcelable.fromGame(game)
        _openFoldersEvent.trySend(OpenScreenEvent(game.name))
    }

    fun setSelectedFolder(folder: CheatFolder) {
        savedStateHandle[KEY_SELECTED_FOLDER] = CheatFolderParcelable.fromCheatFolder(folder)
        _openCheatsEvent.trySend(OpenScreenEvent(folder.name))
    }

    fun addFolder(folderName: String) {
        if (folderName.isBlank()) return

        val selectedGame = savedStateHandle.get<GameParcelable>(KEY_SELECTED_GAME)?.toGame()

        viewModelScope.launch {
            val folderGame = if (selectedGame != null) {
                selectedGame
            } else {
                if (romInfo == null) {
                    // Should never happen
                    return@launch
                } else {
                    // A game needs to be created to be associated with the folder
                    val newGame = Game(
                        id = null,
                        name = romInfo.gameName,
                        gameCode = romInfo.gameCode,
                        gameChecksum = romInfo.headerChecksumString(),
                        cheats = emptyList()
                    )
                    cheatsRepository.addGameCheats(newGame)
                }
            }

            cheatsRepository.addCheatFolder(folderName, folderGame)
            if (selectedGame == null) {
                // Update selected game with the new one
                savedStateHandle[KEY_SELECTED_GAME] = GameParcelable.fromGame(folderGame)
            }
        }
    }

    fun toggleCheat(cheat: Cheat) {
        if (committingCheatsChangesState.value) return
        val effective = modifiedCheatSet.value.firstOrNull { it.id == cheat.id } ?: cheat
        val game = savedStateHandle.get<GameParcelable>(KEY_SELECTED_GAME)?.toGame()
        if (!effective.enabled && game != null) {
            // Enabling a member of a family replaces whatever else is active in its exclusion group.
            modifyCheats {
                val families = currentGameFolders(game).flatMap { ModifierFamilies.recognize(it) }
                val family = families.firstOrNull { it.contains(effective) && it.exclusionGroups.isNotEmpty() }
                stageCheats(if (family != null) ModifierFamilies.activate(family, effective, families) else listOf(effective.copy(enabled = true)))
            }
        } else {
            stageCheats(listOf(effective.copy(enabled = !effective.enabled)))
        }
    }

    fun addNewCheat(cheatSubmissionForm: CheatSubmissionForm) {
        if (!cheatSubmissionForm.isValid()) return
        val selectedFolder = savedStateHandle.get<CheatFolderParcelable>(KEY_SELECTED_FOLDER) ?: return

        viewModelScope.launch {
            cheatsRepository.addCustomCheat(selectedFolder.toCheatFolder(), cheatSubmissionForm)
        }
    }

    fun updateCheat(originalCheat: Cheat, cheatSubmissionForm: CheatSubmissionForm) {
        if (!cheatSubmissionForm.isValid() || committingCheatsChangesState.value) return
        val effective = modifiedCheatSet.value.firstOrNull { it.id == originalCheat.id } ?: originalCheat
        if (effective.name == cheatSubmissionForm.name && effective.description == cheatSubmissionForm.description && effective.code == cheatSubmissionForm.code) return
        val updatedCheat = effective.copy(
            name = cheatSubmissionForm.name,
            description = cheatSubmissionForm.description.takeUnless { it.isBlank() },
            code = cheatSubmissionForm.code,
        )
        modifyCheats {
            cheatsRepository.updateCheat(updatedCheat)
            modifiedCheatSet.update { pending ->
                pending.map { if (it.id == updatedCheat.id) updatedCheat else it }.also {
                    savedStateHandle[KEY_MODIFIED_CHEATS] = it.map(CheatParcelable::fromCheat)
                }
            }
        }
    }

    fun deleteCheat(cheat: Cheat) {
        val selectedFolder = savedStateHandle.get<CheatFolderParcelable>(KEY_SELECTED_FOLDER) ?: return
        modifyCheats {
            val effective = modifiedCheatSet.value.firstOrNull { it.id == cheat.id } ?: cheat
            cheatsRepository.deleteCheat(effective)
            deletedCheats.add(DeletedCheat(effective, selectedFolder.toCheatFolder()))
            modifiedCheatSet.update { pending ->
                pending.filterNot { it.id == cheat.id }.also {
                    savedStateHandle[KEY_MODIFIED_CHEATS] = it.map(CheatParcelable::fromCheat)
                }
            }
        }
    }

    fun undoCheatDeletion(cheat: Cheat) {
        val deletedCheat = deletedCheats.firstOrNull { it.cheat.id == cheat.id } ?: return
        modifyCheats {
            val restored = deletedCheat.cheat
            val game = savedStateHandle.get<GameParcelable>(KEY_SELECTED_GAME)?.toGame()
            val conflicts = if (restored.enabled && game != null) {
                // The restored cheat is not in the repository yet: recognise families with it put back in its folder.
                val folders = currentGameFolders(game).map { if (it.id == deletedCheat.folder.id) it.copy(cheats = it.cheats + restored) else it }
                val families = folders.flatMap { ModifierFamilies.recognize(it) }
                families.firstOrNull { it.contains(restored) && it.exclusionGroups.isNotEmpty() }
                    ?.let { ModifierFamilies.exclusions(it, restored.id, families) }.orEmpty()
            } else emptyList()
            // addCheat allocates a new ID; the deleted ID must not be staged again.
            cheatsRepository.addCheat(deletedCheat.folder, restored)
            stageCheats(conflicts)
            deletedCheats.remove(deletedCheat)
        }
    }

    fun openEnabledCheats() {
        _openEnabledCheatsEvent.trySend(Unit)
    }

    fun commitCheatChanges() {
        if (committingCheatsChangesState.value) {
            // Already commiting changes. Do nothing
            return
        }

        if (modifiedCheatSet.value.isEmpty()) {
            _cheatChangesCommittedEvent.trySend(true)
            return
        }

        _committingCheatsChangesState.value = true
        viewModelScope.launch {
            suspendRunCatching {
                cheatsRepository.updateCheats(modifiedCheatSet.value)
            }.fold(
                onSuccess = { _cheatChangesCommittedEvent.trySend(true) },
                onFailure = { _cheatChangesCommittedEvent.trySend(false) },
            )
            _committingCheatsChangesState.value = false
        }
    }
}
