package fi.kroon.vadret.encryption

import fi.kroon.vadret.encryption.model.KeyAlias

fun SealedMessageKey(keyAlias: KeyAlias): SealedMessageKey =
    SecureEnclaveSealedMessageKey(
        keyAlias = keyAlias,
        privateKeySource = KeychainPrivateKeySource(keyAlias = keyAlias)
    )