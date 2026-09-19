package me.magnum.melonds.common.cheats

import org.junit.Assert.*
import org.junit.Test

class NatureEncounterCheatTest {
    @Test fun recognizesEveryCodeFromTheRomCatalogueGeneratorAndRejectsMutations() {
        val lines = requireNotNull(javaClass.getResourceAsStream("/sgp-nature-codes.tsv")).bufferedReader().readLines()
        assertEquals(75, lines.size)
        for (line in lines) {
            val code = line.split('\t')[2]
            assertTrue(line, NatureEncounterCheat.recognizes(code))
            assertFalse(NatureEncounterCheat.recognizes(code.replaceFirst("B086B5F8", "B086B5F9")))
            assertFalse(NatureEncounterCheat.recognizes(code + " D2000000 00000000"))
        }
        assertFalse(NatureEncounterCheat.recognizes(WildEncounterCheat.code(25, 10)))
    }
}
