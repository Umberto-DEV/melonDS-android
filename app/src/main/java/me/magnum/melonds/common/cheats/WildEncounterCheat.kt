package me.magnum.melonds.common.cheats

import java.util.Locale
import me.magnum.melonds.domain.model.Cheat

/** SGP 1.2/1.2.1 only. Writes grass encounter tables, never the Ball pocket. */
object WildEncounterCheat {
    data class Selection(val species: Int, val level: Int)

    fun supports(gameCode: String, checksum: String) =
        gameCode == "IPKE" && checksum.uppercase(Locale.ROOT) in setOf("19D1EEBB", "1C1F741C")

    private fun normalized(code: String) = code.trim().uppercase(Locale.ROOT).split(Regex("\\s+")).joinToString(" ")

    private const val SPECIES = "52246C94 28038800 6211186C 00000000 B211186C 00000000 D5000000 %08X C0000000 00000027 D7000000 00032A48 D2000000 00000000"
    private const val LEVEL = "52246C94 28038800 6211186C 00000000 B211186C 00000000 D5000000 %08X C0000000 0000000B D8000000 00032A3C D2000000 00000000"

    fun code(species: Int, level: Int): String {
        require(species in 1..493 && level in 1..100)
        return (SPECIES + " " + LEVEL).format(Locale.ROOT, species, level)
    }

    private val pattern = Regex((SPECIES + " " + LEVEL).replace("%08X", "([0-9A-F]{8})"))

    fun selection(code: String): Selection? {
        val match = pattern.matchEntire(normalized(code)) ?: return null
        val species = match.groupValues[1].toLong(16)
        val level = match.groupValues[2].toLong(16)
        return if (species in 1..493 && level in 1..100) Selection(species.toInt(), level.toInt()) else null
    }

    fun isConfigurable(code: String) = selection(code) != null || normalized(code) in legacyCodes

    private val speciesCodes = Regex("94000130 FDFF0000 " + SPECIES.removePrefix("52246C94 28038800 ").replace("%08X", "0000[0-9A-F]{4}"))
    private val levelCodes = Regex("52246C94 28038800 12247BEC 000020[0-9A-F]{2} D2000000 00000000")

    fun conflicts(code: String) = isConfigurable(code) || speciesCodes.matches(normalized(code)) || levelCodes.matches(normalized(code))

    /** One active selection across every folder, including pending user changes. */
    fun configure(selected: Cheat, others: List<Cheat>): List<Cheat> =
        others.filter { it.id != selected.id && it.enabled && conflicts(it.code) }
            .map { it.copy(enabled = false) } + selected.copy(enabled = true)

    private val legacyCodes = setOf<String>(
        "94000130 FCFF0000 6211186C 00000000 B211186C 00000000 0000DCF8 00640002 D2000000 00000000 94000130 FDFF0000 6211186C 00000000 B211186C 00000000 DB000000 0000DCFA C0000000 0000000B D8000000 00032A3C D2000000 00000000",
        "94000130 FCFF0000 6211186C 00000000 B211186C 00000000 0000DCF4 01ED0001 0000DCF8 00640002 D2000000 00000000 94000130 FDFF0000 6211186C 00000000 B211186C 00000000 DA000000 0000DCF6 C0000000 00000027 D7000000 00032A48 D2000000 00000000 94000130 FDFF0000 6211186C 00000000 B211186C 00000000 DB000000 0000DCFA C0000000 0000000B D8000000 00032A3C D2000000 00000000",
        "94000130 FCFF0000 6211186C 00000000 B211186C 00000000 0000DCF4 01ED0001 D2000000 00000000 94000130 FDFF0000 6211186C 00000000 B211186C 00000000 DA000000 0000DCF6 C0000000 00000027 D7000000 00032A48 D2000000 00000000",
        "94000130 FCFF0000 6211186C 00000000 B211186C 00000000 0000DCF4 01ED0001 D2000000 00000000 94000130 FEFF0000 6211186C 00000000 B211186C 00000000 DA000000 0000DCF6 C0000000 00000027 D7000000 00032A48 D2000000 00000000"
    )
}
