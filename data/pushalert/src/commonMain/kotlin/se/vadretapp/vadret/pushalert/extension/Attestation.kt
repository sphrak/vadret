package se.vadretapp.vadret.pushalert.extension

import se.vadretapp.vadret.pushalert.model.AndroidAttestation
import se.vadretapp.vadret.pushalert.model.AndroidAttestationDto
import se.vadretapp.vadret.pushalert.model.Attestation
import se.vadretapp.vadret.pushalert.model.InstallationRequestDto
import se.vadretapp.vadret.pushalert.model.IosAttestation
import se.vadretapp.vadret.pushalert.model.IosAttestationDto
import kotlin.io.encoding.Base64

internal fun Attestation.toInstallationRequestDto(challenge: String): InstallationRequestDto =
    when (this) {
        is AndroidAttestation -> InstallationRequestDto(
            challenge = challenge,
            android = AndroidAttestationDto(certificateChain = certificateChain.map { certificate -> Base64.encode(source = certificate) })
        )
        is IosAttestation -> InstallationRequestDto(
            challenge = challenge,
            ios = IosAttestationDto(
                keyId = keyId,
                attestation = Base64.encode(source = attestationObject)
            )
        )
    }