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

    private val hgV1 = "94000130 FCFF0000 6211186C 00000000 B211186C 00000000 0000DCF4 01ED0001 D2000000 00000000 " +
        "94000130 FDFF0000 6211186C 00000000 B211186C 00000000 DA000000 0000DCF6 C0000000 00000027 D7000000 00032A48 D2000000 00000000"
    private val hgSpeciesAndLevel = "94000130 FCFF0000 6211186C 00000000 B211186C 00000000 0000DCF4 01ED0001 0000DCF8 00640002 D2000000 00000000 " +
        "94000130 FDFF0000 6211186C 00000000 B211186C 00000000 DA000000 0000DCF6 C0000000 00000027 D7000000 00032A48 D2000000 00000000 " +
        "94000130 FDFF0000 6211186C 00000000 B211186C 00000000 DB000000 0000DCFA C0000000 0000000B D8000000 00032A3C D2000000 00000000"
    private val hgLevelOnly = "94000130 FCFF0000 6211186C 00000000 B211186C 00000000 0000DCF8 00640002 D2000000 00000000 " +
        "94000130 FDFF0000 6211186C 00000000 B211186C 00000000 DB000000 0000DCFA C0000000 0000000B D8000000 00032A3C D2000000 00000000"
    private val platinumCalculator = "94000130 FDFF0000 62101D2C 00000000 B2101D2C 00000000 DA000000 0011ECF0 C0000000 0000000B D7000000 000303CC DC000000 00000006 D2000000 00000000 " +
        "94000130 FEFF0000 62101D2C 00000000 B2101D2C 00000000 DA000000 0011ECF0 C0000000 0000000B D7000000 000303C8 DC000000 00000006 D2000000 00000000 " +
        "94000130 FFFB0000 2207404D 00000024 62101D2C 00000000 B2101D2C 00000000 DB000000 0011ECF0 D3000000 00000000 D8000000 0207404C D2000000 00000000"
    private val randomEncounterAbsolute = "923FFFFE 00000001 62250010 00000000 DA000000 02250010 D4000000 00000029 D7000000 02250010 D3000000 00000000 D2000000 00000000"
    private val maxIvs = "1206E012 0000201F 1206E028 0000201F 1206E03E 0000201F 1206E054 0000201F 1206E06A 0000201F 1206E080 0000201F"

    @Test fun itemLoadSpeciesFromHeartGoldMasterBallCode() {
        val cheat = cheat("Wild Pokemon Modifier v1", hgV1, description = "(Press L+R): You will get 493 Master Balls.")
        val family = ModifierFamilies.recognize("Wild Pokemon Modifier Codes", listOf(cheat)).single()
        assertEquals(ModifierSource.ITEM_LOAD, family.source)
        assertEquals(ModifierKind.SPECIES, family.kind)
        assertEquals(listOf(ModifierParameter(1, 3, 16, ModifierKind.SPECIES, null)), family.parameters)
        assertEquals(493, family.options.size)
        assertEquals("#025 Pikachu", family.options[24].label)
        assertFalse(family.hasLevelParameter)
        assertNull(family.current)
        assertEquals("(Press L+R): You will get 493 Master Balls.", family.instructions)
    }

    @Test fun secondBlockIsLevelAndEightBitLoadIsLevel() {
        val both = ModifierFamilies.recognize("Wild Pokemon Modifier Codes", listOf(cheat("Wild Pokemon and Level Modifier", hgSpeciesAndLevel))).single()
        assertEquals(listOf(ModifierKind.SPECIES, ModifierKind.LEVEL), both.parameters.map { it.kind })
        assertTrue(both.hasLevelParameter)
        val level = ModifierFamilies.recognize("Wild Pokemon Level Modifier Codes", listOf(cheat("Level Modifier Code", hgLevelOnly))).single()
        assertEquals(ModifierKind.LEVEL, level.kind)
        assertEquals(8, level.parameters.single().width)
        assertEquals((1..100).toList(), level.options.map { it.value })
        val platinum = ModifierFamilies.recognize("Encounter Codes", listOf(cheat("Wild Pokemon Modifier Code (Calculator)", platinumCalculator))).single()
        assertEquals(listOf(ModifierKind.SPECIES, ModifierKind.LEVEL), platinum.parameters.map { it.kind })
        assertEquals(listOf(0, 1), platinum.parameters.map { it.blockIndex })
    }

    @Test fun fixedFormIsRecognisedWithItsValue() {
        val canonical = cheat("Choose Pokémon and level", WildEncounterCheat.code(25, 5), enabled = true)
        val family = ModifierFamilies.recognize("40 - Wild encounters · choose Pokémon and level", listOf(canonical)).single()
        assertEquals(25, family.parameters[0].current)
        assertEquals(5, family.parameters[1].current)
        assertEquals(25, family.current?.value)
        assertEquals(5, family.currentLevel)
    }

    @Test fun absoluteLoadsAndPlainCodesAreNotFamilies() {
        val folder = "Encounter Codes"
        assertTrue(ModifierFamilies.recognize(folder, listOf(cheat("Encounter Random Wild Pokemon", randomEncounterAbsolute))).isEmpty())
        assertTrue(ModifierFamilies.recognize(folder, listOf(cheat("Wild Pokemon Have Max IVs", maxIvs))).isEmpty())
        assertTrue(ModifierFamilies.recognize("Miscellaneous Codes", listOf(cheat("Recollect 2nd Generation Starters", hgV1))).isEmpty())
        assertTrue(ModifierFamilies.recognize("Miscellaneous Codes", listOf(cheat("Money", hgV1))).isEmpty())
    }
}
