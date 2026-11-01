package se.vadretapp.vadret.encryption

import se.vadretapp.vadret.encryption.model.KeyAlias

fun SealedMessageKey(keyAlias: KeyAlias): SealedMessageKey =
    SecureEnclaveSealedMessageKey(
        keyAlias = keyAlias,
        privateKeySource = KeychainPrivateKeySource(keyAlias = keyAlias)
    )