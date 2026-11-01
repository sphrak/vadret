package fi.kroon.vadret.pushalert.local

import fi.kroon.vadret.pushalert.model.Attestation
import kotlinx.coroutines.CoroutineDispatcher

internal interface AttestationLocalDataSource {
    suspend fun attest(challenge: ByteArray): Attestation
}

internal expect fun AttestationLocalDataSource(ioDispatcher: CoroutineDispatcher): AttestationLocalDataSource