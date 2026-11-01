package se.vadretapp.vadret.pushalert.local

import kotlinx.coroutines.CoroutineDispatcher

internal actual fun AttestationLocalDataSource(ioDispatcher: CoroutineDispatcher): AttestationLocalDataSource = IosAttestationLocalDataSource()