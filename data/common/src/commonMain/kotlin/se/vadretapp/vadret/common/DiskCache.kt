package se.vadretapp.vadret.common

interface DiskCache {
    fun read(key: String): String?
    fun write(
        key: String,
        value: String
    )
    fun remove(key: String)
}

fun DiskCache(directoryPath: String): DiskCache = DefaultDiskCache(directoryPath = directoryPath)