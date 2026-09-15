package me.magnum.melonds.common.cheats

import org.junit.Assert.*
import org.junit.Test
import me.magnum.melonds.domain.model.Cheat

class WildEncounterCheatTest {
    @Test fun catalogAndSearch() {
        assertEquals((1..493).toList(), PokemonSpecies.all.map { it.number })
        assertEquals("Arceus", PokemonSpecies.all.last().name)
        assertEquals(25, PokemonSpecies.search("pikA").single().number)
        assertEquals(25, PokemonSpecies.search("#025").single().number)
        assertEquals(122, PokemonSpecies.search("mr mime").single().number)
        assertEquals(493, PokemonSpecies.search("").size)
        assertTrue(PokemonSpecies.search("no such pokemon").isEmpty())
    }

    @Test fun codeRoundTripAndNoButtonsOrInventoryWrites() {
        for (species in 1..493) for (level in listOf(1, 10, 100)) {
            val code = WildEncounterCheat.code(species, level)
            assertEquals(WildEncounterCheat.Selection(species, level), WildEncounterCheat.selection(code))
            assertFalse(code.contains("94000130"))
            assertFalse(code.contains("0000DCF"))
        }
    }

    @Test fun rejectsInvalidAndUnrelatedCodes() {
        for ((species, level) in listOf(0 to 1, 494 to 1, 1 to 0, 1 to 101)) {
            assertThrows(IllegalArgumentException::class.java) { WildEncounterCheat.code(species, level) }
        }
        assertNull(WildEncounterCheat.selection("D2000000 00000000"))
        assertNull(WildEncounterCheat.selection(WildEncounterCheat.code(25, 10) + " 02000000 00000001"))
        assertFalse(WildEncounterCheat.supports("IPKE", "00000000"))
        assertTrue(WildEncounterCheat.supports("IPKE", "19D1EEBB"))
        assertTrue(WildEncounterCheat.supports("IPKE", "1C1F741C"))
    }

    @Test fun oneSelectionDisablesConflictsButKeepsUnrelatedCheats() {
        val selected = Cheat(1, 1, "Pikachu", null, WildEncounterCheat.code(25, 10), false)
        val previous = selected.copy(id = 2, code = WildEncounterCheat.code(493, 100), enabled = true)
        val species = selected.copy(id = 3, code = "94000130 FDFF0000 6211186C 00000000 B211186C 00000000 D5000000 00000019 C0000000 00000027 D7000000 00032A48 D2000000 00000000", enabled = true)
        val level = selected.copy(id = 4, code = "52246C94 28038800 12247BEC 00002064 D2000000 00000000", enabled = true)
        val unrelated = selected.copy(id = 5, code = "02000000 00000001", enabled = true)
        val changes = WildEncounterCheat.configure(selected, listOf(selected, previous, species, level, unrelated))
        assertEquals(setOf(1L, 2L, 3L, 4L), changes.map { it.id }.toSet())
        assertEquals(listOf(1L), changes.filter { it.enabled }.map { it.id })
        assertEquals(selected.code, changes.single { it.enabled }.code)
    }
}
