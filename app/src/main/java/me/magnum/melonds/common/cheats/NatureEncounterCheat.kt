package me.magnum.melonds.common.cheats

import java.util.Locale

/** Recognizes only the guarded SGP native-creation patches from folders 44–46. */
object NatureEncounterCheat {
    private val prefix = "5206E108 B086B5F8 5206E14C B089B5F0 1206E110 00009C0C 1206E11A 00001C05 1206E15C 00009E0E 1206E15E 00009F0F "

    private val codes: Set<String> = buildSet {
        for (nature in 0..24) {
            add(prefix + "1206E110 %08X 1206E15E %08X D2000000 00000000".format(Locale.ROOT, 0x2400 + nature, 0x2700 + nature))
            for (male in listOf(true, false)) {
                add(prefix + "94000130 FDFF0000 1206E110 %08X 1206E11A %08X 1206E15C %08X 1206E15E %08X D2000000 00000000".format(Locale.ROOT,
                    0x2400 + nature, if (male) 0x25FF else 0x2500, if (male) 0x2600 else 0x2601, 0x2700 + nature))
            }
        }
    }

    fun recognizes(code: String): Boolean = code.trim().uppercase(Locale.ROOT).split(Regex("\\s+")).joinToString(" ") in codes
}
