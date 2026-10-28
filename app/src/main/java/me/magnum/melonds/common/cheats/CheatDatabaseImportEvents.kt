package me.magnum.melonds.common.cheats

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.flowOn
import me.magnum.melonds.domain.model.CheatDatabase
import me.magnum.melonds.domain.model.CheatImportEvent
import me.magnum.melonds.domain.model.Game

/** Keep blocking parsers and their callbacks off Room's transaction executor.
 * Only a few games can wait in memory; a closed/cancelled consumer also stops the producer. */
fun CheatDatabaseParser.importEvents(
    stream: ProgressTrackerInputStream,
    onGameStart: (String) -> Unit,
): Flow<CheatImportEvent> = channelFlow {
    fun emit(event: CheatImportEvent) { trySendBlocking(event).getOrThrow() }
    parseCheatDatabase(stream, object : CheatDatabaseParserListener {
        override fun onDatabaseParseStart(databaseName: String): CheatDatabase {
            emit(CheatImportEvent.Start(databaseName))
            return CheatDatabase(0, databaseName) // Replaced with the persisted ID by the repository.
        }
        override fun onGameParseStart(gameName: String) = onGameStart(gameName)
        override fun onGameParsed(game: Game) = emit(CheatImportEvent.ParsedGame(game))
        override fun onParseComplete() = emit(CheatImportEvent.Complete)
    })
}.buffer(4).flowOn(Dispatchers.IO)
