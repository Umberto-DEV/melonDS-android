package me.magnum.melonds.impl

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import me.magnum.melonds.common.cheats.WildEncounterCheat
import me.magnum.melonds.database.MelonDatabase
import me.magnum.melonds.database.entities.*
import me.magnum.melonds.domain.model.Cheat
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = android.app.Application::class)
class WildEncounterPersistenceTest {
    @Test fun selectionAndConflictsAreSavedTogetherAndRollbackOnFailure() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val database = Room.inMemoryDatabaseBuilder(context, MelonDatabase::class.java).allowMainThreadQueries().build()
        try {
            val source = database.cheatDatabaseDao().insertCheatDatabase(CheatDatabaseEntity(null, "Test"))
            val game = database.gameDao().insertGame(GameEntity(null, "SGP", "IPKE", "19D1EEBB"))
            val folder = database.cheatFolderDao().insertCheatFolder(CheatFolderEntity(null, game, "Wild"))
            val first = database.cheatDao().insertCheat(CheatEntity(null, folder, source, "Choose", null, WildEncounterCheat.code(1, 5), false))
            val old = database.cheatDao().insertCheat(CheatEntity(null, folder, source, "Old", null, WildEncounterCheat.code(25, 10), true))
            val repository = RoomCheatsRepository(context, database)
            val selected = Cheat(first, source, "Arceus · 100", "Instructions", WildEncounterCheat.code(493, 100), true)
            val previous = Cheat(old, source, "Old", null, WildEncounterCheat.code(25, 10), false)
            repository.updateCheats(listOf(previous, selected))
            assertFalse(database.cheatDao().getCheat(old)!!.enabled)
            val saved = database.cheatDao().getCheat(first)!!
            assertTrue(saved.enabled)
            assertEquals(selected.code, saved.code)
            assertEquals(selected.name, saved.name)
            assertEquals(source, saved.cheatDatabaseId)
            try {
                repository.updateCheats(listOf(selected.copy(enabled = false), selected.copy(id = 99999)))
                fail("Missing cheat should fail the whole transaction")
            } catch (_: IllegalArgumentException) {
                assertEquals(saved, database.cheatDao().getCheat(first))
            }
        } finally {
            database.close()
        }
    }
}
