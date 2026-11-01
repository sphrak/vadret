package se.vadretapp.vadret.testing

import se.vadretapp.vadret.common.DiskCache
import kotlin.concurrent.atomics.AtomicReference
import kotlin.concurrent.atomics.update

class FakeDiskCache : DiskCache {

    private val storedEntries: AtomicReference<Map<String, String>> = AtomicReference(value = emptyMap())

    val entries: Map<String, String>
        get() = storedEntries.load()

    override fun read(key: String): String? = storedEntries.load()[key]

    override fun write(
        key: String,
        value: String
    ) {
        storedEntries.update { stored: Map<String, String> -> stored + (key to value) }
    }

    override fun remove(key: String) {
        storedEntries.update { stored: Map<String, String> -> stored - key }
    }
}