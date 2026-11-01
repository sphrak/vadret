package se.vadretapp.vadret.pushalert

import se.vadretapp.vadret.pushalert.error.AttestationException
import se.vadretapp.vadret.pushalert.local.AttestationLocalDataSource
import se.vadretapp.vadret.pushalert.model.Attestation

internal class FakeAttestationLocalDataSource(
    private val attestation: Attestation?
) : AttestationLocalDataSource {
    val challenges = mutableListOf<List<Byte>>()

    override suspend fun attest(challenge: ByteArray): Attestation {
        challenges += challenge.toList()
        return attestation ?: throw AttestationException(message = "not genuine")
    }
}