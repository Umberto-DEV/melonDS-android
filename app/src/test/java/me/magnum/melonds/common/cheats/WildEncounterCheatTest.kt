package me.magnum.melonds.common.cheats

import org.junit.Assert.*
import org.junit.Test

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

    @Test fun canonicalCodeHasNoButtonTriggerAndNoInventoryWrites() {
        for (species in 1..493) for (level in listOf(1, 10, 100)) {
            val code = WildEncounterCheat.code(species, level)
            assertEquals(28, code.split(" ").size)
            assertFalse(code.contains("94000130"))
            assertFalse(code.contains("0000DCF"))
            assertTrue(code.contains("D5000000 %08X".format(species)))
            assertTrue(code.contains("D5000000 %08X".format(level)))
        }
    }

    @Test fun rejectsOutOfRangeAndKnowsOnlySacredGoldPlus() {
        for ((species, level) in listOf(0 to 1, 494 to 1, 1 to 0, 1 to 101)) {
            assertThrows(IllegalArgumentException::class.java) { WildEncounterCheat.code(species, level) }
        }
        assertFalse(WildEncounterCheat.supports("IPKE", "00000000"))
        assertFalse(WildEncounterCheat.supports("IPKE", "4DFFBF91"))
        assertTrue(WildEncounterCheat.supports("IPKE", "19D1EEBB"))
        assertTrue(WildEncounterCheat.supports("IPKE", "1c1f741c"))
    }
}
