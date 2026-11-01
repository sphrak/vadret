package fi.kroon.vadret.pushalert.extension

import fi.kroon.vadret.pushalert.model.AndroidAttestation
import fi.kroon.vadret.pushalert.model.AndroidAttestationDto
import fi.kroon.vadret.pushalert.model.Attestation
import fi.kroon.vadret.pushalert.model.InstallationRequestDto
import fi.kroon.vadret.pushalert.model.IosAttestation
import fi.kroon.vadret.pushalert.model.IosAttestationDto
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