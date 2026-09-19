package me.magnum.melonds.common.cheats

import me.magnum.melonds.domain.model.Cheat
import me.magnum.melonds.domain.model.CheatFolder

enum class ModifierKind { SPECIES, LEVEL, NATURE, GENERIC }
enum class ModifierSource { ENUMERATED, ITEM_LOAD }

data class ModifierOption(val value: Int, val label: String, val cheat: Cheat?)

/** One rewritable load inside an ITEM_LOAD code. [current] is the value when the block is already in fixed form. */
data class ModifierParameter(val blockIndex: Int, val instructionIndex: Int, val width: Int, val kind: ModifierKind, val current: Int?)

/**
 * A group of cheats that differ only by a value (ENUMERATED), or a single cheat whose value is read from an
 * item count or the Pokétch calculator (ITEM_LOAD). Computed from the folder contents, never persisted.
 */
data class ModifierFamily(
    val title: String,
    val kind: ModifierKind,
    val source: ModifierSource,
    val folderName: String,
    val role: String,
    val members: List<Cheat>,
    val options: List<ModifierOption>,
    val parameters: List<ModifierParameter>,
    val instructions: String?,
) {
    val current: ModifierOption?
        get() = when (source) {
            ModifierSource.ENUMERATED -> options.firstOrNull { it.cheat?.enabled == true }
            ModifierSource.ITEM_LOAD -> members.single().takeIf { it.enabled }
                ?.let { parameters.first().current }
                ?.let { value -> options.firstOrNull { it.value == value } }
        }
    val currentLevel: Int? get() = parameters.getOrNull(1)?.current
    val hasLevelParameter: Boolean get() = parameters.size == 2 && parameters[1].kind == ModifierKind.LEVEL

    /** Families sharing a group must not be active at the same time. NATURE ignores the role. */
    val exclusionGroups: Set<Pair<ModifierKind, String>>
        get() = when {
            source == ModifierSource.ITEM_LOAD -> parameters.map { it.kind to roleFor(it.kind) }.toSet()
            kind == ModifierKind.GENERIC -> emptySet()
            else -> setOf(kind to roleFor(kind))
        }

    private fun roleFor(kind: ModifierKind) = if (kind == ModifierKind.NATURE) "" else role
    fun conflictsWith(other: ModifierFamily) = exclusionGroups.any { it in other.exclusionGroups }
    fun contains(cheat: Cheat) = members.any { it.id == cheat.id }
    fun sameAs(other: ModifierFamily) = title == other.title && folderName == other.folderName && source == other.source
}

object ModifierFamilies {
    private val WILD = Regex("wild|encounter|selvatic|incontr", RegexOption.IGNORE_CASE)
    private val STARTER = Regex("starter|iniziale", RegexOption.IGNORE_CASE)
    private val LEVEL = Regex("level|livell", RegexOption.IGNORE_CASE)
    private val NATURE = Regex("natur", RegexOption.IGNORE_CASE)
    private val SLOT = Regex("#\\s*(\\d+)")
    private const val MIN_MEMBERS = 5
    private const val MAX_VARYING = 2
    private const val MAX_SPECIES = 649L
    private const val SPECIES_NAME_RATIO = 0.8

    fun recognize(folder: CheatFolder): List<ModifierFamily> = recognize(folder.name, folder.cheats)

    /** Enumerated families first; a cheat that belongs to one is never examined as an item load. */
    fun recognize(folderName: String, cheats: List<Cheat>): List<ModifierFamily> {
        val parsed = cheats.mapNotNull { cheat -> ArCode.parse(cheat.code)?.let { cheat to it } }
        val enumerated = parsed
            .groupBy { (_, blocks) -> blocks.sumOf { it.instructions.size } }
            .values.filter { it.size >= MIN_MEMBERS }
            .mapNotNull { group -> enumeratedFamily(folderName, group) }
        val taken = enumerated.flatMap { family -> family.members.map { it.id } }.toSet()
        val loads = parsed
            .filter { (cheat, _) -> cheat.id !in taken && (WILD.containsMatchIn(folderName) || WILD.containsMatchIn(cheat.name)) }
            .mapNotNull { (cheat, blocks) -> itemLoadFamily(folderName, cheat, blocks) }
        return enumerated + loads
    }

    private fun enumeratedFamily(folderName: String, group: List<Pair<Cheat, List<ArBlock>>>): ModifierFamily? {
        val words = group.map { (_, blocks) -> blocks.flatMap { it.instructions }.flatMap { listOf(it.a, it.b) } }
        val varying = words.first().indices.filter { i -> words.map { it[i] }.toSet().size > 1 }
        if (varying.isEmpty() || varying.size > MAX_VARYING) return null
        val tuples = words.map { w -> varying.map { w[it] } }
        if (tuples.toSet().size != tuples.size) return null
        val values = words.map { it[varying.first()] }
        if (values.any { it > Int.MAX_VALUE }) return null
        val names = group.map { (cheat, _) -> cheat.name }
        val speciesHits = names.count { PokemonSpecies.normalize(it) in PokemonSpecies.normalizedNames }
        val speciesRange = values.all { it in 1..MAX_SPECIES }
        val kind = when {
            varying.size == 1 && (speciesHits >= SPECIES_NAME_RATIO * names.size ||
                (speciesRange && (WILD.containsMatchIn(folderName) || STARTER.containsMatchIn(folderName)))) -> ModifierKind.SPECIES
            NATURE.containsMatchIn(folderName) -> ModifierKind.NATURE
            LEVEL.containsMatchIn(folderName) -> ModifierKind.LEVEL
            else -> ModifierKind.GENERIC
        }
        val members = group.map { it.first }
        return ModifierFamily(
            title = folderName, kind = kind, source = ModifierSource.ENUMERATED, folderName = folderName, role = roleOf(folderName),
            members = members,
            options = members.mapIndexed { i, cheat -> ModifierOption(values[i].toInt(), cheat.name, cheat) },
            parameters = emptyList(),
            instructions = members.firstNotNullOfOrNull { it.description?.takeIf(String::isNotBlank) },
        )
    }

    @Suppress("UNUSED_PARAMETER")
    private fun itemLoadFamily(folderName: String, cheat: Cheat, blocks: List<ArBlock>): ModifierFamily? = null

    internal fun roleOf(title: String): String = when {
        STARTER.containsMatchIn(title) -> "STARTER" + (SLOT.find(title)?.groupValues?.get(1)?.let { "#$it" } ?: "")
        WILD.containsMatchIn(title) -> "WILD"
        else -> "OTHER"
    }
}
