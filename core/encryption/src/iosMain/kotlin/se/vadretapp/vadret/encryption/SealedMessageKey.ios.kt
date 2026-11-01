package se.vadretapp.vadret.encryption

import se.vadretapp.vadret.encryption.model.KeyAlias
import se.vadretapp.vadret.encryption.model.KeychainAccessGroup

fun SealedMessageKey(
    keyAlias: KeyAlias,
    keychainAccessGroup: KeychainAccessGroup
): SealedMessageKey =
    SecureEnclaveSealedMessageKey(
        keyAlias = keyAlias,
        privateKeySource = KeychainPrivateKeySource(
            keyAlias = keyAlias,
            keychainAccessGroup = keychainAccessGroup
        )
    )