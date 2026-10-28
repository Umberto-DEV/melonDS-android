package me.magnum.melonds.common.cheats

import me.magnum.melonds.domain.model.CheatDatabase
import me.magnum.melonds.domain.model.Game
import org.junit.Assert.*
import org.junit.Test
import java.io.ByteArrayInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

/** A synthetic R4 database: one game, one literal Action Replay code; no commercial data. */
object UsrCheatFixture {
    fun bytes(): ByteArray {
        val data = ByteArray(356)
        "R4 CheatCode".toByteArray().copyInto(data)
        "Probe database".toByteArray().copyInto(data, 16)
        val b = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN)
        b.position(256)
        b.put("TEST".toByteArray()); b.putInt(0x12345678); b.putInt(288); b.putInt(0)
        // The next 16 bytes remain zero: end of the address book.
        b.position(288)
        b.put("Probe\u0000\u0000\u0000".toByteArray())
        b.putInt(1); b.put(ByteArray(32)); b.putInt(4)
        b.put("Test\u0000\u0000\u0000\u0000".toByteArray())
        b.putInt(2); b.putInt(0x02000000); b.putInt(1)
        return data
    }
}

class UsrCheatStreamingTest {
    private class Response(data: ByteArray, private val chunkSize: Int = Int.MAX_VALUE) : ByteArrayInputStream(data) {
        private var eofReads = 0
        override fun read(b: ByteArray, off: Int, len: Int): Int =
            super.read(b, off, minOf(len, chunkSize)).also {
                // A bounded oracle for the old infinite skip loop. A parser failure is an Exception;
                // repeated reads after EOF are an AssertionError, so the regression fails promptly.
                if (it == -1 && ++eofReads > 32) throw AssertionError("Parser repeatedly reads EOF instead of rejecting the offset")
            }
    }
    private class Listener : CheatDatabaseParserListener {
        val games = mutableListOf<Game>()
        var complete = false
        override fun onDatabaseParseStart(databaseName: String) = CheatDatabase(7, databaseName)
        override fun onGameParseStart(gameName: String) = Unit
        override fun onGameParsed(game: Game) { games += game }
        override fun onParseComplete() { complete = true }
    }
    private fun assertValid(chunkSize: Int) {
        val stream = ProgressTrackerInputStream(Response(UsrCheatFixture.bytes(), chunkSize))
        val listener = Listener()
        UsrCheatDatabaseParser().parseCheatDatabase(stream, listener)
        assertTrue(listener.complete)
        val game = listener.games.single()
        assertEquals("TEST", game.gameCode)
        assertEquals("12345678", game.gameChecksum)
        assertEquals("Probe", game.name)
        val cheat = game.cheats.single().cheats.single()
        assertEquals("02000000 00000001", cheat.code)
        assertEquals(7L, cheat.cheatDatabaseId)
        assertEquals(356, stream.totalReadBytes)
    }
    @Test fun fullResponsePreservesContentAndProgress() = assertValid(Int.MAX_VALUE)
    @Test fun fragmentedResponsePreservesContentAndProgress() = assertValid(7)
    @Test fun truncatedAddressBookFailsWithoutCompletion() {
        val listener = Listener()
        assertThrows(Exception::class.java) {
            UsrCheatDatabaseParser().parseCheatDatabase(ProgressTrackerInputStream(Response(UsrCheatFixture.bytes().copyOf(265))), listener)
        }
        assertFalse(listener.complete)
        assertTrue(listener.games.isEmpty())
    }
    @Test fun offsetPastEndFailsWithoutLoopingOrCompletion() {
        val data = UsrCheatFixture.bytes().copyOf(288)
        ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN).putInt(264, 320)
        val listener = Listener()
        assertThrows(Exception::class.java) {
            UsrCheatDatabaseParser().parseCheatDatabase(ProgressTrackerInputStream(Response(data)), listener)
        }
        assertFalse(listener.complete)
        assertTrue(listener.games.isEmpty())
    }
    @Test fun truncatedGameFailsWithoutCompletion() {
        val listener = Listener()
        assertThrows(Exception::class.java) {
            UsrCheatDatabaseParser().parseCheatDatabase(ProgressTrackerInputStream(Response(UsrCheatFixture.bytes().copyOf(350))), listener)
        }
        assertFalse(listener.complete)
    }
    @Test fun progressCountsSingleAndBulkReadsButNeverEof() {
        val stream = ProgressTrackerInputStream(ByteArrayInputStream(byteArrayOf(1, 2, 3)))
        assertEquals(1, stream.read())
        assertEquals(1, stream.totalReadBytes)
        assertEquals(2, stream.read(ByteArray(10)))
        assertEquals(3, stream.totalReadBytes)
        repeat(3) { assertEquals(-1, stream.read(ByteArray(10))) }
        assertEquals(-1, stream.read())
        assertEquals(3, stream.totalReadBytes)
    }
}
