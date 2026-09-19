package me.magnum.melonds.common.cheats

import java.util.Locale

/** One Action Replay DS instruction: two 32-bit words. The opcode lives in the top byte of [a]. */
data class ArInstruction(val a: Long, val b: Long)

/** Instructions up to and including a `D2000000 00000000` terminator, or the tail of the code. */
data class ArBlock(val instructions: List<ArInstruction>)

/** The only place that knows what an Action Replay opcode means. Semantics from the core's AREngine.cpp. */
object ArCode {
    const val SET_DATA = 0xD5000000L   // datareg = b
    const val LOAD_16 = 0xDA000000L    // datareg = u16[b + offset]
    const val LOAD_8 = 0xDB000000L     // datareg = u8[b + offset]
    const val STORE_16 = 0xD7000000L   // u16[b + offset] = datareg
    const val STORE_8 = 0xD8000000L    // u8[b + offset] = datareg
    const val LOOP = 0xC0000000L
    const val DATA_OP = 0xD4000000L
    const val OFFSET_SET = 0xD3000000L
    const val OFFSET_ADD = 0xDC000000L
    const val END = 0xD2000000L
    /** Addresses below this are offsets relative to a pointer loaded with B2xxxxxx, not main RAM. */
    const val RAM_START = 0x02000000L

    /** Null unless the code is a non-empty, even-length list of 8-digit hex words. */
    fun parse(code: String): List<ArBlock>? {
        val words = code.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
        if (words.isEmpty() || words.size % 2 != 0) return null
        val values = LongArray(words.size)
        for (i in words.indices) {
            val word = words[i]
            if (word.length != 8) return null
            values[i] = word.toLongOrNull(16) ?: return null
        }
        val blocks = mutableListOf<ArBlock>()
        var current = mutableListOf<ArInstruction>()
        for (i in values.indices step 2) {
            val instruction = ArInstruction(values[i], values[i + 1])
            current.add(instruction)
            if (instruction.a == END && instruction.b == 0L) {
                blocks.add(ArBlock(current))
                current = mutableListOf()
            }
        }
        if (current.isNotEmpty()) blocks.add(ArBlock(current))
        return blocks
    }

    /** The app's canonical text form: upper-case 8-digit words separated by single spaces. */
    fun render(blocks: List<ArBlock>): String =
        blocks.flatMap { it.instructions }.joinToString(" ") { "%08X %08X".format(Locale.ROOT, it.a, it.b) }
}
