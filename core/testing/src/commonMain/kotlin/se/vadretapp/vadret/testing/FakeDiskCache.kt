package se.vadretapp.vadret.testing

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import se.vadretapp.vadret.common.DiskCache

class FakeDiskCache : DiskCache {

    private val storedEntries: MutableStateFlow<Map<String, String>> = MutableStateFlow(value = emptyMap())

    val entries: Map<String, String>
        get() = storedEntries.value

    override fun read(key: String): String? = storedEntries.value[key]

    override fun observe(key: String): Flow<String?> =
        storedEntries
            .map { stored: Map<String, String> -> stored[key] }
            .distinctUntilChanged()

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