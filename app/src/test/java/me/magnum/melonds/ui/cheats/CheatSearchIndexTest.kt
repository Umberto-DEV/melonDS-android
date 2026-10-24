package me.magnum.melonds.ui.cheats

import me.magnum.melonds.common.cheats.WildEncounterCheat
import me.magnum.melonds.domain.model.Cheat
import me.magnum.melonds.domain.model.CheatFolder
import me.magnum.melonds.ui.cheats.model.CheatSearchIndex
import me.magnum.melonds.ui.cheats.model.CheatSearchRow
import org.junit.Assert.*
import org.junit.Test

class CheatSearchIndexTest {
    private fun plain(id: Long, name: String) = Cheat(id, 1, name, null, "not an AR code $id", false)

    @Test fun blankQueryShowsOnlyFoldersAndUnknownQueryHasNoRows() {
        val folders = listOf(CheatFolder(1, "Inventory", listOf(plain(1, "Money"))), CheatFolder(2, "Empty", emptyList()))
        val index = CheatSearchIndex.build(folders)
        assertEquals(folders, index.search("  ").map { it.folder })
        assertTrue(index.search("  ").all { it is CheatSearchRow.Folder })
        assertEquals(listOf(folders[1]), index.search("empty").map { it.folder })
        assertTrue(index.search("nonexistent").isEmpty())
    }

    @Test fun repeatedNamesKeepDistinctFolderAndCheatDestinations() {
        val index = CheatSearchIndex.build(listOf(
            CheatFolder(1, "One", listOf(plain(1, "Money"), plain(2, "Money"))),
            CheatFolder(2, "Two", listOf(plain(3, "Money"))),
        ))
        val rows = index.search(" mOnEy ")
        val hits = rows.filterIsInstance<CheatSearchRow.Match>()
        assertEquals(listOf(1L, 1L, 2L), hits.map { it.folder.id })
        assertEquals(listOf("1", "2", "3"), hits.map { it.target.rowKey })
        assertEquals(rows.size, rows.map { it.key }.toSet().size)
    }

    @Test fun generatedSpeciesTargetsItsFamilyAndDoesNotModifyThePhysicalCode() {
        val cheat = Cheat(90, 1, "Choose", null, WildEncounterCheat.code(1, 5), false)
        val folder = CheatFolder(9, "Wild", listOf(cheat))
        val index = CheatSearchIndex.build(listOf(folder))
        val match = index.search("Pikachu").filterIsInstance<CheatSearchRow.Match>().single()
        assertEquals("#025 Pikachu", match.label)
        assertEquals("Choose", match.familyTitle)
        assertEquals("family:90:ITEM_LOAD", match.target.rowKey)
        assertEquals(25, match.target.optionValue)
        assertEquals(listOf(cheat), folder.cheats)
        assertFalse(cheat.enabled)
        assertEquals(match, index.search("#025").filterIsInstance<CheatSearchRow.Match>().single())
    }

    @Test fun enumeratedMemberTargetsCollapsedFamilyInsteadOfHiddenRawRow() {
        val folder = CheatFolder(49, "Wild level", (1..5).map {
            Cheat(100L + it, 1, "Level $it", null, "52246C94 28038800 12247BEC %08X D2000000 00000000".format(0x2000 + it), false)
        })
        val match = CheatSearchIndex.build(listOf(folder)).search("Level 3").filterIsInstance<CheatSearchRow.Match>().single()
        assertEquals("family:101:ENUMERATED", match.target.rowKey)
        assertEquals(0x2003, match.target.optionValue)
    }

    @Test fun largeResultsAreFlatAndHaveStableUniqueKeys() {
        val folder = CheatFolder(1, "Many", (0..1199).map { plain(it.toLong(), "Match $it") })
        val index = CheatSearchIndex.build(listOf(folder))
        val rows = index.search("Match")
        assertEquals(1201, rows.size)
        assertEquals(1201, rows.map { it.key }.toSet().size)
        assertEquals(rows.last().key, index.search("Match 1199").last().key)
    }
}
