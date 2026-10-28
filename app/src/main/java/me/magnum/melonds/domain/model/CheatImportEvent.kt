package me.magnum.melonds.domain.model

/** Parsed data awaiting one atomic database replacement; game database IDs are assigned on import. */
sealed interface CheatImportEvent {
    data class Start(val name: String) : CheatImportEvent
    data class ParsedGame(val game: Game) : CheatImportEvent
    data object Complete : CheatImportEvent
}
