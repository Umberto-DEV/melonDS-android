package me.magnum.melonds.common.cheats

import org.junit.Assert.*
import org.junit.Test

class ArCodeTest {
    private val hgV1 = "94000130 FCFF0000 6211186C 00000000 B211186C 00000000 0000DCF4 01ED0001 D2000000 00000000 " +
        "94000130 FDFF0000 6211186C 00000000 B211186C 00000000 DA000000 0000DCF6 C0000000 00000027 D7000000 00032A48 D2000000 00000000"

    @Test fun splitsBlocksOnTerminatorAndKeepsTail() {
        val blocks = requireNotNull(ArCode.parse(hgV1))
        assertEquals(2, blocks.size)
        assertEquals(5, blocks[0].instructions.size)
        assertEquals(ArInstruction(0xDA000000L, 0x0000DCF6L), blocks[1].instructions[3])
        val tail = requireNotNull(ArCode.parse("52246C94 28038800 12247BEC 00002001"))
        assertEquals(1, tail.size)
        assertEquals(2, tail[0].instructions.size)
    }

    @Test fun renderIsTheAppFormatAndRoundTrips() {
        assertEquals(hgV1, ArCode.render(requireNotNull(ArCode.parse(hgV1))))
        assertEquals("D2000000 00000000", ArCode.render(requireNotNull(ArCode.parse("d2000000\n00000000"))))
    }

    @Test fun rejectsMalformedCodes() {
        assertNull(ArCode.parse(""))
        assertNull(ArCode.parse("D2000000"))
        assertNull(ArCode.parse("D200000 00000000"))
        assertNull(ArCode.parse("D2000000 0000000G"))
    }
}
