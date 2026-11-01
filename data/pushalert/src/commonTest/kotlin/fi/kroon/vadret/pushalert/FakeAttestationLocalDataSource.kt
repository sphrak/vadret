package fi.kroon.vadret.pushalert

import fi.kroon.vadret.pushalert.error.AttestationException
import fi.kroon.vadret.pushalert.local.AttestationLocalDataSource
import fi.kroon.vadret.pushalert.model.Attestation

internal class FakeAttestationLocalDataSource(
    private val attestation: Attestation?
) : AttestationLocalDataSource {
    val challenges = mutableListOf<List<Byte>>()

    override suspend fun attest(challenge: ByteArray): Attestation {
        challenges += challenge.toList()
        return attestation ?: throw AttestationException(message = "not genuine")
    }
}