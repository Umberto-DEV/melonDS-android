package me.magnum.melonds.common.cheats

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import me.magnum.melonds.domain.model.Game
import org.junit.Assert.*
import org.junit.Test
import java.io.ByteArrayInputStream
import java.util.concurrent.atomic.AtomicInteger

class CheatImportEventsTest {
    @Test fun cancellingTheConsumerStopsTheBoundedProducer() = runBlocking {
        val attempted = AtomicInteger()
        val stopped = CompletableDeferred<Unit>()
        val parser = object : CheatDatabaseParser {
            override fun parseCheatDatabase(databaseStream: ProgressTrackerInputStream, parseListener: CheatDatabaseParserListener) {
                try {
                    parseListener.onDatabaseParseStart("Many games")
                    repeat(10000) {
                        attempted.incrementAndGet()
                        parseListener.onGameParsed(Game(null, "Game $it", "TEST", "12345678", emptyList()))
                    }
                    parseListener.onParseComplete()
                } finally { stopped.complete(Unit) }
            }
        }
        withTimeout(5000) {
            val events = parser.importEvents(ProgressTrackerInputStream(ByteArrayInputStream(byteArrayOf()))) {}.take(2).toList()
            stopped.await()
            assertEquals(2, events.size)
            assertTrue("Producer must not accumulate the entire database", attempted.get() < 100)
        }
    }

    @Test(timeout = 10000)
    fun cancellingRealDatParserStopsItsNestedProducerAndWorkers() = runBlocking {
        val stream = ProgressTrackerInputStream(ByteArrayInputStream(UsrCheatFixture.manyGames(100)))
        withTimeout(5000) {
            assertEquals(2, UsrCheatDatabaseParser().importEvents(stream) {}.take(2).toList().size)
        }
    }
}
