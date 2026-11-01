package se.vadretapp.vadret.pushalert.model

internal class IosAttestation(
    val keyId: String,
    val attestationObject: ByteArray
) : Attestation