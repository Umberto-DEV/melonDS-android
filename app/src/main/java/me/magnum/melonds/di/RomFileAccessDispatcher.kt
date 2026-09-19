package me.magnum.melonds.di

import javax.inject.Qualifier

/**
 * Single-threaded dispatcher for the ROM library: the directory scan, the ROM lookups by URI or path and the ROM
 * info read by the emulator all go through it, one at a time.
 *
 * It is not shared with [me.magnum.melonds.impl.RomIconProvider], which has its own dispatcher: the ROM list needs
 * its icons while the scan is still running. The memory bound for compressed ROMs (one archive decoded at a time,
 * process-wide) is enforced by the archive lock in
 * [me.magnum.melonds.common.romprocessors.CompressedRomFileProcessor], not by this dispatcher.
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class RomFileAccessDispatcher
