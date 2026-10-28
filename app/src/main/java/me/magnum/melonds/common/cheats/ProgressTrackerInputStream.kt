package me.magnum.melonds.common.cheats

import java.io.FilterInputStream
import java.io.InputStream

class ProgressTrackerInputStream(inputStream: InputStream?) : FilterInputStream(inputStream) {
    var totalReadBytes = 0
        private set

    override fun read(): Int = super.read().also {
        if (it != -1) totalReadBytes++
    }

    override fun read(b: ByteArray?, off: Int, len: Int): Int {
        val readBytes = super.read(b, off, len)
        if (readBytes > 0) totalReadBytes += readBytes

        return readBytes
    }
}
