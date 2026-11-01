package se.vadretapp.vadret.encryption

import se.vadretapp.vadret.encryption.model.KeyAlias

actual fun DeviceCipher(keyAlias: KeyAlias): DeviceCipher =
    SecureEnclaveDeviceCipher(
        keyAlias = keyAlias,
        privateKeySource = KeychainPrivateKeySource(
            keyAlias = keyAlias,
            keychainAccessGroup = null
        )
    )