package me.magnum.melonds.common.cheats

import me.magnum.melonds.domain.model.Cheat
import org.junit.Assert.*
import org.junit.Test

class ModifierFamilyTest {
    private var nextId = 1L
    private fun cheat(name: String, code: String, enabled: Boolean = false, description: String? = null) =
        Cheat(nextId++, 1, name, description, code, enabled)

    // Black USA, "Wild Pokemon Modifier - Generation 1": one word varies (the species)
    private fun blackWild(number: Int, name: String) = cheat(name,
        "94000130 FFFB0000 6214617C 00000000 B214617C 00000000 C0000000 0000002F 00005DD4 %08X DC000000 00000004 D2000000 00000000".format(number),
        description = "(Press and hold Select before encountering)")
    // SGP 44 "Wild · nature": two correlated words vary (0x2400+n, 0x2700+n)
    private fun sgpNature(n: Int, name: String) = cheat(name,
        "5206E108 B086B5F8 5206E14C B089B5F0 1206E110 00009C0C 1206E11A 00001C05 1206E15C 00009E0E 1206E15E 00009F0F 1206E110 %08X 1206E15E %08X D2000000 00000000".format(0x2400 + n, 0x2700 + n))
    // SGP 49 / HG "Level N"
    private fun hgLevel(level: Int) = cheat("Level $level", "52246C94 28038800 12247BEC %08X D2000000 00000000".format(0x2000 + level))

    @Test fun enumeratedSpeciesFamilyFromNamesAndSingleVaryingWord() {
        val cheats = listOf(151 to "Mew", 1 to "Bulbasaur", 2 to "Ivysaur", 3 to "Venusaur", 25 to "Pikachu", 4 to "Charmander").map { blackWild(it.first, it.second) }
        val family = ModifierFamilies.recognize("Wild Pokemon Modifier - Generation 1", cheats).single()
        assertEquals(ModifierKind.SPECIES, family.kind)
        assertEquals(ModifierSource.ENUMERATED, family.source)
        assertEquals("WILD", family.role)
        assertEquals(listOf(151, 1, 2, 3, 25, 4), family.options.map { it.value })
        assertEquals("Pikachu", family.options[4].label)
        assertSame(cheats[4], family.options[4].cheat)
        assertNull(family.current)
        assertEquals("(Press and hold Select before encountering)", family.instructions)
    }

    @Test fun mixedFolderGroupsByLengthAndLeavesTheOddOneOut() {
        val readme = cheat("LEGGIMI", "D2000000 00000000")
        val members = listOf(1, 5, 10, 20, 30, 40).map(::hgLevel)
        val families = ModifierFamilies.recognize("49 - Selvatici · livello 1-100", listOf(readme) + members)
        val family = families.single()
        assertEquals(ModifierKind.LEVEL, family.kind)
        assertEquals(6, family.members.size)
        assertFalse(family.contains(readme))
    }

    @Test fun natureFamilyAllowsTwoCorrelatedVaryingWords() {
        val cheats = listOf("Hardy (=)", "Lonely (+A -D)", "Brave (+A -S)", "Adamant (+A -SA)", "Naughty (+A -SD)").mapIndexed { i, n -> sgpNature(i, n) }
        val family = ModifierFamilies.recognize("44 - Wild · nature", cheats).single()
        assertEquals(ModifierKind.NATURE, family.kind)
        assertEquals(listOf(0x2400, 0x2401, 0x2402, 0x2403, 0x2404), family.options.map { it.value })
    }

    @Test fun speciesByRangeWhenNamesAreUnknownGenFive() {
        val cheats = (494..499).map { blackWild(it, "Gen5 #$it") }
        val family = ModifierFamilies.recognize("Wild Pokemon Modifier - Generation 5", cheats).single()
        assertEquals(ModifierKind.SPECIES, family.kind)
    }

    @Test fun rejectsSmallGroupsDuplicatesAndTooManyVaryingWords() {
        assertTrue(ModifierFamilies.recognize("Wild", (1..4).map { blackWild(it, "x$it") }).isEmpty())
        val dup = (1..5).map { blackWild(7, "same") }
        assertTrue(ModifierFamilies.recognize("Wild", dup).isEmpty())
        val threeVary = (1..5).map { cheat("v$it", "0200%04X 0000%04X 0201%04X 00000000".format(it, it, it)) }
        assertTrue(ModifierFamilies.recognize("Wild", threeVary).isEmpty())
    }

    @Test fun rolesAndExclusionGroups() {
        val wild = ModifierFamilies.recognize("Wild Pokemon Modifier - Generation 1", (1..5).map { blackWild(it, "w$it") }).single()
        val starter1 = ModifierFamilies.recognize("Starter #1 Modifier - Generation 1", (1..5).map { blackWild(it, "s$it") }).single()
        val starter2 = ModifierFamilies.recognize("Starter #2 Modifier - Generation 1", (1..5).map { blackWild(it, "s$it") }).single()
        val playAs = ModifierFamilies.recognize("Play As Pokemon - Generation 1", (1..5).map { blackWild(it, "p$it") }).single()
        assertEquals("STARTER#1", starter1.role)
        assertFalse(wild.conflictsWith(starter1))
        assertFalse(starter1.conflictsWith(starter2))
        assertFalse(wild.conflictsWith(playAs))
        assertTrue(wild.conflictsWith(wild.copy(title = "Wild Pokemon Modifier - Generation 2")))
    }

    @Test fun speciesNormalizationIgnoresParenthesesAndNumbers() {
        assertEquals("meganium", PokemonSpecies.normalize("Meganium (Male)"))
        assertEquals("bulbasaur", PokemonSpecies.normalize("#001 Bulbasaur"))
        assertEquals("mrmime", PokemonSpecies.normalize("Mr. Mime"))
        assertTrue("nidoran" in PokemonSpecies.normalize("Nidoran♀"))
    }
}
