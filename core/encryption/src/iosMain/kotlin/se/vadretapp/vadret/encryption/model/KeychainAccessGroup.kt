package se.vadretapp.vadret.encryption.model

import kotlin.jvm.JvmInline

// A keychain access group the app shares with its extensions, such as its App Group.
@JvmInline
value class KeychainAccessGroup(val value: String) {
    init {
        require(value = value.isNotBlank()) { "A keychain access group cannot be blank" }
    }
}