package se.vadretapp.vadret.testing

import okio.FileSystem
import okio.Path
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
fun temporaryDirectory(prefix: String): Path {
    val directory: Path = FileSystem.SYSTEM_TEMPORARY_DIRECTORY / "$prefix-${Uuid.random()}"
    FileSystem.SYSTEM.createDirectories(dir = directory)
    return directory
}

fun Path.deleteRecursively() {
    FileSystem.SYSTEM.deleteRecursively(fileOrDirectory = this)
}