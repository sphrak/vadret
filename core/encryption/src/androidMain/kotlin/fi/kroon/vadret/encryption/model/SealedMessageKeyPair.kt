package fi.kroon.vadret.encryption.model

import java.security.PrivateKey

internal class SealedMessageKeyPair(
    val publicPoint: ByteArray,
    val privateKey: PrivateKey
)