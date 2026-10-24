package me.magnum.melonds.ui.cheats.model

import me.magnum.melonds.common.cheats.ModifierFamilies
import me.magnum.melonds.common.cheats.familyIdentity
import me.magnum.melonds.domain.model.CheatFolder

/** A destination, never an instruction to enable a cheat. Keys match CheatListItem. */
data class CheatSearchTarget(val rowKey: String, val optionValue: Int? = null)

sealed class CheatSearchRow {
    abstract val folder: CheatFolder
    abstract val key: String

    data class Folder(override val folder: CheatFolder) : CheatSearchRow() {
        override val key = "folder:${folder.id ?: folder.name}"
    }

    data class Match(
        override val folder: CheatFolder,
        val label: String,
        val familyTitle: String?,
        val target: CheatSearchTarget,
    ) : CheatSearchRow() {
        override val key = "match:${folder.id ?: folder.name}/${target.rowKey}/${target.optionValue ?: "row"}"
    }
}

/** Recognize families once per folder snapshot, rather than parsing AR codes on each keystroke. */
class CheatSearchIndex private constructor(
    val folders: List<CheatFolder>,
    private val entries: List<Pair<CheatFolder, List<CheatSearchRow.Match>>>,
) {
    fun search(query: String): List<CheatSearchRow> {
        val needle = query.trim()
        if (needle.isEmpty()) return folders.map { CheatSearchRow.Folder(it) }
        return buildList {
            entries.forEach { (folder, matches) ->
                val found = matches.filter { it.label.contains(needle, ignoreCase = true) }
                if (folder.name.contains(needle, ignoreCase = true) || found.isNotEmpty()) {
                    add(CheatSearchRow.Folder(folder))
                    addAll(found)
                }
            }
        }
    }

    companion object {
        fun build(folders: List<CheatFolder>): CheatSearchIndex = CheatSearchIndex(folders, folders.map { folder ->
            val families = ModifierFamilies.recognize(folder)
            val familyOf = families.flatMap { family -> family.members.map { it.familyIdentity to family } }.toMap()
            val placed = mutableSetOf<Any>()
            folder to buildList {
                folder.cheats.forEach { cheat ->
                    val family = familyOf[cheat.familyIdentity]
                    if (family == null) {
                        add(CheatSearchRow.Match(folder, cheat.name, null, CheatSearchTarget(CheatListItem.Single(cheat).key.toString())))
                    } else if (placed.add(family.identity)) {
                        val key = CheatListItem.Family(family).key.toString()
                        add(CheatSearchRow.Match(folder, family.title, null, CheatSearchTarget(key)))
                        family.options.forEach { option ->
                            add(CheatSearchRow.Match(folder, option.label, family.title, CheatSearchTarget(key, option.value)))
                        }
                    }
                }
            }
        })
    }
}
