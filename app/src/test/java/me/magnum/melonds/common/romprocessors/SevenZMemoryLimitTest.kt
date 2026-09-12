package me.magnum.melonds.common.romprocessors

import org.junit.Assert.assertEquals
import org.junit.Test

class SevenZMemoryLimitTest {

    private val kb = 1024L
    private val mb = 1024L * 1024L
    private val gb = 1024L * 1024L * 1024L

    @Test
    fun `heap is the binding constraint on a high memory handheld`() {
        // Ayn Thor Pro: 12 GB of RAM but a 256 MB heap growth limit. 10% of the RAM would be 1.2 GB, which never
        // limits anything, so the budget must come from the heap: 25% of 256 MB = 64 MB.
        val limitKb = SevenZMemoryLimit.kb(totalDeviceMemoryBytes = 12 * gb, maxHeapBytes = 256 * mb)

        assertEquals((64 * mb / kb).toInt(), limitKb)
    }

    @Test
    fun `device memory is the binding constraint on a low memory device`() {
        // 512 MB device with a comparatively generous 256 MB heap: 10% of the RAM (51.2 MB) is below 25% of the heap
        // (64 MB), so the device memory wins.
        val limitKb = SevenZMemoryLimit.kb(totalDeviceMemoryBytes = 512 * mb, maxHeapBytes = 256 * mb)

        assertEquals(52428, limitKb)
    }

    @Test
    fun `degenerate inputs are clamped to a positive limit`() {
        assertEquals(1, SevenZMemoryLimit.kb(totalDeviceMemoryBytes = 0L, maxHeapBytes = 0L))
        assertEquals(1, SevenZMemoryLimit.kb(totalDeviceMemoryBytes = -1L, maxHeapBytes = 256 * mb))
        assertEquals(1, SevenZMemoryLimit.kb(totalDeviceMemoryBytes = 1 * kb, maxHeapBytes = 1 * kb))
    }

    @Test
    fun `huge inputs do not overflow the integer limit`() {
        val limitKb = SevenZMemoryLimit.kb(totalDeviceMemoryBytes = Long.MAX_VALUE, maxHeapBytes = Long.MAX_VALUE)

        assertEquals(Int.MAX_VALUE, limitKb)
    }
}
