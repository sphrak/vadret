package se.vadretapp.vadret.common

import kotlinx.coroutines.flow.Flow

interface DiskCache {
    fun read(key: String): String?
    fun observe(key: String): Flow<String?>
    fun write(
        key: String,
        value: String
    )
    fun remove(key: String)
}

fun DiskCache(directoryPath: String): DiskCache = DefaultDiskCache(directoryPath = directoryPath)