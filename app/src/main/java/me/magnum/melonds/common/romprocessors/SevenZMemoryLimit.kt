package me.magnum.melonds.common.romprocessors

import kotlin.math.min

/**
 * Memory budget handed to commons-compress when opening a 7z archive.
 *
 * The LZMA2 dictionary is allocated on the Java heap, so the budget must also be bound by the heap growth limit and
 * not only by the device's physical RAM: on a modern handheld 10% of the RAM is several times the whole heap, so a
 * RAM-only limit never triggers and a large dictionary aborts the process with a fatal OutOfMemoryError instead of
 * failing with a recoverable MemoryLimitException.
 */
object SevenZMemoryLimit {

    private const val DEVICE_MEMORY_FRACTION = 0.1
    private const val MAX_HEAP_FRACTION = 0.25
    private const val MINIMUM_LIMIT_KB = 1L
    private const val BYTES_PER_KB = 1024

    /**
     * Returns the maximum amount of memory, in kilobytes, that the 7z decoder is allowed to allocate.
     *
     * @param totalDeviceMemoryBytes total physical memory of the device, as reported by ActivityManager
     * @param maxHeapBytes maximum Java heap size for this process, as reported by [Runtime.maxMemory]
     */
    fun kb(totalDeviceMemoryBytes: Long, maxHeapBytes: Long): Int {
        val budgetBytes = min(totalDeviceMemoryBytes * DEVICE_MEMORY_FRACTION, maxHeapBytes * MAX_HEAP_FRACTION)
        val budgetKb = (budgetBytes / BYTES_PER_KB).toLong()
        return budgetKb.coerceIn(MINIMUM_LIMIT_KB, Int.MAX_VALUE.toLong()).toInt()
    }
}
