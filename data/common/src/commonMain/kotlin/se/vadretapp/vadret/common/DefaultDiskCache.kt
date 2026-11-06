package se.vadretapp.vadret.common

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import okio.FileSystem
import okio.IOException
import okio.Path
import okio.Path.Companion.toPath
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

private const val TEMPORARY_FILE_SUFFIX = ".tmp"
private const val UNCHANGED_VERSION = 0L

internal class DefaultDiskCache(
    directoryPath: String
) : DiskCache {

    private val fileSystem: FileSystem = FileSystem.SYSTEM
    private val directory: Path = directoryPath.toPath()
    private val versions: MutableStateFlow<Map<String, Long>> = MutableStateFlow(value = emptyMap())

    override fun read(key: String): String? {
        val file: Path = file(key = key)
        return if (fileSystem.exists(path = file)) {
            fileSystem.read(file = file) { readUtf8() }
        } else {
            null
        }
    }

    override fun observe(key: String): Flow<String?> =
        versions
            .map { keyVersions: Map<String, Long> -> keyVersions[key] ?: UNCHANGED_VERSION }
            .distinctUntilChanged()
            .map { read(key = key) }

    override fun write(
        key: String,
        value: String
    ) {
        fileSystem.createDirectories(dir = directory)
        val temporaryFile: Path = temporaryFile(key = key)
        fileSystem.write(file = temporaryFile) { writeUtf8(string = value) }
        try {
            fileSystem.atomicMove(
                source = temporaryFile,
                target = file(key = key)
            )
        } catch (exception: IOException) {
            fileSystem.delete(path = temporaryFile)
            throw IOException("Could not write cache entry $key", exception)
        }
        changed(key = key)
    }

    override fun remove(key: String) {
        fileSystem.delete(path = file(key = key))
        changed(key = key)
    }

    private fun changed(key: String) {
        versions.update { keyVersions: Map<String, Long> ->
            val nextVersion: Long = (keyVersions[key] ?: UNCHANGED_VERSION) + 1
            keyVersions + (key to nextVersion)
        }
    }

    private fun file(key: String): Path = directory / key

    @OptIn(ExperimentalUuidApi::class)
    private fun temporaryFile(key: String): Path = directory / "$key.${Uuid.random()}$TEMPORARY_FILE_SUFFIX"
}