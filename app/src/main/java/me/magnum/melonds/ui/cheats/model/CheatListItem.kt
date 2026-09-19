package me.magnum.melonds.ui.cheats.model

import me.magnum.melonds.common.cheats.ModifierFamily
import me.magnum.melonds.domain.model.Cheat

/** A row of the folder list: a plain cheat, or a modifier family collapsed into one row. */
sealed class CheatListItem {
    abstract val key: Any

    data class Single(val cheat: Cheat) : CheatListItem() {
        override val key: Any get() = cheat.id ?: cheat.code
    }

    data class Family(val family: ModifierFamily) : CheatListItem() {
        override val key: Any get() = "family:${family.folderName}:${family.title}:${family.source}"
    }
}
