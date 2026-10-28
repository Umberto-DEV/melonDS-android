package me.magnum.melonds.impl

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.EOFException
import java.net.URLConnection

/** Same production-repository tests run on the host JVM and Android ART. */
class NusDSiWareMetadataRepositoryTest {
    private val metadata = ByteArray(520) { (it % 251 + 1).toByte() }

    private class Response(bytes: ByteArray, private val chunkSize: Int) : ByteArrayInputStream(bytes) {
        var closed = false
        var consumed = 0
        override fun read(bytes: ByteArray, offset: Int, length: Int): Int =
            super.read(bytes, offset, minOf(length, chunkSize)).also { if (it > 0) consumed += it }
        override fun close() { closed = true; super.close() }
    }

    private fun read(response: Response): ByteArray = runBlocking {
        NusDSiWareMetadataRepository { url ->
            object : URLConnection(url) {
                override fun connect() = Unit
                override fun getInputStream() = response
            }
        }.getDSiWareTitleMetadata(0x00030004u, 0x4b414141u)
    }

    @Test fun completeResponsePreservesEveryByte() {
        val response = Response(metadata, 520)
        assertArrayEquals(metadata, read(response))
        assertTrue(response.closed)
    }

    @Test fun fragmentedResponsePreservesEveryByte() {
        val response = Response(metadata, 17)
        assertArrayEquals(metadata, read(response))
        assertTrue(response.closed)
    }

    @Test fun truncatedResponseFailsAndClosesStream() {
        val response = Response(metadata.copyOf(15), 17)
        assertThrows(EOFException::class.java) { read(response) }
        assertTrue(response.closed)
    }

    @Test fun emptyResponseFailsAndClosesStream() {
        val response = Response(byteArrayOf(), 17)
        assertThrows(EOFException::class.java) { read(response) }
        assertTrue(response.closed)
    }

    @Test fun readsOnlyTheRequiredMetadataPrefix() {
        val response = Response(metadata + ByteArray(37) { 99 }, 17)
        assertArrayEquals(metadata, read(response))
        assertEquals(520, response.consumed)
        assertTrue(response.closed)
    }
}
