package me.magnum.melonds.common.cheats

import java.util.Locale

/**
 * Sacred Gold Plus 1.2/1.2.1, a HeartGold hack: the native L+R toggle (WildEncounterToggle.h) understands
 * exactly this code shape, so a species+level family on these ROMs renders it instead of a plain rewrite.
 */
object WildEncounterCheat {
    fun supports(gameCode: String, checksum: String) =
        gameCode == "IPKE" && checksum.uppercase(Locale.ROOT) in setOf("19D1EEBB", "1C1F741C")

    private const val SPECIES = "52246C94 28038800 6211186C 00000000 B211186C 00000000 D5000000 %08X C0000000 00000027 D7000000 00032A48 D2000000 00000000"
    private const val LEVEL = "52246C94 28038800 6211186C 00000000 B211186C 00000000 D5000000 %08X C0000000 0000000B D8000000 00032A3C D2000000 00000000"

    /** Overlay signature instead of a button trigger, fixed species and level; no inventory writes. */
    fun code(species: Int, level: Int): String {
        require(species in 1..493 && level in 1..100)
        return (SPECIES + " " + LEVEL).format(Locale.ROOT, species, level)
    }
}
