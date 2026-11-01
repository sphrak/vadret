package se.vadretapp.vadret.pushalert.local

import kotlinx.coroutines.suspendCancellableCoroutine
import platform.DeviceCheck.DCAppAttestService
import platform.Foundation.NSData
import platform.Foundation.NSError
import se.vadretapp.vadret.pushalert.error.AttestationException
import se.vadretapp.vadret.pushalert.extension.sha256
import se.vadretapp.vadret.pushalert.extension.toByteArray
import se.vadretapp.vadret.pushalert.extension.toNSData
import se.vadretapp.vadret.pushalert.model.Attestation
import se.vadretapp.vadret.pushalert.model.IosAttestation
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

internal class IosAttestationLocalDataSource : AttestationLocalDataSource {
    override suspend fun attest(challenge: ByteArray): Attestation {
        val appAttestService = DCAppAttestService.sharedService
        if (!appAttestService.supported) {
            throw AttestationException(message = "App Attest is not supported on this device")
        }
        val keyId = generateKey(appAttestService = appAttestService)
        return IosAttestation(
            keyId = keyId,
            attestationObject = attestKey(
                appAttestService = appAttestService,
                keyId = keyId,
                clientDataHash = challenge.sha256()
            )
        )
    }

    private suspend fun generateKey(appAttestService: DCAppAttestService): String =
        suspendCancellableCoroutine { continuation ->
            appAttestService.generateKeyWithCompletionHandler { keyId: String?, error: NSError? ->
                if (keyId == null) {
                    continuation.resumeWithException(exception = AttestationException(message = "App Attest made no key: ${error?.localizedDescription}"))
                } else {
                    continuation.resume(value = keyId)
                }
            }
        }

    private suspend fun attestKey(
        appAttestService: DCAppAttestService,
        keyId: String,
        clientDataHash: ByteArray
    ): ByteArray =
        suspendCancellableCoroutine { continuation ->
            appAttestService.attestKey(
                keyId = keyId,
                clientDataHash = clientDataHash.toNSData()
            ) { attestationObject: NSData?, error: NSError? ->
                if (attestationObject == null) {
                    continuation.resumeWithException(exception = AttestationException(message = "App Attest could not attest the key: ${error?.localizedDescription}"))
                } else {
                    continuation.resume(value = attestationObject.toByteArray())
                }
            }
        }
}