package se.vadretapp.vadret.pushalert.local

import kotlinx.coroutines.CoroutineDispatcher
import se.vadretapp.vadret.pushalert.model.Attestation

internal interface AttestationLocalDataSource {
    suspend fun attest(challenge: ByteArray): Attestation
}

internal expect fun AttestationLocalDataSource(ioDispatcher: CoroutineDispatcher): AttestationLocalDataSource