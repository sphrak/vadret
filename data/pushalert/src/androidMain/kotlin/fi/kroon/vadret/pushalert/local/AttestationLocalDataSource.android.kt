package fi.kroon.vadret.pushalert.local

import kotlinx.coroutines.CoroutineDispatcher

internal actual fun AttestationLocalDataSource(ioDispatcher: CoroutineDispatcher): AttestationLocalDataSource =
    AndroidAttestationLocalDataSource(ioDispatcher = ioDispatcher)