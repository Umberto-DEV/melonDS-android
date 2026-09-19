package me.magnum.melonds.common.cheats

import me.magnum.melonds.domain.model.CheatDatabase
import me.magnum.melonds.domain.model.Game
import org.junit.Assert.*
import org.junit.Test

/** Runs the recogniser on real catalogue excerpts, imported through the app's own XML parser. */
class ModifierFamilyFixtureTest {
    private val games: Map<String, Game> by lazy {
        val stream = requireNotNull(javaClass.getResourceAsStream("/cheats/modifier-families.xml"))
        val parsed = mutableListOf<Game>()
        XmlCheatDatabaseParser().parseCheatDatabase(ProgressTrackerInputStream(stream), object : CheatDatabaseParserListener {
            override fun onDatabaseParseStart(databaseName: String) = CheatDatabase(1, databaseName)
            override fun onGameParseStart(gameName: String) {}
            override fun onGameParsed(game: Game) { parsed += game }
            override fun onParseComplete() {}
        })
        // Room assigns ids on import; the recogniser relies on them to tell cheats apart.
        var nextId = 1L
        parsed.associate { game ->
            game.gameChecksum to game.copy(cheats = game.cheats.map { folder -> folder.copy(cheats = folder.cheats.map { it.copy(id = nextId++) }) })
        }
    }

    private fun families(checksum: String) = games.getValue(checksum).cheats.flatMap { ModifierFamilies.recognize(it) }

    @Test fun heartGoldExposesSpeciesLevelAndNatureFamilies() {
        val byTitle = families("4DFFBF91").associateBy { it.title }
        assertEquals(listOf(ModifierKind.SPECIES, ModifierKind.LEVEL), byTitle.getValue("Wild Pokemon and Level Modifier").parameters.map { it.kind })
        assertEquals(ModifierKind.SPECIES, byTitle.getValue("Wild Pokemon Modifier v1").kind)
        assertEquals(ModifierKind.LEVEL, byTitle.getValue("Level Modifier Code").kind)
        assertEquals(12, byTitle.getValue("Wild Pokemon Level Modifier Codes").members.size)
        assertEquals(ModifierKind.NATURE, byTitle.getValue("Wild Pokemon Nature Modifier Codes").kind)
        assertFalse(byTitle.containsKey("Recollect 2nd Generation Starters"))
        assertFalse(byTitle.containsKey("Wild Pokemon Have Max IVs"))
    }

    @Test fun platinumCalculatorAndStarterSlots() {
        val fams = families("D074D1B3")
        val calc = fams.single { it.title == "Wild Pokemon Modifier Code (Calculator)" }
        assertEquals(listOf(ModifierKind.SPECIES, ModifierKind.LEVEL), calc.parameters.map { it.kind })
        val s1 = fams.single { it.title.startsWith("Starter #1") }
        val s2 = fams.single { it.title.startsWith("Starter #2") }
        assertFalse(s1.conflictsWith(s2))
        assertTrue(fams.none { it.title == "Wild Shiny Encounters" })
    }

    @Test fun blackFamiliesAreEnumeratedAndRandomEncountersAreNot() {
        val fams = families("106820A5")
        val gen1 = fams.single { it.title == "Wild Pokemon Modifier - Generation 1" }
        assertEquals(ModifierSource.ENUMERATED, gen1.source)
        assertEquals(ModifierKind.SPECIES, gen1.kind)
        assertEquals("Bulbasaur", gen1.options.first { it.value == 1 }.label)
        assertTrue(gen1.conflictsWith(fams.single { it.title == "Wild Pokemon Modifier - Generation 2" }))
        assertFalse(gen1.conflictsWith(fams.single { it.title == "Starter Modifier - Generation 1" }))
        assertTrue(fams.none { it.title == "Encounter Random Wild Pokemon" })
        assertTrue(fams.none { it.title == "Wild Pokemon Modifier v1" }) // absolute load; the enumerated families cover B/W
    }

    @Test fun sacredGoldPlusIsFullyCovered() {
        val fams = families("19D1EEBB")
        val selector = fams.single { it.title == "Choose Pokémon and level" }
        assertEquals(ModifierSource.ITEM_LOAD, selector.source)
        assertEquals(ModifierKind.SPECIES, selector.kind)
        assertTrue(selector.hasLevelParameter)
        assertEquals(WildEncounterCheat.code(25, 100), ModifierFamilies.select(selector, 25, 100, fams, WildEncounterCheat::code).single().code)
        val species50 = fams.single { it.folderName.startsWith("50 ") }
        assertEquals(10, species50.members.size)
        assertEquals(ModifierKind.SPECIES, species50.kind)
        assertTrue(selector.conflictsWith(species50))
        assertTrue(selector.conflictsWith(fams.single { it.folderName.startsWith("49 ") }))
        val natures = fams.filter { it.kind == ModifierKind.NATURE }
        assertEquals(2, natures.size)
        assertTrue(natures[0].conflictsWith(natures[1]))
        val classic = fams.single { it.folderName.startsWith("41 ") && it.source == ModifierSource.ITEM_LOAD }
        assertEquals(ModifierKind.LEVEL, classic.kind)
    }
}
