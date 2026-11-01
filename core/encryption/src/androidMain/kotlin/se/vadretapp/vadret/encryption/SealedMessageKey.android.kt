package se.vadretapp.vadret.encryption

import java.io.File

fun SealedMessageKey(
    keyFile: File,
    deviceCipher: DeviceCipher
): SealedMessageKey = KeyFileSealedMessageKey(
    keyFile = keyFile,
    deviceCipher = deviceCipher
)