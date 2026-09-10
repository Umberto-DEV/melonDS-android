package me.magnum.melonds.impl

import android.content.Context
import android.content.res.AssetFileDescriptor
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import me.magnum.melonds.domain.model.ConfigurationDirResult
import me.magnum.melonds.domain.model.ConsoleType
import me.magnum.melonds.domain.model.FirmwareValidation
import me.magnum.melonds.domain.repositories.SettingsRepository
import me.magnum.melonds.domain.services.ConfigurationDirectoryVerifier
import java.io.FileNotFoundException

class FileSystemConfigurationDirectoryVerifier(private val context: Context, settingsRepository: SettingsRepository) : ConfigurationDirectoryVerifier(settingsRepository) {
    override fun checkDsConfigurationDirectory(directory: Uri?): ConfigurationDirResult {
        return checkConfigurationDirectory(ConsoleType.DS, directory)
    }

    override fun checkDsiConfigurationDirectory(directory: Uri?): ConfigurationDirResult {
        return checkConfigurationDirectory(ConsoleType.DSi, directory)
    }

    private fun checkConfigurationDirectory(consoleType: ConsoleType, directory: Uri?): ConfigurationDirResult {
        val requiredFiles = getRequiredFilesVerifiers(consoleType)
        if (directory == null) {
            val fileResults = requiredFiles.map { it.key to ConfigurationDirResult.FileStatus.MISSING }
            return ConfigurationDirResult(consoleType, ConfigurationDirResult.Status.UNSET, requiredFiles.keys.toTypedArray(), fileResults.toTypedArray())
        }

        val dirDocument = DocumentFile.fromTreeUri(context, directory)
        if (dirDocument?.isDirectory != true) {
            val fileResults = requiredFiles.map { it.key to ConfigurationDirResult.FileStatus.MISSING }
            return ConfigurationDirResult(consoleType, ConfigurationDirResult.Status.INVALID, requiredFiles.keys.toTypedArray(), fileResults.toTypedArray())
        }

        val fileResults = requiredFiles.map {
            it.key to it.value.invoke(dirDocument)
        }

        val result = if (fileResults.any { it.second != ConfigurationDirResult.FileStatus.PRESENT }) {
            ConfigurationDirResult.Status.INVALID
        } else {
            ConfigurationDirResult.Status.VALID
        }
        return ConfigurationDirResult(consoleType, result, requiredFiles.keys.toTypedArray(), fileResults.toTypedArray())
    }

    private fun getDSBios7Status(configurationDir: DocumentFile): ConfigurationDirResult.FileStatus {
        return getBiosFileStatus(configurationDir, "bios7.bin", 0x4000.toLong())
    }

    private fun getDSBios9Status(configurationDir: DocumentFile): ConfigurationDirResult.FileStatus {
        return getBiosFileStatus(configurationDir, "bios9.bin", 0x1000.toLong())
    }

    private fun getDSFirmwareStatus(configurationDir: DocumentFile): ConfigurationDirResult.FileStatus {
        val firmwareDocument =configurationDir.findFile("firmware.bin") ?: return ConfigurationDirResult.FileStatus.MISSING
        return try {
            context.contentResolver.openAssetFileDescriptor(firmwareDocument.uri, "r")?.use {
                if (it.length == AssetFileDescriptor.UNKNOWN_LENGTH) {
                    ConfigurationDirResult.FileStatus.MISSING
                } else {
                    FirmwareValidation.getDsFirmwareStatus(it.length, getFirmwareConsoleType(it))
                }
            } ?: ConfigurationDirResult.FileStatus.MISSING
        } catch (e: FileNotFoundException) {
            ConfigurationDirResult.FileStatus.MISSING
        }
    }

    private fun getDSiBios7Status(configurationDir: DocumentFile): ConfigurationDirResult.FileStatus {
        return getBiosFileStatus(configurationDir, "bios7.bin", 0x10000.toLong())
    }

    private fun getDSiBios9Status(configurationDir: DocumentFile): ConfigurationDirResult.FileStatus {
        return getBiosFileStatus(configurationDir, "bios9.bin", 0x10000.toLong())
    }

    private fun getDSiFirmwareStatus(configurationDir: DocumentFile): ConfigurationDirResult.FileStatus {
        val firmwareDocument = configurationDir.findFile("firmware.bin") ?: return ConfigurationDirResult.FileStatus.MISSING
        return try {
            context.contentResolver.openAssetFileDescriptor(firmwareDocument.uri, "r")?.use {
                if (it.length == AssetFileDescriptor.UNKNOWN_LENGTH) {
                    ConfigurationDirResult.FileStatus.MISSING
                } else {
                    FirmwareValidation.getDsiFirmwareStatus(it.length, getFirmwareConsoleType(it))
                }
            } ?: ConfigurationDirResult.FileStatus.MISSING
        } catch (e: FileNotFoundException) {
            ConfigurationDirResult.FileStatus.MISSING
        }
    }

    private fun getDSiNandStatus(configurationDir: DocumentFile): ConfigurationDirResult.FileStatus {
        val firmwareDocument = configurationDir.findFile("nand.bin")
        return if (firmwareDocument?.isFile == true) ConfigurationDirResult.FileStatus.PRESENT else ConfigurationDirResult.FileStatus.MISSING
    }

    private fun getBiosFileStatus(configurationDir: DocumentFile, fileName: String, requiredSize: Long): ConfigurationDirResult.FileStatus {
        val biosDocument = configurationDir.findFile(fileName) ?: return ConfigurationDirResult.FileStatus.MISSING
        return try {
            context.contentResolver.openAssetFileDescriptor(biosDocument.uri, "r")?.use {
                when (it.length) {
                    AssetFileDescriptor.UNKNOWN_LENGTH -> ConfigurationDirResult.FileStatus.MISSING
                    requiredSize -> ConfigurationDirResult.FileStatus.PRESENT
                    else -> ConfigurationDirResult.FileStatus.INVALID
                }
            } ?: ConfigurationDirResult.FileStatus.MISSING
        } catch (e: FileNotFoundException) {
            ConfigurationDirResult.FileStatus.MISSING
        }
    }

    /**
     * Reads the "Console type" byte (GBATEK, DS Firmware Header, offset 01Dh) from an
     * already-open firmware.bin descriptor. Returns null if the byte can't be read (e.g. a
     * truncated stream); callers must then treat the console type as unknown rather than
     * guessing.
     */
    private fun getFirmwareConsoleType(firmware: AssetFileDescriptor): Int? {
        return firmware.createInputStream().use { stream ->
            var skipped = 0L
            while (skipped < FirmwareValidation.CONSOLE_TYPE_OFFSET) {
                val n = stream.skip(FirmwareValidation.CONSOLE_TYPE_OFFSET - skipped)
                if (n <= 0) return null
                skipped += n
            }
            val consoleType = stream.read()
            if (consoleType == -1) null else consoleType
        }
    }

    private fun getRequiredFilesVerifiers(consoleType: ConsoleType): Map<String, (DocumentFile) -> ConfigurationDirResult.FileStatus> {
        return when(consoleType) {
            ConsoleType.DS -> mapOf(
                    "bios7.bin" to this::getDSBios7Status,
                    "bios9.bin" to this::getDSBios9Status,
                    "firmware.bin" to this::getDSFirmwareStatus
            )
            ConsoleType.DSi -> mapOf(
                    "bios7.bin" to this::getDSiBios7Status,
                    "bios9.bin" to this::getDSiBios9Status,
                    "firmware.bin" to this::getDSiFirmwareStatus,
                    "nand.bin" to this::getDSiNandStatus
            )
        }
    }
}