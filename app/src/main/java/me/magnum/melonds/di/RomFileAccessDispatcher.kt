package me.magnum.melonds.di

import javax.inject.Qualifier

/**
 * Single-threaded dispatcher shared by every component that reads ROM files. Compressed ROMs allocate large decoder
 * buffers on the Java heap, so all ROM file access must be serialised through one dispatcher to keep peak memory
 * bound to a single archive at a time.
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class RomFileAccessDispatcher
