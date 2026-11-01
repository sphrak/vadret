package se.vadretapp.vadret.encryption

import com.google.crypto.tink.Aead
import com.google.crypto.tink.integration.android.AndroidKeystore
import se.vadretapp.vadret.encryption.model.KeyAlias

actual fun DeviceCipher(keyAlias: KeyAlias): DeviceCipher =
    AeadDeviceCipher(
        keyAlias = keyAlias,
        produceAead = { keystoreAead(keyAlias = keyAlias) }
    )

private fun keystoreAead(keyAlias: KeyAlias): Aead {
    if (!AndroidKeystore.hasKey(keyAlias.value)) {
        AndroidKeystore.generateNewAes256GcmKey(keyAlias.value)
    }
    return AndroidKeystore.getAead(keyAlias.value)
}