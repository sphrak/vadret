package fi.kroon.vadret.encryption

import fi.kroon.vadret.encryption.model.KeyAlias

actual fun DeviceCipher(keyAlias: KeyAlias): DeviceCipher =
    SecureEnclaveDeviceCipher(
        keyAlias = keyAlias,
        privateKeySource = KeychainPrivateKeySource(keyAlias = keyAlias)
    )